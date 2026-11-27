package com.kanso.core.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class CountdownItem(
    val id: Long = 0L,
    val title: String,
    val targetEpochDay: Long,
    val note: String = "",
    val createdAtMillis: Long
)

enum class UnitDomain(val label: String) {
    LENGTH("Length"),
    WEIGHT("Weight"),
    TEMPERATURE("Temperature"),
    VOLUME("Volume"),
    DATA("Data")
}

data class UnitSpec(
    val id: String,
    val symbol: String,
    val label: String,
    val domain: UnitDomain,
    val toBaseFactor: Double
)

object UnitConverter {

    private val allUnits: List<UnitSpec> = listOf(
        // Length (base = meter)
        UnitSpec("mm", "mm", "Millimeter", UnitDomain.LENGTH, 0.001),
        UnitSpec("cm", "cm", "Centimeter", UnitDomain.LENGTH, 0.01),
        UnitSpec("m", "m", "Meter", UnitDomain.LENGTH, 1.0),
        UnitSpec("km", "km", "Kilometer", UnitDomain.LENGTH, 1000.0),
        UnitSpec("in", "in", "Inch", UnitDomain.LENGTH, 0.0254),
        UnitSpec("ft", "ft", "Foot", UnitDomain.LENGTH, 0.3048),
        UnitSpec("mi", "mi", "Mile", UnitDomain.LENGTH, 1609.344),

        // Weight (base = gram)
        UnitSpec("g", "g", "Gram", UnitDomain.WEIGHT, 1.0),
        UnitSpec("kg", "kg", "Kilogram", UnitDomain.WEIGHT, 1000.0),
        UnitSpec("oz", "oz", "Ounce", UnitDomain.WEIGHT, 28.349523125),
        UnitSpec("lb", "lb", "Pound", UnitDomain.WEIGHT, 453.59237),

        // Temperature (special-cased)
        UnitSpec("c", "°C", "Celsius", UnitDomain.TEMPERATURE, 1.0),
        UnitSpec("f", "°F", "Fahrenheit", UnitDomain.TEMPERATURE, 1.0),
        UnitSpec("k", "K", "Kelvin", UnitDomain.TEMPERATURE, 1.0),

        // Volume (base = milliliter)
        UnitSpec("ml", "ml", "Milliliter", UnitDomain.VOLUME, 1.0),
        UnitSpec("l", "L", "Liter", UnitDomain.VOLUME, 1000.0),
        UnitSpec("cup", "cup", "US Cup", UnitDomain.VOLUME, 236.5882365),
        UnitSpec("gal", "gal", "US Gallon", UnitDomain.VOLUME, 3785.411784),

        // Data (base = KB, decimal 1000 or binary 1024 — standard binary 1024)
        UnitSpec("kb", "KB", "Kilobyte", UnitDomain.DATA, 1.0),
        UnitSpec("mb", "MB", "Megabyte", UnitDomain.DATA, 1024.0),
        UnitSpec("gb", "GB", "Gigabyte", UnitDomain.DATA, 1024.0 * 1024.0),
        UnitSpec("tb", "TB", "Terabyte", UnitDomain.DATA, 1024.0 * 1024.0 * 1024.0)
    )

    fun unitsFor(domain: UnitDomain): List<UnitSpec> {
        return allUnits.filter { it.domain == domain }
    }

    fun convert(value: Double, from: UnitSpec, to: UnitSpec): Double {
        if (from.id == to.id) return value
        if (from.domain != to.domain) return 0.0

        if (from.domain == UnitDomain.TEMPERATURE) {
            val celsius = when (from.id) {
                "c" -> value
                "f" -> (value - 32.0) * 5.0 / 9.0
                "k" -> value - 273.15
                else -> value
            }
            return when (to.id) {
                "c" -> celsius
                "f" -> celsius * 9.0 / 5.0 + 32.0
                "k" -> celsius + 273.15
                else -> celsius
            }
        }

        val base = value * from.toBaseFactor
        return base / to.toBaseFactor
    }
}

object OfflineCurrencyConverter {
    val defaultRatesVsUsd: Map<String, Double> = mapOf(
        "USD" to 1.0,
        "IDR" to 15800.0,
        "EUR" to 0.92,
        "SGD" to 1.34,
        "MYR" to 4.45,
        "JPY" to 154.0,
        "GBP" to 0.79
    )

    fun effectiveRates(customOverrides: Map<String, Double>): Map<String, Double> {
        val merged = defaultRatesVsUsd.toMutableMap()
        for ((code, rate) in customOverrides) {
            if (rate > 0.0) {
                merged[code.uppercase()] = rate
            }
        }
        return merged
    }

    fun convert(
        amount: Double,
        from: AppCurrency,
        to: AppCurrency,
        customOverrides: Map<String, Double> = emptyMap()
    ): Double {
        if (from == to) return amount
        val rates = effectiveRates(customOverrides)
        val fromRate = rates[from.code] ?: 1.0
        val toRate = rates[to.code] ?: 1.0
        val usdAmount = amount / fromRate
        return usdAmount * toRate
    }
}

object CountdownCalculations {
    fun daysRemaining(targetEpochDay: Long, todayEpochDay: Long): Long {
        return targetEpochDay - todayEpochDay
    }

    fun upcomingCountdowns(
        items: List<CountdownItem>,
        todayEpochDay: Long,
        withinDays: Int = 7
    ): List<CountdownItem> {
        return items
            .filter { daysRemaining(it.targetEpochDay, todayEpochDay) in 0L..withinDays.toLong() }
            .sortedWith(compareBy<CountdownItem> { it.targetEpochDay }.thenBy { it.id })
    }

    fun formatDelta(daysRemaining: Long): String {
        return when {
            daysRemaining == 0L -> "Today"
            daysRemaining == 1L -> "Tomorrow"
            daysRemaining > 1L -> "In $daysRemaining days"
            daysRemaining == -1L -> "1 day ago"
            else -> "${-daysRemaining} days ago"
        }
    }
}

enum class PomodoroMode(val label: String, val durationSeconds: Int) {
    FOCUS("Focus 25m", 25 * 60),
    SHORT_BREAK("Short 5m", 5 * 60),
    LONG_BREAK("Long 15m", 15 * 60)
}

data class PomodoroState(
    val mode: PomodoroMode = PomodoroMode.FOCUS,
    val remainingSeconds: Int = PomodoroMode.FOCUS.durationSeconds,
    val totalSeconds: Int = PomodoroMode.FOCUS.durationSeconds,
    val isRunning: Boolean = false,
    val completedFocusSessions: Int = 0,
    val endEpochMillis: Long? = null
) {
    val isActiveOrPausedSession: Boolean
        get() = isRunning || (remainingSeconds in 1 until totalSeconds)
}

object PomodoroCalculations {
    fun computeRemainingSeconds(endEpochMillis: Long, nowEpochMillis: Long): Int {
        val diff = endEpochMillis - nowEpochMillis
        if (diff <= 0L) return 0
        return ((diff + 999L) / 1000L).toInt()
    }

    fun formatRemainingMmSs(remainingSeconds: Int): String {
        val safe = remainingSeconds.coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
    }
}
