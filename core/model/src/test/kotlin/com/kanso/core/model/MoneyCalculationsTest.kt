package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset

class MoneyCalculationsTest {

    @Test
    fun `summarizes income, expense, net and category breakdown`() {
        val txs = listOf(
            MoneyTransaction(1L, TransactionType.INCOME, 5_000_000_00L, ExpenseCategory.SALARY, WalletType.BANK, timestampMillis = 1L),
            MoneyTransaction(2L, TransactionType.EXPENSE, 50_000_00L, ExpenseCategory.FOOD, WalletType.EWALLET, timestampMillis = 2L),
            MoneyTransaction(3L, TransactionType.EXPENSE, 75_000_00L, ExpenseCategory.FOOD, WalletType.CASH, timestampMillis = 3L),
            MoneyTransaction(4L, TransactionType.EXPENSE, 200_000_00L, ExpenseCategory.TRANSPORT, WalletType.CARD, timestampMillis = 4L)
        )

        val summary = MoneyCalculations.summarize(txs)
        assertEquals(5_000_000_00L, summary.incomeCents)
        assertEquals(325_000_00L, summary.expenseCents)
        assertEquals(4_675_000_00L, summary.netCents)
        assertEquals(125_000_00L, summary.byCategory[ExpenseCategory.FOOD])
        assertEquals(200_000_00L, summary.byCategory[ExpenseCategory.TRANSPORT])
    }

    @Test
    fun `transfers are excluded from monthly summary and update wallet balances accurately`() {
        val txs = listOf(
            MoneyTransaction(1L, TransactionType.INCOME, 5_000_000_00L, ExpenseCategory.SALARY, WalletType.BANK, timestampMillis = 1L),
            MoneyTransaction(2L, TransactionType.TRANSFER, 1_000_000_00L, ExpenseCategory.OTHER, WalletType.BANK, toWallet = WalletType.EWALLET, note = "Top up", timestampMillis = 2L),
            MoneyTransaction(3L, TransactionType.EXPENSE, 250_000_00L, ExpenseCategory.FOOD, WalletType.EWALLET, timestampMillis = 3L),
            MoneyTransaction(4L, TransactionType.EXPENSE, 100_000_00L, ExpenseCategory.SHOPPING, WalletType.CASH, timestampMillis = 4L)
        )

        val summary = MoneyCalculations.summarize(txs)
        assertEquals(5_000_000_00L, summary.incomeCents)
        assertEquals(350_000_00L, summary.expenseCents)
        assertEquals(4_650_000_00L, summary.netCents)
        assertFalse(summary.byCategory.containsKey(ExpenseCategory.OTHER))

        val balances = MoneyCalculations.computeWalletBalances(
            allTransactions = txs,
            openingBalances = mapOf(
                WalletType.CASH.name to 500_000_00L,
                WalletType.BANK.name to 2_000_000_00L
            )
        ).associateBy { it.wallet }

        assertEquals(6_000_000_00L, balances.getValue(WalletType.BANK).currentBalanceCents)
        assertEquals(750_000_00L, balances.getValue(WalletType.EWALLET).currentBalanceCents)
        assertEquals(400_000_00L, balances.getValue(WalletType.CASH).currentBalanceCents)
        assertEquals(0L, balances.getValue(WalletType.CARD).currentBalanceCents)
    }

    @Test
    fun `computes budget statuses with warning and over-budget thresholds`() {
        val budgets = listOf(
            CategoryBudget(1L, ExpenseCategory.FOOD, 100_000_00L),
            CategoryBudget(2L, ExpenseCategory.TRANSPORT, 100_000_00L),
            CategoryBudget(3L, ExpenseCategory.SHOPPING, 100_000_00L)
        )
        val spent = mapOf(
            ExpenseCategory.FOOD to 85_000_00L,
            ExpenseCategory.TRANSPORT to 120_000_00L,
            ExpenseCategory.SHOPPING to 30_000_00L
        )

        val statuses = MoneyCalculations.computeBudgetStatuses(budgets, spent)
        assertEquals(ExpenseCategory.TRANSPORT, statuses[0].category)
        assertTrue(statuses[0].isOverBudget)
        assertFalse(statuses[0].isWarning)

        assertEquals(ExpenseCategory.FOOD, statuses[1].category)
        assertTrue(statuses[1].isWarning)
        assertFalse(statuses[1].isOverBudget)

        assertEquals(ExpenseCategory.SHOPPING, statuses[2].category)
        assertFalse(statuses[2].isWarning)
        assertFalse(statuses[2].isOverBudget)
    }

