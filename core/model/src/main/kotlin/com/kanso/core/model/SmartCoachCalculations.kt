package com.kanso.core.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class BudgetAnchorSource {
    MONTHLY_BUDGET,
    MONTHLY_INCOME,
    NONE
}

enum class SpendPaceStatus(val badgeLabel: String) {
    UNDER_PACE("Ahead of Goal"),
    ON_TRACK("On Track"),
    WATCH_PACE("Watch Pace"),
    OVER_BUDGET("Over Cap"),
    NO_ANCHOR("Set Budget"),
    PAST_MONTH("Recap"),
    FUTURE_MONTH("Planned")
}

enum class CoachInsightTone {
    POSITIVE,
    NEUTRAL,
    WARNING,
    CRITICAL
}

enum class CoachActionTarget {
    OPEN_MONEY_BUDGETS,
    ADD_WATER_250,
    OPEN_HABITS
}

data class CoachInsight(
    val id: String,
    val title: String,
    val body: String,
    val tone: CoachInsightTone,
    val actionLabel: String? = null,
    val actionTarget: CoachActionTarget? = null
)

data class SafeToSpendReport(
    val targetMonth: YearMonth,
    val isCurrentMonth: Boolean,
    val isPastMonth: Boolean,
    val anchorSource: BudgetAnchorSource,
    val anchorAmountCents: Long,
    val monthExpenseCents: Long,
    val todayExpenseCents: Long,
    val unpaidBillsCents: Long,
    val unpaidBillsCount: Int,
    val flexibleRemainingMonthCents: Long,
    val dailySafeAllowanceCents: Long,
    val todayRemainingAllowanceCents: Long,
    val averageDailySpendCents: Long,
    val projectedMonthEndCents: Long,
    val daysElapsed: Int,
    val daysRemainingIncludingToday: Int,
    val paceStatus: SpendPaceStatus
) {
    val hasAnchor: Boolean
        get() = anchorSource != BudgetAnchorSource.NONE && anchorAmountCents > 0L
}

object SmartCoachCalculations {

