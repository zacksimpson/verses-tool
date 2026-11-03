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

// shared word gaps, also used by memorize's per-word flow row so blanking doesn't
// look different from a normal verse display
val VERSE_WORD_HORIZONTAL_GAP = 4.dp
val VERSE_WORD_VERTICAL_GAP = 4.dp

/** extra gap above a row starting a new paragraph, so a real break reads as
 *  distinct from a line that's just wrapping for width. */
val PARAGRAPH_VERTICAL_GAP = 14.dp

/** marks the start of a poetic line, a tab never shows up in verse text otherwise.
 *  helloao tags every poem line with this; [linesFromVerseText] strips it back off. */
internal const val POETIC_LINE_MARKER = "\t"

/** two spaces per poetic indent level, the convention helloao uses for indentation.
 *  esv and youversion have no such structure, they come back as one plain line. */
internal const val POETIC_INDENT_UNIT = "  "

/** grid units of indent per poetic level, rebased so an unindented line starts at 0. */
internal const val POETIC_INDENT_GRID_UNITS = 2f

/** one line of verse text with its poetic indent level already extracted (0 = no indent). */
internal data class VerseLine(val indentLevel: Int, val text: String)

internal fun linesFromVerseText(text: String): List<VerseLine> =
    text.split("\n").map { rawLine ->
        var remaining = rawLine.removePrefix(POETIC_LINE_MARKER)
        var level = 0
        while (remaining.startsWith(POETIC_INDENT_UNIT)) {
            remaining = remaining.removePrefix(POETIC_INDENT_UNIT)
            level++
        }
        VerseLine(indentLevel = level, text = remaining)
    }

/** whether a verse's text carries helloao's poetic structure, only true for public
 *  domain poetry sources. */
internal fun isPoeticText(text: String): Boolean = POETIC_LINE_MARKER in text

/** joins verse texts into one string. a line break goes between two verses only if
 *  either side is poetic, so a plain-prose range still reads as one paragraph. */
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
 * renders verse text word by word in a flow row per line instead of one text block,
 * so blanking a word in memorize never triggers a rewrap. poetic passages keep their
 * line breaks and indentation, plain prose is unaffected.
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
