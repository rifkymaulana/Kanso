package com.kanso.app.widget

import java.text.NumberFormat
import java.util.Locale

data class WidgetDashboardData(
    val dateLabel: String,
    val monthExpenseFormatted: String,
    val openTasksCount: Int,
    val habitsDone: Int,
    val habitsTotal: Int,
    val waterMl: Int,
    val waterGoalMl: Int,
    val dueBillsCount: Int,
    val expiringCount: Int
)

object WidgetSummaryFormatter {

    fun formatAttentionLine(dueBillsCount: Int, expiringCount: Int): String? {
        val parts = buildList {
            if (dueBillsCount > 0) {
                add(if (dueBillsCount == 1) "1 bill due" else "$dueBillsCount bills due")
            }
            if (expiringCount > 0) {
                add("$expiringCount expiring")
            }
        }
        return if (parts.isEmpty()) null else parts.joinToString(" • ")
    }

    fun formatHabitsBadge(done: Int, total: Int): String {
        return if (total <= 0) "No habits" else "Habits $done/$total"
    }

    fun formatWaterBadge(waterMl: Int, goalMl: Int): String {
        val fmt = NumberFormat.getIntegerInstance(Locale.US)
        return "${fmt.format(waterMl)} / ${fmt.format(goalMl)} ml"
    }
}
