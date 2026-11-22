package com.kanso.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

@Serializable
data class UserPreferences(
    val displayName: String = "",
    val currency: AppCurrency = AppCurrency.IDR,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val overallMonthlyBudgetCents: Long = 0L,
    val lastBackupMillis: Long? = null,
    val waterGoalMl: Int = 2000,
    val remindersEnabled: Boolean = true,
    val customRatesVsUsd: Map<String, Double> = emptyMap(),
    val autoCheckUpdates: Boolean = true,
    val lastUpdateCheckMillis: Long? = null
)
