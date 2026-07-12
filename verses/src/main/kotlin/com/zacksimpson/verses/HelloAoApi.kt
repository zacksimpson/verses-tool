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

/** A whole chapter's verses, in order. */
internal data class BibleChapter(
    val book: String,
    val chapter: Int,
    val verses: List<VerseSegment>,
)

/**
 * Pure parsing logic pulled out of [HelloAoApi] so it's unit testable without network
 * access. bible.helloao.org's chapter JSON is a mixed array of headings, line breaks, and
 * verses. A top-level `{"type": "heading", ...}` or `{"type": "line_break"}` item marks a
 * paragraph/section break before whatever verse comes next (see [VerseSegment.startsNewParagraph]) —
 * confirmed on BSB, which has this structure for both prose (Genesis 1's "The First Day" /
 * "The Second Day" headings) and poetry (Matthew 5's Beatitudes); eng_kjv (KJV's source) has
 * none of this structure, so KJV verses never get startsNewParagraph = true.
 *
 * Within a verse's own content, items mix plain strings with objects like `{"text": "...",
 * "poem": 1}` (a poetic line, at indent level "poem" - 1 — a "poem": 2 item is one indent
 * step in from "poem": 1) or `{"lineBreak": true}` (ends whatever line is currently open —
 * a poetic verse's last line, or a blank-line stanza break) or `{"noteId": 8}` (footnote
 * refs, contribute no text). A poem-tagged object always starts its own line — that's what
 * marks it as poetry in the first place; plain text with no "poem" tag just continues
 * whatever line is open, so ordinary prose still collapses back into a single line exactly
 * as before this existed. Lines come back joined with "\n", every poetic line prefixed with
 * [POETIC_LINE_MARKER] (so even an unindented poetic line stays distinguishable from prose)
 * followed by [POETIC_INDENT_UNIT] per indent level — the encoding VerseText and
 * PassageScreen's NumberedVerseText read back out (see linesFromVerseText).
 */
internal object HelloAoParsing {
    fun versesFromContent(content: List<JsonElement>): List<VerseSegment> {
        var pendingParagraphBreak = false
        return content.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            when (obj["type"]?.jsonPrimitive?.contentOrNull) {
                "heading", "line_break" -> {
                    pendingParagraphBreak = true
                    null
                }
                "verse" -> {
                    val number = obj["number"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                    val verseContent = obj["content"] as? JsonArray ?: return@mapNotNull null
                    val startsNewParagraph = pendingParagraphBreak
                    pendingParagraphBreak = false
                    VerseSegment(number, linesFrom(verseContent).joinToString("\n"), startsNewParagraph)
                }
                else -> null
            }
        }
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
                    current.append(POETIC_LINE_MARKER)
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
        if (builder.isNotEmpty() && !builder.endsWith(" ") && !builder.endsWith(POETIC_LINE_MARKER)) {
            builder.append(" ")
        }
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
        joinVerseTexts(verses.map { it.text })
    }

    /** Same passage as [fetchVerseText], but keeping each verse's number and paragraph
     *  structure attached rather than flattening to one string — lets the UI show verse
     *  numbers and paragraph breaks for a range. */
    suspend fun fetchVerses(reference: String): Result<List<VerseSegment>> = runCatching {
        versesInRange(reference).ifEmpty {
            throw IllegalStateException("helloao returned no passage text for '$reference'.")
        }
    }

    private suspend fun versesInRange(reference: String): List<VerseSegment> {
        val range = UsfmReference.parseRange(UsfmReference.toPassageId(reference))
        val verses = fetchChapterVerses(range.bookCode, range.chapter)
        return verses.filter { it.number in range.startVerse..range.endVerse }
    }

    suspend fun fetchChapter(book: String, chapter: Int): Result<BibleChapter> = runCatching {
        val verses = fetchChapterVerses(UsfmReference.bookCode(book), chapter)
        if (verses.isEmpty()) {
            throw IllegalStateException("helloao returned no verses for '$book $chapter'.")
        }
        BibleChapter(book = book, chapter = chapter, verses = verses)
    }

    private suspend fun fetchChapterVerses(bookCode: String, chapter: Int): List<VerseSegment> {
        val response = client.get("https://bible.helloao.org/api/$translationId/$bookCode/$chapter.json")
        response.throwIfNotSuccess("helloao")
        val parsed: HelloAoChapterResponse = response.body()
        return HelloAoParsing.versesFromContent(parsed.chapter.content)
    }

    fun close() {
        client.close()
    }
}
