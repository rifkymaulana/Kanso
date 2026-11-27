package com.kanso.app.widget

import com.kanso.app.navigation.KansoUris
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.SmartCoachCalculations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class WidgetSummaryFormatterTest {

    @Test
    fun `formatAttentionLine returns null when zero and formats combined counts`() {
        assertNull(WidgetSummaryFormatter.formatAttentionLine(0, 0))
        assertEquals("1 bill due", WidgetSummaryFormatter.formatAttentionLine(1, 0))
        assertEquals("2 bills due • 3 expiring", WidgetSummaryFormatter.formatAttentionLine(2, 3))
        assertEquals("1 expiring", WidgetSummaryFormatter.formatAttentionLine(0, 1))
        assertEquals("1 service due", WidgetSummaryFormatter.formatAttentionLine(0, 0, dueMaintenanceCount = 1))
        assertEquals("2 services due • 1 IOU due", WidgetSummaryFormatter.formatAttentionLine(0, 0, dueMaintenanceCount = 2, dueSoonIousCount = 1))
        assertEquals(
            "1 bill due • 2 expiring • 1 service due • 3 IOUs due",
            WidgetSummaryFormatter.formatAttentionLine(1, 2, 1, 3)
        )
    }

    @Test
    fun `resolveAttentionUri prioritizes bills, expiries, maintenance, and IOUs`() {
        assertEquals(KansoUris.MONEY, WidgetSummaryFormatter.resolveAttentionUri(1, 2, 1, 1))
        assertEquals(KansoUris.LIFEADMIN_EXPIRIES, WidgetSummaryFormatter.resolveAttentionUri(0, 1, 2, 1))
        assertEquals(KansoUris.LIFEADMIN_MAINTENANCE, WidgetSummaryFormatter.resolveAttentionUri(0, 0, 1, 2))
        assertEquals(KansoUris.MONEY, WidgetSummaryFormatter.resolveAttentionUri(0, 0, 0, 1))
    }

    @Test
    fun `formatMoneySection handles safe today, over today, hidden mode, and no anchor`() {
        val month = YearMonth.of(2026, 4)
        val today = LocalDate.of(2026, 4, 15)

        val safeReport = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = month,
            today = today,
            monthlyBudgetCents = 300_000L,
            monthIncomeCents = 0L,
            monthExpenseCents = 100_000L,
            todayExpenseCents = 4_000L,
            bills = emptyList()
        )
        val safeSummary = WidgetSummaryFormatter.formatMoneySection(
            report = safeReport,
            currency = AppCurrency.USD,
            hideAmounts = false
        )
        assertEquals("Safe Today", safeSummary.label)
        assertEquals(
            CurrencyFormatter.formatOrMask(safeReport.todayRemainingAllowanceCents, AppCurrency.USD, hidden = false),
            safeSummary.value
        )
        assertEquals(
            "${safeReport.paceStatus.badgeLabel} • ${CurrencyFormatter.formatOrMask(100_000L, AppCurrency.USD, hidden = false)} spent",
            safeSummary.subtitle
        )

        val overReport = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = month,
            today = today,
            monthlyBudgetCents = 160_000L,
            monthIncomeCents = 0L,
            monthExpenseCents = 100_000L,
            todayExpenseCents = 30_000L,
            bills = emptyList()
        )
        val overSummary = WidgetSummaryFormatter.formatMoneySection(
            report = overReport,
            currency = AppCurrency.USD,
            hideAmounts = false
        )
        assertEquals("Over Today", overSummary.label)

        val hiddenSummary = WidgetSummaryFormatter.formatMoneySection(
            report = safeReport,
            currency = AppCurrency.USD,
            hideAmounts = true
        )
        assertEquals("Safe Today", hiddenSummary.label)
        assertEquals(
            CurrencyFormatter.formatOrMask(safeReport.todayRemainingAllowanceCents, AppCurrency.USD, hidden = true),
            hiddenSummary.value
        )
        assertEquals("Balances hidden", hiddenSummary.subtitle)

        val noAnchorReport = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = month,
            today = today,
            monthlyBudgetCents = 0L,
            monthIncomeCents = 0L,
            monthExpenseCents = 25_000L,
            todayExpenseCents = 5_000L,
            bills = emptyList()
        )
        val noAnchorSummary = WidgetSummaryFormatter.formatMoneySection(
            report = noAnchorReport,
            currency = AppCurrency.USD,
            hideAmounts = false
        )
        assertEquals("Month Spend", noAnchorSummary.label)
        assertEquals(CurrencyFormatter.formatOrMask(25_000L, AppCurrency.USD, hidden = false), noAnchorSummary.value)
        assertEquals("Set cap for Safe Today", noAnchorSummary.subtitle)
    }

    @Test
    fun `formatTasksBadge, formatHabitsBadge, and formatWaterBadge render crisp labels`() {
        assertEquals("All tasks clear", WidgetSummaryFormatter.formatTasksBadge(0))
        assertEquals("1 open task", WidgetSummaryFormatter.formatTasksBadge(1))
        assertEquals("4 open tasks", WidgetSummaryFormatter.formatTasksBadge(4))
        assertEquals("No habits", WidgetSummaryFormatter.formatHabitsBadge(0, 0))
        assertEquals("Habits 3/5", WidgetSummaryFormatter.formatHabitsBadge(3, 5))
        assertEquals("1,250 / 2,000 ml", WidgetSummaryFormatter.formatWaterBadge(1250, 2000))
    }
}
