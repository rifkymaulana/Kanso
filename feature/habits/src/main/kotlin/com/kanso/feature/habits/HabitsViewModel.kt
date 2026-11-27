package com.kanso.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.HabitsRepository
import com.kanso.core.model.DailyHealthLog
import com.kanso.core.model.DailyHealthTrendPoint
import com.kanso.core.model.HabitCalculations
import com.kanso.core.model.HabitCompletion
import com.kanso.core.model.HabitItem
import com.kanso.core.model.HealthTrendWindow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class HabitRowUiModel(
    val habit: HabitItem,
    val isCompletedToday: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val last30DaysGrid: List<Pair<Long, Boolean>>,
    val rate30d: Float
)

data class HabitsUiState(
    val habits: List<HabitRowUiModel> = emptyList(),
    val completedTodayCount: Int = 0,
    val totalHabitsCount: Int = 0,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDateLabel: String = "Today",
    val isSelectedDateToday: Boolean = true,
    val waterGoalMl: Int = 2000,
    val selectedHealthLog: DailyHealthLog? = null,
    val recentHealthLogs: List<DailyHealthLog> = emptyList(),
    val avgWaterMl7d: Int = 0,
    val avgSleepHours7d: Float? = null,
    val avgMood7d: Float? = null,
    val trendWindow: HealthTrendWindow = HealthTrendWindow.DAYS_14,
    val healthTrendPoints: List<DailyHealthTrendPoint> = emptyList(),
    val latestWeightKg: Float? = null,
    val weightDeltaKg: Float? = null,
    val waterGoalHitDays: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitsRepository: HabitsRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private var lastObservedToday = LocalDate.now()
    private val selectedDateFlow = MutableStateFlow(lastObservedToday)
    private val trendWindowFlow = MutableStateFlow(HealthTrendWindow.DAYS_14)
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
    }
        .distinctUntilChanged()
        .onEach { newToday ->
            val previousToday = lastObservedToday
            if (newToday != previousToday) {
                lastObservedToday = newToday
                selectedDateFlow.update { currentSelected ->
                    if (currentSelected == previousToday) newToday else currentSelected
                }
            }
        }
    private val selectedHealthLogFlow = selectedDateFlow.flatMapLatest { date ->
        habitsRepository.observeHealthLog(date.toEpochDay())
    }
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

    private val habitsCoreFlow = todayFlow.flatMapLatest { today ->
        combine(
            habitsRepository.observeActiveHabits(),
            habitsRepository.observeCompletionsSince(today.minusDays(365).toEpochDay())
        ) { habits, completions ->
            Triple(today, habits, completions)
        }
    }

    private val healthCoreFlow = combine(
        selectedDateFlow,
        selectedHealthLogFlow,
        habitsRepository.observeRecentHealthLogs(35),
        trendWindowFlow
    ) { date, log, recent, window ->
        HealthCoreBundle(date, log, recent, window)
    }

    val uiState: StateFlow<HabitsUiState> = combine(
        habitsCoreFlow,
        healthCoreFlow,
        preferencesRepository.preferences
    ) { habitsTriple, healthBundle, prefs ->
        val (today, habits, completions) = habitsTriple
        val selectedDate = healthBundle.date
        val selectedLog = healthBundle.selectedLog
        val recentLogs = healthBundle.recentLogs
        val trendWindow = healthBundle.trendWindow
        val todayEpoch = today.toEpochDay()

        val daysByHabit = completions.groupBy({ it.habitId }, { it.epochDay })
            .mapValues { it.value.toSet() }

        val habitRows = habits.map { habit ->
            val days = daysByHabit[habit.id] ?: emptySet()
            HabitRowUiModel(
                habit = habit,
                isCompletedToday = days.contains(todayEpoch),
                currentStreak = HabitCalculations.currentStreak(days, todayEpoch),
                longestStreak = HabitCalculations.longestStreak(days),
                last30DaysGrid = HabitCalculations.last30DaysGrid(days, todayEpoch),
                rate30d = HabitCalculations.completionRateLast30Days(days, todayEpoch)
            )
        }

        val last7MinDay = todayEpoch - 6L
        val last7Logs = recentLogs.filter { it.epochDay in last7MinDay..todayEpoch }
        val avgWater = if (last7Logs.isNotEmpty()) {
            last7Logs.map { it.waterMl }.average().toInt()
        } else 0
        val sleepEntries = last7Logs.mapNotNull { it.sleepHours }
        val avgSleep = if (sleepEntries.isNotEmpty()) sleepEntries.average().toFloat() else null
        val moodEntries = last7Logs.mapNotNull { it.mood }
        val avgMood = if (moodEntries.isNotEmpty()) moodEntries.average().toFloat() else null

        val trendPoints = HabitCalculations.buildHealthTrendSeries(
            logs = recentLogs,
            todayEpochDay = todayEpoch,
            days = trendWindow.days
        )
        val latestWeight = recentLogs
            .sortedByDescending { it.epochDay }
            .firstOrNull { it.weightKg != null }
            ?.weightKg
        val weightDelta = HabitCalculations.computeWeightDelta(trendPoints)
        val goalHitDays = trendPoints.count { it.waterMl >= prefs.waterGoalMl }

        val dateLabel = when (selectedDate) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> selectedDate.format(dateFormatter)
        }

        HabitsUiState(
            habits = habitRows,
            completedTodayCount = habitRows.count { it.isCompletedToday },
            totalHabitsCount = habitRows.size,
            selectedDate = selectedDate,
            selectedDateLabel = dateLabel,
            isSelectedDateToday = selectedDate == today,
            waterGoalMl = prefs.waterGoalMl,
            selectedHealthLog = selectedLog,
            recentHealthLogs = recentLogs,
            avgWaterMl7d = avgWater,
            avgSleepHours7d = avgSleep,
            avgMood7d = avgMood,
            trendWindow = trendWindow,
            healthTrendPoints = trendPoints,
            latestWeightKg = latestWeight,
            weightDeltaKg = weightDelta,
            waterGoalHitDays = goalHitDays
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitsUiState()
    )

    fun selectTrendWindow(window: HealthTrendWindow) {
        trendWindowFlow.value = window
    }

    fun toggleHabitForToday(habitId: Long) {
        toggleHabitForDay(habitId, LocalDate.now().toEpochDay())
    }

    fun toggleHabitForDay(habitId: Long, epochDay: Long) {
        viewModelScope.launch {
            habitsRepository.toggleCompletion(habitId, epochDay)
        }
    }

    fun saveHabit(
        existing: HabitItem?,
        name: String,
        targetDaysPerWeek: Int,
        reminderHour: Int?,
        reminderMinute: Int?
    ) {
        viewModelScope.launch {
            habitsRepository.upsertHabit(
                HabitItem(
                    id = existing?.id ?: 0L,
                    name = name.trim(),
                    targetDaysPerWeek = targetDaysPerWeek.coerceIn(1, 7),
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    isArchived = existing?.isArchived ?: false,
                    createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch {
            habitsRepository.deleteHabit(id)
        }
    }

    fun deleteHabitWithUndo(
        habit: HabitItem,
        onDeleted: (List<HabitCompletion>) -> Unit
    ) {
        viewModelScope.launch {
            val captured = habitsRepository.deleteHabitAndCaptureCompletions(habit.id)
            onDeleted(captured)
        }
    }

    fun restoreHabit(habit: HabitItem, completions: List<HabitCompletion>) {
        viewModelScope.launch {
            habitsRepository.restoreHabitWithCompletions(habit, completions)
        }
    }

    fun previousDay() {
        selectedDateFlow.update { it.minusDays(1) }
    }

    fun nextDay() {
        val today = LocalDate.now()
        selectedDateFlow.update { current ->
            if (current.isBefore(today)) current.plusDays(1) else current
        }
    }

    fun adjustWater(deltaMl: Int) {
        val epochDay = selectedDateFlow.value.toEpochDay()
        viewModelScope.launch {
            habitsRepository.adjustWater(epochDay, deltaMl)
        }
    }

    fun saveHealthDetails(
        sleepHours: Float?,
        weightKg: Float?,
        mood: Int?,
        workoutNote: String
    ) {
        val epochDay = selectedDateFlow.value.toEpochDay()
        val existingWater = uiState.value.selectedHealthLog?.waterMl ?: 0
        viewModelScope.launch {
            habitsRepository.upsertHealthLog(
                DailyHealthLog(
                    epochDay = epochDay,
                    waterMl = existingWater,
                    sleepHours = sleepHours,
                    weightKg = weightKg,
                    mood = mood,
                    workoutNote = workoutNote.trim(),
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    private data class HealthCoreBundle(
        val date: LocalDate,
        val selectedLog: DailyHealthLog?,
        val recentLogs: List<DailyHealthLog>,
        val trendWindow: HealthTrendWindow
    )
}
