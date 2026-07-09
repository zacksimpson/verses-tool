package com.zacksimpson.verses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant

// Shared word gaps for verse text, also used by MemorizeScreen's per-word FlowRow so
// its blanking never looks visually different from a normal verse display.
val VERSE_WORD_HORIZONTAL_GAP = 4.dp
val VERSE_WORD_VERTICAL_GAP = 4.dp

/**
 * Renders verse text word-by-word in a FlowRow rather than as a single Text block.
 * Originally built for MemorizeScreen (so blanking a word never changes layout or
 * triggers a rewrap), reused everywhere a verse is shown so all three screens match.
 */
@Composable
fun VerseText(text: String, modifier: Modifier = Modifier) {
    val words = remember(text) { text.split(Regex("\\s+")).filter { it.isNotEmpty() } }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(VERSE_WORD_HORIZONTAL_GAP),
        verticalArrangement = Arrangement.spacedBy(VERSE_WORD_VERTICAL_GAP),
    ) {
        words.forEach { word ->
            LightText(text = word, variant = LightTextVariant.Heading)
        }
    }
}
