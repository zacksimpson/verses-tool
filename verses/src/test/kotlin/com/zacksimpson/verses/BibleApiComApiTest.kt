package com.zacksimpson.verses

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests the response model's parsing directly against real captured JSON shapes
 *  (confirmed live against bible-api.com) rather than hitting the network. */
class BibleApiComApiTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a single verse response`() {
        val body = """
            {"reference":"John 3:16","verses":[{"book_id":"JHN","book_name":"John","chapter":3,
            "verse":16,"text":"For God so loved the world...\n"}],"text":"For God so loved the world...\n",
            "translation_id":"kjv","translation_name":"King James Version","translation_note":"Public Domain"}
        """.trimIndent()

        val parsed = json.decodeFromString<BibleApiComResponse>(body)

        assertEquals("John 3:16", parsed.reference)
        assertEquals(1, parsed.verses.size)
        assertEquals("JHN", parsed.verses[0].book_id)
        assertEquals(3, parsed.verses[0].chapter)
        assertEquals(16, parsed.verses[0].verse)
        assertEquals("For God so loved the world...\n", parsed.text)
    }

    @Test
    fun `parses a whole-chapter response into verse-ordered pairs`() {
        val body = """
            {"reference":"John 3","verses":[
                {"book_id":"JHN","book_name":"John","chapter":3,"verse":1,"text":"There was a man...\n"},
                {"book_id":"JHN","book_name":"John","chapter":3,"verse":2,"text":"The same came...\n"}
            ],"text":"combined text"}
        """.trimIndent()

        val parsed = json.decodeFromString<BibleApiComResponse>(body)
        val chapter = BibleChapter(
            book = "John",
            chapter = 3,
            verses = parsed.verses.map { it.verse to it.text.trim() },
        )

        assertEquals(2, chapter.verses.size)
        assertEquals(1 to "There was a man...", chapter.verses[0])
        assertEquals(2 to "The same came...", chapter.verses[1])
    }

    @Test
    fun `parses a verse range response`() {
        val body = """
            {"reference":"Matthew 25:31-33,46","verses":[
                {"book_id":"MAT","book_name":"Matthew","chapter":25,"verse":31,"text":"a"},
                {"book_id":"MAT","book_name":"Matthew","chapter":25,"verse":32,"text":"b"},
                {"book_id":"MAT","book_name":"Matthew","chapter":25,"verse":33,"text":"c"},
                {"book_id":"MAT","book_name":"Matthew","chapter":25,"verse":46,"text":"d"}
            ],"text":"a b c d"}
        """.trimIndent()

        val parsed = json.decodeFromString<BibleApiComResponse>(body)

        assertEquals(4, parsed.verses.size)
        assertEquals(listOf(31, 32, 33, 46), parsed.verses.map { it.verse })
    }
}
