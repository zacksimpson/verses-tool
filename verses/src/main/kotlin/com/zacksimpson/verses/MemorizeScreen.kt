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
 * Word-by-word memorization drill. The forward chevron blanks 2-4 more random words at
 * a time (underline in place of each word, same width, punctuation included in the
 * blank); the back chevron un-blanks exactly the batch the last forward tap added. Both
 * the random reveal order and the batch sizes are generated once when the screen opens
 * and then just walked back and forth by index, so repeatedly tapping forward/back always
 * lands on the same words at the same step — it isn't re-rolled or persisted, so leaving
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
            // Modal-style (DONE in the bottom bar to dismiss, no back chevron) — swipe-back
            // is intentionally off so there's exactly one way out, matching the affordance
            // shown, same pattern as VerseDatePickerScreen's calendar.
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
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 1.5f.gridUnitsAsDp(),
                                    vertical = 1.5f.gridUnitsAsDp(),
                                ),
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
                            // Same "DONE" styling as a ConfirmScreen's confirm button
                            // (LightTextVariant.Button, the bottom bar's own default text
                            // variant) — the only way to leave this modal screen now that
                            // there's no back chevron or swipe-back.
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

    // Draws the underline against this Text's own measured size rather than wrapping it
    // in a Box with fillMaxWidth() — inside a FlowRow, fillMaxWidth() resolves against
    // the row's remaining width, not this word's width, so it overshoots.
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
