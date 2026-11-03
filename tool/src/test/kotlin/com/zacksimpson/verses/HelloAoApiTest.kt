package com.zacksimpson.verses

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests HelloAoParsing directly against real captured JSON shapes (confirmed live
 *  against bible.helloao.org) rather than hitting the network. */
class HelloAoApiTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `flattens a chapter with headings, line breaks, and plain verse text`() {
        val body = """
            {"chapter":{"number":3,"content":[
                {"type":"heading","content":["Jesus and Nicodemus"]},
                {"type":"verse","number":1,"content":["Now there was a man of the Pharisees named Nicodemus."]},
                {"type":"line_break"},
                {"type":"verse","number":3,"content":["Jesus replied, \"Truly, truly, I tell you,",
                    {"noteId":13},"no one can see the kingdom of God.\""]}
            ]}}
        """.trimIndent()

        val parsed = json.decodeFromString<HelloAoChapterResponse>(body)
        val verses = HelloAoParsing.versesFromContent(parsed.chapter.content)

        assertEquals(2, verses.size)
        assertEquals(VerseSegment(1, "Now there was a man of the Pharisees named Nicodemus.", startsNewParagraph = true), verses[0])
        assertEquals(
            VerseSegment(3, "Jesus replied, \"Truly, truly, I tell you, no one can see the kingdom of God.\"", startsNewParagraph = true),
            verses[1],
        )
    }

    @Test
    fun `a verse with no preceding heading or line break doesn't start a new paragraph`() {
        val body = """
            {"chapter":{"number":3,"content":[
                {"type":"heading","content":["Jesus and Nicodemus"]},
                {"type":"verse","number":1,"content":["Now there was a man of the Pharisees named Nicodemus."]},
                {"type":"verse","number":2,"content":["This man came to Jesus by night."]}
            ]}}
        """.trimIndent()

        val parsed = json.decodeFromString<HelloAoChapterResponse>(body)
        val verses = HelloAoParsing.versesFromContent(parsed.chapter.content)

        assertEquals(true, verses[0].startsNewParagraph)
        assertEquals(false, verses[1].startsNewParagraph, "verse 2 follows verse 1 directly, with no heading or line_break between them")
    }

    @Test
    fun `keeps poetry lines separate and indents the second line`() {
        val body = """
            {"chapter":{"number":23,"content":[
                {"type":"heading","content":["The LORD Is My Shepherd"]},
                {"type":"hebrew_subtitle","content":["A Psalm of David."]},
                {"type":"verse","number":1,"content":[
                    {"text":"The LORD is my shepherd;","poem":1},
                    {"noteId":50},
                    {"text":"I shall not want.","poem":2}
                ]}
            ]}}
        """.trimIndent()

        val parsed = json.decodeFromString<HelloAoChapterResponse>(body)
        val verses = HelloAoParsing.versesFromContent(parsed.chapter.content)

        assertEquals(1, verses.size)
        assertEquals(1, verses[0].number)
        assertEquals(
            "$POETIC_LINE_MARKER" + "The LORD is my shepherd;\n" +
                "$POETIC_LINE_MARKER$POETIC_INDENT_UNIT" + "I shall not want.",
            verses[0].text,
        )
    }

    @Test
    fun `a single poetic line with no second line is still marked poetic, not plain prose`() {
        // A verse whose only poetic content is one "poem":1 segment (no "poem":2, so no
        // real line break) — the exact shape that used to collapse to a string
        // indistinguishable from plain prose before POETIC_LINE_MARKER was added.
        val body = """
            {"chapter":{"number":23,"content":[
                {"type":"verse","number":4,"content":[
                    {"text":"Yea, though I walk through the valley of the shadow of death, I will fear no evil.","poem":1},
                    {"lineBreak":true}
                ]}
            ]}}
        """.trimIndent()

        val parsed = json.decodeFromString<HelloAoChapterResponse>(body)
        val verses = HelloAoParsing.versesFromContent(parsed.chapter.content)

        assertEquals(1, verses.size)
        assertEquals(4, verses[0].number)
        assertEquals(
            "$POETIC_LINE_MARKER" + "Yea, though I walk through the valley of the shadow of death, I will fear no evil.",
            verses[0].text,
        )
        assertTrue(isPoeticText(verses[0].text))
    }
}