    @Test
    fun `isBillDueSoon checks active status, paid month, and 5 day window`() {
        val today = LocalDate.of(2026, 11, 22)

        val dueInThreeDays = RecurringBill(
            id = 1L, name = "Internet", amountCents = 350_000_00L,
            category = ExpenseCategory.BILLS, wallet = WalletType.BANK,
            dueDayOfMonth = 25, lastPaidYearMonth = null, isActive = true
        )
        assertTrue(MoneyCalculations.isBillDueSoon(dueInThreeDays, today))

        val alreadyPaidThisMonth = dueInThreeDays.copy(lastPaidYearMonth = "2026-11")
        assertFalse(MoneyCalculations.isBillDueSoon(alreadyPaidThisMonth, today))

        val overdueUnpaid = dueInThreeDays.copy(dueDayOfMonth = 10)
        assertTrue(MoneyCalculations.isBillDueSoon(overdueUnpaid, today))

        val inactiveBill = dueInThreeDays.copy(isActive = false)
        assertFalse(MoneyCalculations.isBillDueSoon(inactiveBill, today))

        val farFuture = dueInThreeDays.copy(dueDayOfMonth = 30)
        assertFalse(MoneyCalculations.isBillDueSoon(farFuture, today))

        // Yearly bill stays paid for 12-month cycle and resets next year
        val yearlyPaidJan = RecurringBill(
            id = 2L, name = "Domain Renewal", amountCents = 250_000_00L,
            category = ExpenseCategory.BILLS, wallet = WalletType.CARD,
            dueDayOfMonth = 15, frequency = BillFrequency.YEARLY,
            lastPaidYearMonth = "2026-01-15", isActive = true
        )
        assertTrue(MoneyCalculations.isBillPaidForMonth(yearlyPaidJan, YearMonth.of(2026, 11)))
        assertFalse(MoneyCalculations.isBillDueSoon(yearlyPaidJan, today))
        assertFalse(MoneyCalculations.isBillPaidForMonth(yearlyPaidJan, YearMonth.of(2027, 1)))
        assertTrue(MoneyCalculations.isBillDueSoon(yearlyPaidJan, LocalDate.of(2027, 1, 12)))

        // Weekly bill tracks 7-day cadence accurately using ISO date strings
        val weeklyPaidNov20 = RecurringBill(
            id = 3L, name = "Cleaner", amountCents = 100_000_00L,
            category = ExpenseCategory.HOUSING, wallet = WalletType.CASH,
            dueDayOfMonth = 20, frequency = BillFrequency.WEEKLY,
            lastPaidYearMonth = "2026-11-20", isActive = true
        )
        assertTrue(MoneyCalculations.isBillPaidForCycle(weeklyPaidNov20, LocalDate.of(2026, 11, 22)))
        assertFalse(MoneyCalculations.isBillDueSoon(weeklyPaidNov20, LocalDate.of(2026, 11, 21)))
        assertFalse(MoneyCalculations.isBillPaidForCycle(weeklyPaidNov20, LocalDate.of(2026, 11, 27)))
        assertTrue(MoneyCalculations.isBillDueSoon(weeklyPaidNov20, LocalDate.of(2026, 11, 27)))
    }

    @Test
    fun `snippet masker hides full value while preserving last 4 chars for longer strings`() {
        assertEquals("••••", SnippetMasker.mask("1234"))
        assertEquals("••••", SnippetMasker.mask("abc"))
        assertEquals("•••• •••• 7890", SnippetMasker.mask("1234567890"))
    }

