package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
