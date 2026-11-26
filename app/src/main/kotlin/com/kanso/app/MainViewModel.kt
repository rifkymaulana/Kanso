package com.kanso.app

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.app.update.AppReleaseInfo
import com.kanso.app.update.AppUpdateManager
import com.kanso.app.update.AppUpdateState
import com.kanso.core.backup.drive.GoogleDriveSyncManager
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.BrainRepository
import com.kanso.core.database.repository.LifeAdminRepository
import com.kanso.core.database.repository.MoneyRepository
import com.kanso.core.model.AppLockPolicy
import com.kanso.core.model.AppLockSession
import com.kanso.core.model.DriveSyncConfig
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.ExpiryCategory
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.NoteItem
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TaskPriority
import com.kanso.core.model.TransactionType
import com.kanso.core.model.UserPreferences
import com.kanso.core.model.WalletType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    private val moneyRepository: MoneyRepository,
    private val brainRepository: BrainRepository,
    private val lifeAdminRepository: LifeAdminRepository,
    private val appUpdateManager: AppUpdateManager,
    private val googleDriveSyncManager: GoogleDriveSyncManager
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences()
        )

    val isPreferencesLoaded: StateFlow<Boolean> = preferencesRepository.preferences
        .map { true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    private val lockSessionFlow = MutableStateFlow(AppLockSession())
    val lockSession: StateFlow<AppLockSession> = lockSessionFlow

    val driveSyncConfig: StateFlow<DriveSyncConfig> = preferencesRepository.driveSyncConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DriveSyncConfig()
        )

    val updateState: StateFlow<AppUpdateState> = appUpdateManager.state
    val dismissedUpdateTag: StateFlow<String?> = appUpdateManager.dismissedTag

    fun onAppForegrounded(nowElapsedMillis: Long) {
        val currentPrefs = preferences.value
        lockSessionFlow.update { current ->
            AppLockPolicy.onForegrounded(
                session = current,
                appLockEnabled = currentPrefs.appLockEnabled,
                timeout = currentPrefs.appLockTimeout,
                nowElapsedMillis = nowElapsedMillis
            )
        }
    }

    fun onAppBackgrounded(isChangingConfigurations: Boolean, nowElapsedMillis: Long) {
        val currentPrefs = preferences.value
        lockSessionFlow.update { current ->
            AppLockPolicy.onBackgrounded(
                session = current,
                appLockEnabled = currentPrefs.appLockEnabled,
                timeout = currentPrefs.appLockTimeout,
                isChangingConfigurations = isChangingConfigurations,
                nowElapsedMillis = nowElapsedMillis
            )
        }
    }

    fun setBiometricPromptActive(active: Boolean) {
        lockSessionFlow.update { it.copy(isPromptActive = active) }
    }

    fun markSessionUnlocked() {
        lockSessionFlow.update {
            it.copy(
                isUnlocked = true,
                backgroundedAtElapsedMillis = null,
                isPromptActive = false
            )
        }
    }

    fun triggerAutoUpdateCheck() {
        viewModelScope.launch {
            appUpdateManager.checkForUpdates(isAutomatic = true)
        }
    }

    fun triggerAutoDriveSync() {
        viewModelScope.launch {
            googleDriveSyncManager.syncSilentlyIfReady(ignoreThrottle = false)
        }
    }

    fun downloadUpdate(release: AppReleaseInfo) {
        viewModelScope.launch {
            appUpdateManager.downloadAndVerify(release)
        }
    }

    fun dismissUpdateBanner(tagName: String) {
        appUpdateManager.dismissBanner(tagName)
    }

    fun canInstallPackages(): Boolean = appUpdateManager.canInstallPackages()

    fun createInstallIntent(apkFile: File): Intent = appUpdateManager.createInstallIntent(apkFile)

    fun createUnknownSourcesIntent(): Intent = appUpdateManager.createUnknownSourcesIntent()

    fun saveQuickTransaction(
        type: TransactionType,
        amountCents: Long,
        category: ExpenseCategory,
        wallet: WalletType,
        note: String,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            moneyRepository.upsertTransaction(
                MoneyTransaction(
                    type = type,
                    amountCents = amountCents,
                    category = category,
                    wallet = wallet,
                    note = note,
                    timestampMillis = timestampMillis
                )
            )
        }
    }

    fun saveQuickTransfer(
        fromWallet: WalletType,
        toWallet: WalletType,
        amountCents: Long,
        feeCents: Long,
        note: String,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            moneyRepository.recordTransfer(
                fromWallet = fromWallet,
                toWallet = toWallet,
                amountCents = amountCents,
                feeCents = feeCents,
                note = note,
                timestampMillis = timestampMillis
            )
        }
    }

    fun saveQuickNote(
        title: String,
        content: String,
        tags: List<String>,
        isPinned: Boolean
    ) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            brainRepository.upsertNote(
                NoteItem(
                    title = title,
                    content = content,
                    tags = tags,
                    isPinned = isPinned,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
        }
    }

    fun saveQuickTask(
        title: String,
        priority: TaskPriority,
        dueDateEpochDay: Long?
    ) {
        viewModelScope.launch {
            brainRepository.upsertTask(
                TaskItem(
                    title = title,
                    priority = priority,
                    dueDateEpochDay = dueDateEpochDay,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveQuickSnippet(
        label: String,
        value: String,
        category: SnippetCategory,
        isSensitive: Boolean,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            brainRepository.upsertSnippet(
                SnippetItem(
                    label = label,
                    value = value,
                    category = category,
                    isSensitive = isSensitive,
                    isPinned = isPinned,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveQuickExpiry(
        title: String,
        category: ExpiryCategory,
        expiryEpochDay: Long,
        remindDaysBefore: Int
    ) {
        viewModelScope.launch {
            lifeAdminRepository.upsertExpiry(
                ExpiryItem(
                    title = title,
                    category = category,
                    expiryEpochDay = expiryEpochDay,
                    remindDaysBefore = remindDaysBefore,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }
}