    @Test
    fun `resolveTransactionTimestampMillis handles today, backdated new tx, and edited tx dates`() {
        val zone = ZoneOffset.UTC
        val nov26 = LocalDate.of(2026, 11, 26)
        val nov24 = LocalDate.of(2026, 11, 24)
        val nov20 = LocalDate.of(2026, 11, 20)

        val nowMillis = nov26.atTime(LocalTime.of(10, 15, 30)).toInstant(zone).toEpochMilli()
        val existingMillis = nov24.atTime(LocalTime.of(14, 22, 10)).toInstant(zone).toEpochMilli()

        // New transaction dated today returns exact nowMillis
        assertEquals(
            nowMillis,
            MoneyCalculations.resolveTransactionTimestampMillis(
                selectedEpochDay = nov26.toEpochDay(),
                existingTimestampMillis = null,
                nowMillis = nowMillis,
                zone = zone
            )
        )

        // New backdated transaction preserves time-of-day from nowMillis on selected date
        val expectedBackdated = nov24.atTime(LocalTime.of(10, 15, 30)).toInstant(zone).toEpochMilli()
        assertEquals(
            expectedBackdated,
            MoneyCalculations.resolveTransactionTimestampMillis(
                selectedEpochDay = nov24.toEpochDay(),
                existingTimestampMillis = null,
                nowMillis = nowMillis,
                zone = zone
            )
        )

        // Edited transaction with unchanged date returns exact existingTimestampMillis
        assertEquals(
            existingMillis,
            MoneyCalculations.resolveTransactionTimestampMillis(
                selectedEpochDay = nov24.toEpochDay(),
                existingTimestampMillis = existingMillis,
                nowMillis = nowMillis,
                zone = zone
            )
        )

        // Edited transaction with changed date keeps existing time-of-day on new date
        val expectedMoved = nov20.atTime(LocalTime.of(14, 22, 10)).toInstant(zone).toEpochMilli()
        assertEquals(
            expectedMoved,
            MoneyCalculations.resolveTransactionTimestampMillis(
                selectedEpochDay = nov20.toEpochDay(),
                existingTimestampMillis = existingMillis,
                nowMillis = nowMillis,
                zone = zone
            )
        )
    }

    @Test
    fun `filterTransactions supports type, wallet including transfer destinations, category, and combined filters`() {
        val expenseFoodEwallet = MoneyTransaction(1L, TransactionType.EXPENSE, 50_000_00L, ExpenseCategory.FOOD, WalletType.EWALLET, timestampMillis = 1L)
        val incomeSalaryBank = MoneyTransaction(2L, TransactionType.INCOME, 5_000_000_00L, ExpenseCategory.SALARY, WalletType.BANK, timestampMillis = 2L)
        val transferBankToEwallet = MoneyTransaction(3L, TransactionType.TRANSFER, 200_000_00L, ExpenseCategory.OTHER, WalletType.BANK, toWallet = WalletType.EWALLET, timestampMillis = 3L)
        val expenseOtherCash = MoneyTransaction(4L, TransactionType.EXPENSE, 25_000_00L, ExpenseCategory.OTHER, WalletType.CASH, timestampMillis = 4L)
        val all = listOf(expenseFoodEwallet, incomeSalaryBank, transferBankToEwallet, expenseOtherCash)

        assertEquals(all, MoneyCalculations.filterTransactions(all, MoneyCalculations.TransactionFilter()))

        // Type filter
        assertEquals(
            listOf(expenseFoodEwallet, expenseOtherCash),
            MoneyCalculations.filterTransactions(all, MoneyCalculations.TransactionFilter(type = TransactionType.EXPENSE))
        )

        // Wallet filter matches both source wallet and transfer destination toWallet
        assertEquals(
            listOf(expenseFoodEwallet, transferBankToEwallet),
            MoneyCalculations.filterTransactions(all, MoneyCalculations.TransactionFilter(wallet = WalletType.EWALLET))
        )

        // Category filter excludes TRANSFER transactions even though transfer carries OTHER
        assertEquals(
            listOf(expenseOtherCash),
            MoneyCalculations.filterTransactions(all, MoneyCalculations.TransactionFilter(category = ExpenseCategory.OTHER))
        )

        // Combined filter
        assertEquals(
            listOf(expenseFoodEwallet),
            MoneyCalculations.filterTransactions(
                all,
                MoneyCalculations.TransactionFilter(
                    type = TransactionType.EXPENSE,
                    wallet = WalletType.EWALLET,
                    category = ExpenseCategory.FOOD
                )
            )
        )
    }