    fun computeSafeToSpend(
        targetMonth: YearMonth,
        today: LocalDate,
        monthlyBudgetCents: Long,
        monthIncomeCents: Long,
        monthExpenseCents: Long,
        todayExpenseCents: Long,
        bills: List<RecurringBill>
    ): SafeToSpendReport {
        val currentMonth = YearMonth.from(today)
        val isCurrentMonth = targetMonth == currentMonth
        val isPastMonth = targetMonth.isBefore(currentMonth)
        val daysInMonth = targetMonth.lengthOfMonth()

        val daysElapsed = when {
            isCurrentMonth -> today.dayOfMonth
            isPastMonth -> daysInMonth
            else -> 0
        }
        val daysRemainingIncludingToday = when {
            isCurrentMonth -> (daysInMonth - today.dayOfMonth + 1).coerceAtLeast(1)
            isPastMonth -> 0
            else -> daysInMonth
        }

        val (anchorSource, anchorAmountCents) = when {
            monthlyBudgetCents > 0L -> BudgetAnchorSource.MONTHLY_BUDGET to monthlyBudgetCents
            monthIncomeCents > 0L -> BudgetAnchorSource.MONTHLY_INCOME to monthIncomeCents
            else -> BudgetAnchorSource.NONE to 0L
        }

        val activeBills = bills.filter { it.isActive }
        var unpaidBillsCount = 0
        var unpaidBillsCents = 0L
        var rawPaidFixedCents = 0L

        if (!isPastMonth) {
            for (bill in activeBills) {
                when (bill.frequency) {
                    BillFrequency.MONTHLY -> {
                        if (MoneyCalculations.isBillPaidForMonth(bill, targetMonth)) {
                            rawPaidFixedCents += bill.amountCents
                        } else {
                            unpaidBillsCount++
                            unpaidBillsCents += bill.amountCents
                        }
                    }
                    BillFrequency.YEARLY -> {
                        val paidYm = MoneyCalculations.parseLastPaidYearMonth(bill.lastPaidYearMonth)
                        if (MoneyCalculations.isBillPaidForMonth(bill, targetMonth)) {
                            if (paidYm == targetMonth) {
                                rawPaidFixedCents += bill.amountCents
                            }
                        } else {
                            val isAnnualDueInTargetMonth = paidYm == null ||
                                paidYm.month == targetMonth.month ||
                                ChronoUnit.MONTHS.between(paidYm, targetMonth) >= 12L
                            if (isAnnualDueInTargetMonth) {
                                unpaidBillsCount++
                                unpaidBillsCents += bill.amountCents
                            }
                        }
                    }
                    BillFrequency.WEEKLY -> {
                        val totalRemainingSlots = if (isCurrentMonth) {
                            ((daysRemainingIncludingToday + 6) / 7).coerceAtLeast(1)
                        } else {
                            (daysInMonth + 6) / 7
                        }
                        val paidCurrentCycle = isCurrentMonth && MoneyCalculations.isBillPaidForCycle(bill, today)
                        val unpaidSlots = if (paidCurrentCycle) {
                            (totalRemainingSlots - 1).coerceAtLeast(0)
                        } else {
                            totalRemainingSlots
                        }
                        if (unpaidSlots > 0) {
                            unpaidBillsCount++
                            unpaidBillsCents += unpaidSlots * bill.amountCents
                        }
                        if (paidCurrentCycle) {
                            rawPaidFixedCents += bill.amountCents
                        }
                    }
                }
            }
        }
        val paidMonthlyBillsCents = rawPaidFixedCents
            .coerceAtMost(monthExpenseCents.coerceAtLeast(0L))

        val effectiveTodayExpense = if (isCurrentMonth) todayExpenseCents.coerceAtLeast(0L) else 0L
        val spentBeforeTodayCents = (monthExpenseCents - effectiveTodayExpense).coerceAtLeast(0L)
        val flexibleBeforeTodayCents = anchorAmountCents - spentBeforeTodayCents - unpaidBillsCents
        val flexibleRemainingMonthCents = if (anchorAmountCents > 0L) {
            anchorAmountCents - monthExpenseCents - unpaidBillsCents
        } else {
            0L
        }
        val dailySafeAllowanceCents = if (anchorAmountCents > 0L && daysRemainingIncludingToday > 0) {
            flexibleBeforeTodayCents.coerceAtLeast(0L) / daysRemainingIncludingToday
        } else {
            0L
        }
        val todayRemainingAllowanceCents = if (isCurrentMonth && anchorAmountCents > 0L) {
            dailySafeAllowanceCents - effectiveTodayExpense
        } else {
            dailySafeAllowanceCents
        }

        val averageDailySpendCents = if (daysElapsed > 0) {
            monthExpenseCents / daysElapsed
        } else {
            0L
        }
        val variableSpendSoFar = (monthExpenseCents - paidMonthlyBillsCents).coerceAtLeast(0L)
        val projectedMonthEndCents = when {
            isPastMonth -> monthExpenseCents
            !isCurrentMonth -> unpaidBillsCents
            daysElapsed >= 3 -> {
                val projectedVariable = (
                    (variableSpendSoFar.toDouble() / daysElapsed.toDouble()) * daysInMonth.toDouble()
                    ).roundToLong()
                paidMonthlyBillsCents + unpaidBillsCents + projectedVariable
            }
            else -> monthExpenseCents + unpaidBillsCents
        }

        val paceStatus = when {
            isPastMonth -> SpendPaceStatus.PAST_MONTH
            !isCurrentMonth -> SpendPaceStatus.FUTURE_MONTH
            anchorAmountCents <= 0L -> SpendPaceStatus.NO_ANCHOR
            monthExpenseCents > anchorAmountCents ||
                (monthExpenseCents + unpaidBillsCents) > anchorAmountCents -> SpendPaceStatus.OVER_BUDGET
            daysElapsed >= 3 &&
                projectedMonthEndCents.toDouble() > anchorAmountCents.toDouble() * 1.05 -> SpendPaceStatus.WATCH_PACE
            daysElapsed >= 5 &&
                projectedMonthEndCents.toDouble() <= anchorAmountCents.toDouble() * 0.85 -> SpendPaceStatus.UNDER_PACE
            else -> SpendPaceStatus.ON_TRACK
        }

        return SafeToSpendReport(
            targetMonth = targetMonth,
            isCurrentMonth = isCurrentMonth,
            isPastMonth = isPastMonth,
            anchorSource = anchorSource,
            anchorAmountCents = anchorAmountCents,
            monthExpenseCents = monthExpenseCents,
            todayExpenseCents = effectiveTodayExpense,
            unpaidBillsCents = unpaidBillsCents,
            unpaidBillsCount = unpaidBillsCount,
            flexibleRemainingMonthCents = flexibleRemainingMonthCents,
            dailySafeAllowanceCents = dailySafeAllowanceCents,
            todayRemainingAllowanceCents = todayRemainingAllowanceCents,
            averageDailySpendCents = averageDailySpendCents,
            projectedMonthEndCents = projectedMonthEndCents,
            daysElapsed = daysElapsed,
            daysRemainingIncludingToday = daysRemainingIncludingToday,
            paceStatus = paceStatus
        )
    }

