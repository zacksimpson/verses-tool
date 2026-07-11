package com.zacksimpson.verses

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal data class HelloAoChapter(val number: Int = 0, val content: List<JsonElement> = emptyList())

@Serializable
internal data class HelloAoChapterResponse(val chapter: HelloAoChapter = HelloAoChapter())

/** A whole chapter's verses, in order, as (verse number, text) pairs. */
internal data class BibleChapter(
    val book: String,
    val chapter: Int,
    val verses: List<Pair<Int, String>>,
)

/**
 * Pure parsing logic pulled out of [HelloAoApi] so it's unit testable without network
 * access. bible.helloao.org's chapter JSON is a mixed array of headings, line breaks, and
 * verses, where a verse's own content mixes plain strings with objects like
 * `{"text": "...", "poem": 1}` (a poetic line, at indent level "poem" - 1 — a "poem": 2
 * item is one indent step in from "poem": 1) or `{"lineBreak": true}` (ends whatever line
 * is currently open — a poetic verse's last line, or a blank-line stanza break) or
 * `{"noteId": 8}` (footnote refs, contribute no text). A poem-tagged object always starts
 * its own line — that's what marks it as poetry in the first place; plain text with no
 * "poem" tag just continues whatever line is open, so ordinary prose still collapses back
 * into a single line exactly as before this existed. Lines come back joined with "\n",
 * indented lines prefixed with [POETIC_INDENT_UNIT] per level — the encoding VerseText and
 * PassageScreen's NumberedVerseText read back out (see linesFromVerseText).
 */
internal object HelloAoParsing {
    fun versesFromContent(content: List<JsonElement>): List<Pair<Int, String>> =
        content.mapNotNull { element ->
            val verse = element as? JsonObject ?: return@mapNotNull null
            if (verse["type"]?.jsonPrimitive?.contentOrNull != "verse") return@mapNotNull null
            val number = verse["number"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val verseContent = verse["content"] as? JsonArray ?: return@mapNotNull null
            number to linesFrom(verseContent).joinToString("\n")
        }

    private fun linesFrom(items: List<JsonElement>): List<String> {
        val lines = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            val line = current.toString().trimEnd()
            if (line.isNotBlank()) lines.add(line)
            current.clear()
        }
        for (item in items) {
            when {
                item is JsonPrimitive -> appendWord(current, item.contentOrNull)
                item is JsonObject && item["lineBreak"]?.jsonPrimitive?.booleanOrNull == true -> flush()
                item is JsonObject && item["poem"] != null -> {
                    flush()
                    val indentLevel = ((item["poem"]?.jsonPrimitive?.intOrNull ?: 1) - 1).coerceAtLeast(0)
                    current.append(POETIC_INDENT_UNIT.repeat(indentLevel))
                    appendWord(current, item["text"]?.jsonPrimitive?.contentOrNull)
                }
                item is JsonObject && item["text"] != null -> appendWord(current, item["text"]?.jsonPrimitive?.contentOrNull)
                else -> Unit // footnote markers and other non-text objects contribute nothing
            }
        }
        flush()
        return lines
    }

    private fun appendWord(builder: StringBuilder, word: String?) {
        if (word.isNullOrEmpty()) return
        if (builder.isNotEmpty() && !builder.endsWith(" ")) builder.append(" ")
        builder.append(word)
    }
}

/**
 * Client for bible.helloao.org — free, keyless, hosting both public domain translations
 * this app uses (KJV as "eng_kjv", BSB as "BSB", see [PublicDomainProvider]). There's no
 * single-verse endpoint, only whole chapters, so a single-verse or verse-range lookup
 * always fetches the containing chapter and filters it down (see [HelloAoParsing]).
 */
internal class HelloAoApi(private val translationId: String) {
    private val client = createBibleApiHttpClient()

    suspend fun fetchVerseText(reference: String): Result<String> = runCatching {
        val verses = versesInRange(reference)
        if (verses.isEmpty()) {
            throw IllegalStateException("helloao returned no passage text for '$reference'.")
        }
        joinVerseTexts(verses.map { it.second })
    }

    /** Same passage as [fetchVerseText], but keeping each verse's number attached rather
     *  than flattening to one string — lets the UI show verse numbers for a range. */
    suspend fun fetchVerses(reference: String): Result<List<Pair<Int, String>>> = runCatching {
        versesInRange(reference).ifEmpty {
            throw IllegalStateException("helloao returned no passage text for '$reference'.")
        }
    }

    private suspend fun versesInRange(reference: String): List<Pair<Int, String>> {
        val range = UsfmReference.parseRange(UsfmReference.toPassageId(reference))
        val verses = fetchChapterVerses(range.bookCode, range.chapter)
        return verses.filter { it.first in range.startVerse..range.endVerse }
    }

    suspend fun fetchChapter(book: String, chapter: Int): Result<BibleChapter> = runCatching {
        val verses = fetchChapterVerses(UsfmReference.bookCode(book), chapter)
        if (verses.isEmpty()) {
            throw IllegalStateException("helloao returned no verses for '$book $chapter'.")
        }
        BibleChapter(book = book, chapter = chapter, verses = verses)
    }

    private suspend fun fetchChapterVerses(bookCode: String, chapter: Int): List<Pair<Int, String>> {
        val response = client.get("https://bible.helloao.org/api/$translationId/$bookCode/$chapter.json")
        response.throwIfNotSuccess("helloao")
        val parsed: HelloAoChapterResponse = response.body()
        return HelloAoParsing.versesFromContent(parsed.chapter.content)
    }

    fun close() {
        client.close()
    }
}
