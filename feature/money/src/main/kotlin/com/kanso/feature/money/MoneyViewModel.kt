package com.kanso.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.MoneyRepository
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.BillFrequency
import com.kanso.core.model.CategoryBudget
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.IouDirection
import com.kanso.core.model.IouEntry
import com.kanso.core.model.MoneyCalculations
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.TransactionType
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
import java.time.YearMonth
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
    val budgetStatuses: List<MoneyCalculations.BudgetStatus> = emptyList(),
    val budgets: List<CategoryBudget> = emptyList(),
    val bills: List<RecurringBill> = emptyList(),
    val openIous: List<IouEntry> = emptyList(),
    val settledIous: List<IouEntry> = emptyList(),
    val owesMeTotalCents: Long = 0L,
    val iOweTotalCents: Long = 0L
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val selectedMonthFlow = MutableStateFlow(YearMonth.now())
    private val monthTransactionsFlow = selectedMonthFlow.flatMapLatest { month ->
        moneyRepository.observeTransactionsForMonth(month)
    }

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    private val coreMoneyFlow = combine(
        selectedMonthFlow,
        monthTransactionsFlow,
        moneyRepository.observeBudgets()
    ) { month, transactions, budgets ->
        Triple(month, transactions, budgets)
    }

    val uiState: StateFlow<MoneyUiState> = combine(
        coreMoneyFlow,
        moneyRepository.observeBills(),
        moneyRepository.observeIous(),
        preferencesRepository.preferences
    ) { triple, bills, ious, prefs ->
        val (month, transactions, budgets) = triple
        val summary = MoneyCalculations.summarize(transactions)
        val budgetStatuses = MoneyCalculations.computeBudgetStatuses(budgets, summary.byCategory)
        val openIous = ious.filter { !it.isSettled }
        val settledIous = ious.filter { it.isSettled }

        MoneyUiState(
            selectedMonth = month,
            selectedMonthLabel = month.format(monthFormatter),
            currency = prefs.currency,
            overallMonthlyBudgetCents = prefs.overallMonthlyBudgetCents,
            summary = summary,
            transactions = transactions,
            budgetStatuses = budgetStatuses,
            budgets = budgets,
            bills = bills,
            openIous = openIous,
            settledIous = settledIous,
            owesMeTotalCents = openIous.filter { it.direction == IouDirection.OWES_ME }.sumOf { it.amountCents },
            iOweTotalCents = openIous.filter { it.direction == IouDirection.I_OWE }.sumOf { it.amountCents }
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

    fun saveTransaction(
        existingId: Long = 0L,
        type: TransactionType,
        amountCents: Long,
        category: ExpenseCategory,
        wallet: WalletType,
        note: String,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            moneyRepository.upsertTransaction(
                MoneyTransaction(
                    id = existingId,
                    type = type,
                    amountCents = amountCents,
                    category = category,
                    wallet = wallet,
                    note = note.trim(),
                    timestampMillis = timestampMillis
                )
            )
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
}
