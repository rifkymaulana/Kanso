package com.kanso.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.BrainRepository
import com.kanso.core.database.repository.HabitsRepository
import com.kanso.core.database.repository.LifeAdminRepository
import com.kanso.core.database.repository.MoneyRepository
import com.kanso.core.database.repository.TaskToggleOutcome
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CategoryBudget
import com.kanso.core.model.CoachInsight
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.HabitItem
import com.kanso.core.model.IouDirection
import com.kanso.core.model.IouEntry
import com.kanso.core.model.LifeAdminCalculations
import com.kanso.core.model.MaintenanceLog
import com.kanso.core.model.MoneyCalculations
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.NoteItem
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.SafeToSpendReport
import com.kanso.core.model.SmartCoachCalculations
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class TodayUiState(
    val greeting: String = "Good day",
    val formattedDate: String = "",
    val currency: AppCurrency = AppCurrency.IDR,
    val monthExpenseCents: Long = 0L,
    val monthIncomeCents: Long = 0L,
    val todayExpenseCents: Long = 0L,
    val monthlyBudgetCents: Long = 0L,
    val dueBills: List<RecurringBill> = emptyList(),
    val expiringItems: List<ExpiryItem> = emptyList(),
    val dueMaintenanceLogs: List<MaintenanceLog> = emptyList(),
    val dueOrOverdueTasksCount: Int = 0,
    val openIouCount: Int = 0,
    val dueSoonIousCount: Int = 0,
    val openIouNetCents: Long = 0L,
    val todayHabitsDone: Int = 0,
    val todayHabitsTotal: Int = 0,
    val pendingTodayHabits: List<HabitItem> = emptyList(),
    val todayWaterMl: Int = 0,
    val waterGoalMl: Int = 2000,
    val focusTasks: List<TaskItem> = emptyList(),
    val pinnedSnippets: List<SnippetItem> = emptyList(),
    val recentNotes: List<NoteItem> = emptyList(),
    val safeToSpend: SafeToSpendReport? = null,
    val coachInsights: List<CoachInsight> = emptyList(),
    val hideBalances: Boolean = false
) {
    val hasAttentionItems: Boolean
        get() = dueBills.isNotEmpty() ||
            expiringItems.isNotEmpty() ||
            dueMaintenanceLogs.isNotEmpty() ||
            dueOrOverdueTasksCount > 0 ||
            openIouCount > 0
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val moneyRepository: MoneyRepository,
    private val brainRepository: BrainRepository,
    private val habitsRepository: HabitsRepository,
    lifeAdminRepository: LifeAdminRepository
) : ViewModel() {

    private val currentMonth = YearMonth.now()
    private val todayEpoch = LocalDate.now().toEpochDay()

    private val moneyFlow = combine(
        moneyRepository.observeTransactionsForMonth(currentMonth),
        moneyRepository.observeTransactionsForMonth(currentMonth.minusMonths(1)),
        moneyRepository.observeBudgets(),
        moneyRepository.observeBills(),
        moneyRepository.observeIous()
    ) { transactions, previousTransactions, budgets, bills, ious ->
        MoneyBundle(
            transactions = transactions,
            previousTransactions = previousTransactions,
            budgets = budgets,
            bills = bills,
            ious = ious
        )
    }

    private val brainFlow = combine(
        brainRepository.observeTasks(),
        brainRepository.observeNotes(),
        brainRepository.observeSnippets()
    ) { tasks, notes, snippets ->
        Triple(tasks, notes, snippets)
    }

    private val lifeHabitsFlow = combine(
        habitsRepository.observeActiveHabits(),
        habitsRepository.observeCompletionsSince(todayEpoch),
        habitsRepository.observeHealthLog(todayEpoch),
        lifeAdminRepository.observeExpiries(),
        lifeAdminRepository.observeMaintenanceLogs()
    ) { habits, todayCompletions, healthLog, expiries, maintenanceLogs ->
        LifeHabitsBundle(
            habits = habits,
            completedTodayIds = todayCompletions.map { it.habitId }.toSet(),
            todayWaterMl = healthLog?.waterMl ?: 0,
            expiries = expiries,
            maintenanceLogs = maintenanceLogs
        )
    }

    val uiState: StateFlow<TodayUiState> = combine(
        preferencesRepository.preferences,
        moneyFlow,
        brainFlow,
        lifeHabitsFlow
    ) { prefs, moneyBundle, brainTriple, lifeHabits ->
        val transactions = moneyBundle.transactions
        val bills = moneyBundle.bills
        val ious = moneyBundle.ious
        val (tasks, notes, snippets) = brainTriple

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayEpochDay = today.toEpochDay()
        val startOfTodayMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfTodayMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val summary = MoneyCalculations.summarize(transactions)
        val prevSummary = MoneyCalculations.summarize(moneyBundle.previousTransactions)
        val budgetStatuses = MoneyCalculations.computeBudgetStatuses(moneyBundle.budgets, summary.byCategory)
        val todayExpense = transactions
            .filter {
                it.type == TransactionType.EXPENSE &&
                    it.timestampMillis in startOfTodayMillis until endOfTodayMillis
            }
            .sumOf { it.amountCents }

        val dueBills = bills.filter { MoneyCalculations.isBillDueSoon(it, today, withinDays = 5) }
        val expiring = lifeHabits.expiries
            .filter { LifeAdminCalculations.needsAttention(it, todayEpochDay) }
            .take(3)
        val dueMaintenance = LifeAdminCalculations.activeDueMaintenanceLogs(
            logs = lifeHabits.maintenanceLogs,
            todayEpochDay = todayEpochDay,
            withinDays = 14
        ).take(3)

        val openTasks = tasks.filter { !it.isCompleted }
        val dueOrOverdueCount = openTasks.count { task ->
            val due = task.dueDateEpochDay
            due != null && due <= todayEpochDay
        }
        val focusTasks = openTasks
            .sortedWith(
                compareBy<TaskItem>(
                    { it.dueDateEpochDay ?: Long.MAX_VALUE },
                    { -it.priority.ordinal },
                    { -it.createdAtMillis }
                )
            )
            .take(5)

        val openIous = ious.filter { !it.isSettled }
        val dueSoonIousCount = openIous.count { MoneyCalculations.isIouDueSoon(it, todayEpochDay, withinDays = 3) }
        val iouNet = openIous.sumOf { entry ->
            if (entry.direction == IouDirection.OWES_ME) entry.amountCents else -entry.amountCents
        }

        val doneHabitCount = lifeHabits.habits.count { lifeHabits.completedTodayIds.contains(it.id) }
        val pendingHabits = lifeHabits.habits
            .filter { !lifeHabits.completedTodayIds.contains(it.id) }
            .take(3)

        val pinnedSnippets = snippets.filter { it.isPinned }.take(6)
        val topNotes = notes.take(3)

        val hour = LocalTime.now(zone).hour
        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        val greeting = if (prefs.displayName.isNotBlank()) {
            "$timeGreeting, ${prefs.displayName}"
        } else {
            timeGreeting
        }

        val dateLabel = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))

        val safeToSpend = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = YearMonth.from(today),
            today = today,
            monthlyBudgetCents = prefs.overallMonthlyBudgetCents,
            monthIncomeCents = summary.incomeCents,
            monthExpenseCents = summary.expenseCents,
            todayExpenseCents = todayExpense,
            bills = bills
        )
        val coachInsights = SmartCoachCalculations.buildTodayInsights(
            report = safeToSpend,
            currency = prefs.currency,
            currentSummary = summary,
            previousSummary = prevSummary,
            budgetStatuses = budgetStatuses,
            todayWaterMl = lifeHabits.todayWaterMl,
            waterGoalMl = prefs.waterGoalMl,
            todayHabitsDone = doneHabitCount,
            todayHabitsTotal = lifeHabits.habits.size,
            hourOfDay = hour,
            hideAmounts = prefs.hideBalances
        )

        TodayUiState(
            greeting = greeting,
            formattedDate = dateLabel,
            currency = prefs.currency,
            monthExpenseCents = summary.expenseCents,
            monthIncomeCents = summary.incomeCents,
            todayExpenseCents = todayExpense,
            monthlyBudgetCents = prefs.overallMonthlyBudgetCents,
            dueBills = dueBills,
            expiringItems = expiring,
            dueMaintenanceLogs = dueMaintenance,
            dueOrOverdueTasksCount = dueOrOverdueCount,
            openIouCount = openIous.size,
            dueSoonIousCount = dueSoonIousCount,
            openIouNetCents = iouNet,
            todayHabitsDone = doneHabitCount,
            todayHabitsTotal = lifeHabits.habits.size,
            pendingTodayHabits = pendingHabits,
            todayWaterMl = lifeHabits.todayWaterMl,
            waterGoalMl = prefs.waterGoalMl,
            focusTasks = focusTasks,
            pinnedSnippets = pinnedSnippets,
            recentNotes = topNotes,
            safeToSpend = safeToSpend,
            coachInsights = coachInsights,
            hideBalances = prefs.hideBalances
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(
            formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
        )
    )

    fun toggleTaskComplete(
        task: TaskItem,
        onRescheduled: (previous: TaskItem, nextDueEpochDay: Long) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val outcome = brainRepository.toggleTaskCompleted(task)
            if (outcome is TaskToggleOutcome.Rescheduled) {
                onRescheduled(outcome.previous, outcome.nextDueEpochDay)
            }
        }
    }

    fun restoreTask(task: TaskItem) {
        viewModelScope.launch {
            brainRepository.upsertTask(task)
        }
    }

    fun toggleNoteChecklistLine(note: NoteItem, lineIndex: Int) {
        viewModelScope.launch {
            brainRepository.toggleNoteChecklistLine(note, lineIndex)
        }
    }

    fun payBillNow(bill: RecurringBill) {
        viewModelScope.launch {
            moneyRepository.payRecurringBill(
                bill = bill,
                yearMonth = YearMonth.now(),
                nowMillis = Instant.now().toEpochMilli()
            )
        }
    }

    fun toggleHabitToday(habitId: Long) {
        viewModelScope.launch {
            habitsRepository.toggleCompletion(habitId, LocalDate.now().toEpochDay())
        }
    }

    fun addWater250ml() {
        viewModelScope.launch {
            habitsRepository.adjustWater(LocalDate.now().toEpochDay(), 250)
        }
    }

    fun toggleHideBalances() {
        viewModelScope.launch {
            preferencesRepository.toggleHideBalances()
        }
    }

    private data class MoneyBundle(
        val transactions: List<MoneyTransaction>,
        val previousTransactions: List<MoneyTransaction>,
        val budgets: List<CategoryBudget>,
        val bills: List<RecurringBill>,
        val ious: List<IouEntry>
    )

    private data class LifeHabitsBundle(
        val habits: List<HabitItem>,
        val completedTodayIds: Set<Long>,
        val todayWaterMl: Int,
        val expiries: List<ExpiryItem>,
        val maintenanceLogs: List<MaintenanceLog>
    )
}
