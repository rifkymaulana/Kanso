package com.kanso.app.widget

import com.kanso.app.navigation.KansoUris
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.SafeToSpendReport
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

data class WidgetMoneySummary(
    val label: String,
    val value: String,
    val subtitle: String
)

data class WidgetDashboardData(
    val dateLabel: String,
    val moneyLabel: String,
    val moneyValue: String,
    val moneySubtitle: String,
    val openTasksCount: Int,
    val habitsDone: Int,
    val habitsTotal: Int,
    val waterMl: Int,
    val waterGoalMl: Int,
    val dueBillsCount: Int,
    val expiringCount: Int,
    val dueMaintenanceCount: Int = 0,
    val dueSoonIousCount: Int = 0
)

object WidgetSummaryFormatter {

    fun formatMoneySection(
        report: SafeToSpendReport,
        currency: AppCurrency,
        hideAmounts: Boolean
    ): WidgetMoneySummary {
        val monthFormatted = CurrencyFormatter.formatOrMask(
            report.monthExpenseCents,
            currency,
            hidden = hideAmounts
        )
        if (!report.hasAnchor) {
            return WidgetMoneySummary(
                label = "Month Spend",
                value = monthFormatted,
                subtitle = if (hideAmounts) "Balances hidden" else "Set cap for Safe Today"
            )
        }

        if (hideAmounts) {
            return WidgetMoneySummary(
                label = "Safe Today",
                value = CurrencyFormatter.formatOrMask(
                    report.todayRemainingAllowanceCents,
                    currency,
                    hidden = true
                ),
                subtitle = "Balances hidden"
            )
        }

        val isOverToday = report.todayRemainingAllowanceCents < 0L
        val displayCents = if (isOverToday) {
            abs(report.todayRemainingAllowanceCents)
        } else {
            report.todayRemainingAllowanceCents
        }
        return WidgetMoneySummary(
            label = if (isOverToday) "Over Today" else "Safe Today",
            value = CurrencyFormatter.formatOrMask(displayCents, currency, hidden = false),
            subtitle = "${report.paceStatus.badgeLabel} • $monthFormatted spent"
        )
    }

    fun formatAttentionLine(
        dueBillsCount: Int,
        expiringCount: Int,
        dueMaintenanceCount: Int = 0,
        dueSoonIousCount: Int = 0
    ): String? {
        val parts = buildList {
            if (dueBillsCount > 0) {
                add(if (dueBillsCount == 1) "1 bill due" else "$dueBillsCount bills due")
            }
            if (expiringCount > 0) {
                add("$expiringCount expiring")
            }
            if (dueMaintenanceCount > 0) {
                add(if (dueMaintenanceCount == 1) "1 service due" else "$dueMaintenanceCount services due")
            }
            if (dueSoonIousCount > 0) {
                add(if (dueSoonIousCount == 1) "1 IOU due" else "$dueSoonIousCount IOUs due")
            }
        }
        return if (parts.isEmpty()) null else parts.joinToString(" • ")
    }

    fun resolveAttentionUri(
        dueBillsCount: Int,
        expiringCount: Int,
        dueMaintenanceCount: Int = 0,
        dueSoonIousCount: Int = 0
    ): String {
        return when {
            dueBillsCount > 0 -> KansoUris.MONEY
            expiringCount > 0 -> KansoUris.LIFEADMIN_EXPIRIES
            dueMaintenanceCount > 0 -> KansoUris.LIFEADMIN_MAINTENANCE
            dueSoonIousCount > 0 -> KansoUris.MONEY
            else -> KansoUris.MONEY
        }
    }

    fun formatTasksBadge(openTasksCount: Int): String {
        return when {
            openTasksCount <= 0 -> "All tasks clear"
            openTasksCount == 1 -> "1 open task"
            else -> "$openTasksCount open tasks"
        }
    }

    fun formatHabitsBadge(done: Int, total: Int): String {
        return if (total <= 0) "No habits" else "Habits $done/$total"
    }

    fun formatWaterBadge(waterMl: Int, goalMl: Int): String {
        val fmt = NumberFormat.getIntegerInstance(Locale.US)
        return "${fmt.format(waterMl)} / ${fmt.format(goalMl)} ml"
    }
}
