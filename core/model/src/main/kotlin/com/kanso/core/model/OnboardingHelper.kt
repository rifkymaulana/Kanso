package com.kanso.core.model

import java.util.Currency
import java.util.Locale

enum class StarterHabitPreset(
    val habitTitle: String,
    val targetDaysPerWeek: Int = 7,
    val reminderHour: Int? = null
) {
    DRINK_WATER("Drink 2L water", targetDaysPerWeek = 7, reminderHour = 9),
    REVIEW_SPEND("Log daily expenses", targetDaysPerWeek = 7, reminderHour = 20),
    MOVE_BODY("Exercise / Walk 20m", targetDaysPerWeek = 5, reminderHour = null),
    PLAN_TOP_TASKS("Plan top 3 tasks", targetDaysPerWeek = 7, reminderHour = 8),
    READ_15M("Read 15 mins", targetDaysPerWeek = 7, reminderHour = null)
}

object OnboardingHelper {

    private val eurCountries = setOf(
        "DE", "FR", "IT", "ES", "NL", "BE", "AT", "PT", "IE", "FI", "GR", "LU", "SK", "SI", "LV", "LT", "EE"
    )
    private val usdFallbackCountries = setOf("US", "AU", "CA", "NZ", "PH", "TW", "HK")

    fun suggestCurrencyForLocale(locale: Locale = Locale.getDefault()): AppCurrency {
        val isoCode = runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()
        if (!isoCode.isNullOrBlank()) {
            val matched = AppCurrency.entries.firstOrNull { it.code.equals(isoCode, ignoreCase = true) }
            if (matched != null) return matched
        }

        val country = locale.country.uppercase(Locale.ROOT)
        when (country) {
            "ID" -> return AppCurrency.IDR
            "SG" -> return AppCurrency.SGD
            "MY" -> return AppCurrency.MYR
            "JP" -> return AppCurrency.JPY
            "GB", "UK" -> return AppCurrency.GBP
            in eurCountries -> return AppCurrency.EUR
            in usdFallbackCountries -> return AppCurrency.USD
        }

        return when (locale.language.lowercase(Locale.ROOT)) {
            "id", "in" -> AppCurrency.IDR
            "ja" -> AppCurrency.JPY
            "ms" -> AppCurrency.MYR
            "en" -> AppCurrency.USD
            else -> AppCurrency.IDR
        }
    }

    fun shouldAutoCompleteForExistingInstall(
        hasCompletedFlag: Boolean,
        prefs: UserPreferences,
        driveConfig: DriveSyncConfig,
        stats: StorageStats
    ): Boolean {
        if (hasCompletedFlag) return false

        val hasAnyRecords = (
            stats.transactionCount +
                stats.noteCount +
                stats.openTaskCount +
                stats.totalTaskCount +
                stats.snippetCount +
                stats.habitCount +
                stats.expiryCount +
                stats.maintenanceCount +
                stats.vaultCount +
                stats.activeBillCount +
                stats.openIouCount +
                stats.activeGoalCount +
                stats.countdownCount
            ) > 0

        return prefs.displayName.isNotBlank() ||
            prefs.overallMonthlyBudgetCents > 0L ||
            prefs.lastBackupMillis != null ||
            prefs.appLockEnabled ||
            prefs.walletOpeningBalancesCents.isNotEmpty() ||
            driveConfig.isConnected ||
            hasAnyRecords
    }

    fun suggestedMonthlyCaps(currency: AppCurrency): List<Long> = when (currency) {
        AppCurrency.IDR -> listOf(3_000_000_00L, 5_000_000_00L, 10_000_000_00L)
        AppCurrency.USD -> listOf(1_500_00L, 3_000_00L, 5_000_00L)
        AppCurrency.EUR -> listOf(1_500_00L, 2_500_00L, 4_000_00L)
        AppCurrency.SGD -> listOf(2_000_00L, 3_500_00L, 5_000_00L)
        AppCurrency.MYR -> listOf(3_000_00L, 5_000_00L, 8_000_00L)
        AppCurrency.JPY -> listOf(150_000_00L, 250_000_00L, 400_000_00L)
        AppCurrency.GBP -> listOf(1_200_00L, 2_500_00L, 4_000_00L)
    }
}
