package com.zacksimpson.verses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerseTextTest {

    @Test
    fun `plain prose is a single unindented line`() {
        val lines = linesFromVerseText("For God so loved the world.")
        assertEquals(listOf(VerseLine(0, "For God so loved the world.")), lines)
    }

    @Test
    fun `an indented second poetic line decodes to indent level 1`() {
        val lines = linesFromVerseText(
            "${POETIC_LINE_MARKER}The LORD is my shepherd;\n" +
                "$POETIC_LINE_MARKER$POETIC_INDENT_UNIT" + "I shall not want.",
        )
        assertEquals(
            listOf(
                VerseLine(0, "The LORD is my shepherd;"),
                VerseLine(1, "I shall not want."),
            ),
            lines,
        )
    }

    @Test
    fun `a single unindented poetic line still decodes to indent level 0 with the marker stripped`() {
        val lines = linesFromVerseText("${POETIC_LINE_MARKER}I shall not want.")
        assertEquals(listOf(VerseLine(0, "I shall not want.")), lines)
    }

    @Test
    fun `isPoeticText is false for plain prose and true for any marked poetic line`() {
        assertFalse(isPoeticText("For God so loved the world."))
        assertTrue(
            isPoeticText(
                "${POETIC_LINE_MARKER}The LORD is my shepherd;\n" +
                    "$POETIC_LINE_MARKER$POETIC_INDENT_UNIT" + "I shall not want.",
            ),
        )
    }

    @Test
    fun `isPoeticText is true for a single unindented poetic line, unlike a plain indent-based check`() {
        // Regression test: indent level 0 means no POETIC_INDENT_UNIT is present, so a
        // check that only looked for "\n" or a leading indent would miss this — the
        // marker is what makes a lone poetic line still distinguishable from prose.
        assertTrue(isPoeticText("${POETIC_LINE_MARKER}I shall not want."))
    }

    @Test
    fun `joinVerseTexts keeps consecutive prose verses on one flowing line`() {
        val joined = joinVerseTexts(listOf("In the beginning God created the heavens and the earth.", "Now the earth was formless and void."))
        assertEquals(
            "In the beginning God created the heavens and the earth. Now the earth was formless and void.",
            joined,
        )
    }

    @Test
    fun `joinVerseTexts breaks onto a new line around a poetic verse`() {
        val poeticVerse = "${POETIC_LINE_MARKER}The LORD is my shepherd;\n" +
            "$POETIC_LINE_MARKER$POETIC_INDENT_UNIT" + "I shall not want."
        val joined = joinVerseTexts(
            listOf(
                "For God so loved the world.",
                poeticVerse,
                "He restoreth my soul.",
            ),
        )
        assertEquals(
            "For God so loved the world.\n$poeticVerse\nHe restoreth my soul.",
            joined,
        )
    }

    @Test
    fun `joinVerseTexts breaks a single unindented poetic verse away from neighboring prose`() {
        // The exact scenario this fix targets: a single-line poetic verse (e.g. OT poetry
        // quoted in NT prose) must not silently merge into the surrounding paragraph.
        val joined = joinVerseTexts(
            listOf(
                "But seek ye first the kingdom of God, and his righteousness;",
                "${POETIC_LINE_MARKER}and all these things shall be added unto you.",
                "So then faith cometh by hearing, and hearing by the word of God.",
            ),
        )
        assertEquals(
            "But seek ye first the kingdom of God, and his righteousness;\n" +
                "${POETIC_LINE_MARKER}and all these things shall be added unto you.\n" +
                "So then faith cometh by hearing, and hearing by the word of God.",
            joined,
        )
    }
}
