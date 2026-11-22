package com.kanso.core.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth

@Serializable
enum class TransactionType {
    EXPENSE,
    INCOME
}

@Serializable
enum class WalletType(val label: String) {
    CASH("Cash"),
    BANK("Bank"),
    EWALLET("E-Wallet"),
    CARD("Card")
}

@Serializable
enum class ExpenseCategory(val label: String, val isIncomeDefault: Boolean = false) {
    FOOD("Food & Drink"),
    TRANSPORT("Transport"),
    GROCERIES("Groceries"),
    BILLS("Bills & Utilities"),
    SHOPPING("Shopping"),
    HEALTH("Health"),
    ENTERTAINMENT("Entertainment"),
    HOUSING("Housing"),
    EDUCATION("Education"),
    SALARY("Salary", isIncomeDefault = true),
    FREELANCE("Freelance", isIncomeDefault = true),
    GIFT("Gift", isIncomeDefault = true),
    OTHER("Other");

    companion object {
        val expenseCategories: List<ExpenseCategory>
            get() = entries.filter { !it.isIncomeDefault }

        val incomeCategories: List<ExpenseCategory>
            get() = entries.filter { it.isIncomeDefault || it == OTHER }
    }
}

@Serializable
data class MoneyTransaction(
    val id: Long = 0L,
    val type: TransactionType,
    val amountCents: Long,
    val category: ExpenseCategory,
    val wallet: WalletType,
    val note: String = "",
    val timestampMillis: Long
)

@Serializable
data class CategoryBudget(
    val id: Long = 0L,
    val category: ExpenseCategory,
    val limitCents: Long,
    val yearMonth: String = DEFAULT_YEAR_MONTH
) {
    companion object {
        const val DEFAULT_YEAR_MONTH = "DEFAULT"
    }
}

@Serializable
enum class BillFrequency(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

@Serializable
data class RecurringBill(
    val id: Long = 0L,
    val name: String,
    val amountCents: Long,
    val category: ExpenseCategory,
    val wallet: WalletType,
    val dueDayOfMonth: Int,
    val frequency: BillFrequency = BillFrequency.MONTHLY,
    val lastPaidYearMonth: String? = null,
    val isActive: Boolean = true
)

@Serializable
enum class IouDirection(val label: String) {
    OWES_ME("Owes Me"),
    I_OWE("I Owe")
}

@Serializable
data class IouEntry(
    val id: Long = 0L,
    val personName: String,
    val amountCents: Long,
    val direction: IouDirection,
    val note: String = "",
    val createdAtMillis: Long,
    val dueDateEpochDay: Long? = null,
    val isSettled: Boolean = false,
    val settledAtMillis: Long? = null
)

object MoneyCalculations {

    data class MonthlySummary(
        val incomeCents: Long,
        val expenseCents: Long,
        val netCents: Long,
        val byCategory: Map<ExpenseCategory, Long>
    )

    data class BudgetStatus(
        val category: ExpenseCategory,
        val spentCents: Long,
        val limitCents: Long,
        val ratio: Float,
        val isWarning: Boolean,
        val isOverBudget: Boolean
    )

    fun summarize(transactions: List<MoneyTransaction>): MonthlySummary {
        var income = 0L
        var expense = 0L
        val byCategory = mutableMapOf<ExpenseCategory, Long>()

        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME) {
                income += tx.amountCents
            } else {
                expense += tx.amountCents
                byCategory[tx.category] = (byCategory[tx.category] ?: 0L) + tx.amountCents
            }
        }

        return MonthlySummary(
            incomeCents = income,
            expenseCents = expense,
            netCents = income - expense,
            byCategory = byCategory
        )
    }

    fun computeBudgetStatuses(
        budgets: List<CategoryBudget>,
        spentByCategory: Map<ExpenseCategory, Long>
    ): List<BudgetStatus> {
        return budgets
            .filter { it.limitCents > 0L }
            .map { budget ->
                val spent = spentByCategory[budget.category] ?: 0L
                val ratio = spent.toFloat() / budget.limitCents.toFloat()
                BudgetStatus(
                    category = budget.category,
                    spentCents = spent,
                    limitCents = budget.limitCents,
                    ratio = ratio,
                    isWarning = ratio >= 0.8f && ratio <= 1.0f,
                    isOverBudget = ratio > 1.0f
                )
            }
            .sortedByDescending { it.ratio }
    }

    fun isBillPaidForMonth(bill: RecurringBill, yearMonth: YearMonth): Boolean {
        return bill.lastPaidYearMonth == yearMonth.toString()
    }

    fun dueDateForMonth(bill: RecurringBill, yearMonth: YearMonth): LocalDate {
        val clampedDay = bill.dueDayOfMonth.coerceIn(1, yearMonth.lengthOfMonth())
        return yearMonth.atDay(clampedDay)
    }

    fun isBillDueSoon(
        bill: RecurringBill,
        today: LocalDate,
        withinDays: Int = 5
    ): Boolean {
        if (!bill.isActive) return false
        val currentMonth = YearMonth.from(today)
        if (isBillPaidForMonth(bill, currentMonth)) return false
        val dueDate = dueDateForMonth(bill, currentMonth)
        return !dueDate.isAfter(today.plusDays(withinDays.toLong()))
    }
}
