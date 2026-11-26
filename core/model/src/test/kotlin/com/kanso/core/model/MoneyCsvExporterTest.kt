package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class MoneyCsvExporterTest {

    @Test
    fun `buildTransactionsCsv includes UTF8 BOM, RFC-4180 escaping, and signed amounts for IDR and USD`() {
        val zone = ZoneOffset.UTC
        val t1 = LocalDate.of(2026, 11, 25).atTime(LocalTime.of(9, 5)).toInstant(zone).toEpochMilli()
        val t2 = LocalDate.of(2026, 11, 26).atTime(LocalTime.of(13, 30)).toInstant(zone).toEpochMilli()
        val t3 = LocalDate.of(2026, 11, 26).atTime(LocalTime.of(18, 0)).toInstant(zone).toEpochMilli()

        val txs = listOf(
            MoneyTransaction(
                id = 1L,
                type = TransactionType.INCOME,
                amountCents = 5_000_00L,
                category = ExpenseCategory.SALARY,
                wallet = WalletType.BANK,
                note = "Monthly \"bonus\", Q4",
                timestampMillis = t1
            ),
            MoneyTransaction(
                id = 2L,
                type = TransactionType.EXPENSE,
                amountCents = 12_50L,
                category = ExpenseCategory.FOOD,
                wallet = WalletType.EWALLET,
                note = "Coffee\nLine2",
                timestampMillis = t2
            ),
            MoneyTransaction(
                id = 3L,
                type = TransactionType.TRANSFER,
                amountCents = 100_00L,
                category = ExpenseCategory.OTHER,
                wallet = WalletType.BANK,
                toWallet = WalletType.EWALLET,
                note = "Top up",
                timestampMillis = t3
            )
        )

        val usdCsv = MoneyCsvExporter.buildTransactionsCsv(txs, AppCurrency.USD, zone = zone, includeUtf8Bom = true)
        assertTrue(usdCsv.startsWith("\uFEFFDate,Time,Type,Category,Wallet,To Wallet,Amount,Signed Cashflow,Currency,Formatted,Note"))
        // Sorted descending by timestampMillis -> Transfer (t3), Expense (t2), Income (t1)
        assertTrue(usdCsv.contains("2026-11-26,18:00,Transfer,,Bank,E-Wallet,100.00,0.00,USD,\$100.00,Top up"))
        assertTrue(usdCsv.contains("2026-11-26,13:30,Expense,Food & Drink,E-Wallet,,12.50,-12.50,USD,-\$12.50,\"Coffee\nLine2\""))
        assertTrue(usdCsv.contains("2026-11-25,09:05,Income,Salary,Bank,,5000.00,5000.00,USD,\"+\$5,000.00\",\"Monthly \"\"bonus\"\", Q4\""))

        val idrCsv = MoneyCsvExporter.buildTransactionsCsv(
            listOf(txs[1].copy(amountCents = 45_000_00L, note = "Coffee")),
            AppCurrency.IDR,
            zone = zone,
            includeUtf8Bom = false
        )
        assertFalse(idrCsv.startsWith("\uFEFF"))
        assertEquals(2, idrCsv.trim().lines().size)
        assertTrue(idrCsv.contains(",45000,-45000,IDR,"))
    }
}