    fun buildMoneyInsights(
        report: SafeToSpendReport,
        currency: AppCurrency,
        currentSummary: MoneyCalculations.MonthlySummary,
        previousSummary: MoneyCalculations.MonthlySummary,
        budgetStatuses: List<MoneyCalculations.BudgetStatus>,
        maxItems: Int = 3,
        hideAmounts: Boolean = false
    ): List<CoachInsight> {
        val insights = mutableListOf<CoachInsight>()
        fun fmt(cents: Long): String = CurrencyFormatter.formatOrMask(cents, currency, hidden = hideAmounts)

        if (report.isCurrentMonth && report.hasAnchor) {
            when (report.paceStatus) {
                SpendPaceStatus.OVER_BUDGET -> {
                    val overBy = (report.monthExpenseCents + report.unpaidBillsCents - report.anchorAmountCents)
                        .coerceAtLeast(0L)
                    insights += CoachInsight(
                        id = "pace_over_budget",
                        title = "Over monthly target by ${fmt(overBy)}",
                        body = "Pause discretionary spend or adjust your monthly cap.",
                        tone = CoachInsightTone.CRITICAL,
                        actionLabel = "Adjust Cap",
                        actionTarget = CoachActionTarget.OPEN_MONEY_BUDGETS
                    )
                }

                SpendPaceStatus.WATCH_PACE -> {
                    val projectedOver = (report.projectedMonthEndCents - report.anchorAmountCents)
                        .coerceAtLeast(0L)
                    insights += CoachInsight(
                        id = "pace_watch",
                        title = "Projected +${fmt(projectedOver)} over cap",
                        body = "Aim for ${fmt(report.dailySafeAllowanceCents)}/day over the next ${report.daysRemainingIncludingToday} days.",
                        tone = CoachInsightTone.WARNING
                    )
                }

                SpendPaceStatus.UNDER_PACE -> {
                    val projectedSaved = (report.anchorAmountCents - report.projectedMonthEndCents)
                        .coerceAtLeast(0L)
                    insights += CoachInsight(
                        id = "pace_under",
                        title = "On pace to save ${fmt(projectedSaved)} under cap",
                        body = "Projected month-end spend is ${fmt(report.projectedMonthEndCents)}.",
                        tone = CoachInsightTone.POSITIVE
                    )
                }

                else -> Unit
            }
        }

        if (report.isCurrentMonth) {
            val overCat = budgetStatuses.firstOrNull { it.isOverBudget }
            if (overCat != null) {
                insights += CoachInsight(
                    id = "cat_over_${overCat.category.name}",
                    title = "${overCat.category.label} exceeded limit by ${fmt(overCat.spentCents - overCat.limitCents)}",
                    body = "${fmt(overCat.spentCents)} spent vs ${fmt(overCat.limitCents)} cap.",
                    tone = CoachInsightTone.CRITICAL
                )
            } else {
                val warnCat = budgetStatuses.firstOrNull { it.isWarning }
                if (warnCat != null) {
                    val pct = (warnCat.ratio * 100f).roundToInt()
                    val remaining = (warnCat.limitCents - warnCat.spentCents).coerceAtLeast(0L)
                    insights += CoachInsight(
                        id = "cat_warn_${warnCat.category.name}",
                        title = "${warnCat.category.label} at $pct% of limit",
                        body = "${fmt(remaining)} remaining for ${report.daysRemainingIncludingToday}d.",
                        tone = CoachInsightTone.WARNING
                    )
                }
            }
        }

        val momSpike = currentSummary.byCategory.entries
            .mapNotNull { (cat, curr) ->
                val prev = previousSummary.byCategory[cat] ?: 0L
                if (prev > 0L && curr.toDouble() >= prev.toDouble() * 1.20) {
                    Triple(cat, curr, prev)
                } else {
                    null
                }
            }
            .maxByOrNull { (_, curr, prev) -> curr - prev }

        if (momSpike != null) {
            val (cat, curr, prev) = momSpike
            val pct = (((curr - prev).toDouble() / prev.toDouble()) * 100.0).roundToInt()
            insights += CoachInsight(
                id = "mom_spike_${cat.name}",
                title = "${cat.label} up +$pct% vs last month",
                body = "${fmt(curr)} logged vs ${fmt(prev)} last month.",
                tone = CoachInsightTone.WARNING
            )
        } else if (report.isPastMonth && previousSummary.expenseCents > 0L && currentSummary.expenseCents > 0L) {
            val delta = currentSummary.expenseCents - previousSummary.expenseCents
            val pct = ((abs(delta).toDouble() / previousSummary.expenseCents.toDouble()) * 100.0).roundToInt()
            if (delta < 0L && pct >= 5) {
                insights += CoachInsight(
                    id = "mom_saved",
                    title = "Spent $pct% less than prior month",
                    body = "Saved ${fmt(-delta)} compared to ${fmt(previousSummary.expenseCents)}.",
                    tone = CoachInsightTone.POSITIVE
                )
            } else if (delta > 0L && pct >= 10) {
                insights += CoachInsight(
                    id = "mom_higher",
                    title = "Spent +$pct% more than prior month",
                    body = "Up ${fmt(delta)} compared to ${fmt(previousSummary.expenseCents)}.",
                    tone = CoachInsightTone.NEUTRAL
                )
            }
        }

        if (report.isCurrentMonth && report.unpaidBillsCount > 0 && report.hasAnchor) {
            val billWord = if (report.unpaidBillsCount == 1) "1 unpaid bill" else "${report.unpaidBillsCount} unpaid bills"
            insights += CoachInsight(
                id = "bills_reserved",
                title = "${fmt(report.unpaidBillsCents)} reserved for $billWord",
                body = "Already deducted from your Safe-to-Spend allowance.",
                tone = CoachInsightTone.NEUTRAL
            )
        }

        if (currentSummary.incomeCents > 0L && currentSummary.expenseCents > 0L) {
            val savingsPct = (
                ((currentSummary.incomeCents - currentSummary.expenseCents).toDouble() /
                    currentSummary.incomeCents.toDouble()) * 100.0
                ).roundToInt()
            if (savingsPct >= 20) {
                insights += CoachInsight(
                    id = "savings_rate",
                    title = "Saving $savingsPct% of logged income",
                    body = "Net +${fmt(currentSummary.netCents)} kept this month.",
                    tone = CoachInsightTone.POSITIVE
                )
            } else if (currentSummary.netCents < 0L) {
                insights += CoachInsight(
                    id = "negative_cashflow",
                    title = "Spending exceeds income by ${fmt(-currentSummary.netCents)}",
                    body = "Review top categories or log any missing income.",
                    tone = CoachInsightTone.WARNING
                )
            }
        }

        if (insights.isEmpty() && report.isCurrentMonth && !report.hasAnchor) {
            insights += CoachInsight(
                id = "setup_budget",
                title = "Set a monthly budget to unlock Safe-to-Spend",
                body = "Get a daily spending allowance and automatic month-end pace forecasts.",
                tone = CoachInsightTone.NEUTRAL,
                actionLabel = "Set Budget",
                actionTarget = CoachActionTarget.OPEN_MONEY_BUDGETS
            )
        }

        return insights.take(maxItems.coerceAtLeast(0))
    }

