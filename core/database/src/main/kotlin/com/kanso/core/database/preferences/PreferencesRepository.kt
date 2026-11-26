package com.kanso.core.database.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.AppLockTimeout
import com.kanso.core.model.DriveSyncConfig
import com.kanso.core.model.ThemeMode
import com.kanso.core.model.UserPreferences
import com.kanso.core.model.WalletType
import com.kanso.core.security.VaultCrypto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.kansoDataStore: DataStore<Preferences> by preferencesDataStore(name = "kanso_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultCrypto: VaultCrypto
) {
    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val OVERALL_MONTHLY_BUDGET_CENTS = longPreferencesKey("overall_monthly_budget_cents")
        val LAST_BACKUP_MILLIS = longPreferencesKey("last_backup_millis")
        val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val CUSTOM_RATES_JSON = stringPreferencesKey("custom_rates_vs_usd_json")
        val WALLET_OPENING_BALANCES_JSON = stringPreferencesKey("wallet_opening_balances_cents_json")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val LAST_UPDATE_CHECK_MILLIS = longPreferencesKey("last_update_check_millis")
        val GITHUB_TOKEN_ENCRYPTED = stringPreferencesKey("github_token_encrypted")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_TIMEOUT = stringPreferencesKey("app_lock_timeout")
        val HIDE_RECENT_APPS_PREVIEW = booleanPreferencesKey("hide_recent_apps_preview")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")

        val DRIVE_SYNC_CONNECTED = booleanPreferencesKey("drive_sync_connected")
        val DRIVE_AUTO_SYNC = booleanPreferencesKey("drive_auto_sync")
        val DRIVE_ACCOUNT_EMAIL = stringPreferencesKey("drive_account_email")
        val DRIVE_REMOTE_FILE_ID = stringPreferencesKey("drive_remote_file_id")
        val DRIVE_LAST_SYNC_MILLIS = longPreferencesKey("drive_last_sync_millis")
        val DRIVE_LAST_REMOTE_MODIFIED_MILLIS = longPreferencesKey("drive_last_remote_modified_millis")
        val DRIVE_LAST_SYNCED_CONTENT_HASH = stringPreferencesKey("drive_last_synced_content_hash")
        val DRIVE_SYNC_PASSPHRASE_ENCRYPTED = stringPreferencesKey("drive_sync_passphrase_encrypted")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val preferences: Flow<UserPreferences> = context.kansoDataStore.data.map { prefs ->
        val customRates = prefs[Keys.CUSTOM_RATES_JSON]?.let { raw ->
            runCatching { json.decodeFromString<Map<String, Double>>(raw) }.getOrNull()
        } ?: emptyMap()
        val walletOpeningBalances = prefs[Keys.WALLET_OPENING_BALANCES_JSON]?.let { raw ->
            runCatching { json.decodeFromString<Map<String, Long>>(raw) }.getOrNull()
        } ?: emptyMap()

        UserPreferences(
            displayName = prefs[Keys.DISPLAY_NAME].orEmpty(),
            currency = AppCurrency.fromCode(prefs[Keys.CURRENCY_CODE] ?: AppCurrency.IDR.code),
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: false,
            overallMonthlyBudgetCents = prefs[Keys.OVERALL_MONTHLY_BUDGET_CENTS] ?: 0L,
            lastBackupMillis = prefs[Keys.LAST_BACKUP_MILLIS],
            waterGoalMl = (prefs[Keys.WATER_GOAL_ML] ?: 2000).coerceAtLeast(250),
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true,
            customRatesVsUsd = customRates,
            autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true,
            lastUpdateCheckMillis = prefs[Keys.LAST_UPDATE_CHECK_MILLIS],
            walletOpeningBalancesCents = walletOpeningBalances,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            appLockTimeout = runCatching {
                AppLockTimeout.valueOf(prefs[Keys.APP_LOCK_TIMEOUT] ?: AppLockTimeout.IMMEDIATE.name)
            }.getOrDefault(AppLockTimeout.IMMEDIATE),
            hideRecentAppsPreview = prefs[Keys.HIDE_RECENT_APPS_PREVIEW] ?: false,
            hideBalances = prefs[Keys.HIDE_BALANCES] ?: false
        )
    }

    val hasGithubToken: Flow<Boolean> = context.kansoDataStore.data.map { prefs ->
        !prefs[Keys.GITHUB_TOKEN_ENCRYPTED].isNullOrBlank()
    }

    val driveSyncConfig: Flow<DriveSyncConfig> = context.kansoDataStore.data.map { prefs ->
        DriveSyncConfig(
            isConnected = prefs[Keys.DRIVE_SYNC_CONNECTED] ?: false,
            autoSyncEnabled = prefs[Keys.DRIVE_AUTO_SYNC] ?: true,
            accountEmail = prefs[Keys.DRIVE_ACCOUNT_EMAIL]?.takeIf { it.isNotBlank() },
            remoteFileId = prefs[Keys.DRIVE_REMOTE_FILE_ID]?.takeIf { it.isNotBlank() },
            lastSyncMillis = prefs[Keys.DRIVE_LAST_SYNC_MILLIS],
            lastRemoteModifiedMillis = prefs[Keys.DRIVE_LAST_REMOTE_MODIFIED_MILLIS],
            lastSyncedContentHash = prefs[Keys.DRIVE_LAST_SYNCED_CONTENT_HASH]?.takeIf { it.isNotBlank() },
            hasPassphrase = !prefs[Keys.DRIVE_SYNC_PASSPHRASE_ENCRYPTED].isNullOrBlank()
        )
    }

    suspend fun getCurrentPreferences(): UserPreferences = preferences.first()

    suspend fun getDriveSyncConfig(): DriveSyncConfig = driveSyncConfig.first()

    suspend fun getDriveSyncPassphrase(): String? {
        val encrypted = context.kansoDataStore.data.first()[Keys.DRIVE_SYNC_PASSPHRASE_ENCRYPTED]
        if (encrypted.isNullOrBlank()) return null
        return runCatching { vaultCrypto.decrypt(encrypted) }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    suspend fun setDriveSyncPassphrase(passphrase: String?) {
        val normalized = passphrase.orEmpty()
        context.kansoDataStore.edit { prefs ->
            if (normalized.isEmpty()) {
                prefs.remove(Keys.DRIVE_SYNC_PASSPHRASE_ENCRYPTED)
            } else {
                prefs[Keys.DRIVE_SYNC_PASSPHRASE_ENCRYPTED] = vaultCrypto.encrypt(normalized)
            }
        }
    }

    suspend fun setDriveAutoSyncEnabled(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.DRIVE_AUTO_SYNC] = enabled }
    }

    suspend fun updateDriveSyncConnected(accountEmail: String?) {
        context.kansoDataStore.edit { prefs ->
            prefs[Keys.DRIVE_SYNC_CONNECTED] = true
            if (!accountEmail.isNullOrBlank()) {
                prefs[Keys.DRIVE_ACCOUNT_EMAIL] = accountEmail.trim()
            }
        }
    }

    suspend fun updateDriveSyncMetadata(
        fileId: String?,
        syncedAtMillis: Long,
        remoteModifiedMillis: Long?,
        contentHash: String,
        accountEmail: String? = null
    ) {
        context.kansoDataStore.edit { prefs ->
            prefs[Keys.DRIVE_SYNC_CONNECTED] = true
            if (!fileId.isNullOrBlank()) {
                prefs[Keys.DRIVE_REMOTE_FILE_ID] = fileId
            }
            prefs[Keys.DRIVE_LAST_SYNC_MILLIS] = syncedAtMillis
            if (remoteModifiedMillis != null && remoteModifiedMillis > 0L) {
                prefs[Keys.DRIVE_LAST_REMOTE_MODIFIED_MILLIS] = remoteModifiedMillis
            } else {
                prefs.remove(Keys.DRIVE_LAST_REMOTE_MODIFIED_MILLIS)
            }
            prefs[Keys.DRIVE_LAST_SYNCED_CONTENT_HASH] = contentHash
            if (!accountEmail.isNullOrBlank()) {
                prefs[Keys.DRIVE_ACCOUNT_EMAIL] = accountEmail.trim()
            }
        }
    }

    suspend fun clearDriveSyncState() {
        context.kansoDataStore.edit { prefs ->
            prefs.remove(Keys.DRIVE_SYNC_CONNECTED)
            prefs.remove(Keys.DRIVE_AUTO_SYNC)
            prefs.remove(Keys.DRIVE_ACCOUNT_EMAIL)
            prefs.remove(Keys.DRIVE_REMOTE_FILE_ID)
            prefs.remove(Keys.DRIVE_LAST_SYNC_MILLIS)
            prefs.remove(Keys.DRIVE_LAST_REMOTE_MODIFIED_MILLIS)
            prefs.remove(Keys.DRIVE_LAST_SYNCED_CONTENT_HASH)
            prefs.remove(Keys.DRIVE_SYNC_PASSPHRASE_ENCRYPTED)
        }
    }

    suspend fun getGithubToken(): String? {
        val encrypted = context.kansoDataStore.data.first()[Keys.GITHUB_TOKEN_ENCRYPTED]
        if (encrypted.isNullOrBlank()) return null
        return runCatching { vaultCrypto.decrypt(encrypted) }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    suspend fun setGithubToken(token: String?) {
        val trimmed = token?.trim().orEmpty()
        context.kansoDataStore.edit { prefs ->
            if (trimmed.isEmpty()) {
                prefs.remove(Keys.GITHUB_TOKEN_ENCRYPTED)
            } else {
                prefs[Keys.GITHUB_TOKEN_ENCRYPTED] = vaultCrypto.encrypt(trimmed)
            }
        }
    }

    suspend fun setDisplayName(name: String) {
        context.kansoDataStore.edit { it[Keys.DISPLAY_NAME] = name.trim() }
    }

    suspend fun setCurrency(currency: AppCurrency) {
        context.kansoDataStore.edit { it[Keys.CURRENCY_CODE] = currency.code }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.kansoDataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setOverallMonthlyBudgetCents(amountCents: Long) {
        context.kansoDataStore.edit { it[Keys.OVERALL_MONTHLY_BUDGET_CENTS] = amountCents.coerceAtLeast(0L) }
    }

    suspend fun setLastBackupMillis(timestampMillis: Long) {
        context.kansoDataStore.edit { it[Keys.LAST_BACKUP_MILLIS] = timestampMillis }
    }

    suspend fun setWaterGoalMl(goalMl: Int) {
        context.kansoDataStore.edit { it[Keys.WATER_GOAL_ML] = goalMl.coerceIn(250, 10_000) }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }

    suspend fun setCustomRatesVsUsd(rates: Map<String, Double>) {
        context.kansoDataStore.edit { prefs ->
            if (rates.isEmpty()) {
                prefs.remove(Keys.CUSTOM_RATES_JSON)
            } else {
                prefs[Keys.CUSTOM_RATES_JSON] = json.encodeToString(rates)
            }
        }
    }

    suspend fun setWalletOpeningBalance(wallet: WalletType, openingBalanceCents: Long) {
        context.kansoDataStore.edit { prefs ->
            val current = prefs[Keys.WALLET_OPENING_BALANCES_JSON]?.let { raw ->
                runCatching { json.decodeFromString<Map<String, Long>>(raw) }.getOrNull()
            }?.toMutableMap() ?: mutableMapOf()
            if (openingBalanceCents == 0L) {
                current.remove(wallet.name)
            } else {
                current[wallet.name] = openingBalanceCents
            }
            if (current.isEmpty()) {
                prefs.remove(Keys.WALLET_OPENING_BALANCES_JSON)
            } else {
                prefs[Keys.WALLET_OPENING_BALANCES_JSON] = json.encodeToString(current)
            }
        }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.AUTO_CHECK_UPDATES] = enabled }
    }

    suspend fun setLastUpdateCheckMillis(timestampMillis: Long) {
        context.kansoDataStore.edit { it[Keys.LAST_UPDATE_CHECK_MILLIS] = timestampMillis }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setAppLockTimeout(timeout: AppLockTimeout) {
        context.kansoDataStore.edit { it[Keys.APP_LOCK_TIMEOUT] = timeout.name }
    }

    suspend fun setHideRecentAppsPreview(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.HIDE_RECENT_APPS_PREVIEW] = enabled }
    }

    suspend fun setHideBalances(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.HIDE_BALANCES] = enabled }
    }

    suspend fun toggleHideBalances() {
        context.kansoDataStore.edit { prefs ->
            prefs[Keys.HIDE_BALANCES] = !(prefs[Keys.HIDE_BALANCES] ?: false)
        }
    }

    suspend fun restorePreferences(userPreferences: UserPreferences) {
        context.kansoDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = userPreferences.displayName
            prefs[Keys.CURRENCY_CODE] = userPreferences.currency.code
            prefs[Keys.THEME_MODE] = userPreferences.themeMode.name
            prefs[Keys.USE_DYNAMIC_COLOR] = userPreferences.useDynamicColor
            prefs[Keys.OVERALL_MONTHLY_BUDGET_CENTS] = userPreferences.overallMonthlyBudgetCents
            prefs[Keys.WATER_GOAL_ML] = userPreferences.waterGoalMl.coerceIn(250, 10_000)
            prefs[Keys.REMINDERS_ENABLED] = userPreferences.remindersEnabled
            prefs[Keys.AUTO_CHECK_UPDATES] = userPreferences.autoCheckUpdates
            prefs[Keys.HIDE_BALANCES] = userPreferences.hideBalances
            if (userPreferences.customRatesVsUsd.isEmpty()) {
                prefs.remove(Keys.CUSTOM_RATES_JSON)
            } else {
                prefs[Keys.CUSTOM_RATES_JSON] = json.encodeToString(userPreferences.customRatesVsUsd)
            }
            if (userPreferences.walletOpeningBalancesCents.isEmpty()) {
                prefs.remove(Keys.WALLET_OPENING_BALANCES_JSON)
            } else {
                prefs[Keys.WALLET_OPENING_BALANCES_JSON] = json.encodeToString(userPreferences.walletOpeningBalancesCents)
            }
            val restoredBackupMillis = userPreferences.lastBackupMillis
            if (restoredBackupMillis != null) {
                prefs[Keys.LAST_BACKUP_MILLIS] = restoredBackupMillis
            } else {
                prefs.remove(Keys.LAST_BACKUP_MILLIS)
            }
        }
    }
}
