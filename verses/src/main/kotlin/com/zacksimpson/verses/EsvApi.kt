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
 * Pure parsing logic pulled out of [EsvApi] so it's unit testable without network access.
 * With include-verse-numbers=true, ESV's plain text already carries real structure we were
 * previously throwing away: each verse starts with an inline `[N]` marker, poetic lines are
 * real "\n" breaks indented in multiples of [POETRY_INDENT_STEP] spaces (requested via
 * indent-poetry-lines, see [EsvApi.fetchVerses]), and a verse beginning a new prose
 * paragraph has its `[N]` marker sitting at the start of a line (preceded by "\n") rather
 * than inline after the previous verse's text — the same signal a poetic verse's marker
 * always has too (poetry always starts a fresh line), which is harmless since a poetic
 * verse already gets its own row in PassageScreen regardless of startsNewParagraph.
 */
internal object EsvTextParsing {
    // Matches an "[N]" verse marker together with any whitespace immediately around it —
    // group 1 is a leading newline (present when this verse starts a new line, whether a
    // fresh paragraph or a poetic line), group 2 is the marker's own leading indent (used
    // as the baseline when computing a poetic verse's relative indent levels), group 3 is
    // the verse number itself.
    private val VERSE_MARKER = Regex("""(\n)?([ \t]*)\[(\d+)]\s*""")

    // Matches indent-poetry-lines, which EsvApi requests explicitly rather than relying on
    // ESV's default so this stays correct even if ESV changes its default later.
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
 * Client for the ESV API. Requests real verse-number markers and fixed indent widths (see
 * [EsvTextParsing]) so a passage's paragraph and poetry structure survives into the app
 * instead of coming back as one flat block of text.
 */
internal class EsvApi(private val apiKey: String) {
    private val client = createBibleApiHttpClient()

    suspend fun fetchVerseText(reference: String): Result<String> =
        fetchVerses(reference).mapCatching { verses -> joinVerseTexts(verses.map { it.text }) }

    /** Same passage as [fetchVerseText], but keeping each verse's number and paragraph
     *  structure attached rather than flattening to one string — lets the UI show verse
     *  numbers and paragraph breaks for a range. */
    suspend fun fetchVerses(reference: String): Result<List<VerseSegment>> = runCatching {
        val encoded = URLEncoder.encode(reference, UTF_8.name())
        val response = client.get(
            "https://api.esv.org/v3/passage/text/" +
                "?q=$encoded" +
                "&include-headings=false" +
                "&include-footnotes=false" +
                "&include-verse-numbers=true" +
                "&include-passage-references=false" +
                // We attach "(ESV)" to the reference line ourselves instead — see VersesHomeScreen.
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
