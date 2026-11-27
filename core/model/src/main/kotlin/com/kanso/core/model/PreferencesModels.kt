package com.kanso.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

@Serializable
enum class AppLockTimeout(val label: String, val timeoutMillis: Long) {
    IMMEDIATE("Immediately", 0L),
    ONE_MINUTE("1 min", 60_000L),
    FIVE_MINUTES("5 min", 300_000L)
}

@Serializable
data class UserPreferences(
    val displayName: String = "",
    val currency: AppCurrency = AppCurrency.IDR,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val overallMonthlyBudgetCents: Long = 0L,
    val lastBackupMillis: Long? = null,
    val waterGoalMl: Int = 2000,
    val remindersEnabled: Boolean = true,
    val customRatesVsUsd: Map<String, Double> = emptyMap(),
    val autoCheckUpdates: Boolean = true,
    val lastUpdateCheckMillis: Long? = null,
    val walletOpeningBalancesCents: Map<String, Long> = emptyMap(),
    val appLockEnabled: Boolean = false,
    val appLockTimeout: AppLockTimeout = AppLockTimeout.IMMEDIATE,
    val hideRecentAppsPreview: Boolean = false,
    val hideBalances: Boolean = false,
    val morningReminderEnabled: Boolean = true,
    val morningReminderHour: Int = 8,
    val morningReminderMinute: Int = 0,
    val eveningReminderEnabled: Boolean = true,
    val eveningReminderHour: Int = 20,
    val eveningReminderMinute: Int = 30,
    val paymentLoggerEnabled: Boolean = false,
    val paymentCaptureMode: PaymentCaptureMode = PaymentCaptureMode.PROMPT,
    val paymentAllowedPackages: Set<String> = KnownFinanceApps.defaultPackages,
    val paymentAllowAllApps: Boolean = false
)

data class AppLockSession(
    val isUnlocked: Boolean = false,
    val backgroundedAtElapsedMillis: Long? = null,
    val isPromptActive: Boolean = false
)

object AppLockPolicy {
    fun shouldRequireUnlock(
        appLockEnabled: Boolean,
        isBiometricAvailable: Boolean,
        session: AppLockSession
    ): Boolean {
        return appLockEnabled && isBiometricAvailable && !session.isUnlocked
    }

    fun onBackgrounded(
        session: AppLockSession,
        appLockEnabled: Boolean,
        timeout: AppLockTimeout,
        isChangingConfigurations: Boolean,
        nowElapsedMillis: Long
    ): AppLockSession {
        if (isChangingConfigurations || session.isPromptActive || !appLockEnabled) return session
        return if (timeout == AppLockTimeout.IMMEDIATE) {
            session.copy(isUnlocked = false, backgroundedAtElapsedMillis = nowElapsedMillis)
        } else {
            session.copy(backgroundedAtElapsedMillis = nowElapsedMillis)
        }
    }

    fun onForegrounded(
        session: AppLockSession,
        appLockEnabled: Boolean,
        timeout: AppLockTimeout,
        nowElapsedMillis: Long
    ): AppLockSession {
        val bgAt = session.backgroundedAtElapsedMillis ?: return session
        if (!appLockEnabled) {
            return session.copy(backgroundedAtElapsedMillis = null)
        }
        val elapsed = (nowElapsedMillis - bgAt).coerceAtLeast(0L)
        val shouldLock = elapsed >= timeout.timeoutMillis
        return session.copy(
            isUnlocked = if (shouldLock) false else session.isUnlocked,
            backgroundedAtElapsedMillis = null
        )
    }
}

data class DriveSyncConfig(
    val isConnected: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val accountEmail: String? = null,
    val remoteFileId: String? = null,
    val lastSyncMillis: Long? = null,
    val lastRemoteModifiedMillis: Long? = null,
    val lastSyncedContentHash: String? = null,
    val hasPassphrase: Boolean = false
)
