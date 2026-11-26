package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class SmartCoachCalculationsTest {

    @Test
    fun `computeSafeToSpend reserves unpaid active monthly bills and deducts today spend`() {
        val today = LocalDate.of(2026, 11, 21) // 30-day month -> 10 days remaining incl. today
        val bills = listOf(
            RecurringBill(
                id = 1L,
                name = "Internet",
                amountCents = 500_000_00L,
                category = ExpenseCategory.BILLS,
                wallet = WalletType.BANK,
                dueDayOfMonth = 25,
                frequency = BillFrequency.MONTHLY,
                lastPaidYearMonth = null,
                isActive = true
            ),
            RecurringBill(
                id = 2L,
                name = "Paused Gym",
                amountCents = 300_000_00L,
                category = ExpenseCategory.HEALTH,
                wallet = WalletType.CARD,
                dueDayOfMonth = 28,
                frequency = BillFrequency.MONTHLY,
                lastPaidYearMonth = null,
                isActive = false
            )
        )

        val report = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = YearMonth.of(2026, 11),
            today = today,
            monthlyBudgetCents = 3_000_000_00L,
            monthIncomeCents = 5_000_000_00L,
            monthExpenseCents = 1_100_000_00L, // 1,000,000 prior + 100,000 today
            todayExpenseCents = 100_000_00L,
            bills = bills
        )

        assertTrue(report.hasAnchor)
        assertEquals(BudgetAnchorSource.MONTHLY_BUDGET, report.anchorSource)
        assertEquals(1, report.unpaidBillsCount)
        assertEquals(500_000_00L, report.unpaidBillsCents)
        assertEquals(10, report.daysRemainingIncludingToday)
        // Flexible before today = 3,000,000 - 1,000,000 - 500,000 = 1,500,000 -> /10 = 150,000/day
        assertEquals(150_000_00L, report.dailySafeAllowanceCents)
        assertEquals(50_000_00L, report.todayRemainingAllowanceCents)
        assertEquals(1_400_000_00L, report.flexibleRemainingMonthCents)
    }

    @Test
    fun `computeSafeToSpend does not extrapolate paid monthly bills across remaining days`() {
        val today = LocalDate.of(2026, 11, 5) // 30-day month
        val rentBill = RecurringBill(
            id = 1L,
            name = "Rent",
            amountCents = 2_000_000_00L,
            category = ExpenseCategory.HOUSING,
            wallet = WalletType.BANK,
            dueDayOfMonth = 1,
            frequency = BillFrequency.MONTHLY,
            lastPaidYearMonth = "2026-11",
            isActive = true
        )

        val report = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = YearMonth.of(2026, 11),
            today = today,
            monthlyBudgetCents = 4_000_000_00L,
            monthIncomeCents = 6_000_000_00L,
            monthExpenseCents = 2_250_000_00L, // 2,000,000 rent + 250,000 variable (50,000/day)
            todayExpenseCents = 50_000_00L,
            bills = listOf(rentBill)
        )

        // Variable 50,000/day * 30 = 1,500,000 + paid rent 2,000,000 = 3,500,000 (not 13,500,000)
        assertEquals(3_500_000_00L, report.projectedMonthEndCents)
        assertEquals(SpendPaceStatus.ON_TRACK, report.paceStatus)
    }

    @Test
    fun `computeSafeToSpend handles income fallback, no anchor, and past month status`() {
        val today = LocalDate.of(2026, 11, 15)

        val incomeFallback = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = YearMonth.of(2026, 11),
            today = today,
            monthlyBudgetCents = 0L,
            monthIncomeCents = 3_200_000_00L,
            monthExpenseCents = 1_500_000_00L,
            todayExpenseCents = 0L,
            bills = emptyList()
        )
        assertEquals(BudgetAnchorSource.MONTHLY_INCOME, incomeFallback.anchorSource)
        assertTrue(incomeFallback.hasAnchor)
        assertEquals(SpendPaceStatus.ON_TRACK, incomeFallback.paceStatus)

        val aheadOfGoal = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = YearMonth.of(2026, 11),
            today = today,
            monthlyBudgetCents = 3_200_000_00L,
            monthIncomeCents = 3_200_000_00L,
            monthExpenseCents = 1_000_000_00L,
            todayExpenseCents = 0L,
            bills = emptyList()
        )
        assertEquals(SpendPaceStatus.UNDER_PACE, aheadOfGoal.paceStatus)

        val noAnchor = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = YearMonth.of(2026, 11),
            today = today,
            monthlyBudgetCents = 0L,
            monthIncomeCents = 0L,
            monthExpenseCents = 200_000_00L,
            todayExpenseCents = 0L,
            bills = emptyList()
        )
        assertFalse(noAnchor.hasAnchor)
        assertEquals(SpendPaceStatus.NO_ANCHOR, noAnchor.paceStatus)

        val pastMonth = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = YearMonth.of(2026, 10),
            today = today,
            monthlyBudgetCents = 3_000_000_00L,
            monthIncomeCents = 5_000_000_00L,
            monthExpenseCents = 1_550_000_00L,
            todayExpenseCents = 0L,
            bills = emptyList()
        )
        assertTrue(pastMonth.isPastMonth)
        assertEquals(SpendPaceStatus.PAST_MONTH, pastMonth.paceStatus)
        assertEquals(31, pastMonth.daysElapsed)
        assertEquals(50_000_00L, pastMonth.averageDailySpendCents)
    }

    @Test
    fun `buildMoneyInsights flags category budget warning and month over month spike`() {
        val today = LocalDate.of(2026, 11, 15)
        val report = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = YearMonth.of(2026, 11),
            today = today,
            monthlyBudgetCents = 5_000_000_00L,
            monthIncomeCents = 8_000_000_00L,
            monthExpenseCents = 1_500_000_00L,
            todayExpenseCents = 50_000_00L,
            bills = emptyList()
        )
        val currentSummary = MoneyCalculations.MonthlySummary(
            incomeCents = 8_000_000_00L,
            expenseCents = 1_500_000_00L,
            netCents = 6_500_000_00L,
            byCategory = mapOf(
                ExpenseCategory.FOOD to 900_000_00L,
                ExpenseCategory.TRANSPORT to 600_000_00L
            )
        )
        val prevSummary = MoneyCalculations.MonthlySummary(
            incomeCents = 8_000_000_00L,
            expenseCents = 1_000_000_00L,
            netCents = 7_000_000_00L,
            byCategory = mapOf(
                ExpenseCategory.FOOD to 600_000_00L,
                ExpenseCategory.TRANSPORT to 550_000_00L
            )
        )
        val budgets = listOf(
            CategoryBudget(1L, ExpenseCategory.FOOD, 1_000_000_00L)
        )
        val statuses = MoneyCalculations.computeBudgetStatuses(budgets, currentSummary.byCategory)

        val insights = SmartCoachCalculations.buildMoneyInsights(
            report = report,
            currency = AppCurrency.IDR,
            currentSummary = currentSummary,
            previousSummary = prevSummary,
            budgetStatuses = statuses,
            maxItems = 4
        )

        assertTrue(insights.any { it.id == "cat_warn_FOOD" && it.tone == CoachInsightTone.WARNING })
        assertTrue(insights.any { it.id == "mom_spike_FOOD" && it.title.contains("+50%") })
    }

    @Test
    fun `buildTodayInsights includes hydration afternoon nudge and all habits completion praise`() {
        val today = LocalDate.of(2026, 11, 15)
        val report = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = YearMonth.of(2026, 11),
            today = today,
            monthlyBudgetCents = 0L,
            monthIncomeCents = 0L,
            monthExpenseCents = 0L,
            todayExpenseCents = 0L,
            bills = emptyList()
        )
        val emptySummary = MoneyCalculations.MonthlySummary(0L, 0L, 0L, emptyMap())

        val afternoonInsights = SmartCoachCalculations.buildTodayInsights(
            report = report,
            currency = AppCurrency.IDR,
            currentSummary = emptySummary,
            previousSummary = emptySummary,
            budgetStatuses = emptyList(),
            todayWaterMl = 500,
            waterGoalMl = 2000,
            todayHabitsDone = 1,
            todayHabitsTotal = 3,
            hourOfDay = 15
        )
        assertTrue(afternoonInsights.any { it.id == "hydration_nudge" && it.actionTarget == CoachActionTarget.ADD_WATER_250 })

        val completedHabitsInsights = SmartCoachCalculations.buildTodayInsights(
            report = report,
            currency = AppCurrency.IDR,
            currentSummary = emptySummary,
            previousSummary = emptySummary,
            budgetStatuses = emptyList(),
            todayWaterMl = 1800,
            waterGoalMl = 2000,
            todayHabitsDone = 3,
            todayHabitsTotal = 3,
            hourOfDay = 20
        )
        assertTrue(completedHabitsInsights.any { it.id == "habits_done" && it.tone == CoachInsightTone.POSITIVE })
    }

    @Test
    fun `buildMoneyInsights masks currency amounts when hideAmounts is true`() {
        val today = LocalDate.of(2026, 11, 15)
        val report = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = YearMonth.of(2026, 11),
            today = today,
            monthlyBudgetCents = 5_000_000_00L,
            monthIncomeCents = 8_000_000_00L,
            monthExpenseCents = 1_500_000_00L,
            todayExpenseCents = 50_000_00L,
            bills = emptyList()
        )
        val currentSummary = MoneyCalculations.MonthlySummary(
            incomeCents = 8_000_000_00L,
            expenseCents = 1_500_000_00L,
            netCents = 6_500_000_00L,
            byCategory = mapOf(ExpenseCategory.FOOD to 900_000_00L)
        )
        val prevSummary = MoneyCalculations.MonthlySummary(0L, 0L, 0L, emptyMap())
        val statuses = MoneyCalculations.computeBudgetStatuses(
            budgets = listOf(CategoryBudget(1L, ExpenseCategory.FOOD, 1_000_000_00L)),
            spentByCategory = currentSummary.byCategory
        )

        val insights = SmartCoachCalculations.buildMoneyInsights(
            report = report,
            currency = AppCurrency.IDR,
            currentSummary = currentSummary,
            previousSummary = prevSummary,
            budgetStatuses = statuses,
            maxItems = 3,
            hideAmounts = true
        )

        val warnInsight = insights.first { it.id == "cat_warn_FOOD" }
        assertTrue(warnInsight.body.contains("Rp ••••••"))
        assertFalse(warnInsight.body.contains("100.000"))
    }
}
