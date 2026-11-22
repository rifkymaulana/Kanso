package com.kanso.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.app.update.AppReleaseInfo
import com.kanso.app.update.AppUpdateManager
import com.kanso.app.update.AppUpdateState
import com.kanso.core.backup.BackupManager
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.model.AppCurrency
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
    val updateState: AppUpdateState = AppUpdateState.Idle
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val backupManager: BackupManager,
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {

    private val backupStatusFlow = MutableStateFlow<BackupOperationStatus>(BackupOperationStatus.Idle)

    val uiState: StateFlow<SettingsUiState> = combine(
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

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoCheckUpdates(enabled) }
    }

    fun setGithubToken(token: String?) {
        viewModelScope.launch { preferencesRepository.setGithubToken(token) }
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
                        "Restored ${summary.transactionsCount} transactions, ${summary.notesCount} notes, " +
                            "${summary.tasksCount} tasks, ${summary.habitsCount} habits, " +
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
