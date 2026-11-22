package com.kanso.feature.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.UtilitiesRepository
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CountdownItem
import com.kanso.core.model.OfflineCurrencyConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PomodoroMode(val label: String, val durationSeconds: Int) {
    FOCUS("Focus 25m", 25 * 60),
    SHORT_BREAK("Short 5m", 5 * 60),
    LONG_BREAK("Long 15m", 15 * 60)
}

data class PomodoroState(
    val mode: PomodoroMode = PomodoroMode.FOCUS,
    val remainingSeconds: Int = PomodoroMode.FOCUS.durationSeconds,
    val totalSeconds: Int = PomodoroMode.FOCUS.durationSeconds,
    val isRunning: Boolean = false,
    val completedFocusSessions: Int = 0
)

data class UtilitiesUiState(
    val defaultCurrency: AppCurrency = AppCurrency.IDR,
    val effectiveRatesVsUsd: Map<String, Double> = OfflineCurrencyConverter.defaultRatesVsUsd,
    val countdowns: List<CountdownItem> = emptyList(),
    val pomodoro: PomodoroState = PomodoroState()
)

@HiltViewModel
class UtilitiesViewModel @Inject constructor(
    private val utilitiesRepository: UtilitiesRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val pomodoroFlow = MutableStateFlow(PomodoroState())
    private var timerJob: Job? = null

    val uiState: StateFlow<UtilitiesUiState> = combine(
        preferencesRepository.preferences,
        utilitiesRepository.observeCountdowns(),
        pomodoroFlow
    ) { prefs, countdowns, pomodoro ->
        UtilitiesUiState(
            defaultCurrency = prefs.currency,
            effectiveRatesVsUsd = OfflineCurrencyConverter.effectiveRates(prefs.customRatesVsUsd),
            countdowns = countdowns,
            pomodoro = pomodoro
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UtilitiesUiState()
    )

    fun selectTimerMode(mode: PomodoroMode) {
        timerJob?.cancel()
        pomodoroFlow.update {
            it.copy(
                mode = mode,
                remainingSeconds = mode.durationSeconds,
                totalSeconds = mode.durationSeconds,
                isRunning = false
            )
        }
    }

    fun toggleTimer() {
        val current = pomodoroFlow.value
        if (current.isRunning) {
            timerJob?.cancel()
            pomodoroFlow.update { it.copy(isRunning = false) }
        } else {
            val startingSeconds = if (current.remainingSeconds <= 0) current.totalSeconds else current.remainingSeconds
            pomodoroFlow.update { it.copy(remainingSeconds = startingSeconds, isRunning = true) }
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (pomodoroFlow.value.remainingSeconds > 0 && pomodoroFlow.value.isRunning) {
                    delay(1_000L)
                    pomodoroFlow.update { state ->
                        val next = (state.remainingSeconds - 1).coerceAtLeast(0)
                        if (next == 0) {
                            val completed = if (state.mode == PomodoroMode.FOCUS) {
                                state.completedFocusSessions + 1
                            } else {
                                state.completedFocusSessions
                            }
                            state.copy(
                                remainingSeconds = 0,
                                isRunning = false,
                                completedFocusSessions = completed
                            )
                        } else {
                            state.copy(remainingSeconds = next)
                        }
                    }
                }
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        pomodoroFlow.update {
            it.copy(
                remainingSeconds = it.mode.durationSeconds,
                totalSeconds = it.mode.durationSeconds,
                isRunning = false
            )
        }
    }

    fun updateCustomRates(updatedRates: Map<String, Double>) {
        viewModelScope.launch {
            preferencesRepository.setCustomRatesVsUsd(updatedRates)
        }
    }

    fun resetCustomRates() {
        viewModelScope.launch {
            preferencesRepository.setCustomRatesVsUsd(emptyMap())
        }
    }

    fun saveCountdown(
        existing: CountdownItem?,
        title: String,
        targetEpochDay: Long,
        note: String
    ) {
        viewModelScope.launch {
            utilitiesRepository.upsertCountdown(
                CountdownItem(
                    id = existing?.id ?: 0L,
                    title = title.trim(),
                    targetEpochDay = targetEpochDay,
                    note = note.trim(),
                    createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteCountdown(id: Long) {
        viewModelScope.launch {
            utilitiesRepository.deleteCountdown(id)
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
