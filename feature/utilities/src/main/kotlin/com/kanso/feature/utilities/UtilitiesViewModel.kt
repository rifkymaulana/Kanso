package com.kanso.feature.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PomodoroTimerManager
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.UtilitiesRepository
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CountdownItem
import com.kanso.core.model.OfflineCurrencyConverter
import com.kanso.core.model.PomodoroMode
import com.kanso.core.model.PomodoroState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UtilitiesUiState(
    val defaultCurrency: AppCurrency = AppCurrency.IDR,
    val effectiveRatesVsUsd: Map<String, Double> = OfflineCurrencyConverter.defaultRatesVsUsd,
    val countdowns: List<CountdownItem> = emptyList(),
    val pomodoro: PomodoroState = PomodoroState()
)

@HiltViewModel
class UtilitiesViewModel @Inject constructor(
    private val utilitiesRepository: UtilitiesRepository,
    private val preferencesRepository: PreferencesRepository,
    private val pomodoroTimerManager: PomodoroTimerManager
) : ViewModel() {

    val uiState: StateFlow<UtilitiesUiState> = combine(
        preferencesRepository.preferences,
        utilitiesRepository.observeCountdowns(),
        pomodoroTimerManager.state
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
        pomodoroTimerManager.selectMode(mode)
    }

    fun toggleTimer() {
        pomodoroTimerManager.toggle()
    }

    fun resetTimer() {
        pomodoroTimerManager.reset()
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

    fun restoreCountdown(item: CountdownItem) {
        viewModelScope.launch {
            utilitiesRepository.upsertCountdown(item)
        }
    }
}
