package com.kanso.core.database.preferences

import android.content.Context
import com.kanso.core.model.PomodoroCalculations
import com.kanso.core.model.PomodoroMode
import com.kanso.core.model.PomodoroState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroTimerManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    private val _completedEvents = MutableSharedFlow<PomodoroMode>(extraBufferCapacity = 1)
    val completedEvents: SharedFlow<PomodoroMode> = _completedEvents.asSharedFlow()

    init {
        reconcileWithClock()
        if (_state.value.isRunning) {
            startTicker()
        }
    }

    fun selectMode(mode: PomodoroMode) {
        synchronized(lock) {
            stopTicker()
            prefs.edit()
                .putString(KEY_MODE, mode.name)
                .putInt(KEY_PAUSED_REMAINING, mode.durationSeconds)
                .putLong(KEY_END_EPOCH_MILLIS, -1L)
                .apply()
            _state.value = PomodoroState(
                mode = mode,
                remainingSeconds = mode.durationSeconds,
                totalSeconds = mode.durationSeconds,
                isRunning = false,
                completedFocusSessions = prefs.getInt(KEY_COMPLETED_FOCUS, 0),
                endEpochMillis = null
            )
        }
    }

    fun toggle() {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val current = _state.value
            if (current.isRunning) {
                val endMillis = current.endEpochMillis ?: now
                val remaining = PomodoroCalculations.computeRemainingSeconds(endMillis, now)
                stopTicker()
                prefs.edit()
                    .putInt(KEY_PAUSED_REMAINING, remaining)
                    .putLong(KEY_END_EPOCH_MILLIS, -1L)
                    .apply()
                _state.value = current.copy(
                    remainingSeconds = remaining,
                    isRunning = false,
                    endEpochMillis = null
                )
            } else {
                val startingSeconds = if (current.remainingSeconds <= 0) {
                    current.totalSeconds
                } else {
                    current.remainingSeconds
                }
                val newEndMillis = now + startingSeconds * 1000L
                prefs.edit()
                    .putInt(KEY_PAUSED_REMAINING, startingSeconds)
                    .putLong(KEY_END_EPOCH_MILLIS, newEndMillis)
                    .apply()
                _state.value = current.copy(
                    remainingSeconds = startingSeconds,
                    isRunning = true,
                    endEpochMillis = newEndMillis
                )
                startTicker()
            }
        }
    }

    fun reset() {
        synchronized(lock) {
            stopTicker()
            val mode = _state.value.mode
            prefs.edit()
                .putInt(KEY_PAUSED_REMAINING, mode.durationSeconds)
                .putLong(KEY_END_EPOCH_MILLIS, -1L)
                .apply()
            _state.value = _state.value.copy(
                remainingSeconds = mode.durationSeconds,
                totalSeconds = mode.durationSeconds,
                isRunning = false,
                endEpochMillis = null
            )
        }
    }

    fun reconcileWithClock(nowMillis: Long = System.currentTimeMillis()): PomodoroMode? {
        synchronized(lock) {
            val modeName = prefs.getString(KEY_MODE, PomodoroMode.FOCUS.name)
            val mode = runCatching { PomodoroMode.valueOf(modeName ?: PomodoroMode.FOCUS.name) }
                .getOrDefault(PomodoroMode.FOCUS)
            val completedFocus = prefs.getInt(KEY_COMPLETED_FOCUS, 0)
            val endMillis = prefs.getLong(KEY_END_EPOCH_MILLIS, -1L)

            if (endMillis > 0L) {
                val remaining = PomodoroCalculations.computeRemainingSeconds(endMillis, nowMillis)
                if (remaining > 0) {
                    _state.value = PomodoroState(
                        mode = mode,
                        remainingSeconds = remaining,
                        totalSeconds = mode.durationSeconds,
                        isRunning = true,
                        completedFocusSessions = completedFocus,
                        endEpochMillis = endMillis
                    )
                    return null
                } else {
                    stopTicker()
                    val lastNotifiedEnd = prefs.getLong(KEY_LAST_NOTIFIED_END_MILLIS, -1L)
                    val shouldNotify = lastNotifiedEnd != endMillis
                    val updatedFocusCount = if (shouldNotify && mode == PomodoroMode.FOCUS) {
                        completedFocus + 1
                    } else {
                        completedFocus
                    }
                    prefs.edit()
                        .putInt(KEY_PAUSED_REMAINING, 0)
                        .putLong(KEY_END_EPOCH_MILLIS, -1L)
                        .putInt(KEY_COMPLETED_FOCUS, updatedFocusCount)
                        .apply {
                            if (shouldNotify) {
                                putLong(KEY_LAST_NOTIFIED_END_MILLIS, endMillis)
                            }
                        }
                        .apply()
                    _state.value = PomodoroState(
                        mode = mode,
                        remainingSeconds = 0,
                        totalSeconds = mode.durationSeconds,
                        isRunning = false,
                        completedFocusSessions = updatedFocusCount,
                        endEpochMillis = null
                    )
                    return if (shouldNotify) {
                        _completedEvents.tryEmit(mode)
                        mode
                    } else {
                        null
                    }
                }
            } else {
                val pausedRemaining = prefs.getInt(KEY_PAUSED_REMAINING, mode.durationSeconds)
                    .coerceIn(0, mode.durationSeconds)
                _state.value = PomodoroState(
                    mode = mode,
                    remainingSeconds = pausedRemaining,
                    totalSeconds = mode.durationSeconds,
                    isRunning = false,
                    completedFocusSessions = completedFocus,
                    endEpochMillis = null
                )
                return null
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                reconcileWithClock()
                if (!_state.value.isRunning) break
                delay(500L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    companion object {
        private const val PREFS_NAME = "kanso_pomodoro_prefs"
        private const val KEY_MODE = "pomodoro_mode"
        private const val KEY_PAUSED_REMAINING = "pomodoro_paused_remaining"
        private const val KEY_END_EPOCH_MILLIS = "pomodoro_end_epoch_millis"
        private const val KEY_COMPLETED_FOCUS = "pomodoro_completed_focus"
        private const val KEY_LAST_NOTIFIED_END_MILLIS = "pomodoro_last_notified_end_millis"
    }
}
