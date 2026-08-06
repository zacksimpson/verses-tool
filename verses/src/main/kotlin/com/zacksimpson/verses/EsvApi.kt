package com.zacksimpson.verses

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.Serializable
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

@Serializable
internal data class EsvPassageResponse(
    val query: String = "",
    val canonical: String = "",
    val passages: List<String> = emptyList(),
)

/**
 * parsing logic pulled out of [EsvApi] so it's unit testable without network access.
 * with include-verse-numbers=true, esv's text already carries real structure: each verse
 * starts with an inline "[N]" marker, poetic lines are real newlines indented in multiples
 * of [POETRY_INDENT_STEP] spaces, and a verse starting a new paragraph has its marker at
 * the start of a line instead of inline after the previous verse.
 */
internal object EsvTextParsing {
    // matches an [N] verse marker plus surrounding whitespace: group 1 is a leading
    // newline, group 2 is the marker's own indent, group 3 is the verse number
    private val VERSE_MARKER = Regex("""(\n)?([ \t]*)\[(\d+)]\s*""")

    // matches the indent-poetry-lines value we request explicitly, so this stays
    // correct even if esv changes its default
    private const val POETRY_INDENT_STEP = 4

    fun versesFromPassageText(raw: String): List<VerseSegment> {
        val matches = VERSE_MARKER.findAll(raw).toList()
        return matches.mapIndexed { index, match ->
            val baseIndent = match.groupValues[2].length
            val number = match.groupValues[3].toInt()
            val chunkStart = match.range.last + 1
            val chunkEnd = if (index + 1 < matches.size) matches[index + 1].range.first else raw.length
            val text = textFromChunk(raw.substring(chunkStart, chunkEnd), baseIndent)
            VerseSegment(number, text, startsNewParagraph = match.groups[1] != null)
        }
    }

    private fun textFromChunk(chunk: String, baseIndent: Int): String {
        val lines = chunk.split("\n").map { it.trimEnd() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return ""
        if (lines.size == 1) return lines.single().trim()
        return lines.mapIndexed { index, line ->
            val indent = if (index == 0) baseIndent else line.takeWhile { it == ' ' }.length
            val level = ((indent - baseIndent) / POETRY_INDENT_STEP).coerceAtLeast(0)
            POETIC_LINE_MARKER + POETIC_INDENT_UNIT.repeat(level) + line.trim()
        }.joinToString("\n")
    }
}

/**
 * client for the ESV API. requests real verse markers and fixed indent widths so a
 * passage's paragraph and poetry structure survives instead of coming back as one flat
 * block of text.
 */
internal class EsvApi(private val apiKey: String) {
    private val client = createBibleApiHttpClient()

    suspend fun fetchVerseText(reference: String): Result<String> =
        fetchVerses(reference).mapCatching { verses -> joinVerseTexts(verses.map { it.text }) }

    /** same passage as [fetchVerseText] but keeps verse numbers and paragraph structure
     *  instead of flattening to one string. */
    suspend fun fetchVerses(reference: String): Result<List<VerseSegment>> = runCatching {
        val encoded = URLEncoder.encode(reference, UTF_8.name())
        val response = client.get(
            "https://api.esv.org/v3/passage/text/" +
                "?q=$encoded" +
                "&include-headings=false" +
                "&include-footnotes=false" +
                "&include-verse-numbers=true" +
                "&include-passage-references=false" +
                // we attach "(ESV)" to the reference line ourselves, see VersesHomeScreen
                "&include-short-copyright=false" +
                "&indent-using=space" +
                "&indent-paragraphs=2" +
                "&indent-poetry=true" +
                "&indent-poetry-lines=4",
        ) {
            header("Authorization", "Token $apiKey")
        }

        response.throwIfNotSuccess("ESV")

        val parsed: EsvPassageResponse = response.body()
        val raw = parsed.passages.firstOrNull()
            ?: throw IllegalStateException("ESV API returned no passage text for '$reference'.")
        EsvTextParsing.versesFromPassageText(raw).ifEmpty {
            throw IllegalStateException("ESV API returned no verse markers for '$reference'.")
        }
    }

    fun close() {
        client.close()
    }
}
