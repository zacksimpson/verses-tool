package com.zacksimpson.verses

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests HelloAoParsing directly against real captured JSON shapes (confirmed live
 *  against bible.helloao.org) rather than hitting the network. */
class HelloAoApiTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses passage ids into a verse range`() {
        assertEquals(PassageRange("JHN", 3, 16, 16), HelloAoParsing.parsePassageId("JHN.3.16"))
        assertEquals(PassageRange("EPH", 2, 8, 9), HelloAoParsing.parsePassageId("EPH.2.8-9"))
    }

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
        assertEquals(1 to "Now there was a man of the Pharisees named Nicodemus.", verses[0])
        assertEquals(
            3 to "Jesus replied, \"Truly, truly, I tell you, no one can see the kingdom of God.\"",
            verses[1],
        )
    }

    @Test
    fun `flattens poetry lines and skips footnote-only markers`() {
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
        assertEquals(1 to "The LORD is my shepherd; I shall not want.", verses[0])
    }
}
