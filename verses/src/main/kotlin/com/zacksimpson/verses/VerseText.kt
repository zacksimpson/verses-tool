package com.zacksimpson.verses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.gridUnitsAsDp

// Shared word gaps for verse text, also used by MemorizeScreen's per-word FlowRow so
// its blanking never looks visually different from a normal verse display.
val VERSE_WORD_HORIZONTAL_GAP = 4.dp
val VERSE_WORD_VERTICAL_GAP = 4.dp

/** Two literal spaces per poetic indent level — the convention HelloAoParsing encodes a
 *  verse's poetic line breaks and indentation with (see HelloAoParsing.linesFrom), since
 *  fetchVerseText/fetchVerses still hand back a plain String everywhere else in the app.
 *  [linesFromVerseText] below reads it back out. Sources with no such structure (ESV,
 *  YouVersion) just come back as a single unindented line, same as before this existed. */
internal const val POETIC_INDENT_UNIT = "  "

/** Grid units of leading indent per poetic level — same "poem":1/2/3 numbering the source
 *  JSON uses, just re-based to start at 0 for an unindented first line. Shared with
 *  PassageScreen's NumberedVerseText, which applies the same indent per row group. */
internal const val POETIC_INDENT_GRID_UNITS = 2f

/** One line of verse text with its poetic indent level already extracted (0 = no indent). */
internal data class VerseLine(val indentLevel: Int, val text: String)

internal fun linesFromVerseText(text: String): List<VerseLine> =
    text.split("\n").map { rawLine ->
        var remaining = rawLine
        var level = 0
        while (remaining.startsWith(POETIC_INDENT_UNIT)) {
            remaining = remaining.removePrefix(POETIC_INDENT_UNIT)
            level++
        }
        VerseLine(indentLevel = level, text = remaining)
    }

/** Whether a single verse's text carries HelloAoParsing's poetic structure (more than one
 *  line, or a first line that's itself indented) — a plain prose verse always collapses
 *  to one unindented line, so this only ever fires for public domain sources' poetry. */
internal fun isPoeticText(text: String): Boolean = "\n" in text || text.startsWith(POETIC_INDENT_UNIT)

/** Joins multiple verses' texts into one combined string for callers that only need flat
 *  text (fetchVerseText's multi-verse ranges) — a hard line break goes between two verses
 *  only when either side is itself poetic, so a plain-prose range still reads as one
 *  continuous paragraph while a poetic range keeps each verse on its own line(s). */
internal fun joinVerseTexts(texts: List<String>): String {
    if (texts.isEmpty()) return ""
    val result = StringBuilder(texts.first())
    for (i in 1 until texts.size) {
        result.append(if (isPoeticText(texts[i - 1]) || isPoeticText(texts[i])) "\n" else " ")
        result.append(texts[i])
    }
    return result.toString()
}

/**
 * Renders verse text word-by-word in a FlowRow per line (rather than as a single Text
 * block) — originally built for MemorizeScreen (so blanking a word never changes layout
 * or triggers a rewrap), reused everywhere a verse is shown so all three screens match.
 * Poetic passages (Psalms, Proverbs, OT poetry quoted in the NT) keep their line breaks
 * and indentation instead of collapsing into one flowing paragraph — plain prose is
 * unaffected, since it always comes back as a single line here.
 */
@Composable
fun VerseText(text: String, modifier: Modifier = Modifier) {
    val lines = remember(text) { linesFromVerseText(text) }
    Column(modifier = modifier) {
        lines.forEachIndexed { index, line ->
            val words = remember(line.text) { line.text.split(Regex("\\s+")).filter { it.isNotEmpty() } }
            FlowRow(
                modifier = Modifier
                    .padding(top = if (index > 0) VERSE_WORD_VERTICAL_GAP else 0.dp)
                    .padding(start = (line.indentLevel * POETIC_INDENT_GRID_UNITS).gridUnitsAsDp()),
                horizontalArrangement = Arrangement.spacedBy(VERSE_WORD_HORIZONTAL_GAP),
                verticalArrangement = Arrangement.spacedBy(VERSE_WORD_VERTICAL_GAP),
            ) {
                words.forEach { word ->
                    LightText(text = word, variant = LightTextVariant.Heading)
                }
            }
        }
    }
}
