package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

/**
 * word-by-word memorization drill. forward blanks 2-4 more random words at a time, back
 * un-blanks exactly the last batch added. reveal order and batch sizes are generated once
 * when the screen opens and just walked by index, not re-rolled or persisted, so leaving
 * and reopening starts a fresh attempt.
 */
class MemorizeScreen(
    sealedActivity: SealedLightActivity,
    private val reference: String,
    private val verseText: String,
    private val translation: Translation,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val words = remember(verseText) {
            verseText.split(Regex("\\s+")).filter { it.isNotEmpty() }
        }
        val blankOrder = remember(verseText) { words.indices.shuffled() }
        val stepSizes = remember(verseText) {
            val sizes = mutableListOf<Int>()
            var remaining = words.size
            while (remaining > 0) {
                val size = (2..4).random().coerceAtMost(remaining)
                sizes.add(size)
                remaining -= size
            }
            sizes
        }
        var stepIndex by remember(verseText) { mutableStateOf(0) }
        val blankedCount = stepSizes.take(stepIndex).sum()
        val blankedIndices = remember(blankedCount) { blankOrder.take(blankedCount).toSet() }

        LightTheme(colors = themeColors) {
            // modal style, DONE in the bottom bar dismisses. swipe-back is off so there's
            // one way out, same as VerseDatePickerScreen's calendar
            SwipeBackContainer(enabled = false, onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        center = LightTopBarCenter.Text("Memorize"),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LightScrollView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 1.5f.gridUnitsAsDp()),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 1.5f.gridUnitsAsDp()),
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(VERSE_WORD_HORIZONTAL_GAP),
                                    verticalArrangement = Arrangement.spacedBy(VERSE_WORD_VERTICAL_GAP),
                                ) {
                                    words.forEachIndexed { index, word ->
                                        BlankableWord(word = word, isBlank = index in blankedIndices)
                                    }
                                }
                                LightText(
                                    text = "$reference (${translation.abbreviation})",
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                                )
                            }
                        }
                    }

                    LightBottomBar(
                        items = listOf(
                            LightBarButton.LightIcon(
                                icon = LightIcons.BACK,
                                onClick = if (stepIndex > 0) {
                                    { stepIndex-- }
                                } else {
                                    null
                                },
                                contentDescription = "Show previous words",
                            ),
                            // same DONE styling as a confirm screen, the only way to leave
                            // since there's no back chevron or swipe-back
                            LightBarButton.Text(
                                text = "DONE",
                                onClick = { goBack(Unit) },
                            ),
                            LightBarButton.LightIcon(
                                icon = LightIcons.ARROW_RIGHT,
                                onClick = if (stepIndex < stepSizes.size) {
                                    { stepIndex++ }
                                } else {
                                    null
                                },
                                contentDescription = "Blank next words",
                            ),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun BlankableWord(word: String, isBlank: Boolean) {
    val lineColor = LightThemeTokens.colors.content
    val strokeWidthPx = with(LocalDensity.current) { 2.dp.toPx() }

    // draws the underline against the text's own size. fillMaxWidth() inside a FlowRow
    // would resolve against the row's remaining width instead of this word, and overshoot
    LightText(
        text = word,
        variant = LightTextVariant.Heading,
        color = if (isBlank) Color.Transparent else null,
        modifier = if (isBlank) {
            Modifier.drawBehind {
                val y = size.height - strokeWidthPx / 2
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidthPx,
                )
            }
        } else {
            Modifier
        },
    )
}
