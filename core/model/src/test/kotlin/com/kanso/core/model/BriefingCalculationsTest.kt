package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BriefingCalculationsTest {

    @Test
    fun `formatClockTime and computeDelayUntilNextTriggerMillis handle same-day and next-day rollovers`() {
        assertEquals("08:05", BriefingCalculations.formatClockTime(8, 5))
        assertEquals("23:59", BriefingCalculations.formatClockTime(29, 70))

        val morningBefore = LocalDateTime.of(2026, 11, 26, 7, 30)
        assertEquals(
            30 * 60_000L,
            BriefingCalculations.computeDelayUntilNextTriggerMillis(morningBefore, 8, 0)
        )

        val morningAlreadyPassed = LocalDateTime.of(2026, 11, 26, 8, 5)
        assertEquals(
            (23 * 60L + 55L) * 60_000L,
            BriefingCalculations.computeDelayUntilNextTriggerMillis(morningAlreadyPassed, 8, 0)
        )
    }

    @Test
    fun `morning and evening briefings build accurate content and redacted lockscreen summaries`() {
        assertNull(BriefingCalculations.buildMorningBriefing(0, 0, 0, 0, 0, 0))
        val morning = BriefingCalculations.buildMorningBriefing(
            dueBillsCount = 1,
            dueTasksCount = 2,
            expiringCount = 1,
            dueMaintenanceCount = 1,
            dueIousCount = 0,
            morningHabitsCount = 3
        )
        assertNotNull(morning)
        assertEquals("Kanso Morning Briefing", morning!!.title)
        assertTrue(morning.body.contains("1 bill due"))
        assertTrue(morning.body.contains("2 tasks due"))
        assertTrue(morning.body.contains("1 service due"))
        assertEquals(8, morning.totalAttentionCount)
        assertEquals("8 items need your attention today", morning.publicRedactedBody)

        assertNull(BriefingCalculations.buildEveningCheckIn(0, 0, 0))
        val evening = BriefingCalculations.buildEveningCheckIn(
            pendingHabitsCount = 2,
            waterRemainingMl = 500,
            dueTasksCount = 1
        )
        assertNotNull(evening)
        assertEquals("2 habits left today • 500 ml water left • 1 open due task", evening!!.body)
        assertEquals("4 check-in items left today", evening.publicRedactedBody)
    }

    @Test
    fun `activeDueMaintenanceLogs deduplicates superseded services and isIouDueSoon checks window`() {
        val today = LocalDate.of(2026, 11, 26).toEpochDay()
        val oldServiceOverdue = MaintenanceLog(
            id = 1L,
            assetName = "Honda Jazz",
            category = MaintenanceCategory.CAR,
            serviceDescription = "Oil Change",
            serviceEpochDay = today - 180L,
            nextDueEpochDay = today - 5L
        )
        val newerServiceFarOut = MaintenanceLog(
            id = 2L,
            assetName = "Honda Jazz",
            category = MaintenanceCategory.CAR,
            serviceDescription = "Oil Change",
            serviceEpochDay = today - 2L,
            nextDueEpochDay = today + 180L
        )
        val dueAcCleaning = MaintenanceLog(
            id = 3L,
            assetName = "Living Room AC",
            category = MaintenanceCategory.APPLIANCE,
            serviceDescription = "Filter Clean",
            serviceEpochDay = today - 90L,
            nextDueEpochDay = today + 7L
        )

        val dueLogs = LifeAdminCalculations.activeDueMaintenanceLogs(
            logs = listOf(oldServiceOverdue, newerServiceFarOut, dueAcCleaning),
            todayEpochDay = today,
            withinDays = 14
        )
        assertEquals(listOf(dueAcCleaning), dueLogs)

        val dueSoonIou = IouEntry(
            id = 1L,
            personName = "Budi",
            amountCents = 100_000_00L,
            direction = IouDirection.OWES_ME,
            createdAtMillis = 1L,
            dueDateEpochDay = today + 2L,
            isSettled = false
        )
        assertTrue(MoneyCalculations.isIouDueSoon(dueSoonIou, today, withinDays = 3))
        assertFalse(MoneyCalculations.isIouDueSoon(dueSoonIou.copy(isSettled = true), today, withinDays = 3))
        assertFalse(MoneyCalculations.isIouDueSoon(dueSoonIou.copy(dueDateEpochDay = today + 10L), today, withinDays = 3))
        assertFalse(MoneyCalculations.isIouDueSoon(dueSoonIou.copy(dueDateEpochDay = null), today, withinDays = 3))
    }
}
