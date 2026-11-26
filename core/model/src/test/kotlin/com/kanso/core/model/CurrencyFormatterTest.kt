package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun `formats IDR with dot separators and no decimals`() {
        assertEquals("Rp 0", CurrencyFormatter.format(0L, AppCurrency.IDR))
        assertEquals("Rp 50.000", CurrencyFormatter.format(50_000_00L, AppCurrency.IDR))
        assertEquals("Rp 15.000.000", CurrencyFormatter.format(15_000_000_00L, AppCurrency.IDR))
        assertEquals("-Rp 25.000", CurrencyFormatter.format(-25_000_00L, AppCurrency.IDR))
        assertEquals("+Rp 100.000", CurrencyFormatter.format(100_000_00L, AppCurrency.IDR, showSign = true))
    }

    @Test
    fun `formats USD with comma grouping and two decimals`() {
        assertEquals("$0.00", CurrencyFormatter.format(0L, AppCurrency.USD))
        assertEquals("$12.50", CurrencyFormatter.format(12_50L, AppCurrency.USD))
        assertEquals("$1,250.05", CurrencyFormatter.format(125_005L, AppCurrency.USD))
        assertEquals("-$9.99", CurrencyFormatter.format(-9_99L, AppCurrency.USD))
        assertEquals("+$5.00", CurrencyFormatter.format(5_00L, AppCurrency.USD, showSign = true))
    }

    @Test
    fun `parses IDR inputs cleanly`() {
        assertEquals(50_000_00L, CurrencyFormatter.parseInputToCents("50000", AppCurrency.IDR))
        assertEquals(50_000_00L, CurrencyFormatter.parseInputToCents("50.000", AppCurrency.IDR))
        assertEquals(1_500_000_00L, CurrencyFormatter.parseInputToCents("Rp 1.500.000", AppCurrency.IDR))
        assertEquals(0L, CurrencyFormatter.parseInputToCents("0", AppCurrency.IDR))
        assertNull(CurrencyFormatter.parseInputToCents("", AppCurrency.IDR))
        assertNull(CurrencyFormatter.parseInputToCents("-500", AppCurrency.IDR))
        assertNull(CurrencyFormatter.parseInputToCents("abc", AppCurrency.IDR))
    }

    @Test
    fun `parses USD decimal inputs cleanly`() {
        assertEquals(12_50L, CurrencyFormatter.parseInputToCents("12.50", AppCurrency.USD))
        assertEquals(12_50L, CurrencyFormatter.parseInputToCents("$12.5", AppCurrency.USD))
        assertEquals(1_250_75L, CurrencyFormatter.parseInputToCents("1,250.75", AppCurrency.USD))
        assertEquals(10_00L, CurrencyFormatter.parseInputToCents("10", AppCurrency.USD))
        assertNull(CurrencyFormatter.parseInputToCents("-1.25", AppCurrency.USD))
        assertNull(CurrencyFormatter.parseInputToCents("   ", AppCurrency.USD))
    }

    @Test
    fun `formats compact input for editing`() {
        assertEquals("50000", CurrencyFormatter.formatCompactInput(50_000_00L, AppCurrency.IDR))
        assertEquals("12.50", CurrencyFormatter.formatCompactInput(12_50L, AppCurrency.USD))
    }

    @Test
    fun `sanitizeInput strips separators and leading zeros per currency`() {
        assertEquals("", CurrencyFormatter.sanitizeInput("", AppCurrency.IDR))
        assertEquals("0", CurrencyFormatter.sanitizeInput("000", AppCurrency.IDR))
        assertEquals("1000", CurrencyFormatter.sanitizeInput("1.000", AppCurrency.IDR))
        assertEquals("1500000", CurrencyFormatter.sanitizeInput("Rp 1.500.000", AppCurrency.IDR))
        assertEquals("1250.50", CurrencyFormatter.sanitizeInput("1,250.509", AppCurrency.USD))
        assertEquals("0.5", CurrencyFormatter.sanitizeInput(".5", AppCurrency.USD))
    }

    @Test
    fun `formatLiveInput groups thousands and preserves valid Compose offset invariants`() {
        assertEquals("1.000", CurrencyFormatter.formatLiveInput("1000", AppCurrency.IDR).formatted)
        assertEquals("1.500.000", CurrencyFormatter.formatLiveInput("1500000", AppCurrency.IDR).formatted)
        assertEquals("1,250.50", CurrencyFormatter.formatLiveInput("1250.50", AppCurrency.USD).formatted)

        val samples = listOf("", "5", "500", "1000", "10000", "1500000", "1234567.89")
        for (currency in listOf(AppCurrency.IDR, AppCurrency.USD)) {
            for (raw in samples) {
                val res = CurrencyFormatter.formatLiveInput(raw, currency)
                assertEquals(raw.length + 1, res.originalToTransformed.size)
                assertEquals(res.formatted.length + 1, res.transformedToOriginal.size)

                var prevTrans = 0
                for (i in 0..raw.length) {
                    val mapped = res.originalToTransformed[i]
                    assert(mapped in 0..res.formatted.length)
                    assert(mapped >= prevTrans)
                    assertEquals(i, res.transformedToOriginal[mapped])
                    prevTrans = mapped
                }

                var prevOrig = 0
                for (j in 0..res.formatted.length) {
                    val mapped = res.transformedToOriginal[j]
                    assert(mapped in 0..raw.length)
                    assert(mapped >= prevOrig)
                    prevOrig = mapped
                }
            }
        }
    }

    @Test
    fun `mask and formatOrMask hide exact currency figures cleanly`() {
        assertEquals("Rp ••••••", CurrencyFormatter.mask(AppCurrency.IDR))
        assertEquals("$••••••", CurrencyFormatter.mask(AppCurrency.USD))
        assertEquals("Rp ••••••", CurrencyFormatter.formatOrMask(250_000_00L, AppCurrency.IDR, hidden = true))
        assertEquals("Rp 250.000", CurrencyFormatter.formatOrMask(250_000_00L, AppCurrency.IDR, hidden = false))
    }

    @Test
    fun `app lock policy enforces lock availability, grace timeout, and ignores config change`() {
        // Cold start when enabled + biometric available -> locked
        val lockedColdStart = AppLockSession(isUnlocked = false)
        assertTrue(AppLockPolicy.shouldRequireUnlock(appLockEnabled = true, isBiometricAvailable = true, lockedColdStart))
        // Fail open if device has no biometrics enrolled
        assertFalse(AppLockPolicy.shouldRequireUnlock(appLockEnabled = true, isBiometricAvailable = false, lockedColdStart))
        // Disabled -> never locks
        assertFalse(AppLockPolicy.shouldRequireUnlock(appLockEnabled = false, isBiometricAvailable = true, lockedColdStart))

        // Immediate timeout locks on background unless changing configurations or prompt active
        val unlocked = AppLockSession(isUnlocked = true)
        val afterRotation = AppLockPolicy.onBackgrounded(
            session = unlocked,
            appLockEnabled = true,
            timeout = AppLockTimeout.IMMEDIATE,
            isChangingConfigurations = true,
            nowElapsedMillis = 1_000L
        )
        assertTrue(afterRotation.isUnlocked)

        val afterPromptBg = AppLockPolicy.onBackgrounded(
            session = unlocked.copy(isPromptActive = true),
            appLockEnabled = true,
            timeout = AppLockTimeout.IMMEDIATE,
            isChangingConfigurations = false,
            nowElapsedMillis = 1_000L
        )
        assertTrue(afterPromptBg.isUnlocked)

        val afterImmediateBg = AppLockPolicy.onBackgrounded(
            session = unlocked,
            appLockEnabled = true,
            timeout = AppLockTimeout.IMMEDIATE,
            isChangingConfigurations = false,
            nowElapsedMillis = 1_000L
        )
        assertFalse(afterImmediateBg.isUnlocked)

        // 1-minute grace window: stays unlocked at +30s, locks at +60s
        val afterGraceBg = AppLockPolicy.onBackgrounded(
            session = unlocked,
            appLockEnabled = true,
            timeout = AppLockTimeout.ONE_MINUTE,
            isChangingConfigurations = false,
            nowElapsedMillis = 10_000L
        )
        assertTrue(afterGraceBg.isUnlocked)

        val returnedEarly = AppLockPolicy.onForegrounded(
            session = afterGraceBg,
            appLockEnabled = true,
            timeout = AppLockTimeout.ONE_MINUTE,
            nowElapsedMillis = 40_000L
        )
        assertTrue(returnedEarly.isUnlocked)

        val returnedLate = AppLockPolicy.onForegrounded(
            session = afterGraceBg,
            appLockEnabled = true,
            timeout = AppLockTimeout.ONE_MINUTE,
            nowElapsedMillis = 70_000L
        )
        assertFalse(returnedLate.isUnlocked)
    }
}