    @Test
    fun `groupTransactionsByDay groups descending by day and excludes transfers from daily totals`() {
        val zone = ZoneOffset.UTC
        val nov25Morning = LocalDate.of(2026, 11, 25).atTime(LocalTime.of(9, 0)).toInstant(zone).toEpochMilli()
        val nov25Evening = LocalDate.of(2026, 11, 25).atTime(LocalTime.of(19, 30)).toInstant(zone).toEpochMilli()
        val nov26Noon = LocalDate.of(2026, 11, 26).atTime(LocalTime.of(12, 0)).toInstant(zone).toEpochMilli()

        val tx1 = MoneyTransaction(1L, TransactionType.INCOME, 1_000_000_00L, ExpenseCategory.SALARY, WalletType.BANK, timestampMillis = nov25Morning)
        val tx2 = MoneyTransaction(2L, TransactionType.EXPENSE, 150_000_00L, ExpenseCategory.FOOD, WalletType.EWALLET, timestampMillis = nov25Evening)
        val tx3 = MoneyTransaction(3L, TransactionType.TRANSFER, 300_000_00L, ExpenseCategory.OTHER, WalletType.BANK, toWallet = WalletType.CASH, timestampMillis = nov25Evening + 1_000L)
        val tx4 = MoneyTransaction(4L, TransactionType.EXPENSE, 40_000_00L, ExpenseCategory.TRANSPORT, WalletType.CASH, timestampMillis = nov26Noon)

        val groups = MoneyCalculations.groupTransactionsByDay(listOf(tx1, tx2, tx3, tx4), zone)
        assertEquals(2, groups.size)

        // Nov 26 comes first
        assertEquals(LocalDate.of(2026, 11, 26).toEpochDay(), groups[0].epochDay)
        assertEquals(listOf(tx4), groups[0].transactions)
        assertEquals(0L, groups[0].dayIncomeCents)
        assertEquals(40_000_00L, groups[0].dayExpenseCents)
        assertEquals(-40_000_00L, groups[0].dayNetCents)

        // Nov 25 comes second, sorted descending within the day, transfer excluded from income/expense
        assertEquals(LocalDate.of(2026, 11, 25).toEpochDay(), groups[1].epochDay)
        assertEquals(listOf(tx3, tx2, tx1), groups[1].transactions)
        assertEquals(1_000_000_00L, groups[1].dayIncomeCents)
        assertEquals(150_000_00L, groups[1].dayExpenseCents)
        assertEquals(850_000_00L, groups[1].dayNetCents)
    }

    @Test
    fun `computeGoalStatus computes pace for active deadline, reached, overdue, and flexible goals`() {
        val today = LocalDate.of(2026, 11, 26).toEpochDay()

        val active = SavingsGoal(
            id = 1L,
            name = "Japan Trip",
            category = GoalCategory.TRAVEL,
            targetAmountCents = 10_000_000_00L,
            savedAmountCents = 4_000_000_00L,
            linkedWallet = WalletType.BANK,
            targetEpochDay = today + 60L,
            createdAtMillis = 1L
        )
        val activeStatus = MoneyCalculations.computeGoalStatus(active, today)
        assertEquals(6_000_000_00L, activeStatus.remainingCents)
        assertEquals(0.4f, activeStatus.progressRatio, 0.001f)
        assertFalse(activeStatus.isTargetReached)
        assertEquals(60L, activeStatus.daysRemaining)
        assertEquals(100_000_00L, activeStatus.requiredDailyCents)
        assertEquals(3_000_000_00L, activeStatus.requiredMonthlyCents)
        assertEquals(MoneyCalculations.GoalPaceState.ON_TRACK, activeStatus.paceState)

        val reached = active.copy(savedAmountCents = 10_500_000_00L)
        val reachedStatus = MoneyCalculations.computeGoalStatus(reached, today)
        assertEquals(0L, reachedStatus.remainingCents)
        assertEquals(1.0f, reachedStatus.progressRatio, 0.001f)
        assertTrue(reachedStatus.isTargetReached)
        assertEquals(MoneyCalculations.GoalPaceState.COMPLETED, reachedStatus.paceState)

        val overdue = active.copy(targetEpochDay = today - 2L)
        val overdueStatus = MoneyCalculations.computeGoalStatus(overdue, today)
        assertEquals(MoneyCalculations.GoalPaceState.DEADLINE_PASSED, overdueStatus.paceState)
        assertEquals(6_000_000_00L, overdueStatus.requiredMonthlyCents)

        val flexible = active.copy(targetEpochDay = null)
        val flexibleStatus = MoneyCalculations.computeGoalStatus(flexible, today)
        assertEquals(MoneyCalculations.GoalPaceState.NO_DEADLINE, flexibleStatus.paceState)
        assertNull(flexibleStatus.daysRemaining)
        assertNull(flexibleStatus.requiredMonthlyCents)
    }

