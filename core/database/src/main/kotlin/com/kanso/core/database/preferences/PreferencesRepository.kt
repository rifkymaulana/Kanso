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
import com.kanso.core.model.ThemeMode
import com.kanso.core.model.UserPreferences
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
    @ApplicationContext private val context: Context
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
    }

    private val json = Json { ignoreUnknownKeys = true }

    val preferences: Flow<UserPreferences> = context.kansoDataStore.data.map { prefs ->
        val customRates = prefs[Keys.CUSTOM_RATES_JSON]?.let { raw ->
            runCatching { json.decodeFromString<Map<String, Double>>(raw) }.getOrNull()
        } ?: emptyMap()

        UserPreferences(
            displayName = prefs[Keys.DISPLAY_NAME].orEmpty(),
            currency = AppCurrency.fromCode(prefs[Keys.CURRENCY_CODE] ?: AppCurrency.IDR.code),
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true,
            overallMonthlyBudgetCents = prefs[Keys.OVERALL_MONTHLY_BUDGET_CENTS] ?: 0L,
            lastBackupMillis = prefs[Keys.LAST_BACKUP_MILLIS],
            waterGoalMl = (prefs[Keys.WATER_GOAL_ML] ?: 2000).coerceAtLeast(250),
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true,
            customRatesVsUsd = customRates
        )
    }

    suspend fun getCurrentPreferences(): UserPreferences = preferences.first()

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

    suspend fun restorePreferences(userPreferences: UserPreferences) {
        context.kansoDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = userPreferences.displayName
            prefs[Keys.CURRENCY_CODE] = userPreferences.currency.code
            prefs[Keys.THEME_MODE] = userPreferences.themeMode.name
            prefs[Keys.USE_DYNAMIC_COLOR] = userPreferences.useDynamicColor
            prefs[Keys.OVERALL_MONTHLY_BUDGET_CENTS] = userPreferences.overallMonthlyBudgetCents
            prefs[Keys.WATER_GOAL_ML] = userPreferences.waterGoalMl.coerceIn(250, 10_000)
            prefs[Keys.REMINDERS_ENABLED] = userPreferences.remindersEnabled
            if (userPreferences.customRatesVsUsd.isEmpty()) {
                prefs.remove(Keys.CUSTOM_RATES_JSON)
            } else {
                prefs[Keys.CUSTOM_RATES_JSON] = json.encodeToString(userPreferences.customRatesVsUsd)
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
