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

/** a whole chapter's verses, in order. */
internal data class BibleChapter(
    val book: String,
    val chapter: Int,
    val verses: List<VerseSegment>,
)

/**
 * parsing logic pulled out of [HelloAoApi] so it's unit testable without network access.
 * bible.helloao.org's chapter json mixes headings, line breaks, and verses in one array.
 * a heading or line_break item marks a paragraph break before whatever verse comes next.
 * confirmed on bsb, which has this for both prose and poetry. kjv's source has none of it,
 * so kjv verses never get startsNewParagraph = true.
 *
 * inside a verse, a poem-tagged item starts its own line at an indent level, lineBreak ends
 * the current line, and plain text just continues whatever line is open. lines come back
 * joined with "\n", poetic lines prefixed with [POETIC_LINE_MARKER] and [POETIC_INDENT_UNIT]
 * per indent level, which linesFromVerseText reads back out.
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
 * client for bible.helloao.org, free and keyless, hosts both public domain translations
 * this app uses (kjv as eng_kjv, bsb as BSB). no single-verse endpoint, so a lookup always
 * fetches the whole chapter and filters it down.
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

    /** same passage as [fetchVerseText] but keeps verse numbers and paragraph structure
     *  instead of flattening to one string. */
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
