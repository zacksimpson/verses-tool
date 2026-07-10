package com.zacksimpson.verses

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal data class HelloAoChapter(val number: Int = 0, val content: List<JsonElement> = emptyList())

@Serializable
internal data class HelloAoChapterResponse(val chapter: HelloAoChapter = HelloAoChapter())

internal data class PassageRange(val bookCode: String, val chapter: Int, val startVerse: Int, val endVerse: Int)

/**
 * Pure parsing logic pulled out of [HelloAoApi] so it's unit testable without network
 * access. bible.helloao.org's chapter JSON is a mixed array of headings, line breaks, and
 * verses, where a verse's own content mixes plain strings with objects like
 * `{"text": "...", "poem": 1}` (poetry line markers) or `{"noteId": 8}` (footnote refs,
 * no text) — this flattens all of that down to plain reading text.
 */
internal object HelloAoParsing {
    fun parsePassageId(passageId: String): PassageRange {
        val (bookCode, chapterPart, versePart) = passageId.split(".")
        val (start, end) = if ("-" in versePart) {
            val (from, to) = versePart.split("-")
            from.toInt() to to.toInt()
        } else {
            versePart.toInt().let { it to it }
        }
        return PassageRange(bookCode, chapterPart.toInt(), start, end)
    }

    fun versesFromContent(content: List<JsonElement>): List<Pair<Int, String>> =
        content.mapNotNull { element ->
            val verse = element as? JsonObject ?: return@mapNotNull null
            if (verse["type"]?.jsonPrimitive?.contentOrNull != "verse") return@mapNotNull null
            val number = verse["number"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val verseContent = verse["content"] as? JsonArray ?: return@mapNotNull null
            number to textFrom(verseContent).trim()
        }

    private fun textFrom(items: List<JsonElement>): String =
        items.mapNotNull { item ->
            when (item) {
                is JsonPrimitive -> item.contentOrNull
                is JsonObject -> item["text"]?.jsonPrimitive?.contentOrNull
                else -> null
            }
        }.joinToString(" ")
}

/**
 * Client for bible.helloao.org (the public-domain Berean Standard Bible) — free, keyless,
 * hosting a translation dedicated to the public domain by its publisher on April 30, 2023.
 * There's no single-verse endpoint, only whole chapters, so a single-verse or verse-range
 * lookup always fetches the containing chapter and filters it down (see [HelloAoParsing]).
 */
internal class HelloAoApi {
    private val client = createBibleApiHttpClient()

    suspend fun fetchVerseText(reference: String): Result<String> = runCatching {
        val range = HelloAoParsing.parsePassageId(UsfmReference.toPassageId(reference))
        val verses = fetchChapterVerses(range.bookCode, range.chapter)
        verses.filter { it.first in range.startVerse..range.endVerse }
            .joinToString(" ") { it.second }
            .trim()
            .ifEmpty { throw IllegalStateException("helloao returned no passage text for '$reference'.") }
    }

    suspend fun fetchChapter(book: String, chapter: Int): Result<BibleChapter> = runCatching {
        val verses = fetchChapterVerses(UsfmReference.bookCode(book), chapter)
        if (verses.isEmpty()) {
            throw IllegalStateException("helloao returned no verses for '$book $chapter'.")
        }
        BibleChapter(book = book, chapter = chapter, verses = verses)
    }

    private suspend fun fetchChapterVerses(bookCode: String, chapter: Int): List<Pair<Int, String>> {
        val response = client.get("https://bible.helloao.org/api/BSB/$bookCode/$chapter.json")
        response.throwIfNotSuccess("helloao")
        val parsed: HelloAoChapterResponse = response.body()
        return HelloAoParsing.versesFromContent(parsed.chapter.content)
    }

    fun close() {
        client.close()
    }
}
