package com.kanso.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.kanso.app.BuildConfig
import com.kanso.core.database.preferences.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data object Checking : AppUpdateState
    data class UpToDate(val currentVersion: String) : AppUpdateState
    data class UpdateAvailable(
        val release: AppReleaseInfo,
        val currentVersion: String
    ) : AppUpdateState

    data class Downloading(
        val release: AppReleaseInfo,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : AppUpdateState

    data class Verifying(val release: AppReleaseInfo) : AppUpdateState
    data class ReadyToInstall(
        val release: AppReleaseInfo,
        val apkFile: File
    ) : AppUpdateState

    data class Error(
        val message: String,
        val needsToken: Boolean = false
    ) : AppUpdateState
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    private val _dismissedTag = MutableStateFlow<String?>(null)
    val dismissedTag: StateFlow<String?> = _dismissedTag.asStateFlow()

    suspend fun checkForUpdates(isAutomatic: Boolean = false) = withContext(Dispatchers.IO) {
        val current = _state.value
        if (current is AppUpdateState.Checking ||
            current is AppUpdateState.Downloading ||
            current is AppUpdateState.Verifying
        ) {
            return@withContext
        }

        val prefs = preferencesRepository.getCurrentPreferences()
        val token = preferencesRepository.getGithubToken()
        val now = System.currentTimeMillis()

        if (isAutomatic) {
            if (!prefs.autoCheckUpdates || token.isNullOrBlank()) return@withContext
            val lastCheck = prefs.lastUpdateCheckMillis
            if (lastCheck != null && (now - lastCheck) < AUTO_CHECK_INTERVAL_MILLIS) {
                return@withContext
            }
            if (current is AppUpdateState.ReadyToInstall) return@withContext
        } else {
            _state.value = AppUpdateState.Checking
        }

        runCatching {
            val conn = openGitHubConnection(
                urlString = LATEST_RELEASE_API_URL,
                token = token,
                acceptHeader = "application/vnd.github+json"
            )
            try {
                val code = conn.responseCode
                preferencesRepository.setLastUpdateCheckMillis(now)

                when (code) {
                    in 200..299 -> {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val release = GitHubReleaseParser.parseLatestRelease(body)
                        if (release == null) {
                            if (!isAutomatic) {
                                _state.value = AppUpdateState.Error(
                                    "No installable APK found in the latest GitHub release."
                                )
                            }
                            return@withContext
                        }

                        if (SemVerComparator.isNewer(release.versionName, BuildConfig.VERSION_NAME)) {
                            _dismissedTag.value = null
                            _state.value = AppUpdateState.UpdateAvailable(
                                release = release,
                                currentVersion = BuildConfig.VERSION_NAME
                            )
                        } else if (!isAutomatic) {
                            _state.value = AppUpdateState.UpToDate(BuildConfig.VERSION_NAME)
                        }
                    }

                    401, 403, 404 -> {
                        if (!isAutomatic) {
                            _state.value = AppUpdateState.Error(
                                message = "Cannot access $REPO_OWNER/$REPO_NAME releases (HTTP $code). Add a read-only GitHub token below for private repo updates.",
                                needsToken = true
                            )
                        }
                    }

                    else -> {
                        if (!isAutomatic) {
                            _state.value = AppUpdateState.Error(
                                message = "GitHub update check failed (HTTP $code)."
                            )
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
        }.onFailure { error ->
            if (!isAutomatic) {
                _state.value = AppUpdateState.Error(
                    message = error.message ?: "Unable to reach GitHub. Check your internet connection."
                )
            }
        }
    }

    suspend fun downloadAndVerify(release: AppReleaseInfo) = withContext(Dispatchers.IO) {
        val current = _state.value
        if (current is AppUpdateState.Downloading || current is AppUpdateState.Verifying) {
            return@withContext
        }

        val token = preferencesRepository.getGithubToken()
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        updatesDir.listFiles()?.forEach { it.delete() }

        val partFile = File(updatesDir, "${release.apkAssetName}.part")
        val finalFile = File(updatesDir, release.apkAssetName)

        runCatching {
            val useApi = !token.isNullOrBlank() && release.apkApiUrl.isNotBlank()
            val targetUrl = if (useApi) release.apkApiUrl else release.apkBrowserUrl
            val accept = if (useApi) "application/octet-stream" else "*/*"

            _state.value = AppUpdateState.Downloading(
                release = release,
                downloadedBytes = 0L,
                totalBytes = release.apkSizeBytes
            )

            val conn = openRedirectSafeConnection(
                initialUrl = targetUrl,
                token = token,
                acceptHeader = accept
            )

            val actualSha256 = try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("APK download failed (HTTP $code).")
                }
                val contentLength = conn.contentLengthLong.takeIf { it > 0L } ?: release.apkSizeBytes
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(16 * 1024)
                var totalRead = 0L
                var lastProgressAt = 0L

                conn.inputStream.use { input ->
                    FileOutputStream(partFile).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            totalRead += read

                            val now = System.currentTimeMillis()
                            if (now - lastProgressAt >= 150L) {
                                lastProgressAt = now
                                _state.value = AppUpdateState.Downloading(
                                    release = release,
                                    downloadedBytes = totalRead,
                                    totalBytes = maxOf(contentLength, totalRead)
                                )
                            }
                        }
                        output.flush()
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } finally {
                conn.disconnect()
            }

            _state.value = AppUpdateState.Verifying(release)

            val shaTargetUrl = when {
                !token.isNullOrBlank() && !release.sha256ApiUrl.isNullOrBlank() -> release.sha256ApiUrl
                !release.sha256BrowserUrl.isNullOrBlank() -> release.sha256BrowserUrl
                else -> null
            }

            if (shaTargetUrl != null) {
                val shaAccept = if (!token.isNullOrBlank() && shaTargetUrl == release.sha256ApiUrl) {
                    "application/octet-stream"
                } else {
                    "*/*"
                }
                val shaConn = openRedirectSafeConnection(
                    initialUrl = shaTargetUrl,
                    token = token,
                    acceptHeader = shaAccept
                )
                try {
                    if (shaConn.responseCode in 200..299) {
                        val shaText = shaConn.inputStream.bufferedReader().use { it.readText() }
                        val expectedSha = GitHubReleaseParser.parseExpectedSha256(shaText)
                        if (expectedSha != null && !expectedSha.equals(actualSha256, ignoreCase = true)) {
                            partFile.delete()
                            throw IllegalStateException("APK SHA-256 verification failed.")
                        }
                    }
                } finally {
                    shaConn.disconnect()
                }
            }

            if (finalFile.exists()) finalFile.delete()
            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }

            _state.value = AppUpdateState.ReadyToInstall(release = release, apkFile = finalFile)
        }.onFailure { error ->
            partFile.delete()
            _state.value = AppUpdateState.Error(
                message = error.message ?: "Download failed."
            )
        }
    }

    fun dismissBanner(tagName: String) {
        _dismissedTag.value = tagName
    }

    fun canInstallPackages(): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    fun createInstallIntent(apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createUnknownSourcesIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createReleasesPageIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_WEB_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun openGitHubConnection(
        urlString: String,
        token: String?,
        acceptHeader: String
    ): HttpURLConnection {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 20_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", acceptHeader)
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Kanso-Android/${BuildConfig.VERSION_NAME}")
            if (!token.isNullOrBlank() && isGitHubHost(urlString)) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
        return conn
    }

    private fun openRedirectSafeConnection(
        initialUrl: String,
        token: String?,
        acceptHeader: String
    ): HttpURLConnection {
        var currentUrl = initialUrl
        for (hop in 0 until 6) {
            val conn = openGitHubConnection(currentUrl, token, acceptHeader)
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_SEE_OTHER ||
                code == 307 ||
                code == 308
            ) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("Redirect without Location header.")
                }
                currentUrl = URI(currentUrl).resolve(location).toString()
            } else {
                return conn
            }
        }
        throw IllegalStateException("Too many HTTP redirects while downloading update.")
    }

    private fun isGitHubHost(urlString: String): Boolean {
        val host = runCatching { URI(urlString).host }.getOrNull().orEmpty()
        return host.equals("api.github.com", ignoreCase = true) ||
            host.equals("github.com", ignoreCase = true)
    }

    companion object {
        const val REPO_OWNER = "rifkymaulana"
        const val REPO_NAME = "Kanso"
        const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        const val RELEASES_WEB_URL = "https://github.com/$REPO_OWNER/$REPO_NAME/releases"
        private const val AUTO_CHECK_INTERVAL_MILLIS = 6L * 60L * 60L * 1000L
    }
}
