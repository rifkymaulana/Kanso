package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BrainChecklistAndRecurrenceTest {

    @Test
    fun `NoteChecklistParser parses items, toggles lines cleanly, and appends new items`() {
        val raw = """
            Weekend errands
            - [ ] Buy coffee beans
            * [x] Pay internet
            - [X] Pack charger
            Call mom later
        """.trimIndent()

        val parsed = NoteChecklistParser.parse(raw)
        assertTrue(parsed.hasChecklist)
        assertFalse(parsed.isAllChecked)
        assertEquals(2, parsed.checkedCount)
        assertEquals(3, parsed.totalCount)
        assertEquals(listOf("Weekend errands", "Call mom later"), parsed.plainPreviewLines)

        assertFalse(parsed.items[0].isChecked)
        assertEquals("Buy coffee beans", parsed.items[0].text)
        assertTrue(parsed.items[1].isChecked)
        assertEquals("Pay internet", parsed.items[1].text)

        val toggledFirst = NoteChecklistParser.toggleLine(raw, parsed.items[0].lineIndex)
        val reparsed = NoteChecklistParser.parse(toggledFirst)
        assertEquals(3, reparsed.checkedCount)
        assertTrue(reparsed.isAllChecked)

        val uncheckedSecond = NoteChecklistParser.toggleLine(toggledFirst, parsed.items[1].lineIndex)
        assertFalse(NoteChecklistParser.parse(uncheckedSecond).items[1].isChecked)

        assertEquals("- [ ] ", NoteChecklistParser.appendChecklistItem(""))
        assertEquals("Notes\n- [ ] ", NoteChecklistParser.appendChecklistItem("Notes"))
        assertEquals("Notes\n- [ ] ", NoteChecklistParser.appendChecklistItem("Notes\n"))
    }

    @Test
    fun `TaskCalculations computes next recurring due date for daily, weekly, and monthly tasks`() {
        val today = LocalDate.of(2026, 11, 26)
        val todayEpoch = today.toEpochDay()

        assertNull(
            TaskCalculations.nextRecurringDueEpochDay(
                currentDueEpochDay = null,
                recurrence = TaskRecurrence.NONE,
                todayEpochDay = todayEpoch
            )
        )

        // Daily
        assertEquals(
            LocalDate.of(2026, 11, 27).toEpochDay(),
            TaskCalculations.nextRecurringDueEpochDay(todayEpoch - 3L, TaskRecurrence.DAILY, todayEpoch)
        )
        assertEquals(
            LocalDate.of(2026, 11, 29).toEpochDay(),
            TaskCalculations.nextRecurringDueEpochDay(
                LocalDate.of(2026, 11, 28).toEpochDay(),
                TaskRecurrence.DAILY,
                todayEpoch
            )
        )

        // Weekly preserves day-of-week even across multiple overdue weeks
        val threeWeeksAgoMonday = LocalDate.of(2026, 11, 2) // Monday
        val nextMonday = TaskCalculations.nextRecurringDueEpochDay(
            currentDueEpochDay = threeWeeksAgoMonday.toEpochDay(),
            recurrence = TaskRecurrence.WEEKLY,
            todayEpochDay = todayEpoch // Thu Nov 26 -> next Monday is Nov 30
        )
        assertEquals(LocalDate.of(2026, 11, 30).toEpochDay(), nextMonday)

        // Monthly clamps month-end and moves strictly past today
        val jan31 = LocalDate.of(2026, 1, 31)
        val nextMonthlyFromJan31 = TaskCalculations.nextRecurringDueEpochDay(
            currentDueEpochDay = jan31.toEpochDay(),
            recurrence = TaskRecurrence.MONTHLY,
            todayEpochDay = LocalDate.of(2026, 2, 15).toEpochDay()
        )
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), nextMonthlyFromJan31)

        val overdueMonths = TaskCalculations.nextRecurringDueEpochDay(
            currentDueEpochDay = LocalDate.of(2026, 9, 30).toEpochDay(),
            recurrence = TaskRecurrence.MONTHLY,
            todayEpochDay = LocalDate.of(2026, 11, 30).toEpochDay()
        )
        assertEquals(LocalDate.of(2026, 12, 30).toEpochDay(), overdueMonths)
    }
}
