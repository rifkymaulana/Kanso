package com.kanso.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

data class CategorySpendHighlight(
    val category: ExpenseCategory,
    val amountCents: Long,
    val shareRatio: Float
)

data class MonthlyReviewReport(
    val yearMonth: String,
    val monthLabel: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val netSavingsCents: Long,
    val savingsRatePercent: Int?,
    val previousExpenseCents: Long,
    val expenseDeltaPercent: Int?,
    val topCategories: List<CategorySpendHighlight>,
    val transactionCount: Int,
    val activeHabitsCount: Int,
    val habitCompletionPercent: Int?,
    val topHabitName: String?,
    val topHabitCompletions: Int,
    val avgWaterMl: Int,
    val waterGoalHitDays: Int,
    val elapsedDaysInMonth: Int,
    val avgSleepHours: Float?,
    val avgMood: Float?,
    val completedTasksCount: Int
)

object MonthlyReviewCalculations {

    fun buildReport(
        targetMonth: YearMonth,
        today: LocalDate,
        monthLabel: String,
        transactions: List<MoneyTransaction>,
        previousMonthTransactions: List<MoneyTransaction>,
        activeHabits: List<HabitItem>,
        monthCompletions: List<HabitCompletion>,
        monthHealthLogs: List<DailyHealthLog>,
        waterGoalMl: Int,
        allTasks: List<TaskItem>,
        zone: ZoneId = ZoneId.systemDefault()
    ): MonthlyReviewReport {
        val summary = MoneyCalculations.summarize(transactions)
        val prevSummary = MoneyCalculations.summarize(previousMonthTransactions)

        val savingsRatePercent = if (summary.incomeCents > 0L) {
            (((summary.incomeCents - summary.expenseCents).toDouble() / summary.incomeCents.toDouble()) * 100.0)
                .roundToInt()
        } else {
            null
        }

        val expenseDeltaPercent = if (prevSummary.expenseCents > 0L) {
            (((summary.expenseCents - prevSummary.expenseCents).toDouble() / prevSummary.expenseCents.toDouble()) * 100.0)
                .roundToInt()
        } else {
            null
        }

        val topCategories = summary.byCategory.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { (cat, amount) ->
                val share = if (summary.expenseCents > 0L) {
                    (amount.toFloat() / summary.expenseCents.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                CategorySpendHighlight(category = cat, amountCents = amount, shareRatio = share)
            }

        val currentMonth = YearMonth.from(today)
        val elapsedDaysInMonth = when {
            targetMonth.isBefore(currentMonth) -> targetMonth.lengthOfMonth()
            targetMonth == currentMonth -> today.dayOfMonth.coerceAtLeast(1)
            else -> 0
        }

        val monthStartEpoch = targetMonth.atDay(1).toEpochDay()
        val monthEndEpoch = targetMonth.atEndOfMonth().toEpochDay()

        val activeHabitIds = activeHabits.map { it.id }.toSet()
        val filteredCompletions = monthCompletions.filter {
            it.habitId in activeHabitIds && it.epochDay in monthStartEpoch..monthEndEpoch
        }

        val habitCompletionPercent = if (activeHabits.isNotEmpty() && elapsedDaysInMonth > 0) {
            val possible = activeHabits.size * elapsedDaysInMonth
            ((filteredCompletions.size.toDouble() / possible.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
        } else {
            null
        }

        val completionCountsByHabit = filteredCompletions.groupingBy { it.habitId }.eachCount()
        val bestHabitEntry = completionCountsByHabit.maxByOrNull { it.value }
        val topHabit = bestHabitEntry?.let { entry ->
            activeHabits.firstOrNull { it.id == entry.key }?.let { habit ->
                habit.name to entry.value
            }
        }

        val filteredHealthLogs = monthHealthLogs.filter { it.epochDay in monthStartEpoch..monthEndEpoch }
        val avgWaterMl = if (filteredHealthLogs.isNotEmpty()) {
            filteredHealthLogs.map { it.waterMl }.average().roundToInt()
        } else {
            0
        }
        val safeWaterGoal = waterGoalMl.coerceAtLeast(250)
        val waterGoalHitDays = filteredHealthLogs.count { it.waterMl >= safeWaterGoal }

        val sleepEntries = filteredHealthLogs.mapNotNull { it.sleepHours }
        val avgSleepHours = if (sleepEntries.isNotEmpty()) {
            sleepEntries.average().toFloat()
        } else {
            null
        }

        val moodEntries = filteredHealthLogs.mapNotNull { it.mood }
        val avgMood = if (moodEntries.isNotEmpty()) {
            moodEntries.average().toFloat()
        } else {
            null
        }

        val completedTasksCount = allTasks.count { task ->
            val completedAt = task.completedAtMillis
            task.isCompleted && completedAt != null &&
                YearMonth.from(Instant.ofEpochMilli(completedAt).atZone(zone).toLocalDate()) == targetMonth
        }

        return MonthlyReviewReport(
            yearMonth = targetMonth.toString(),
            monthLabel = monthLabel,
            incomeCents = summary.incomeCents,
            expenseCents = summary.expenseCents,
            netSavingsCents = summary.netCents,
            savingsRatePercent = savingsRatePercent,
            previousExpenseCents = prevSummary.expenseCents,
            expenseDeltaPercent = expenseDeltaPercent,
            topCategories = topCategories,
            transactionCount = transactions.size,
            activeHabitsCount = activeHabits.size,
            habitCompletionPercent = habitCompletionPercent,
            topHabitName = topHabit?.first,
            topHabitCompletions = topHabit?.second ?: 0,
            avgWaterMl = avgWaterMl,
            waterGoalHitDays = waterGoalHitDays,
            elapsedDaysInMonth = elapsedDaysInMonth,
            avgSleepHours = avgSleepHours,
            avgMood = avgMood,
            completedTasksCount = completedTasksCount
        )
    }

    fun formatShareableSummary(
        report: MonthlyReviewReport,
        currency: AppCurrency,
        hideBalances: Boolean = false
    ): String {
        return buildString {
            appendLine("Kanso Monthly Review — ${report.monthLabel}")
            appendLine()
            appendLine("Money (${report.transactionCount} transactions)")
            appendLine("- Income: ${CurrencyFormatter.formatOrMask(report.incomeCents, currency, hidden = hideBalances)}")
            appendLine("- Spent: ${CurrencyFormatter.formatOrMask(report.expenseCents, currency, hidden = hideBalances)}")
            val netText = CurrencyFormatter.formatOrMask(report.netSavingsCents, currency, hidden = hideBalances, showSign = true)
            val rateText = report.savingsRatePercent?.let { " ($it% savings rate)" }.orEmpty()
            appendLine("- Net: $netText$rateText")
            report.expenseDeltaPercent?.let { delta ->
                val sign = if (delta > 0) "+" else ""
                appendLine("- vs Previous Month: $sign$delta% spend")
            }
            if (report.topCategories.isNotEmpty()) {
                appendLine("Top Categories:")
                report.topCategories.forEachIndexed { idx, cat ->
                    val pct = (cat.shareRatio * 100f).roundToInt()
                    val amt = CurrencyFormatter.formatOrMask(cat.amountCents, currency, hidden = hideBalances)
                    appendLine("  ${idx + 1}. ${cat.category.label}: $amt ($pct%)")
                }
            }
            appendLine()
            appendLine("Habits & Health")
            report.habitCompletionPercent?.let { pct ->
                append("- Consistency: $pct%")
                report.topHabitName?.let { append(" · Top: $it (${report.topHabitCompletions}x)") }
                appendLine()
            }
            appendLine("- Avg Water: ${report.avgWaterMl} ml · Goal hit: ${report.waterGoalHitDays} days")
            report.avgSleepHours?.let {
                appendLine(String.format(java.util.Locale.US, "- Avg Sleep: %.1f h", it))
            }
            report.avgMood?.let {
                appendLine(String.format(java.util.Locale.US, "- Avg Mood: %.1f / 5", it))
            }
            appendLine("- Completed Tasks: ${report.completedTasksCount}")
        }.trim()
    }
}
