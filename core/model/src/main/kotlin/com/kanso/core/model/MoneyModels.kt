package com.kanso.core.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Serializable
enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer")
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
    val toWallet: WalletType? = null,
    val note: String = "",
    val timestampMillis: Long
)

data class WalletBalanceSummary(
    val wallet: WalletType,
    val openingBalanceCents: Long,
    val netDeltaCents: Long,
    val currentBalanceCents: Long
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

@Serializable
enum class GoalCategory(val label: String) {
    EMERGENCY("Emergency Fund"),
    TRAVEL("Travel & Holiday"),
    GADGET("Tech & Gadget"),
    VEHICLE("Vehicle & Tax"),
    HOME("Home & Living"),
    EDUCATION("Education"),
    INVESTMENT("Investment"),
    GIFT("Event & Gift"),
    OTHER("General Goal")
}

@Serializable
data class SavingsGoal(
    val id: Long = 0L,
    val name: String,
    val category: GoalCategory = GoalCategory.OTHER,
    val targetAmountCents: Long,
    val savedAmountCents: Long = 0L,
    val linkedWallet: WalletType? = WalletType.BANK,
    val targetEpochDay: Long? = null,
    val note: String = "",
    val isArchived: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long = createdAtMillis
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

    data class TransactionFilter(
        val type: TransactionType? = null,
        val wallet: WalletType? = null,
        val category: ExpenseCategory? = null
    ) {
        val isActive: Boolean
            get() = type != null || wallet != null || category != null
    }

    data class DailyTransactionGroup(
        val epochDay: Long,
        val transactions: List<MoneyTransaction>,
        val dayIncomeCents: Long,
        val dayExpenseCents: Long,
        val dayNetCents: Long
    )

    fun resolveTransactionTimestampMillis(
        selectedEpochDay: Long,
        existingTimestampMillis: Long?,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
        if (existingTimestampMillis != null) {
            val existingZdt = Instant.ofEpochMilli(existingTimestampMillis).atZone(zone)
            return if (existingZdt.toLocalDate() == selectedDate) {
                existingTimestampMillis
            } else {
                selectedDate.atTime(existingZdt.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
            }
        }
        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zone)
        return if (selectedDate == nowZdt.toLocalDate()) {
            nowMillis
        } else {
            selectedDate.atTime(nowZdt.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
        }
    }

    fun filterTransactions(
        transactions: List<MoneyTransaction>,
        filter: TransactionFilter
    ): List<MoneyTransaction> {
        if (!filter.isActive) return transactions
        return transactions.filter { tx ->
            val typeOk = filter.type == null || tx.type == filter.type
            val walletOk = filter.wallet == null ||
                tx.wallet == filter.wallet ||
                (tx.type == TransactionType.TRANSFER && tx.toWallet == filter.wallet)
            val categoryOk = filter.category == null ||
                (tx.type != TransactionType.TRANSFER && tx.category == filter.category)
            typeOk && walletOk && categoryOk
        }
    }

    fun groupTransactionsByDay(
        transactions: List<MoneyTransaction>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<DailyTransactionGroup> {
        if (transactions.isEmpty()) return emptyList()
        return transactions
            .groupBy { tx ->
                Instant.ofEpochMilli(tx.timestampMillis).atZone(zone).toLocalDate().toEpochDay()
            }
            .entries
            .sortedByDescending { it.key }
            .map { (epochDay, dayTxs) ->
                var dayIncome = 0L
                var dayExpense = 0L
                for (tx in dayTxs) {
                    when (tx.type) {
                        TransactionType.INCOME -> dayIncome += tx.amountCents
                        TransactionType.EXPENSE -> dayExpense += tx.amountCents
                        TransactionType.TRANSFER -> Unit
                    }
                }
                DailyTransactionGroup(
                    epochDay = epochDay,
                    transactions = dayTxs.sortedByDescending { it.timestampMillis },
                    dayIncomeCents = dayIncome,
                    dayExpenseCents = dayExpense,
                    dayNetCents = dayIncome - dayExpense
                )
            }
    }

    fun summarize(transactions: List<MoneyTransaction>): MonthlySummary {
        var income = 0L
        var expense = 0L
        val byCategory = mutableMapOf<ExpenseCategory, Long>()

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amountCents
                TransactionType.EXPENSE -> {
                    expense += tx.amountCents
                    byCategory[tx.category] = (byCategory[tx.category] ?: 0L) + tx.amountCents
                }
                TransactionType.TRANSFER -> Unit
            }
        }

        return MonthlySummary(
            incomeCents = income,
            expenseCents = expense,
            netCents = income - expense,
            byCategory = byCategory
        )
    }

    fun computeWalletBalances(
        allTransactions: List<MoneyTransaction>,
        openingBalances: Map<String, Long>
    ): List<WalletBalanceSummary> {
        val deltas = mutableMapOf<WalletType, Long>()
        for (tx in allTransactions) {
            when (tx.type) {
                TransactionType.INCOME -> {
                    deltas[tx.wallet] = (deltas[tx.wallet] ?: 0L) + tx.amountCents
                }
                TransactionType.EXPENSE -> {
                    deltas[tx.wallet] = (deltas[tx.wallet] ?: 0L) - tx.amountCents
                }
                TransactionType.TRANSFER -> {
                    val target = tx.toWallet
                    if (target != null && target != tx.wallet) {
                        deltas[tx.wallet] = (deltas[tx.wallet] ?: 0L) - tx.amountCents
                        deltas[target] = (deltas[target] ?: 0L) + tx.amountCents
                    }
                }
            }
        }
        return WalletType.entries.map { wallet ->
            val opening = openingBalances[wallet.name] ?: 0L
            val delta = deltas[wallet] ?: 0L
            WalletBalanceSummary(
                wallet = wallet,
                openingBalanceCents = opening,
                netDeltaCents = delta,
                currentBalanceCents = opening + delta
            )
        }
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

    enum class GoalPaceState(val label: String) {
        COMPLETED("Target Reached"),
        ON_TRACK("Active Target"),
        DEADLINE_PASSED("Past Target Date"),
        NO_DEADLINE("Flexible")
    }

    data class GoalStatus(
        val goal: SavingsGoal,
        val remainingCents: Long,
        val progressRatio: Float,
        val isTargetReached: Boolean,
        val daysRemaining: Long?,
        val requiredMonthlyCents: Long?,
        val requiredDailyCents: Long?,
        val paceState: GoalPaceState
    )

    data class GoalsOverview(
        val activeGoalsCount: Int,
        val reachedGoalsCount: Int,
        val totalSavedCents: Long,
        val totalTargetCents: Long,
        val overallProgressRatio: Float,
        val unallocatedBalanceCents: Long,
        val reservedByWallet: Map<WalletType, Long>
    )

    data class MonthlyTrendPoint(
        val yearMonth: YearMonth,
        val incomeCents: Long,
        val expenseCents: Long,
        val netCents: Long
    )

    data class DailySpendPoint(
        val epochDay: Long,
        val dayOfMonth: Int,
        val expenseCents: Long?
    )

    fun computeGoalStatus(goal: SavingsGoal, todayEpochDay: Long): GoalStatus {
        val target = goal.targetAmountCents.coerceAtLeast(0L)
        val saved = goal.savedAmountCents.coerceAtLeast(0L)
        val remaining = (target - saved).coerceAtLeast(0L)
        val progressRatio = if (target > 0L) (saved.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
        val isTargetReached = target > 0L && remaining == 0L
        val targetDay = goal.targetEpochDay
        val daysRemaining = targetDay?.minus(todayEpochDay)

        val (paceState, requiredMonthly, requiredDaily) = when {
            isTargetReached -> Triple(GoalPaceState.COMPLETED, 0L, 0L)
            targetDay == null || daysRemaining == null -> Triple(GoalPaceState.NO_DEADLINE, null, null)
            daysRemaining <= 0L -> Triple(GoalPaceState.DEADLINE_PASSED, remaining, remaining)
            else -> {
                val daily = (remaining + daysRemaining - 1L) / daysRemaining
                val monthly = if (daysRemaining <= 30L) {
                    remaining
                } else {
                    (remaining * 30L + daysRemaining - 1L) / daysRemaining
                }
                Triple(GoalPaceState.ON_TRACK, monthly, daily)
            }
        }

        return GoalStatus(
            goal = goal,
            remainingCents = remaining,
            progressRatio = progressRatio,
            isTargetReached = isTargetReached,
            daysRemaining = daysRemaining,
            requiredMonthlyCents = requiredMonthly,
            requiredDailyCents = requiredDaily,
            paceState = paceState
        )
    }

    fun computeGoalsOverview(
        goals: List<SavingsGoal>,
        totalWalletBalanceCents: Long
    ): GoalsOverview {
        val activeGoals = goals.filter { !it.isArchived }
        var totalSaved = 0L
        var totalTarget = 0L
        var reachedCount = 0
        val reservedByWallet = mutableMapOf<WalletType, Long>()

        for (goal in activeGoals) {
            val saved = goal.savedAmountCents.coerceAtLeast(0L)
            val target = goal.targetAmountCents.coerceAtLeast(0L)
            totalSaved += saved
            totalTarget += target
            if (target > 0L && saved >= target) {
                reachedCount++
            }
            val wallet = goal.linkedWallet
            if (wallet != null && saved > 0L) {
                reservedByWallet[wallet] = (reservedByWallet[wallet] ?: 0L) + saved
            }
        }

        val overallProgress = if (totalTarget > 0L) {
            (totalSaved.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        return GoalsOverview(
            activeGoalsCount = activeGoals.size,
            reachedGoalsCount = reachedCount,
            totalSavedCents = totalSaved,
            totalTargetCents = totalTarget,
            overallProgressRatio = overallProgress,
            unallocatedBalanceCents = totalWalletBalanceCents - totalSaved,
            reservedByWallet = reservedByWallet
        )
    }

    fun computeMonthlyTrend(
        allTransactions: List<MoneyTransaction>,
        anchorMonth: YearMonth,
        monthsCount: Int = 6,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<MonthlyTrendPoint> {
        if (monthsCount <= 0) return emptyList()
        val firstMonth = anchorMonth.minusMonths((monthsCount - 1).toLong())
        val totalsByMonth = HashMap<YearMonth, Pair<Long, Long>>(monthsCount * 2)

        for (tx in allTransactions) {
            if (tx.type == TransactionType.TRANSFER) continue
            val txMonth = YearMonth.from(Instant.ofEpochMilli(tx.timestampMillis).atZone(zone))
            if (txMonth < firstMonth || txMonth > anchorMonth) continue
            val current = totalsByMonth[txMonth] ?: (0L to 0L)
            totalsByMonth[txMonth] = when (tx.type) {
                TransactionType.INCOME -> (current.first + tx.amountCents) to current.second
                TransactionType.EXPENSE -> current.first to (current.second + tx.amountCents)
                TransactionType.TRANSFER -> current
            }
        }

        return (0 until monthsCount).map { offset ->
            val month = firstMonth.plusMonths(offset.toLong())
            val (income, expense) = totalsByMonth[month] ?: (0L to 0L)
            MonthlyTrendPoint(
                yearMonth = month,
                incomeCents = income,
                expenseCents = expense,
                netCents = income - expense
            )
        }
    }

    fun computeDailyExpenseSeries(
        monthTransactions: List<MoneyTransaction>,
        month: YearMonth,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<DailySpendPoint> {
        val spendByDay = HashMap<LocalDate, Long>()
        for (tx in monthTransactions) {
            if (tx.type != TransactionType.EXPENSE) continue
            val date = Instant.ofEpochMilli(tx.timestampMillis).atZone(zone).toLocalDate()
            if (YearMonth.from(date) == month) {
                spendByDay[date] = (spendByDay[date] ?: 0L) + tx.amountCents
            }
        }
        return (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            val spend = if (date.isAfter(today)) null else (spendByDay[date] ?: 0L)
            DailySpendPoint(
                epochDay = date.toEpochDay(),
                dayOfMonth = dayOfMonth,
                expenseCents = spend
            )
        }
    }
}