    @Test
    fun `computeGoalsOverview calculates unallocated balance and wallet reservations excluding archived goals`() {
        val goals = listOf(
            SavingsGoal(1L, "Emergency", GoalCategory.EMERGENCY, 10_000_000_00L, 3_000_000_00L, WalletType.BANK, createdAtMillis = 1L),
            SavingsGoal(2L, "Gadget", GoalCategory.GADGET, 5_000_000_00L, 1_500_000_00L, WalletType.EWALLET, createdAtMillis = 2L),
            SavingsGoal(3L, "Completed Old", GoalCategory.OTHER, 2_000_000_00L, 2_000_000_00L, WalletType.BANK, isArchived = true, createdAtMillis = 3L)
        )
        val overview = MoneyCalculations.computeGoalsOverview(goals, totalWalletBalanceCents = 8_000_000_00L)
        assertEquals(2, overview.activeGoalsCount)
        assertEquals(0, overview.reachedGoalsCount)
        assertEquals(4_500_000_00L, overview.totalSavedCents)
        assertEquals(15_000_000_00L, overview.totalTargetCents)
        assertEquals(3_500_000_00L, overview.unallocatedBalanceCents)
        assertEquals(3_000_000_00L, overview.reservedByWallet[WalletType.BANK])
        assertEquals(1_500_000_00L, overview.reservedByWallet[WalletType.EWALLET])
    }

    @Test
    fun `computeMonthlyTrend and computeDailyExpenseSeries summarize cashflow and mark future days null`() {
        val zone = ZoneOffset.UTC
        val oct10 = LocalDate.of(2026, 10, 10).atTime(LocalTime.NOON).toInstant(zone).toEpochMilli()
        val nov05 = LocalDate.of(2026, 11, 5).atTime(LocalTime.NOON).toInstant(zone).toEpochMilli()
        val nov12 = LocalDate.of(2026, 11, 12).atTime(LocalTime.NOON).toInstant(zone).toEpochMilli()

        val txs = listOf(
            MoneyTransaction(1L, TransactionType.INCOME, 5_000_00L, ExpenseCategory.SALARY, WalletType.BANK, timestampMillis = oct10),
            MoneyTransaction(2L, TransactionType.EXPENSE, 1_200_00L, ExpenseCategory.FOOD, WalletType.EWALLET, timestampMillis = nov05),
            MoneyTransaction(3L, TransactionType.TRANSFER, 9_000_00L, ExpenseCategory.OTHER, WalletType.BANK, toWallet = WalletType.EWALLET, timestampMillis = nov12)
        )

        val trend = MoneyCalculations.computeMonthlyTrend(txs, YearMonth.of(2026, 11), monthsCount = 6, zone = zone)
        assertEquals(6, trend.size)
        assertEquals(YearMonth.of(2026, 6), trend.first().yearMonth)
        assertEquals(YearMonth.of(2026, 10), trend[4].yearMonth)
        assertEquals(5_000_00L, trend[4].incomeCents)
        assertEquals(YearMonth.of(2026, 11), trend[5].yearMonth)
        assertEquals(1_200_00L, trend[5].expenseCents)
        assertEquals(-1_200_00L, trend[5].netCents)

        val daily = MoneyCalculations.computeDailyExpenseSeries(
            monthTransactions = txs,
            month = YearMonth.of(2026, 11),
            today = LocalDate.of(2026, 11, 15),
            zone = zone
        )
        assertEquals(30, daily.size)
        assertEquals(1_200_00L, daily[4].expenseCents) // Nov 5
        assertEquals(0L, daily[11].expenseCents) // Nov 12 transfer excluded
        assertNull(daily[15].expenseCents) // Nov 16 is in the future relative to Nov 15
    }
}
