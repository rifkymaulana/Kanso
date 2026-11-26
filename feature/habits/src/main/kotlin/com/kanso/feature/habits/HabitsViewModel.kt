package com.kanso.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.HabitsRepository
import com.kanso.core.model.DailyHealthLog
import com.kanso.core.model.HabitCalculations
import com.kanso.core.model.HabitCompletion
import com.kanso.core.model.HabitItem
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
    val avgMood7d: Float? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitsRepository: HabitsRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(LocalDate.now())
    private val selectedHealthLogFlow = selectedDateFlow.flatMapLatest { date ->
        habitsRepository.observeHealthLog(date.toEpochDay())
    }
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

    private val habitsCoreFlow = combine(
        habitsRepository.observeActiveHabits(),
        habitsRepository.observeCompletionsSince(LocalDate.now().minusDays(365).toEpochDay())
    ) { habits, completions ->
        Pair(habits, completions)
    }

    private val healthCoreFlow = combine(
        selectedDateFlow,
        selectedHealthLogFlow,
        habitsRepository.observeRecentHealthLogs(14)
    ) { date, log, recent ->
        Triple(date, log, recent)
    }

    val uiState: StateFlow<HabitsUiState> = combine(
        habitsCoreFlow,
        healthCoreFlow,
        preferencesRepository.preferences
    ) { habitsPair, healthTriple, prefs ->
        val (habits, completions) = habitsPair
        val (selectedDate, selectedLog, recentLogs) = healthTriple
        val today = LocalDate.now()
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
            avgMood7d = avgMood
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitsUiState()
    )

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
}
