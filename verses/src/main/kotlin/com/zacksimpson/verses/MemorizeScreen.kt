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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import com.thelightphone.sdk.ui.LightScrollBarPosition
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
 * blank); the back chevron un-blanks exactly the batch the last forward tap added. The
 * random order is generated once when the screen opens and just walked back and forth —
 * it isn't re-rolled or persisted, so leaving and reopening starts a fresh attempt.
 */
class MemorizeScreen(
    sealedActivity: SealedLightActivity,
    private val reference: String,
    private val verseText: String,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val words = remember(verseText) {
            verseText.split(Regex("\\s+")).filter { it.isNotEmpty() }
        }
        val blankOrder = remember(verseText) { words.indices.shuffled() }
        val stepSizes = remember(verseText) { mutableStateListOf<Int>() }
        val blankedCount = stepSizes.sum()
        val blankedIndices = remember(blankedCount) { blankOrder.take(blankedCount).toSet() }

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text("Memorize"),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LightScrollView(
                            modifier = Modifier.fillMaxSize(),
                            scrollBarPosition = LightScrollBarPosition.Inside,
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 1f.gridUnitsAsDp(),
                                    vertical = 1.5f.gridUnitsAsDp(),
                                ),
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    words.forEachIndexed { index, word ->
                                        BlankableWord(word = word, isBlank = index in blankedIndices)
                                    }
                                }
                                LightText(
                                    text = "$reference (ESV)",
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
                                onClick = if (stepSizes.isNotEmpty()) {
                                    { stepSizes.removeAt(stepSizes.lastIndex) }
                                } else {
                                    null
                                },
                                contentDescription = "Show previous words",
                            ),
                            LightBarButton.LightIcon(
                                icon = LightIcons.ARROW_RIGHT,
                                onClick = if (blankedCount < words.size) {
                                    {
                                        val remaining = words.size - blankedCount
                                        stepSizes.add((2..4).random().coerceAtMost(remaining))
                                    }
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
