package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Milestone2CalculationsTest {

    @Test
    fun `habit currentStreak handles completed today, completed yesterday, and broken streaks`() {
        val today = 20_000L
        assertEquals(3, HabitCalculations.currentStreak(setOf(19_998L, 19_999L, 20_000L), today))
        assertEquals(2, HabitCalculations.currentStreak(setOf(19_998L, 19_999L), today))
        assertEquals(0, HabitCalculations.currentStreak(setOf(19_995L, 19_996L), today))
        assertEquals(0, HabitCalculations.currentStreak(emptySet(), today))
    }

    @Test
    fun `habit longestStreak and 30 day grid calculate accurately`() {
        val days = setOf(10L, 11L, 12L, 20L, 21L, 22L, 23L, 24L)
        assertEquals(5, HabitCalculations.longestStreak(days))

        val grid = HabitCalculations.last30DaysGrid(setOf(19_990L, 20_000L), 20_000L)
        assertEquals(30, grid.size)
        assertEquals(20_000L - 29L, grid.first().first)
        assertEquals(20_000L, grid.last().first)
        assertTrue(grid.last().second)
        assertEquals(2f / 30f, HabitCalculations.completionRateLast30Days(setOf(19_990L, 20_000L), 20_000L), 0.0001f)
    }

    @Test
    fun `life admin expiry urgency categorizes expired, critical, upcoming, and safe`() {
        val today = 20_000L
        val base = ExpiryItem(
            id = 1L,
            title = "Passport",
            category = ExpiryCategory.PASSPORT,
            expiryEpochDay = today,
            remindDaysBefore = 30,
            updatedAtMillis = 1L
        )
        assertEquals(ExpiryUrgency.EXPIRED, LifeAdminCalculations.urgency(base.copy(expiryEpochDay = today - 1L), today))
        assertEquals(ExpiryUrgency.CRITICAL_7_DAYS, LifeAdminCalculations.urgency(base.copy(expiryEpochDay = today + 5L), today))
        assertEquals(ExpiryUrgency.UPCOMING, LifeAdminCalculations.urgency(base.copy(expiryEpochDay = today + 20L), today))
        assertEquals(ExpiryUrgency.SAFE, LifeAdminCalculations.urgency(base.copy(expiryEpochDay = today + 60L), today))
        assertTrue(LifeAdminCalculations.needsAttention(base.copy(expiryEpochDay = today + 10L), today))
        assertFalse(LifeAdminCalculations.needsAttention(base.copy(expiryEpochDay = today + 60L), today))
    }

    @Test
    fun `unit converter converts length, weight, temperature, and data accurately`() {
        val km = UnitConverter.unitsFor(UnitDomain.LENGTH).first { it.id == "km" }
        val m = UnitConverter.unitsFor(UnitDomain.LENGTH).first { it.id == "m" }
        assertEquals(2500.0, UnitConverter.convert(2.5, km, m), 0.0001)

        val c = UnitConverter.unitsFor(UnitDomain.TEMPERATURE).first { it.id == "c" }
        val f = UnitConverter.unitsFor(UnitDomain.TEMPERATURE).first { it.id == "f" }
        val k = UnitConverter.unitsFor(UnitDomain.TEMPERATURE).first { it.id == "k" }
        assertEquals(212.0, UnitConverter.convert(100.0, c, f), 0.0001)
        assertEquals(0.0, UnitConverter.convert(32.0, f, c), 0.0001)
        assertEquals(273.15, UnitConverter.convert(0.0, c, k), 0.0001)

        val gb = UnitConverter.unitsFor(UnitDomain.DATA).first { it.id == "gb" }
        val mb = UnitConverter.unitsFor(UnitDomain.DATA).first { it.id == "mb" }
        assertEquals(1024.0, UnitConverter.convert(1.0, gb, mb), 0.0001)
    }

    @Test
    fun `offline currency converter respects default and custom rates`() {
        val usdToIdr = OfflineCurrencyConverter.convert(10.0, AppCurrency.USD, AppCurrency.IDR)
        assertEquals(158_000.0, usdToIdr, 0.01)

        val custom = OfflineCurrencyConverter.convert(
            amount = 10.0,
            from = AppCurrency.USD,
            to = AppCurrency.IDR,
            customOverrides = mapOf("IDR" to 16_000.0)
        )
        assertEquals(160_000.0, custom, 0.01)
    }

    @Test
    fun `countdown delta formatting handles past, today, and future`() {
        assertEquals("Today", CountdownCalculations.formatDelta(0L))
        assertEquals("Tomorrow", CountdownCalculations.formatDelta(1L))
        assertEquals("In 12 days", CountdownCalculations.formatDelta(12L))
        assertEquals("1 day ago", CountdownCalculations.formatDelta(-1L))
        assertEquals("5 days ago", CountdownCalculations.formatDelta(-5L))
    }

    @Test
    fun `health trend series aligns chronological days and computes weight delta`() {
        val today = 20_000L
        val logs = listOf(
            DailyHealthLog(epochDay = today - 10L, waterMl = 1500, sleepHours = 7.0f, weightKg = 70.5f, mood = 3, updatedAtMillis = 1L),
            DailyHealthLog(epochDay = today - 2L, waterMl = 2250, sleepHours = 8.0f, weightKg = null, mood = 4, updatedAtMillis = 2L),
            DailyHealthLog(epochDay = today, waterMl = 2500, sleepHours = null, weightKg = 69.8f, mood = 5, updatedAtMillis = 3L)
        )

        val series14 = HabitCalculations.buildHealthTrendSeries(logs, todayEpochDay = today, days = 14)
        assertEquals(14, series14.size)
        assertEquals(today - 13L, series14.first().epochDay)
        assertEquals(0, series14.first().waterMl)
        assertFalse(series14.first().hasEntry)

        assertTrue(series14.last().hasEntry)
        assertEquals(2500, series14.last().waterMl)
        assertEquals(-0.7f, HabitCalculations.computeWeightDelta(series14) ?: 0f, 0.001f)

        val series30 = HabitCalculations.buildHealthTrendSeries(logs, todayEpochDay = today, days = 30)
        assertEquals(30, series30.size)

        val singleWeight = HabitCalculations.buildHealthTrendSeries(
            listOf(DailyHealthLog(epochDay = today, weightKg = 70.0f, updatedAtMillis = 1L)),
            todayEpochDay = today,
            days = 14
        )
        assertNull(HabitCalculations.computeWeightDelta(singleWeight))
    }
}
