package com.kanso.core.model

import kotlinx.serialization.Serializable

@Serializable
data class HabitItem(
    val id: Long = 0L,
    val name: String,
    val targetDaysPerWeek: Int = 7,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val isArchived: Boolean = false,
    val createdAtMillis: Long
)

@Serializable
data class HabitCompletion(
    val habitId: Long,
    val epochDay: Long
)

@Serializable
data class DailyHealthLog(
    val epochDay: Long,
    val waterMl: Int = 0,
    val sleepHours: Float? = null,
    val weightKg: Float? = null,
    val mood: Int? = null,
    val workoutNote: String = "",
    val updatedAtMillis: Long
)

object HabitCalculations {

    fun currentStreak(completedDays: Set<Long>, todayEpochDay: Long): Int {
        if (completedDays.isEmpty()) return 0
        var cursor = when {
            completedDays.contains(todayEpochDay) -> todayEpochDay
            completedDays.contains(todayEpochDay - 1L) -> todayEpochDay - 1L
            else -> return 0
        }
        var streak = 0
        while (completedDays.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    fun longestStreak(completedDays: Set<Long>): Int {
        if (completedDays.isEmpty()) return 0
        val sorted = completedDays.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1L) {
                run++
                if (run > best) best = run
            } else {
                run = 1
            }
        }
        return best
    }

    fun last30DaysGrid(completedDays: Set<Long>, todayEpochDay: Long): List<Pair<Long, Boolean>> {
        val start = todayEpochDay - 29L
        return (0L..29L).map { offset ->
            val day = start + offset
            day to completedDays.contains(day)
        }
    }

    fun completionRateLast30Days(completedDays: Set<Long>, todayEpochDay: Long): Float {
        val start = todayEpochDay - 29L
        val count = completedDays.count { it in start..todayEpochDay }
        return count / 30f
    }
}
