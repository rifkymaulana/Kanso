package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class OnboardingHelperTest {

    @Test
    fun `suggestCurrencyForLocale maps supported countries and language fallbacks`() {
        assertEquals(AppCurrency.IDR, OnboardingHelper.suggestCurrencyForLocale(Locale("id", "ID")))
        assertEquals(AppCurrency.IDR, OnboardingHelper.suggestCurrencyForLocale(Locale("in", "ID")))
        assertEquals(AppCurrency.USD, OnboardingHelper.suggestCurrencyForLocale(Locale.US))
        assertEquals(AppCurrency.SGD, OnboardingHelper.suggestCurrencyForLocale(Locale("en", "SG")))
        assertEquals(AppCurrency.MYR, OnboardingHelper.suggestCurrencyForLocale(Locale("ms", "MY")))
        assertEquals(AppCurrency.JPY, OnboardingHelper.suggestCurrencyForLocale(Locale.JAPAN))
        assertEquals(AppCurrency.GBP, OnboardingHelper.suggestCurrencyForLocale(Locale.UK))
        assertEquals(AppCurrency.EUR, OnboardingHelper.suggestCurrencyForLocale(Locale.GERMANY))
        assertEquals(AppCurrency.USD, OnboardingHelper.suggestCurrencyForLocale(Locale("en", "AU")))
        assertEquals(AppCurrency.USD, OnboardingHelper.suggestCurrencyForLocale(Locale("en")))
        assertEquals(AppCurrency.IDR, OnboardingHelper.suggestCurrencyForLocale(Locale("id")))
    }

    @Test
    fun `shouldAutoCompleteForExistingInstall detects upgraded users without flagging fresh installs`() {
        val emptyStats = StorageStats(0, 0, 0, 0)
        assertFalse(
            OnboardingHelper.shouldAutoCompleteForExistingInstall(
                hasCompletedFlag = false,
                prefs = UserPreferences(),
                driveConfig = DriveSyncConfig(),
                stats = emptyStats
            )
        )
        assertFalse(
            OnboardingHelper.shouldAutoCompleteForExistingInstall(
                hasCompletedFlag = true,
                prefs = UserPreferences(displayName = "Rifky"),
                driveConfig = DriveSyncConfig(),
                stats = emptyStats
            )
        )
        assertTrue(
            OnboardingHelper.shouldAutoCompleteForExistingInstall(
                hasCompletedFlag = false,
                prefs = UserPreferences(displayName = "Rifky"),
                driveConfig = DriveSyncConfig(),
                stats = emptyStats
            )
        )
        assertTrue(
            OnboardingHelper.shouldAutoCompleteForExistingInstall(
                hasCompletedFlag = false,
                prefs = UserPreferences(overallMonthlyBudgetCents = 5_000_000_00L),
                driveConfig = DriveSyncConfig(),
                stats = emptyStats
            )
        )
        assertTrue(
            OnboardingHelper.shouldAutoCompleteForExistingInstall(
                hasCompletedFlag = false,
                prefs = UserPreferences(lastBackupMillis = 1_700_000_000L),
                driveConfig = DriveSyncConfig(),
                stats = emptyStats
            )
        )
        assertTrue(
            OnboardingHelper.shouldAutoCompleteForExistingInstall(
                hasCompletedFlag = false,
                prefs = UserPreferences(appLockEnabled = true),
                driveConfig = DriveSyncConfig(),
                stats = emptyStats
            )
        )
        assertTrue(
            OnboardingHelper.shouldAutoCompleteForExistingInstall(
                hasCompletedFlag = false,
                prefs = UserPreferences(),
                driveConfig = DriveSyncConfig(isConnected = true),
                stats = emptyStats
            )
        )
        assertTrue(
            OnboardingHelper.shouldAutoCompleteForExistingInstall(
                hasCompletedFlag = false,
                prefs = UserPreferences(),
                driveConfig = DriveSyncConfig(),
                stats = StorageStats(transactionCount = 1, noteCount = 0, openTaskCount = 0, snippetCount = 0)
            )
        )
    }

    @Test
    fun `suggestedMonthlyCaps returns three increasing positive presets per currency`() {
        AppCurrency.entries.forEach { currency ->
            val caps = OnboardingHelper.suggestedMonthlyCaps(currency)
            assertEquals(3, caps.size)
            assertTrue(caps[0] > 0L)
            assertTrue(caps[0] < caps[1] && caps[1] < caps[2])
        }
    }
}
