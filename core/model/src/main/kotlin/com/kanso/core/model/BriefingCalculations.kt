package com.kanso.core.model

import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale

enum class BriefingSlot {
    MORNING,
    EVENING
}

data class BriefingNotificationContent(
    val title: String,
    val body: String,
    val publicRedactedBody: String,
    val totalAttentionCount: Int
)

object BriefingCalculations {

    fun formatClockTime(hour: Int, minute: Int): String {
        return String.format(
            Locale.US,
            "%02d:%02d",
            hour.coerceIn(0, 23),
            minute.coerceIn(0, 59)
        )
    }

    fun computeDelayUntilNextTriggerMillis(
        now: LocalDateTime,
        targetHour: Int,
        targetMinute: Int
    ): Long {
        val safeHour = targetHour.coerceIn(0, 23)
        val safeMinute = targetMinute.coerceIn(0, 59)
        val todayTarget = now.toLocalDate().atTime(safeHour, safeMinute)
        val nextTrigger = if (todayTarget.isAfter(now.plusSeconds(30))) {
            todayTarget
        } else {
            todayTarget.plusDays(1L)
        }
        return Duration.between(now, nextTrigger).toMillis().coerceAtLeast(60_000L)
    }

    fun buildMorningBriefing(
        dueBillsCount: Int,
        dueTasksCount: Int,
        expiringCount: Int,
        dueMaintenanceCount: Int,
        dueIousCount: Int,
        morningHabitsCount: Int
    ): BriefingNotificationContent? {
        val parts = buildList {
            if (dueBillsCount > 0) add(if (dueBillsCount == 1) "1 bill due" else "$dueBillsCount bills due")
            if (dueTasksCount > 0) add(if (dueTasksCount == 1) "1 task due" else "$dueTasksCount tasks due")
            if (expiringCount > 0) add(if (expiringCount == 1) "1 expiry alert" else "$expiringCount expiry alerts")
            if (dueMaintenanceCount > 0) add(if (dueMaintenanceCount == 1) "1 service due" else "$dueMaintenanceCount services due")
            if (dueIousCount > 0) add(if (dueIousCount == 1) "1 IOU due" else "$dueIousCount IOUs due")
            if (morningHabitsCount > 0) add(if (morningHabitsCount == 1) "1 morning habit" else "$morningHabitsCount morning habits")
        }
        if (parts.isEmpty()) return null
        val total = dueBillsCount + dueTasksCount + expiringCount + dueMaintenanceCount + dueIousCount + morningHabitsCount
        return BriefingNotificationContent(
            title = "Kanso Morning Briefing",
            body = parts.joinToString(" • "),
            publicRedactedBody = if (total == 1) "1 item needs your attention today" else "$total items need your attention today",
            totalAttentionCount = total
        )
    }

    fun buildEveningCheckIn(
        pendingHabitsCount: Int,
        waterRemainingMl: Int,
        dueTasksCount: Int
    ): BriefingNotificationContent? {
        val parts = buildList {
            if (pendingHabitsCount > 0) add(if (pendingHabitsCount == 1) "1 habit left today" else "$pendingHabitsCount habits left today")
            if (waterRemainingMl > 0) add("$waterRemainingMl ml water left")
            if (dueTasksCount > 0) add(if (dueTasksCount == 1) "1 open due task" else "$dueTasksCount open due tasks")
        }
        if (parts.isEmpty()) return null
        val total = pendingHabitsCount + dueTasksCount + (if (waterRemainingMl > 0) 1 else 0)
        return BriefingNotificationContent(
            title = "Kanso Evening Check-In",
            body = parts.joinToString(" • "),
            publicRedactedBody = if (total == 1) "1 check-in item left before bed" else "$total check-in items left today",
            totalAttentionCount = total
        )
    }
}
