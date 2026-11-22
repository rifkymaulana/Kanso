package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
    }

    @Test
    fun `snippet masker hides full value while preserving last 4 chars for longer strings`() {
        assertEquals("••••", SnippetMasker.mask("1234"))
        assertEquals("••••", SnippetMasker.mask("abc"))
        assertEquals("•••• •••• 7890", SnippetMasker.mask("1234567890"))
    }
}
