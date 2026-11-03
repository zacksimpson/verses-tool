package com.zacksimpson.verses

import kotlin.test.Test
import kotlin.test.assertEquals

class VersePickerScreenTest {

    @Test
    fun `tapping the anchor again resolves to a single verse`() {
        assertEquals("John 3:16", resolvePassageReference("John", 3, anchor = 16, tapped = 16))
    }

    @Test
    fun `tapping a later verse resolves to a range`() {
        assertEquals("John 3:16-18", resolvePassageReference("John", 3, anchor = 16, tapped = 18))
    }

    @Test
    fun `tapping an earlier verse still resolves lo-to-hi`() {
        assertEquals("John 3:16-18", resolvePassageReference("John", 3, anchor = 18, tapped = 16))
    }
}
