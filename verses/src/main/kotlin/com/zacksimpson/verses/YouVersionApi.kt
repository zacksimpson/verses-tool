package com.zacksimpson.verses

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.Serializable

@Serializable
internal data class YouVersionPassageResponse(
    val id: String = "",
    val content: String = "",
    val reference: String = "",
)

/**
 * Pure parsing logic pulled out of [YouVersionApi] so it's unit testable without network
 * access. `format=html` (unlike the flat `format=text`/default response, which throws away
 * verse numbers and all line structure) returns a shallow, single-level-nested markup:
 * each top-level `<div class="...">` is one row — `"d"` a section heading (skipped, its
 * text isn't surfaced), `"p"` (or any other non-"q*" class) a prose paragraph, `"q1"`/`"q2"`/
 * `"q3"` (or plain `"q"`, seen on NASB) a poetry line at that indent level. A verse's start
 * is marked with an empty `<span class="yv-v" v="N"></span>`, immediately followed by a
 * redundant visible `<span class="yv-vlbl">N</span>` (stripped here, since PassageScreen
 * renders its own verse numbers); a div with no yv-v span at all is a continuation line of
 * whatever verse most recently started (e.g. a poetic verse's second line, which YouVersion
 * gives its own div). A `<div class="p">` groups every verse of one paragraph together, so
 * only the first verse-marker inside a *prose* div actually starts a new paragraph — poetic
 * divs mark every single verse this way too, which is harmless since a poetic verse always
 * gets its own row in PassageScreen regardless of startsNewParagraph.
 */
internal object YouVersionHtmlParsing {
    private val DIV_REGEX = Regex("""<div class="([a-zA-Z0-9]+)">(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
    private val VERSE_START_REGEX = Regex("""<span class="yv-v" v="(\d+)"></span>""")
    private val VERSE_LABEL_REGEX = Regex("""<span class="yv-vlbl">\d+</span>""")
    private val TAG_REGEX = Regex("""<[^>]+>""")

    fun versesFromHtml(html: String): List<VerseSegment> {
        val segments = mutableListOf<VerseSegment>()
        var lastVerseIndex = -1

        for (block in DIV_REGEX.findAll(html)) {
            val className = block.groupValues[1]
            if (className == "d") continue

            val isPoetic = className.startsWith("q")
            val indentLevel = when (className) {
                "q2" -> 1
                "q3" -> 2
                else -> 0
            }
            var firstChunkInBlock = true

            for ((verseNumber, rawText) in splitByVerseMarkers(block.groupValues[2])) {
                val text = cleanText(rawText)
                if (text.isNotEmpty()) {
                    val lineText = if (isPoetic) POETIC_LINE_MARKER + POETIC_INDENT_UNIT.repeat(indentLevel) + text else text
                    if (verseNumber != null) {
                        segments.add(VerseSegment(verseNumber, lineText, startsNewParagraph = firstChunkInBlock && !isPoetic))
                        lastVerseIndex = segments.size - 1
                    } else if (lastVerseIndex >= 0) {
                        val existing = segments[lastVerseIndex]
                        segments[lastVerseIndex] = existing.copy(text = existing.text + "\n" + lineText)
                    }
                }
                firstChunkInBlock = false
            }
        }
        return segments
    }

    /** Splits a div's inner HTML at each yv-v verse-start span — a div with none at all
     *  (a continuation line) comes back as a single chunk with a null verse number. */
    private fun splitByVerseMarkers(inner: String): List<Pair<Int?, String>> {
        val matches = VERSE_START_REGEX.findAll(inner).toList()
        if (matches.isEmpty()) return listOf(null to inner)
        return matches.mapIndexed { index, match ->
            val chunkStart = match.range.last + 1
            val chunkEnd = if (index + 1 < matches.size) matches[index + 1].range.first else inner.length
            match.groupValues[1].toInt() to inner.substring(chunkStart, chunkEnd)
        }
    }

    private fun cleanText(html: String): String =
        html.replace(VERSE_LABEL_REGEX, "").replace(TAG_REGEX, "").replace("¶", "").trim()
}

/**
 * Client for the YouVersion Platform REST API (used for NIV/NASB), hit directly with
 * Ktor rather than their official Kotlin SDK — the SDK artifact isn't on the Light SDK
 * plugin's dependency allowlist, but Ktor already is, and this is a thin enough API that
 * a raw client is no more code than wrapping the SDK would have been.
 */
internal class YouVersionApi(private val appKey: String) {
    private val client = createBibleApiHttpClient()

    suspend fun fetchVerseText(versionId: Int, reference: String): Result<String> =
        fetchVerses(versionId, reference).mapCatching { verses -> joinVerseTexts(verses.map { it.text }) }

    /** Same passage as [fetchVerseText], but keeping each verse's number and paragraph
     *  structure attached rather than flattening to one string — lets the UI show verse
     *  numbers and paragraph breaks for a range. */
    suspend fun fetchVerses(versionId: Int, reference: String): Result<List<VerseSegment>> = runCatching {
        val passageId = UsfmReference.toPassageId(reference)
        val response = client.get(
            "https://api.youversion.com/v1/bibles/$versionId/passages/$passageId?format=html",
        ) {
            header("X-YVP-App-Key", appKey)
        }

        response.throwIfNotSuccess("YouVersion")

        val parsed: YouVersionPassageResponse = response.body()
        if (parsed.content.isBlank()) {
            throw IllegalStateException("YouVersion API returned no passage text for '$reference'.")
        }
        YouVersionHtmlParsing.versesFromHtml(parsed.content).ifEmpty {
            throw IllegalStateException("YouVersion API returned no verse markers for '$reference'.")
        }
    }

    fun close() {
        client.close()
    }
}
