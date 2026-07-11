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
    fun `an indented second line decodes to indent level 1`() {
        val lines = linesFromVerseText("The LORD is my shepherd;\n${POETIC_INDENT_UNIT}I shall not want.")
        assertEquals(
            listOf(
                VerseLine(0, "The LORD is my shepherd;"),
                VerseLine(1, "I shall not want."),
            ),
            lines,
        )
    }

    @Test
    fun `isPoeticText is false for plain prose and true once a line break is present`() {
        assertFalse(isPoeticText("For God so loved the world."))
        assertTrue(isPoeticText("The LORD is my shepherd;\n${POETIC_INDENT_UNIT}I shall not want."))
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
        val joined = joinVerseTexts(
            listOf(
                "For God so loved the world.",
                "The LORD is my shepherd;\n${POETIC_INDENT_UNIT}I shall not want.",
                "He restoreth my soul.",
            ),
        )
        assertEquals(
            "For God so loved the world.\nThe LORD is my shepherd;\n${POETIC_INDENT_UNIT}I shall not want.\nHe restoreth my soul.",
            joined,
        )
    }
}
