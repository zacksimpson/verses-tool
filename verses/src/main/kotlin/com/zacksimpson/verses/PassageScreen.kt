package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcon
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
import java.time.LocalDate

/**
 * shows the passage [VersePickerScreen] already resolved, no fetch here. tap does
 * nothing, long-press opens the actions menu, same as the other verse screens.
 * smaller text with inline verse numbers, since a passage at heading size gets
 * unwieldy fast.
 */
class PassageScreen(
    sealedActivity: SealedLightActivity,
    private val reference: String,
    private val verses: List<VerseSegment>,
    private val translation: Translation,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val today = remember { LocalDate.now().toString() }
        val notesRepo = remember { VerseNotesRepository(lightContext.dataStore) }
        val notes by notesRepo.notes.collectAsState(initial = emptyList())
        // match on reference too, not just date, since a lookup can share a date with the daily verse
        val hasNote = remember(notes, reference) { notes.any { it.date == today && it.reference == reference } }
        // flat text is all the actions screen needs, verse boundaries don't matter for copy/memorize/notes
        val flatText = remember(verses) { joinVerseTexts(verses.map { it.text }) }

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text(reference),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LightScrollView(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .combinedClickable(
                                        interactionSource = null,
                                        indication = null,
                                        onClick = {},
                                        onLongClick = {
                                            navigateTo(
                                                screenFactory = {
                                                    VerseActionsScreen(it, today, reference, flatText, translation)
                                                },
                                            )
                                        },
                                    )
                                    .padding(
                                        horizontal = 1.5f.gridUnitsAsDp(),
                                        vertical = 1.5f.gridUnitsAsDp(),
                                    ),
                            ) {
                                NumberedVerseText(
                                    verses = verses,
                                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LightText(
                                        text = "(${translation.abbreviation})",
                                        variant = LightTextVariant.Copy,
                                    )
                                    if (hasNote) {
                                        LightIcon(
                                            icon = LightIcons.PENCIL,
                                            size = 1f,
                                            modifier = Modifier.padding(start = 0.4f.gridUnitsAsDp()),
                                            contentDescription = "Note saved",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class VersePiece {
    data class Number(val number: Int) : VersePiece()
    data class Word(val text: String) : VersePiece()
}

/** startsNewParagraph marks a row that gets the wider gap above it, only true where a
 *  paragraph or poem actually starts, not a wrapped continuation line. */
private data class VerseRowGroup(val indentLevel: Int, val pieces: List<VersePiece>, val startsNewParagraph: Boolean)

/** groups a passage into rows: plain-prose verses merge into one flowing row until a real
 *  paragraph break splits them. a poetic verse always closes the open row and gets its own
 *  indented row instead of sharing one. */
private fun rowGroupsFor(verses: List<VerseSegment>): List<VerseRowGroup> {
    val groups = mutableListOf<VerseRowGroup>()
    var openRow: MutableList<VersePiece>? = null
    var openRowStartsNewParagraph = false

    fun closeOpenRow() {
        openRow?.let {
            if (it.isNotEmpty()) groups.add(VerseRowGroup(indentLevel = 0, pieces = it, startsNewParagraph = openRowStartsNewParagraph))
        }
        openRow = null
    }

    fun wordsOf(text: String) = text.split(Regex("\\s+")).filter { it.isNotEmpty() }

    for (verse in verses) {
        if (isPoeticText(verse.text)) {
            closeOpenRow()
            linesFromVerseText(verse.text).forEachIndexed { lineIndex, line ->
                val pieces = mutableListOf<VersePiece>()
                if (lineIndex == 0) pieces.add(VersePiece.Number(verse.number))
                wordsOf(line.text).forEach { pieces.add(VersePiece.Word(it)) }
                groups.add(
                    VerseRowGroup(
                        indentLevel = line.indentLevel,
                        pieces = pieces,
                        startsNewParagraph = lineIndex == 0 && verse.startsNewParagraph,
                    ),
                )
            }
        } else {
            if (verse.startsNewParagraph) closeOpenRow()
            if (openRow == null) {
                openRow = mutableListOf()
                openRowStartsNewParagraph = verse.startsNewParagraph
            }
            openRow!!.add(VersePiece.Number(verse.number))
            wordsOf(verse.text).forEach { openRow!!.add(VersePiece.Word(it)) }
        }
    }
    closeOpenRow()
    return groups
}

/** same word-by-word rendering as [VerseText], sized down to paragraph since heading size
 *  is unwieldy for more than one verse. verse numbers sit inline, smaller and unbolded so
 *  they read as markers. poetic lines keep their indentation, and a real paragraph break
 *  gets extra space above it so it doesn't look like a plain wrap. */
@Composable
private fun NumberedVerseText(verses: List<VerseSegment>, modifier: Modifier = Modifier) {
    val rows = remember(verses) { rowGroupsFor(verses) }
    Column(modifier = modifier) {
        rows.forEachIndexed { index, row ->
            val topGap = when {
                index == 0 -> 0.dp
                row.startsNewParagraph -> PARAGRAPH_VERTICAL_GAP
                else -> VERSE_WORD_VERTICAL_GAP
            }
            FlowRow(
                modifier = Modifier
                    .padding(top = topGap)
                    .padding(start = (row.indentLevel * POETIC_INDENT_GRID_UNITS).gridUnitsAsDp()),
                horizontalArrangement = Arrangement.spacedBy(VERSE_WORD_HORIZONTAL_GAP),
                verticalArrangement = Arrangement.spacedBy(VERSE_WORD_VERTICAL_GAP),
            ) {
                row.pieces.forEach { piece ->
                    when (piece) {
                        is VersePiece.Number -> LightText(text = piece.number.toString(), variant = LightTextVariant.Detail)
                        is VersePiece.Word -> LightText(text = piece.text, variant = LightTextVariant.Paragraph)
                    }
                }
            }
        }
    }
}
