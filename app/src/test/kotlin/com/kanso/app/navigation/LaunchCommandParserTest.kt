package com.kanso.app.navigation

import com.kanso.core.ui.components.QuickCaptureMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchCommandParserTest {

    @Test
    fun `parses quick capture routes accurately`() {
        assertEquals(
            LaunchCommand.OpenQuickCapture(QuickCaptureMode.EXPENSE),
            LaunchCommandParser.parseUri(KansoUris.QUICK_CAPTURE_EXPENSE)
        )
        assertEquals(
            LaunchCommand.OpenQuickCapture(QuickCaptureMode.INCOME),
            LaunchCommandParser.parseUri(KansoUris.QUICK_CAPTURE_INCOME)
        )
        assertEquals(
            LaunchCommand.OpenQuickCapture(QuickCaptureMode.NOTE),
            LaunchCommandParser.parseUri(KansoUris.QUICK_CAPTURE_NOTE)
        )
        assertEquals(
            LaunchCommand.OpenQuickCapture(QuickCaptureMode.TASK),
            LaunchCommandParser.parseUri(KansoUris.QUICK_CAPTURE_TASK)
        )
        assertEquals(
            LaunchCommand.OpenQuickCapture(QuickCaptureMode.SNIPPET),
            LaunchCommandParser.parseUri(KansoUris.QUICK_CAPTURE_SNIPPET)
        )
        assertEquals(
            LaunchCommand.OpenQuickCapture(QuickCaptureMode.EXPIRY),
            LaunchCommandParser.parseUri(KansoUris.QUICK_CAPTURE_EXPIRY)
        )
    }

    @Test
    fun `parses module and tab deep links cleanly`() {
        assertEquals(LaunchCommand.OpenBrainTab(0), LaunchCommandParser.parseUri(KansoUris.BRAIN_NOTES))
        assertEquals(LaunchCommand.OpenBrainTab(1), LaunchCommandParser.parseUri(KansoUris.BRAIN_TASKS))
        assertEquals(LaunchCommand.OpenBrainTab(2), LaunchCommandParser.parseUri(KansoUris.BRAIN_SNIPPETS))
        assertEquals(LaunchCommand.OpenUtilitiesTab(0), LaunchCommandParser.parseUri(KansoUris.UTILITIES_QR))
        assertEquals(LaunchCommand.OpenUtilitiesTab(2), LaunchCommandParser.parseUri(KansoUris.UTILITIES_FOCUS))
        assertEquals(LaunchCommand.OpenHabits, LaunchCommandParser.parseUri(KansoUris.HABITS))
        assertEquals(LaunchCommand.OpenLifeAdminTab(0), LaunchCommandParser.parseUri(KansoUris.LIFEADMIN_EXPIRIES))
        assertEquals(LaunchCommand.OpenLifeAdminTab(2), LaunchCommandParser.parseUri(KansoUris.LIFEADMIN_VAULT))
        assertEquals(LaunchCommand.OpenMoney, LaunchCommandParser.parseUri(KansoUris.MONEY))
        assertEquals(LaunchCommand.OpenUtilitiesTab(0), LaunchCommandParser.parseUri("KANSO://UTILITIES/QR"))
    }

    @Test
    fun `returns null for blank, non-kanso, or unknown uris`() {
        assertNull(LaunchCommandParser.parseUri(null))
        assertNull(LaunchCommandParser.parseUri("   "))
        assertNull(LaunchCommandParser.parseUri("https://example.com"))
        assertNull(LaunchCommandParser.parseUri("kanso://unknown/path"))
        assertNull(LaunchCommandParser.parseUri("kanso://brain/invalid"))
    }
}
