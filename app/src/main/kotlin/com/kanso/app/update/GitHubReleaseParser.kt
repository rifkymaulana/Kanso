package com.kanso.app.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubAssetDto(
    val id: Long = 0L,
    val name: String,
    val size: Long = 0L,
    val url: String = "",
    val browser_download_url: String = ""
)

@Serializable
data class GitHubReleaseDto(
    val tag_name: String,
    val name: String = "",
    val body: String = "",
    val html_url: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList()
)

data class AppReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseName: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkAssetName: String,
    val apkApiUrl: String,
    val apkBrowserUrl: String,
    val apkSizeBytes: Long,
    val sha256ApiUrl: String?,
    val sha256BrowserUrl: String?
)

object GitHubReleaseParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseLatestRelease(jsonText: String): AppReleaseInfo? {
        val dto = runCatching { json.decodeFromString<GitHubReleaseDto>(jsonText) }.getOrNull()
            ?: return null
        if (dto.draft) return null

        val apkAsset = dto.assets.firstOrNull {
            it.name.endsWith("-release.apk", ignoreCase = true)
        } ?: dto.assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true)
        } ?: return null

        val shaAsset = dto.assets.firstOrNull {
            it.name.equals("${apkAsset.name}.sha256", ignoreCase = true)
        } ?: dto.assets.firstOrNull {
            it.name.endsWith(".sha256", ignoreCase = true)
        }

        return AppReleaseInfo(
            tagName = dto.tag_name,
            versionName = SemVerComparator.normalizeVersion(dto.tag_name),
            releaseName = dto.name.ifBlank { dto.tag_name },
            releaseNotes = dto.body.trim(),
            htmlUrl = dto.html_url,
            apkAssetName = apkAsset.name,
            apkApiUrl = apkAsset.url,
            apkBrowserUrl = apkAsset.browser_download_url,
            apkSizeBytes = apkAsset.size,
            sha256ApiUrl = shaAsset?.url?.takeIf { it.isNotBlank() },
            sha256BrowserUrl = shaAsset?.browser_download_url?.takeIf { it.isNotBlank() }
        )
    }

    fun parseExpectedSha256(content: String): String? {
        val candidate = content.trim().split(Regex("\\s+")).firstOrNull()?.lowercase() ?: return null
        if (candidate.length != 64) return null
        return if (candidate.all { it in '0'..'9' || it in 'a'..'f' }) candidate else null
    }
}
