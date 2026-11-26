package com.kanso.core.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

enum class CsvExportScope {
    SELECTED_MONTH,
    ALL_HISTORY
}

object MoneyCsvExporter {

    private const val UTF8_BOM = "\uFEFF"
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    /**
     * Builds an RFC-4180 CSV string with optional UTF-8 BOM for direct import into Excel or Sheets.
     */
    fun buildTransactionsCsv(
        transactions: List<MoneyTransaction>,
        currency: AppCurrency,
        zone: ZoneId = ZoneId.systemDefault(),
        includeUtf8Bom: Boolean = true
    ): String {
        val sb = StringBuilder()
        if (includeUtf8Bom) {
            sb.append(UTF8_BOM)
        }
        sb.appendLine("Date,Time,Type,Category,Wallet,To Wallet,Amount,Signed Cashflow,Currency,Formatted,Note")

        val sorted = transactions.sortedWith(
            compareByDescending<MoneyTransaction> { it.timestampMillis }.thenByDescending { it.id }
        )
        for (tx in sorted) {
            val zdt = Instant.ofEpochMilli(tx.timestampMillis).atZone(zone)
            val dateStr = zdt.toLocalDate().format(dateFormatter)
            val timeStr = zdt.toLocalTime().format(timeFormatter)
            val categoryStr = if (tx.type == TransactionType.TRANSFER) "" else tx.category.label
            val toWalletStr = if (tx.type == TransactionType.TRANSFER) tx.toWallet?.label.orEmpty() else ""
            val plainAmount = formatPlainDecimal(tx.amountCents, currency)
            val signedCashflow = when (tx.type) {
                TransactionType.INCOME -> plainAmount
                TransactionType.EXPENSE -> if (tx.amountCents == 0L) formatPlainDecimal(0L, currency) else "-$plainAmount"
                TransactionType.TRANSFER -> formatPlainDecimal(0L, currency)
            }
            val formattedDisplay = when (tx.type) {
                TransactionType.INCOME -> "+${CurrencyFormatter.format(tx.amountCents, currency)}"
                TransactionType.EXPENSE -> "-${CurrencyFormatter.format(tx.amountCents, currency)}"
                TransactionType.TRANSFER -> CurrencyFormatter.format(tx.amountCents, currency)
            }

            val row = listOf(
                dateStr,
                timeStr,
                tx.type.label,
                categoryStr,
                tx.wallet.label,
                toWalletStr,
                plainAmount,
                signedCashflow,
                currency.code,
                formattedDisplay,
                tx.note
            ).joinToString(",") { escapeCsvField(it) }
            sb.appendLine(row)
        }
        return sb.toString()
    }

    private fun formatPlainDecimal(amountCents: Long, currency: AppCurrency): String {
        val absCents = abs(amountCents)
        return if (currency.decimalDigits == 0) {
            ((absCents + 50L) / 100L).toString()
        } else {
            String.format(Locale.US, "%.${currency.decimalDigits}f", absCents / 100.0)
        }
    }

    private fun escapeCsvField(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (!needsQuotes) {
            value
        } else {
            buildString(value.length + 4) {
                append('"')
                for (ch in value) {
                    if (ch == '"') append("\"\"") else append(ch)
                }
                append('"')
            }
        }
    }
}
