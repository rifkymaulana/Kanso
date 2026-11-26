package com.kanso.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.MoneyRepository
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.BillFrequency
import com.kanso.core.model.CategoryBudget
import com.kanso.core.model.CoachInsight
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.IouDirection
import com.kanso.core.model.IouEntry
import com.kanso.core.model.MoneyCalculations
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.SafeToSpendReport
import com.kanso.core.model.SmartCoachCalculations
import com.kanso.core.model.TransactionType
import com.kanso.core.model.WalletBalanceSummary
import com.kanso.core.model.WalletType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class MoneyUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val selectedMonthLabel: String = "",
    val currency: AppCurrency = AppCurrency.IDR,
    val overallMonthlyBudgetCents: Long = 0L,
    val summary: MoneyCalculations.MonthlySummary = MoneyCalculations.MonthlySummary(0L, 0L, 0L, emptyMap()),
    val transactions: List<MoneyTransaction> = emptyList(),
    val transactionFilter: MoneyCalculations.TransactionFilter = MoneyCalculations.TransactionFilter(),
    val filteredTransactions: List<MoneyTransaction> = emptyList(),
    val dailyGroups: List<MoneyCalculations.DailyTransactionGroup> = emptyList(),
    val budgetStatuses: List<MoneyCalculations.BudgetStatus> = emptyList(),
    val budgets: List<CategoryBudget> = emptyList(),
    val bills: List<RecurringBill> = emptyList(),
    val openIous: List<IouEntry> = emptyList(),
    val settledIous: List<IouEntry> = emptyList(),
    val owesMeTotalCents: Long = 0L,
    val iOweTotalCents: Long = 0L,
    val safeToSpend: SafeToSpendReport? = null,
    val coachInsights: List<CoachInsight> = emptyList(),
    val walletBalances: List<WalletBalanceSummary> = emptyList(),
    val totalWalletBalanceCents: Long = 0L
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val selectedMonthFlow = MutableStateFlow(YearMonth.now())
    private val filterFlow = MutableStateFlow(MoneyCalculations.TransactionFilter())
    private val monthTransactionsFlow = selectedMonthFlow.flatMapLatest { month ->
        moneyRepository.observeTransactionsForMonth(month)
    }
    private val previousMonthTransactionsFlow = selectedMonthFlow.flatMapLatest { month ->
        moneyRepository.observeTransactionsForMonth(month.minusMonths(1))
    }

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    private val coreMoneyFlow = combine(
        selectedMonthFlow,
        monthTransactionsFlow,
        previousMonthTransactionsFlow,
        moneyRepository.observeAllTransactions(),
        moneyRepository.observeBudgets()
    ) { month, transactions, previousTransactions, allTransactions, budgets ->
        CoreMoneyBundle(month, transactions, previousTransactions, allTransactions, budgets)
    }

    val uiState: StateFlow<MoneyUiState> = combine(
        coreMoneyFlow,
        moneyRepository.observeBills(),
        moneyRepository.observeIous(),
        preferencesRepository.preferences,
        filterFlow
    ) { bundle, bills, ious, prefs, filter ->
        val month = bundle.month
        val transactions = bundle.transactions
        val budgets = bundle.budgets

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val currentMonth = YearMonth.from(today)
        val startOfTodayMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfTodayMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val todayExpenseCents = if (month == currentMonth) {
            transactions
                .filter {
                    it.type == TransactionType.EXPENSE &&
                        it.timestampMillis in startOfTodayMillis until endOfTodayMillis
                }
                .sumOf { it.amountCents }
        } else {
            0L
        }

        val summary = MoneyCalculations.summarize(transactions)
        val prevSummary = MoneyCalculations.summarize(bundle.previousTransactions)
        val filteredTransactions = MoneyCalculations.filterTransactions(transactions, filter)
        val dailyGroups = MoneyCalculations.groupTransactionsByDay(filteredTransactions, zone)
        val budgetStatuses = MoneyCalculations.computeBudgetStatuses(budgets, summary.byCategory)
        val safeToSpend = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = month,
            today = today,
            monthlyBudgetCents = prefs.overallMonthlyBudgetCents,
            monthIncomeCents = summary.incomeCents,
            monthExpenseCents = summary.expenseCents,
            todayExpenseCents = todayExpenseCents,
            bills = bills
        )
        val coachInsights = SmartCoachCalculations.buildMoneyInsights(
            report = safeToSpend,
            currency = prefs.currency,
            currentSummary = summary,
            previousSummary = prevSummary,
            budgetStatuses = budgetStatuses
        )
        val walletBalances = MoneyCalculations.computeWalletBalances(
            allTransactions = bundle.allTransactions,
            openingBalances = prefs.walletOpeningBalancesCents
        )
        val openIous = ious.filter { !it.isSettled }
        val settledIous = ious.filter { it.isSettled }

        MoneyUiState(
            selectedMonth = month,
            selectedMonthLabel = month.format(monthFormatter),
            currency = prefs.currency,
            overallMonthlyBudgetCents = prefs.overallMonthlyBudgetCents,
            summary = summary,
            transactions = transactions,
            transactionFilter = filter,
            filteredTransactions = filteredTransactions,
            dailyGroups = dailyGroups,
            budgetStatuses = budgetStatuses,
            budgets = budgets,
            bills = bills,
            openIous = openIous,
            settledIous = settledIous,
            owesMeTotalCents = openIous.filter { it.direction == IouDirection.OWES_ME }.sumOf { it.amountCents },
            iOweTotalCents = openIous.filter { it.direction == IouDirection.I_OWE }.sumOf { it.amountCents },
            safeToSpend = safeToSpend,
            coachInsights = coachInsights,
            walletBalances = walletBalances,
            totalWalletBalanceCents = walletBalances.sumOf { it.currentBalanceCents }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MoneyUiState(
            selectedMonthLabel = YearMonth.now().format(monthFormatter)
        )
    )

    fun previousMonth() {
        selectedMonthFlow.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        selectedMonthFlow.update { it.plusMonths(1) }
    }

    fun toggleTypeFilter(type: TransactionType) {
        filterFlow.update { current ->
            current.copy(type = if (current.type == type) null else type)
        }
    }

    fun toggleWalletFilter(wallet: WalletType) {
        filterFlow.update { current ->
            current.copy(wallet = if (current.wallet == wallet) null else wallet)
        }
    }

    fun toggleCategoryFilter(category: ExpenseCategory) {
        filterFlow.update { current ->
            current.copy(category = if (current.category == category) null else category)
        }
    }

    fun clearTransactionFilters() {
        filterFlow.value = MoneyCalculations.TransactionFilter()
    }

    fun saveTransaction(
        existingId: Long = 0L,
        type: TransactionType,
        amountCents: Long,
        category: ExpenseCategory,
        wallet: WalletType,
        toWallet: WalletType? = null,
        transferFeeCents: Long = 0L,
        note: String,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            if (existingId == 0L && type == TransactionType.TRANSFER && toWallet != null) {
                moneyRepository.recordTransfer(
                    fromWallet = wallet,
                    toWallet = toWallet,
                    amountCents = amountCents,
                    feeCents = transferFeeCents,
                    note = note,
                    timestampMillis = timestampMillis
                )
            } else {
                moneyRepository.upsertTransaction(
                    MoneyTransaction(
                        id = existingId,
                        type = type,
                        amountCents = amountCents,
                        category = category,
                        wallet = wallet,
                        toWallet = if (type == TransactionType.TRANSFER) toWallet else null,
                        note = note.trim(),
                        timestampMillis = timestampMillis
                    )
                )
            }
        }
    }

    fun adjustWalletCurrentBalance(wallet: WalletType, desiredCurrentBalanceCents: Long) {
        viewModelScope.launch {
            val netDelta = uiState.value.walletBalances
                .firstOrNull { it.wallet == wallet }
                ?.netDeltaCents ?: 0L
            val newOpening = desiredCurrentBalanceCents - netDelta
            preferencesRepository.setWalletOpeningBalance(wallet, newOpening)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            moneyRepository.deleteTransaction(id)
        }
    }

    fun setOverallMonthlyBudget(limitCents: Long) {
        viewModelScope.launch {
            preferencesRepository.setOverallMonthlyBudgetCents(limitCents)
        }
    }

    fun saveCategoryBudget(existingId: Long = 0L, category: ExpenseCategory, limitCents: Long) {
        viewModelScope.launch {
            val existingForCategory = uiState.value.budgets.firstOrNull { it.category == category }
            moneyRepository.upsertBudget(
                CategoryBudget(
                    id = if (existingId != 0L) existingId else (existingForCategory?.id ?: 0L),
                    category = category,
                    limitCents = limitCents,
                    yearMonth = CategoryBudget.DEFAULT_YEAR_MONTH
                )
            )
        }
    }

    fun deleteCategoryBudget(id: Long) {
        viewModelScope.launch {
            moneyRepository.deleteBudget(id)
        }
    }

    fun saveRecurringBill(
        existing: RecurringBill? = null,
        name: String,
        amountCents: Long,
        category: ExpenseCategory,
        wallet: WalletType,
        dueDayOfMonth: Int,
        frequency: BillFrequency,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            moneyRepository.upsertBill(
                RecurringBill(
                    id = existing?.id ?: 0L,
                    name = name.trim(),
                    amountCents = amountCents,
                    category = category,
                    wallet = wallet,
                    dueDayOfMonth = dueDayOfMonth.coerceIn(1, 31),
                    frequency = frequency,
                    lastPaidYearMonth = existing?.lastPaidYearMonth,
                    isActive = isActive
                )
            )
        }
    }

    fun deleteRecurringBill(id: Long) {
        viewModelScope.launch {
            moneyRepository.deleteBill(id)
        }
    }

    fun payRecurringBill(bill: RecurringBill) {
        viewModelScope.launch {
            moneyRepository.payRecurringBill(bill = bill, yearMonth = YearMonth.now())
        }
    }

    fun saveIou(
        existing: IouEntry? = null,
        personName: String,
        amountCents: Long,
        direction: IouDirection,
        note: String,
        dueDateEpochDay: Long?
    ) {
        viewModelScope.launch {
            moneyRepository.upsertIou(
                IouEntry(
                    id = existing?.id ?: 0L,
                    personName = personName.trim(),
                    amountCents = amountCents,
                    direction = direction,
                    note = note.trim(),
                    createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
                    dueDateEpochDay = dueDateEpochDay,
                    isSettled = existing?.isSettled ?: false,
                    settledAtMillis = existing?.settledAtMillis
                )
            )
        }
    }

    fun settleIou(iou: IouEntry, recordTransaction: Boolean, wallet: WalletType) {
        viewModelScope.launch {
            moneyRepository.settleIou(iou, recordTransaction, wallet)
        }
    }

    fun deleteIou(id: Long) {
        viewModelScope.launch {
            moneyRepository.deleteIou(id)
        }
    }

    private data class CoreMoneyBundle(
        val month: YearMonth,
        val transactions: List<MoneyTransaction>,
        val previousTransactions: List<MoneyTransaction>,
        val allTransactions: List<MoneyTransaction>,
        val budgets: List<CategoryBudget>
    )
}
