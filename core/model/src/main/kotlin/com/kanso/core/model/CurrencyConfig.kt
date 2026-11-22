package com.kanso.core.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

@Serializable
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val decimalDigits: Int,
    val quickPresets: List<Long>
) {
    IDR(
        code = "IDR",
        symbol = "Rp",
        displayName = "Indonesian Rupiah",
        decimalDigits = 0,
        quickPresets = listOf(15_000_00L, 25_000_00L, 50_000_00L, 100_000_00L)
    ),
    USD(
        code = "USD",
        symbol = "$",
        displayName = "US Dollar",
        decimalDigits = 2,
        quickPresets = listOf(5_00L, 10_00L, 25_00L, 50_00L)
    ),
    EUR(
        code = "EUR",
        symbol = "€",
        displayName = "Euro",
        decimalDigits = 2,
        quickPresets = listOf(5_00L, 10_00L, 25_00L, 50_00L)
    ),
    SGD(
        code = "SGD",
        symbol = "S$",
        displayName = "Singapore Dollar",
        decimalDigits = 2,
        quickPresets = listOf(5_00L, 10_00L, 25_00L, 50_00L)
    ),
    MYR(
        code = "MYR",
        symbol = "RM",
        displayName = "Malaysian Ringgit",
        decimalDigits = 2,
        quickPresets = listOf(10_00L, 20_00L, 50_00L, 100_00L)
    ),
    JPY(
        code = "JPY",
        symbol = "¥",
        displayName = "Japanese Yen",
        decimalDigits = 0,
        quickPresets = listOf(500_00L, 1_000_00L, 3_000_00L, 5_000_00L)
    ),
    GBP(
        code = "GBP",
        symbol = "£",
        displayName = "British Pound",
        decimalDigits = 2,
        quickPresets = listOf(5_00L, 10_00L, 25_00L, 50_00L)
    );

    companion object {
        fun fromCode(code: String): AppCurrency {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: IDR
        }
    }
}

object CurrencyFormatter {

    fun format(
        amountCents: Long,
        currency: AppCurrency,
        showSign: Boolean = false
    ): String {
        val sign = when {
            amountCents < 0L -> "-"
            showSign && amountCents > 0L -> "+"
            else -> ""
        }
        val absCents = if (amountCents == Long.MIN_VALUE) Long.MAX_VALUE else abs(amountCents)
        val groupingSep = if (currency == AppCurrency.IDR) '.' else ','
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = groupingSep
            decimalSeparator = '.'
        }
        val formattedNumber = if (currency.decimalDigits == 0) {
            val wholeUnits = (absCents + 50L) / 100L
            DecimalFormat("#,##0", symbols).format(wholeUnits)
        } else {
            val value = BigDecimal.valueOf(absCents, 2)
            DecimalFormat("#,##0.00", symbols).format(value)
        }
        val spacer = if (currency == AppCurrency.IDR || currency == AppCurrency.MYR) " " else ""
        return "$sign${currency.symbol}$spacer$formattedNumber"
    }

    fun formatCompactInput(amountCents: Long, currency: AppCurrency): String {
        val absCents = abs(amountCents)
        return if (currency.decimalDigits == 0) {
            (absCents / 100L).toString()
        } else {
            BigDecimal.valueOf(absCents, 2).setScale(2, RoundingMode.HALF_UP).toPlainString()
        }
    }

    fun parseInputToCents(raw: String, currency: AppCurrency): Long? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("-")) return null
        val cleaned = trimmed
            .replace(currency.symbol, "", ignoreCase = true)
            .replace(currency.code, "", ignoreCase = true)
            .replace(" ", "")

        if (cleaned.isEmpty()) return null

        return if (currency.decimalDigits == 0) {
            val digitsOnly = cleaned.replace(".", "").replace(",", "")
            if (digitsOnly.isEmpty() || !digitsOnly.all { it.isDigit() }) return null
            val units = digitsOnly.toLongOrNull() ?: return null
            if (units < 0L || units > Long.MAX_VALUE / 100L) return null
            units * 100L
        } else {
            val normalized = normalizeDecimalString(cleaned) ?: return null
            val decimal = runCatching { BigDecimal(normalized) }.getOrNull() ?: return null
            if (decimal < BigDecimal.ZERO) return null
            val centsBig = decimal.setScale(2, RoundingMode.HALF_UP).movePointRight(2)
            runCatching { centsBig.longValueExact() }.getOrNull()
        }
    }

    private fun normalizeDecimalString(input: String): String? {
        val lastDot = input.lastIndexOf('.')
        val lastComma = input.lastIndexOf(',')
        return when {
            lastDot >= 0 && lastComma >= 0 -> {
                if (lastDot > lastComma) {
                    input.replace(",", "")
                } else {
                    input.replace(".", "").replace(',', '.')
                }
            }
            lastComma >= 0 -> {
                val afterComma = input.length - lastComma - 1
                if (afterComma in 1..2 && input.count { it == ',' } == 1) {
                    input.replace(',', '.')
                } else {
                    input.replace(",", "")
                }
            }
            lastDot >= 0 -> {
                if (input.count { it == '.' } > 1) {
                    input.replace(".", "")
                } else {
                    input
                }
            }
            else -> input
        }
    }
}