    fun buildTodayInsights(
        report: SafeToSpendReport,
        currency: AppCurrency,
        currentSummary: MoneyCalculations.MonthlySummary,
        previousSummary: MoneyCalculations.MonthlySummary,
        budgetStatuses: List<MoneyCalculations.BudgetStatus>,
        todayWaterMl: Int,
        waterGoalMl: Int,
        todayHabitsDone: Int,
        todayHabitsTotal: Int,
        hourOfDay: Int,
        maxItems: Int = 2,
        hideAmounts: Boolean = false
    ): List<CoachInsight> {
        val moneyInsights = buildMoneyInsights(
            report = report,
            currency = currency,
            currentSummary = currentSummary,
            previousSummary = previousSummary,
            budgetStatuses = budgetStatuses,
            maxItems = maxItems.coerceAtLeast(1),
            hideAmounts = hideAmounts
        )

        val pulseInsight = when {
            hourOfDay >= 13 && waterGoalMl > 0 && todayWaterMl * 100 < waterGoalMl * 45 -> CoachInsight(
                id = "hydration_nudge",
                title = "Hydration behind pace ($todayWaterMl / $waterGoalMl ml)",
                body = "Grab a 250 ml glass of water this afternoon.",
                tone = CoachInsightTone.NEUTRAL,
                actionLabel = "+250 ml",
                actionTarget = CoachActionTarget.ADD_WATER_250
            )

            hourOfDay >= 18 && todayHabitsTotal > 0 && todayHabitsDone < todayHabitsTotal -> {
                val left = todayHabitsTotal - todayHabitsDone
                CoachInsight(
                    id = "habits_evening",
                    title = "$left habit${if (left == 1) "" else "s"} left tonight",
                    body = "Check one off before midnight to protect your streak.",
                    tone = CoachInsightTone.WARNING,
                    actionLabel = "Habits",
                    actionTarget = CoachActionTarget.OPEN_HABITS
                )
            }

            todayHabitsTotal > 0 && todayHabitsDone == todayHabitsTotal -> CoachInsight(
                id = "habits_done",
                title = "All $todayHabitsTotal habits completed today",
                body = "Daily streak locked in — nice consistency.",
                tone = CoachInsightTone.POSITIVE
            )

            else -> null
        }

        val combined = if (pulseInsight != null && moneyInsights.isNotEmpty()) {
            listOf(moneyInsights.first(), pulseInsight)
        } else {
            moneyInsights + listOfNotNull(pulseInsight)
        }
        return combined.take(maxItems.coerceAtLeast(0))
    }
}
