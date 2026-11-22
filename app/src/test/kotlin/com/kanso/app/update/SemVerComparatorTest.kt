package com.kanso.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerComparatorTest {

    @Test
    fun `normalizes optional leading v prefix`() {
        assertEquals("1.1.0", SemVerComparator.normalizeVersion("v1.1.0"))
        assertEquals("1.0.0-m2", SemVerComparator.normalizeVersion(" V1.0.0-m2 "))
    }

    @Test
    fun `compares major, minor, and patch numerically`() {
        assertTrue(SemVerComparator.isNewer("1.1.0", "1.0.0"))
        assertTrue(SemVerComparator.isNewer("v1.10.0", "1.2.0"))
        assertTrue(SemVerComparator.isNewer("2.0.0", "1.9.9"))
        assertFalse(SemVerComparator.isNewer("1.0.0", "1.0.0"))
        assertFalse(SemVerComparator.isNewer("1.0.0", "1.1.0"))
    }

    @Test
    fun `treats stable version as newer than pre-release of same numeric version`() {
        assertTrue(SemVerComparator.isNewer("1.0.0", "1.0.0-m2"))
        assertFalse(SemVerComparator.isNewer("1.0.0-m2", "1.0.0"))
        assertTrue(SemVerComparator.isNewer("1.0.0-rc2", "1.0.0-rc1"))
    }

    @Test
    fun `handles malformed versions safely without throwing`() {
        assertFalse(SemVerComparator.isNewer("", "1.0.0"))
        assertFalse(SemVerComparator.isNewer("not-a-version", "1.0.0"))
        assertFalse(SemVerComparator.isNewer("1.1.0", "invalid"))
    }
}
