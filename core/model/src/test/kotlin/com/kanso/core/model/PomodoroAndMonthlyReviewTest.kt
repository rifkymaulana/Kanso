package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class PomodoroAndMonthlyReviewTest {

    @Test
    fun `pomodoro calculations compute ceil seconds, mmss formatting, and session activity`() {
        assertEquals(0, PomodoroCalculations.computeRemainingSeconds(10_000L, 10_000L))
        assertEquals(0, PomodoroCalculations.computeRemainingSeconds(10_000L, 12_000L))
        assertEquals(1, PomodoroCalculations.computeRemainingSeconds(10_001L, 10_000L))
        assertEquals(61, PomodoroCalculations.computeRemainingSeconds(70_500L, 10_000L))

        assertEquals("25:00", PomodoroCalculations.formatRemainingMmSs(25 * 60))
        assertEquals("04:09", PomodoroCalculations.formatRemainingMmSs(4 * 60 + 9))
        assertEquals("00:00", PomodoroCalculations.formatRemainingMmSs(-5))

        val idle = PomodoroState()
        assertFalse(idle.isActiveOrPausedSession)
        assertTrue(idle.copy(isRunning = true, endEpochMillis = 99_999L).isActiveOrPausedSession)
        assertTrue(idle.copy(remainingSeconds = 600, isRunning = false).isActiveOrPausedSession)
        assertFalse(idle.copy(remainingSeconds = 0, isRunning = false).isActiveOrPausedSession)
    }

    @Test
    fun `monthly review aggregates cashflow, top categories, habits, health, tasks, and masking`() {
        val zone = ZoneId.of("UTC")
        val targetMonth = YearMonth.of(2026, 11)
        val today = LocalDate.of(2026, 11, 20)
        val day10Epoch = LocalDate.of(2026, 11, 10).toEpochDay()
        val novMillis = LocalDate.of(2026, 11, 10).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

        val currentTxs = listOf(
            MoneyTransaction(
                id = 1L,
                type = TransactionType.INCOME,
                amountCents = 10_000_000_00L,
                category = ExpenseCategory.SALARY,
                wallet = WalletType.BANK,
                timestampMillis = novMillis
            ),
            MoneyTransaction(
                id = 2L,
                type = TransactionType.EXPENSE,
                amountCents = 1_500_000_00L,
                category = ExpenseCategory.FOOD,
                wallet = WalletType.EWALLET,
                timestampMillis = novMillis
            ),
            MoneyTransaction(
                id = 3L,
                type = TransactionType.EXPENSE,
                amountCents = 500_000_00L,
                category = ExpenseCategory.TRANSPORT,
                wallet = WalletType.EWALLET,
                timestampMillis = novMillis
            )
        )
        val prevTxs = listOf(
            MoneyTransaction(
                id = 4L,
                type = TransactionType.EXPENSE,
                amountCents = 1_000_000_00L,
                category = ExpenseCategory.FOOD,
                wallet = WalletType.EWALLET,
                timestampMillis = novMillis - 30L * 86_400_000L
            )
        )
        val habits = listOf(
            HabitItem(id = 1L, name = "Read 20m", createdAtMillis = 1L),
            HabitItem(id = 2L, name = "Stretch", createdAtMillis = 2L)
        )
        val completions = (0L until 10L).map { HabitCompletion(habitId = 1L, epochDay = day10Epoch - it) } +
            (0L until 2L).map { HabitCompletion(habitId = 2L, epochDay = day10Epoch - it) }
        val healthLogs = listOf(
            DailyHealthLog(epochDay = day10Epoch, waterMl = 2200, sleepHours = 7.5f, mood = 5, updatedAtMillis = 1L),
            DailyHealthLog(epochDay = day10Epoch - 1L, waterMl = 1800, sleepHours = 6.5f, mood = 3, updatedAtMillis = 1L)
        )
        val tasks = listOf(
            TaskItem(id = 1L, title = "Done in Nov", isCompleted = true, createdAtMillis = 1L, completedAtMillis = novMillis),
            TaskItem(id = 2L, title = "Open", isCompleted = false, createdAtMillis = 2L, completedAtMillis = null)
        )

        val report = MonthlyReviewCalculations.buildReport(
            targetMonth = targetMonth,
            today = today,
            monthLabel = "November 2026",
            transactions = currentTxs,
            previousMonthTransactions = prevTxs,
            activeHabits = habits,
            monthCompletions = completions,
            monthHealthLogs = healthLogs,
            waterGoalMl = 2000,
            allTasks = tasks,
            zone = zone
        )

        assertEquals(10_000_000_00L, report.incomeCents)
        assertEquals(2_000_000_00L, report.expenseCents)
        assertEquals(8_000_000_00L, report.netSavingsCents)
        assertEquals(80, report.savingsRatePercent)
        assertEquals(100, report.expenseDeltaPercent)
        assertEquals(ExpenseCategory.FOOD, report.topCategories.first().category)
        assertEquals(30, report.habitCompletionPercent)
        assertEquals("Read 20m", report.topHabitName)
        assertEquals(10, report.topHabitCompletions)
        assertEquals(2000, report.avgWaterMl)
        assertEquals(1, report.waterGoalHitDays)
        assertEquals(7.0f, report.avgSleepHours!!, 0.01f)
        assertEquals(4.0f, report.avgMood!!, 0.01f)
        assertEquals(1, report.completedTasksCount)

        val masked = MonthlyReviewCalculations.formatShareableSummary(
            report = report,
            currency = AppCurrency.IDR,
            hideBalances = true
        )
        assertTrue(masked.contains("Rp ••••••"))
        assertFalse(masked.contains("10.000.000"))
    }
}
