package com.kanso.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetSummaryFormatterTest {

    @Test
    fun `formatAttentionLine returns null when zero and formats combined counts`() {
        assertNull(WidgetSummaryFormatter.formatAttentionLine(0, 0))
        assertEquals("1 bill due", WidgetSummaryFormatter.formatAttentionLine(1, 0))
        assertEquals("2 bills due • 3 expiring", WidgetSummaryFormatter.formatAttentionLine(2, 3))
        assertEquals("1 expiring", WidgetSummaryFormatter.formatAttentionLine(0, 1))
    }

    @Test
    fun `formatHabitsBadge and formatWaterBadge render crisp labels`() {
        assertEquals("No habits", WidgetSummaryFormatter.formatHabitsBadge(0, 0))
        assertEquals("Habits 3/5", WidgetSummaryFormatter.formatHabitsBadge(3, 5))
        assertEquals("1,250 / 2,000 ml", WidgetSummaryFormatter.formatWaterBadge(1250, 2000))
    }
}
