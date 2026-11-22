package com.kanso.core.backup.drive

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import com.kanso.core.backup.BackupDecryptionException
import com.kanso.core.backup.BackupManager
import com.kanso.core.backup.EncryptedBackupCrypto
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.model.KansoBackupSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DriveSyncState {
    data object Idle : DriveSyncState
    data object Authorizing : DriveSyncState
    data class Syncing(val label: String) : DriveSyncState
    data class NeedsPassphrase(val pendingForceMode: DriveSyncForceMode) : DriveSyncState
    data class NeedsConsent(
        val pendingIntent: PendingIntent,
        val pendingForceMode: DriveSyncForceMode
    ) : DriveSyncState
    data class Synced(val message: String, val syncedAtMillis: Long) : DriveSyncState
    data class Conflict(val remoteFile: DriveRemoteFile, val localItemCount: Int) : DriveSyncState
    data class Error(
        val message: String,
        val isPassphraseError: Boolean = false,
        val isConfigError: Boolean = false
    ) : DriveSyncState
}

@Singleton
class GoogleDriveSyncManager @Inject constructor(
    private val backupManager: BackupManager,
    private val preferencesRepository: PreferencesRepository,
    private val authHelper: GoogleDriveAuthHelper,
    private val driveClient: GoogleDriveRestClient
) {
    private val syncMutex = Mutex()
    private val _state = MutableStateFlow<DriveSyncState>(DriveSyncState.Idle)
    val state: StateFlow<DriveSyncState> = _state.asStateFlow()

    @Volatile
    private var pendingForceMode: DriveSyncForceMode = DriveSyncForceMode.SMART

    @Volatile
    private var lastAutoSyncAttemptMillis: Long = 0L

    fun clearStatus() {
        val current = _state.value
        if (current is DriveSyncState.Synced ||
            current is DriveSyncState.Error ||
            current is DriveSyncState.NeedsPassphrase
        ) {
            _state.value = DriveSyncState.Idle
        }
    }

    fun consumeConsentPrompt() {
        if (_state.value is DriveSyncState.NeedsConsent) {
            _state.value = DriveSyncState.Authorizing
        }
    }

    suspend fun startSync(
        forceMode: DriveSyncForceMode = DriveSyncForceMode.SMART,
        interactive: Boolean = true
    ): Boolean {
        if (!syncMutex.tryLock()) {
            return false
        }
        return try {
            runSyncLocked(forceMode = forceMode, interactive = interactive)
        } finally {
            syncMutex.unlock()
        }
    }

    suspend fun syncSilentlyIfReady(ignoreThrottle: Boolean = false): Boolean {
        val config = preferencesRepository.getDriveSyncConfig()
        if (!config.isConnected || !config.autoSyncEnabled || !config.hasPassphrase) {
            return false
        }
        val now = System.currentTimeMillis()
        if (!ignoreThrottle && now - lastAutoSyncAttemptMillis < AUTO_SYNC_THROTTLE_MS) {
            return false
        }
        lastAutoSyncAttemptMillis = now
        return startSync(forceMode = DriveSyncForceMode.SMART, interactive = false)
    }

    suspend fun saveSyncPassphrase(
        passphrase: String,
        resumePendingSync: Boolean
    ): Result<Unit> {
        if (passphrase.length < EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH) {
            return Result.failure(
                IllegalArgumentException(
                    "Sync passphrase must be at least ${EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH} characters."
                )
            )
        }
        preferencesRepository.setDriveSyncPassphrase(passphrase)
        if (_state.value is DriveSyncState.NeedsPassphrase ||
            (_state.value as? DriveSyncState.Error)?.isPassphraseError == true
        ) {
            _state.value = DriveSyncState.Idle
        }
        if (resumePendingSync) {
            startSync(forceMode = pendingForceMode, interactive = true)
        }
        return Result.success(Unit)
    }

    suspend fun onConsentActivityResult(resultOk: Boolean, data: Intent?) {
        if (!resultOk) {
            _state.value = DriveSyncState.Error("Google Drive authorization was cancelled.")
            return
        }
        if (!syncMutex.tryLock()) return
        try {
            val passphrase = preferencesRepository.getDriveSyncPassphrase()
            if (passphrase.isNullOrBlank()) {
                _state.value = DriveSyncState.NeedsPassphrase(pendingForceMode)
                return
            }
            when (val auth = authHelper.parseConsentIntent(data)) {
                is DriveAuthResult.Authorized -> {
                    performSyncWithToken(
                        accessToken = auth.accessToken,
                        passphrase = passphrase,
                        forceMode = pendingForceMode,
                        interactive = true
                    )
                }

                is DriveAuthResult.NeedsUserConsent -> {
                    _state.value = DriveSyncState.NeedsConsent(auth.pendingIntent, pendingForceMode)
                }

                is DriveAuthResult.Failed -> {
                    _state.value = DriveSyncState.Error(
                        message = auth.message,
                        isConfigError = auth.isConfigurationError
                    )
                }
            }
        } finally {
            syncMutex.unlock()
        }
    }

    suspend fun resolveConflict(keepCloud: Boolean) {
        val mode = if (keepCloud) {
            DriveSyncForceMode.FORCE_DOWNLOAD
        } else {
            DriveSyncForceMode.FORCE_UPLOAD
        }
        startSync(forceMode = mode, interactive = true)
    }

    suspend fun disconnect() {
        syncMutex.lock()
        try {
            preferencesRepository.clearDriveSyncState()
            pendingForceMode = DriveSyncForceMode.SMART
            _state.value = DriveSyncState.Idle
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun runSyncLocked(
        forceMode: DriveSyncForceMode,
        interactive: Boolean
    ): Boolean {
        val passphrase = preferencesRepository.getDriveSyncPassphrase()
        if (passphrase.isNullOrBlank()) {
            if (interactive) {
                pendingForceMode = forceMode
                _state.value = DriveSyncState.NeedsPassphrase(forceMode)
            }
            return false
        }

        if (interactive) {
            _state.value = DriveSyncState.Authorizing
        }

        return when (val auth = authHelper.requestDriveAuthorization()) {
            is DriveAuthResult.NeedsUserConsent -> {
                if (interactive) {
                    pendingForceMode = forceMode
                    _state.value = DriveSyncState.NeedsConsent(auth.pendingIntent, forceMode)
                }
                false
            }

            is DriveAuthResult.Failed -> {
                if (interactive) {
                    _state.value = DriveSyncState.Error(
                        message = auth.message,
                        isConfigError = auth.isConfigurationError
                    )
                }
                false
            }

            is DriveAuthResult.Authorized -> {
                performSyncWithToken(
                    accessToken = auth.accessToken,
                    passphrase = passphrase,
                    forceMode = forceMode,
                    interactive = interactive
                )
            }
        }
    }

    private suspend fun performSyncWithToken(
        accessToken: String,
        passphrase: String,
        forceMode: DriveSyncForceMode,
        interactive: Boolean
    ): Boolean {
        return try {
            if (interactive) {
                _state.value = DriveSyncState.Syncing("Checking Google Drive status...")
            }

            val config = preferencesRepository.getDriveSyncConfig()
            val userInfo = if (config.accountEmail.isNullOrBlank()) {
                driveClient.fetchUserInfo(accessToken)
            } else {
                null
            }
            val resolvedEmail = userInfo?.emailAddress ?: config.accountEmail
            if (!config.isConnected || !userInfo?.emailAddress.isNullOrBlank()) {
                preferencesRepository.updateDriveSyncConnected(resolvedEmail)
            }

            val localState = backupManager.inspectLocalState()
            val remoteFile = driveClient.findSyncFile(accessToken)

            val decision = DriveSyncPlanner.decide(
                localContentHash = localState.contentHash,
                localItemCount = localState.itemCount,
                lastSyncedContentHash = config.lastSyncedContentHash,
                lastSyncedRemoteModifiedMillis = config.lastRemoteModifiedMillis,
                remoteFile = remoteFile,
                forceMode = forceMode
            )

            executeDecision(
                decision = decision,
                accessToken = accessToken,
                passphrase = passphrase,
                localContentHash = localState.contentHash,
                resolvedEmail = resolvedEmail,
                interactive = interactive
            )
        } catch (error: Exception) {
            val isPassphraseFailure = error is BackupDecryptionException ||
                error is GeneralSecurityException ||
                error.cause is GeneralSecurityException ||
                error.message?.contains("passphrase", ignoreCase = true) == true
            val isUnauthorized = error is DriveUnauthorizedException

            if (interactive || isPassphraseFailure) {
                val message = when {
                    isPassphraseFailure ->
                        "Could not decrypt your Google Drive backup. Verify your Sync Passphrase matches your other device."
                    isUnauthorized ->
                        error.message ?: "Google Drive session expired. Tap Sync Now to re-authorize."
                    else ->
                        error.message ?: "Google Drive sync failed."
                }
                _state.value = DriveSyncState.Error(
                    message = message,
                    isPassphraseError = isPassphraseFailure
                )
            }
            false
        }
    }

    private suspend fun executeDecision(
        decision: DriveSyncDecision,
        accessToken: String,
        passphrase: String,
        localContentHash: String,
        resolvedEmail: String?,
        interactive: Boolean
    ): Boolean {
        val deviceModel = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android"
        return when (decision) {
            is DriveSyncDecision.UploadInitial -> {
                if (interactive) {
                    _state.value = DriveSyncState.Syncing("Encrypting and uploading to Google Drive...")
                }
                val prepared = backupManager.createEncryptedSnapshot(
                    passphrase = passphrase,
                    includeBackupTimestamp = false
                ).getOrThrow()
                val uploaded = driveClient.createSyncFile(
                    accessToken = accessToken,
                    payload = prepared.encryptedBytes,
                    contentHash = prepared.contentHash,
                    itemCount = prepared.itemCount,
                    schemaVersion = KansoBackupSnapshot.CURRENT_SCHEMA_VERSION,
                    deviceModel = deviceModel
                )
                val now = System.currentTimeMillis()
                preferencesRepository.updateDriveSyncMetadata(
                    fileId = uploaded.id,
                    syncedAtMillis = now,
                    remoteModifiedMillis = uploaded.modifiedTimeMillis,
                    contentHash = prepared.contentHash,
                    accountEmail = resolvedEmail
                )
                _state.value = DriveSyncState.Synced(
                    message = "Uploaded ${prepared.itemCount} encrypted items to Google Drive.",
                    syncedAtMillis = now
                )
                true
            }

            is DriveSyncDecision.PushLocal -> {
                if (interactive) {
                    _state.value = DriveSyncState.Syncing("Uploading changes to Google Drive...")
                }
                val prepared = backupManager.createEncryptedSnapshot(
                    passphrase = passphrase,
                    includeBackupTimestamp = false
                ).getOrThrow()
                val uploaded = try {
                    driveClient.updateSyncFile(
                        accessToken = accessToken,
                        fileId = decision.remoteFileId,
                        payload = prepared.encryptedBytes,
                        contentHash = prepared.contentHash,
                        itemCount = prepared.itemCount,
                        schemaVersion = KansoBackupSnapshot.CURRENT_SCHEMA_VERSION,
                        deviceModel = deviceModel
                    )
                } catch (_: DriveNotFoundException) {
                    driveClient.createSyncFile(
                        accessToken = accessToken,
                        payload = prepared.encryptedBytes,
                        contentHash = prepared.contentHash,
                        itemCount = prepared.itemCount,
                        schemaVersion = KansoBackupSnapshot.CURRENT_SCHEMA_VERSION,
                        deviceModel = deviceModel
                    )
                }
                val now = System.currentTimeMillis()
                preferencesRepository.updateDriveSyncMetadata(
                    fileId = uploaded.id,
                    syncedAtMillis = now,
                    remoteModifiedMillis = uploaded.modifiedTimeMillis,
                    contentHash = prepared.contentHash,
                    accountEmail = resolvedEmail
                )
                _state.value = DriveSyncState.Synced(
                    message = "Synced ${prepared.itemCount} encrypted items to Google Drive.",
                    syncedAtMillis = now
                )
                true
            }

            is DriveSyncDecision.PullRemote -> {
                if (interactive) {
                    _state.value = DriveSyncState.Syncing("Downloading and decrypting from Google Drive...")
                }
                val payload = driveClient.downloadFile(accessToken, decision.remoteFile.id)
                val restored = backupManager.restoreFromEncryptedBytes(payload, passphrase).getOrThrow()
                val now = System.currentTimeMillis()
                preferencesRepository.updateDriveSyncMetadata(
                    fileId = decision.remoteFile.id,
                    syncedAtMillis = now,
                    remoteModifiedMillis = decision.remoteFile.modifiedTimeMillis,
                    contentHash = restored.contentHash,
                    accountEmail = resolvedEmail
                )
                _state.value = DriveSyncState.Synced(
                    message = "Restored ${restored.itemCount} items from Google Drive.",
                    syncedAtMillis = now
                )
                true
            }

            is DriveSyncDecision.UpToDate -> {
                val now = System.currentTimeMillis()
                preferencesRepository.updateDriveSyncMetadata(
                    fileId = decision.remoteFile.id,
                    syncedAtMillis = now,
                    remoteModifiedMillis = decision.remoteFile.modifiedTimeMillis,
                    contentHash = localContentHash,
                    accountEmail = resolvedEmail
                )
                if (interactive) {
                    _state.value = DriveSyncState.Synced(
                        message = "Already up to date with Google Drive.",
                        syncedAtMillis = now
                    )
                }
                true
            }

            is DriveSyncDecision.Conflict -> {
                _state.value = DriveSyncState.Conflict(
                    remoteFile = decision.remoteFile,
                    localItemCount = decision.localItemCount
                )
                false
            }

            DriveSyncDecision.NoRemoteBackup -> {
                _state.value = DriveSyncState.Error(
                    message = "No cloud backup found in Google Drive yet. Upload from this device first."
                )
                false
            }
        }
    }

    companion object {
        private const val AUTO_SYNC_THROTTLE_MS = 2 * 60 * 1000L
    }
}
