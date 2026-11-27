package com.kanso.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PomodoroTimerManager
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.BrainRepository
import com.kanso.core.database.repository.HabitsRepository
import com.kanso.core.database.repository.LifeAdminRepository
import com.kanso.core.database.repository.MoneyRepository
import com.kanso.core.database.repository.TaskToggleOutcome
import com.kanso.core.database.repository.UtilitiesRepository
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CategoryBudget
import com.kanso.core.model.CoachInsight
import com.kanso.core.model.CountdownCalculations
import com.kanso.core.model.CountdownItem
import com.kanso.core.model.DailyHealthLog
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.HabitCompletion
import com.kanso.core.model.HabitItem
import com.kanso.core.model.IouDirection
import com.kanso.core.model.IouEntry
import com.kanso.core.model.LifeAdminCalculations
import com.kanso.core.model.MaintenanceLog
import com.kanso.core.model.MoneyCalculations
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.MonthlyReviewCalculations
import com.kanso.core.model.MonthlyReviewReport
import com.kanso.core.model.NoteItem
import com.kanso.core.model.PomodoroState
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.SafeToSpendReport
import com.kanso.core.model.SmartCoachCalculations
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class TodayUiState(
    val greeting: String = "Good day",
    val formattedDate: String = "",
    val todayEpochDay: Long = LocalDate.now().toEpochDay(),
    val currency: AppCurrency = AppCurrency.IDR,
    val monthExpenseCents: Long = 0L,
    val monthIncomeCents: Long = 0L,
    val todayExpenseCents: Long = 0L,
    val monthlyBudgetCents: Long = 0L,
    val dueBills: List<RecurringBill> = emptyList(),
    val expiringItems: List<ExpiryItem> = emptyList(),
    val totalExpiringCount: Int = 0,
    val dueMaintenanceLogs: List<MaintenanceLog> = emptyList(),
    val totalDueMaintenanceCount: Int = 0,
    val upcomingCountdowns: List<CountdownItem> = emptyList(),
    val totalUpcomingCountdownsCount: Int = 0,
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
    val activePomodoro: PomodoroState? = null,
    val monthlyReview: MonthlyReviewReport? = null,
    val hideBalances: Boolean = false
) {
    val hasAttentionItems: Boolean
        get() = dueBills.isNotEmpty() ||
            totalExpiringCount > 0 ||
            totalDueMaintenanceCount > 0 ||
            totalUpcomingCountdownsCount > 0 ||
            dueOrOverdueTasksCount > 0 ||
            dueSoonIousCount > 0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val moneyRepository: MoneyRepository,
    private val brainRepository: BrainRepository,
    private val habitsRepository: HabitsRepository,
    lifeAdminRepository: LifeAdminRepository,
    utilitiesRepository: UtilitiesRepository,
    private val pomodoroTimerManager: PomodoroTimerManager
) : ViewModel() {

    private val todayFlow: Flow<LocalDate> = flow {
        while (currentCoroutineContext().isActive) {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            val today = now.toLocalDate()
            emit(today)
            val nextMidnightMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val waitMillis = (nextMidnightMillis - now.toInstant().toEpochMilli() + 250L)
                .coerceIn(5_000L, 60_000L)
            delay(waitMillis)
        }
    }.distinctUntilChanged()

    private val moneyFlow = todayFlow
        .map { YearMonth.from(it) }
        .distinctUntilChanged()
        .flatMapLatest { currentMonth ->
            combine(
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
        }

    private val brainFlow = combine(
        brainRepository.observeTasks(),
        brainRepository.observeNotes(),
        brainRepository.observeSnippets(),
        utilitiesRepository.observeCountdowns(),
        pomodoroTimerManager.state
    ) { tasks, notes, snippets, countdowns, pomodoro ->
        BrainBundle(
            tasks = tasks,
            notes = notes,
            snippets = snippets,
            countdowns = countdowns,
            pomodoro = pomodoro
        )
    }

    private val lifeHabitsFlow = todayFlow.flatMapLatest { today ->
        val todayEpoch = today.toEpochDay()
        val monthStartEpoch = today.withDayOfMonth(1).toEpochDay()
        combine(
            habitsRepository.observeActiveHabits(),
            habitsRepository.observeCompletionsInRange(monthStartEpoch, todayEpoch),
            habitsRepository.observeHealthLogsInRange(monthStartEpoch, todayEpoch),
            lifeAdminRepository.observeExpiries(),
            lifeAdminRepository.observeMaintenanceLogs()
        ) { habits, monthCompletions, monthHealthLogs, expiries, maintenanceLogs ->
            val todayWaterMl = monthHealthLogs.firstOrNull { it.epochDay == todayEpoch }?.waterMl ?: 0
            LifeHabitsBundle(
                today = today,
                habits = habits,
                completedTodayIds = monthCompletions
                    .filter { it.epochDay == todayEpoch }
                    .map { it.habitId }
                    .toSet(),
                monthCompletions = monthCompletions,
                monthHealthLogs = monthHealthLogs,
                todayWaterMl = todayWaterMl,
                expiries = expiries,
                maintenanceLogs = maintenanceLogs
            )
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(
        preferencesRepository.preferences,
        moneyFlow,
        brainFlow,
        lifeHabitsFlow
    ) { prefs, moneyBundle, brainBundle, lifeHabits ->
        val transactions = moneyBundle.transactions
        val bills = moneyBundle.bills
        val ious = moneyBundle.ious
        val tasks = brainBundle.tasks
        val notes = brainBundle.notes
        val snippets = brainBundle.snippets

        val zone = ZoneId.systemDefault()
        val today = lifeHabits.today
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
        val allExpiring = lifeHabits.expiries
            .filter { LifeAdminCalculations.needsAttention(it, todayEpochDay) }
        val allDueMaintenance = LifeAdminCalculations.activeDueMaintenanceLogs(
            logs = lifeHabits.maintenanceLogs,
            todayEpochDay = todayEpochDay,
            withinDays = 14
        )
        val allUpcomingCountdowns = CountdownCalculations.upcomingCountdowns(
            items = brainBundle.countdowns,
            todayEpochDay = todayEpochDay,
            withinDays = 7
        )

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
        val dueSoonIous = openIous.filter { MoneyCalculations.isIouDueSoon(it, todayEpochDay, withinDays = 3) }
        val iouNet = dueSoonIous.sumOf { entry ->
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

        val targetMonth = YearMonth.from(today)
        val monthLabel = targetMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        val monthlyReview = MonthlyReviewCalculations.buildReport(
            targetMonth = targetMonth,
            today = today,
            monthLabel = monthLabel,
            transactions = transactions,
            previousMonthTransactions = moneyBundle.previousTransactions,
            activeHabits = lifeHabits.habits,
            monthCompletions = lifeHabits.monthCompletions,
            monthHealthLogs = lifeHabits.monthHealthLogs,
            waterGoalMl = prefs.waterGoalMl,
            allTasks = tasks,
            zone = zone
        )

        TodayUiState(
            greeting = greeting,
            formattedDate = dateLabel,
            todayEpochDay = todayEpochDay,
            currency = prefs.currency,
            monthExpenseCents = summary.expenseCents,
            monthIncomeCents = summary.incomeCents,
            todayExpenseCents = todayExpense,
            monthlyBudgetCents = prefs.overallMonthlyBudgetCents,
            dueBills = dueBills,
            expiringItems = allExpiring.take(3),
            totalExpiringCount = allExpiring.size,
            dueMaintenanceLogs = allDueMaintenance.take(3),
            totalDueMaintenanceCount = allDueMaintenance.size,
            upcomingCountdowns = allUpcomingCountdowns.take(3),
            totalUpcomingCountdownsCount = allUpcomingCountdowns.size,
            dueOrOverdueTasksCount = dueOrOverdueCount,
            openIouCount = openIous.size,
            dueSoonIousCount = dueSoonIous.size,
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
            activePomodoro = brainBundle.pomodoro.takeIf { it.isActiveOrPausedSession },
            monthlyReview = monthlyReview,
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

    fun payBillNow(
        bill: RecurringBill,
        onPaid: (MoneyRepository.BillPaymentRecord) -> Unit = {}
    ) {
        viewModelScope.launch {
            val record = moneyRepository.payRecurringBill(
                bill = bill,
                paidDate = LocalDate.now(),
                nowMillis = Instant.now().toEpochMilli()
            )
            onPaid(record)
        }
    }

    fun undoPayBill(record: MoneyRepository.BillPaymentRecord) {
        viewModelScope.launch {
            moneyRepository.undoPayRecurringBill(record)
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

    fun togglePomodoro() {
        pomodoroTimerManager.toggle()
    }

    fun saveMonthlyReviewToNotes(title: String, content: String) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            brainRepository.upsertNote(
                NoteItem(
                    title = title.trim(),
                    content = content.trim(),
                    tags = listOf("monthly-review"),
                    isPinned = false,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
        }
    }

    private data class MoneyBundle(
        val transactions: List<MoneyTransaction>,
        val previousTransactions: List<MoneyTransaction>,
        val budgets: List<CategoryBudget>,
        val bills: List<RecurringBill>,
        val ious: List<IouEntry>
    )

    private data class BrainBundle(
        val tasks: List<TaskItem>,
        val notes: List<NoteItem>,
        val snippets: List<SnippetItem>,
        val countdowns: List<CountdownItem>,
        val pomodoro: PomodoroState
    )

    private data class LifeHabitsBundle(
        val today: LocalDate,
        val habits: List<HabitItem>,
        val completedTodayIds: Set<Long>,
        val monthCompletions: List<HabitCompletion>,
        val monthHealthLogs: List<DailyHealthLog>,
        val todayWaterMl: Int,
        val expiries: List<ExpiryItem>,
        val maintenanceLogs: List<MaintenanceLog>
    )
}
