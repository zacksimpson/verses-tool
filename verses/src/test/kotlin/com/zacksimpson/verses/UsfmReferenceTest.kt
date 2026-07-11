package com.zacksimpson.verses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UsfmReferenceTest {

    @Test
    fun `converts single verse references`() {
        assertEquals("JHN.3.16", UsfmReference.toPassageId("John 3:16"))
        assertEquals("PSA.23.1", UsfmReference.toPassageId("Psalm 23:1"))
        assertEquals("1TI.4.12", UsfmReference.toPassageId("1 Timothy 4:12"))
        assertEquals("2CO.5.17", UsfmReference.toPassageId("2 Corinthians 5:17"))
    }

    @Test
    fun `converts verse ranges to the short dash form`() {
        assertEquals("EPH.2.8-9", UsfmReference.toPassageId("Ephesians 2:8-9"))
        assertEquals("1CO.13.4-7", UsfmReference.toPassageId("1 Corinthians 13:4-7"))
        assertEquals("JUD.1.24-25", UsfmReference.toPassageId("Jude 1:24-25"))
    }

    @Test
    fun `rejects an unknown book name`() {
        assertFailsWith<IllegalStateException> { UsfmReference.toPassageId("Frodo 1:1") }
    }

    @Test
    fun `every entry in VerseCatalog converts without error`() {
        for (reference in VerseCatalog.references) {
            UsfmReference.toPassageId(reference)
        }
    }

    @Test
    fun `parses passage ids into a verse range`() {
        assertEquals(PassageRange("JHN", 3, 16, 16), UsfmReference.parseRange("JHN.3.16"))
        assertEquals(PassageRange("EPH", 2, 8, 9), UsfmReference.parseRange("EPH.2.8-9"))
    }
}
