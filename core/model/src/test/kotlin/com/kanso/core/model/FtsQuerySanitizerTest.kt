package com.kanso.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FtsQuerySanitizerTest {

    @Test
    fun `blank or punctuation only returns empty string`() {
        assertEquals("", FtsQuerySanitizer.toFtsQuery(""))
        assertEquals("", FtsQuerySanitizer.toFtsQuery("   "))
        assertEquals("", FtsQuerySanitizer.toFtsQuery("!@#$%^&*()"))
    }

    @Test
    fun `sanitizes multi word queries with wildcard suffix`() {
        assertEquals("hello* world*", FtsQuerySanitizer.toFtsQuery("hello world"))
        assertEquals("don* t* panic*", FtsQuerySanitizer.toFtsQuery("don't panic!"))
        assertEquals("foo* bar* baz*", FtsQuerySanitizer.toFtsQuery("foo-bar:baz*()"))
        assertEquals("c* 2026*", FtsQuerySanitizer.toFtsQuery("c++ 2026"))
    }

    @Test
    fun `preserves unicode letters and deduplicates tokens`() {
        assertEquals("catatan* belanja*", FtsQuerySanitizer.toFtsQuery("catatan belanja catatan"))
        assertEquals("café* résumé*", FtsQuerySanitizer.toFtsQuery("café résumé"))
    }
}
