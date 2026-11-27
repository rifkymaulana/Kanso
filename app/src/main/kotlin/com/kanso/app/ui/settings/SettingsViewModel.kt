package com.kanso.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.app.notifications.PaymentNotificationHandler
import com.kanso.app.update.AppReleaseInfo
import com.kanso.app.update.AppUpdateManager
import com.kanso.app.update.AppUpdateState
import com.kanso.core.backup.BackupManager
import com.kanso.core.backup.drive.DriveSyncForceMode
import com.kanso.core.backup.drive.DriveSyncState
import com.kanso.core.backup.drive.GoogleDriveSyncManager
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.AppLockTimeout
import com.kanso.core.model.DetectedPaymentAlert
import com.kanso.core.model.DriveSyncConfig
import com.kanso.core.model.PaymentCaptureMode
import com.kanso.core.model.PaymentNotificationLogEntry
import com.kanso.core.model.ThemeMode
import com.kanso.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface BackupOperationStatus {
    data object Idle : BackupOperationStatus
    data class InProgress(val label: String) : BackupOperationStatus
    data class Success(val message: String) : BackupOperationStatus
    data class Error(val message: String) : BackupOperationStatus
}

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val backupStatus: BackupOperationStatus = BackupOperationStatus.Idle,
    val hasGithubToken: Boolean = false,
    val updateState: AppUpdateState = AppUpdateState.Idle,
    val driveSyncConfig: DriveSyncConfig = DriveSyncConfig(),
    val driveSyncState: DriveSyncState = DriveSyncState.Idle,
    val recentPaymentDetections: List<DetectedPaymentAlert> = emptyList(),
    val recentPaymentLogs: List<PaymentNotificationLogEntry> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val backupManager: BackupManager,
    private val appUpdateManager: AppUpdateManager,
    private val googleDriveSyncManager: GoogleDriveSyncManager,
    private val paymentNotificationHandler: PaymentNotificationHandler
) : ViewModel() {

    private val backupStatusFlow = MutableStateFlow<BackupOperationStatus>(BackupOperationStatus.Idle)

    private val baseStateFlow = combine(
        preferencesRepository.preferences,
        backupStatusFlow,
        preferencesRepository.hasGithubToken,
        appUpdateManager.state
    ) { prefs, status, hasToken, updateState ->
        SettingsUiState(
            preferences = prefs,
            backupStatus = status,
            hasGithubToken = hasToken,
            updateState = updateState
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        baseStateFlow,
        preferencesRepository.driveSyncConfig,
        googleDriveSyncManager.state,
        paymentNotificationHandler.recentDetections,
        paymentNotificationHandler.recentLogs
    ) { base, driveConfig, driveState, recentDetections, recentLogs ->
        base.copy(
            driveSyncConfig = driveConfig,
            driveSyncState = driveState,
            recentPaymentDetections = recentDetections,
            recentPaymentLogs = recentLogs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setDisplayName(name: String) {
        viewModelScope.launch { preferencesRepository.setDisplayName(name) }
    }

    fun setCurrency(currency: AppCurrency) {
        viewModelScope.launch { preferencesRepository.setCurrency(currency) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setUseDynamicColor(enabled) }
    }

    fun setOverallMonthlyBudget(amountCents: Long) {
        viewModelScope.launch { preferencesRepository.setOverallMonthlyBudgetCents(amountCents) }
    }

    fun setWaterGoalMl(goalMl: Int) {
        viewModelScope.launch { preferencesRepository.setWaterGoalMl(goalMl) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setRemindersEnabled(enabled) }
    }

    fun setMorningReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setMorningReminderEnabled(enabled) }
    }

    fun setMorningReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { preferencesRepository.setMorningReminderTime(hour, minute) }
    }

    fun setEveningReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setEveningReminderEnabled(enabled) }
    }

    fun setEveningReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { preferencesRepository.setEveningReminderTime(hour, minute) }
    }

    fun setPaymentLoggerEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setPaymentLoggerEnabled(enabled) }
    }

    fun setPaymentCaptureMode(mode: PaymentCaptureMode) {
        viewModelScope.launch { preferencesRepository.setPaymentCaptureMode(mode) }
    }

    fun setPaymentAllowAllApps(allowAll: Boolean) {
        viewModelScope.launch { preferencesRepository.setPaymentAllowAllApps(allowAll) }
    }

    fun togglePaymentAllowedPackage(packageName: String) {
        viewModelScope.launch {
            val current = preferencesRepository.getCurrentPreferences().paymentAllowedPackages.toMutableSet()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            preferencesRepository.setPaymentAllowedPackages(current)
        }
    }

    fun resetPaymentAllowedPackages() {
        viewModelScope.launch { preferencesRepository.resetPaymentAllowedPackages() }
    }

    fun sendTestPaymentAlert(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val posted = paymentNotificationHandler.processIncomingNotification(
                packageName = "com.gojek.gopay",
                appLabel = "GoPay",
                title = "Pembayaran Berhasil",
                text = "Pembayaran Rp 45.000 ke KOPI KENANGAN berhasil",
                isTestSimulation = true
            )
            onResult(posted)
        }
    }

    fun clearRecentPaymentLogs() {
        paymentNotificationHandler.clearRecentLogs()
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoCheckUpdates(enabled) }
    }

    fun setGithubToken(token: String?) {
        viewModelScope.launch { preferencesRepository.setGithubToken(token) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAppLockEnabled(enabled) }
    }

    fun setAppLockTimeout(timeout: AppLockTimeout) {
        viewModelScope.launch { preferencesRepository.setAppLockTimeout(timeout) }
    }

    fun setHideRecentAppsPreview(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHideRecentAppsPreview(enabled) }
    }

    fun setHideBalances(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHideBalances(enabled) }
    }

    fun connectOrSyncDrive(activity: Activity? = null) {
        viewModelScope.launch {
            googleDriveSyncManager.startSync(
                forceMode = DriveSyncForceMode.SMART,
                interactive = true,
                activity = activity
            )
        }
    }

    fun forceUploadToDrive(activity: Activity? = null) {
        viewModelScope.launch {
            googleDriveSyncManager.startSync(
                forceMode = DriveSyncForceMode.FORCE_UPLOAD,
                interactive = true,
                activity = activity
            )
        }
    }

    fun forceRestoreFromDrive(activity: Activity? = null) {
        viewModelScope.launch {
            googleDriveSyncManager.startSync(
                forceMode = DriveSyncForceMode.FORCE_DOWNLOAD,
                interactive = true,
                activity = activity
            )
        }
    }

    fun resolveDriveConflict(keepCloud: Boolean, activity: Activity? = null) {
        viewModelScope.launch {
            googleDriveSyncManager.resolveConflict(keepCloud, activity)
        }
    }

    fun onDriveConsentResult(resultCode: Int, data: Intent?, activity: Activity? = null) {
        viewModelScope.launch {
            googleDriveSyncManager.onConsentActivityResult(resultCode, data, activity)
        }
    }

    fun consumeDriveConsentPrompt() {
        googleDriveSyncManager.consumeConsentPrompt()
    }

    fun saveDriveSyncPassphrase(passphrase: String, resumeSync: Boolean, activity: Activity? = null) {
        viewModelScope.launch {
            googleDriveSyncManager.saveSyncPassphrase(passphrase, resumeSync, activity)
        }
    }

    fun setDriveAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDriveAutoSyncEnabled(enabled)
        }
    }

    fun disconnectDrive() {
        viewModelScope.launch {
            googleDriveSyncManager.disconnect()
        }
    }

    fun clearDriveSyncStatus() {
        googleDriveSyncManager.clearStatus()
    }

    fun checkForUpdates() {
        viewModelScope.launch { appUpdateManager.checkForUpdates(isAutomatic = false) }
    }

    fun downloadUpdate(release: AppReleaseInfo) {
        viewModelScope.launch { appUpdateManager.downloadAndVerify(release) }
    }

    fun canInstallPackages(): Boolean = appUpdateManager.canInstallPackages()

    fun createInstallIntent(apkFile: File): Intent = appUpdateManager.createInstallIntent(apkFile)

    fun createUnknownSourcesIntent(): Intent = appUpdateManager.createUnknownSourcesIntent()

    fun createReleasesPageIntent(): Intent = appUpdateManager.createReleasesPageIntent()

    fun clearBackupStatus() {
        backupStatusFlow.value = BackupOperationStatus.Idle
    }

    fun exportBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            backupStatusFlow.value = BackupOperationStatus.InProgress("Encrypting and saving backup...")
            val result = backupManager.exportToUri(uri, passphrase)
            backupStatusFlow.value = result.fold(
                onSuccess = {
                    BackupOperationStatus.Success("Encrypted backup saved successfully.")
                },
                onFailure = { error ->
                    BackupOperationStatus.Error(error.message ?: "Backup export failed.")
                }
            )
        }
    }

    fun restoreBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            backupStatusFlow.value = BackupOperationStatus.InProgress("Decrypting and restoring backup...")
            val result = backupManager.importFromUri(uri, passphrase)
            backupStatusFlow.value = result.fold(
                onSuccess = { summary ->
                    BackupOperationStatus.Success(
                        "Restored ${summary.transactionsCount} transactions, ${summary.savingsGoalsCount} goals, " +
                            "${summary.notesCount} notes, ${summary.tasksCount} tasks, ${summary.habitsCount} habits, " +
                            "${summary.expiriesCount} expiries, and ${summary.vaultCount} vault items."
                    )
                },
                onFailure = { error ->
                    BackupOperationStatus.Error(error.message ?: "Backup restore failed.")
                }
            )
        }
    }
}
