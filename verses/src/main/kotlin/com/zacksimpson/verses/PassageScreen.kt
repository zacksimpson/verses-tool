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
import java.time.LocalDate

/**
 * Final screen of the verse lookup flow — shows the passage [VersePickerScreen] already
 * resolved (no fetch here, the verses are already in hand), so the user can actually read
 * their selection instead of landing straight on the actions menu. A plain tap does
 * nothing; long-press opens VerseActionsScreen, mirroring VerseForDateScreen/
 * VersesHomeScreen's own verse display.
 *
 * Rendered smaller than the single-verse displays (Paragraph, not Heading) and with each
 * verse's number marked inline — reading a multi-verse passage at Heading size with no
 * numbers gets unwieldy fast, unlike the daily verse which is always short.
 */
class PassageScreen(
    sealedActivity: SealedLightActivity,
    private val reference: String,
    private val verses: List<Pair<Int, String>>,
    private val translation: Translation,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val today = remember { LocalDate.now().toString() }
        val notesRepo = remember { VerseNotesRepository(lightContext.dataStore) }
        val notes by notesRepo.notes.collectAsState(initial = emptyList())
        // Distinguishes this specific lookup note from any other note also dated today
        // (the daily verse's note, or a different lookup) — unlike VerseForDateScreen,
        // where a date always maps to exactly one possible reference.
        val hasNote = remember(notes, reference) { notes.any { it.date == today && it.reference == reference } }
        // VerseActionsScreen (Copy/Memorize/Add Notes) only needs flat text, not verse
        // boundaries — Memorize already splits on whitespace regardless of verse numbers.
        val flatText = remember(verses) { joinVerseTexts(verses.map { it.second }) }

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
                            scrollBarPosition = LightScrollBarPosition.Inside,
                        ) {
                            Column(
                                modifier = Modifier
                                    .combinedClickable(
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

private data class VerseRowGroup(val indentLevel: Int, val pieces: List<VersePiece>)

/** Groups a passage's verses into rows to render: consecutive plain-prose verses merge
 *  into one open row (so e.g. Genesis 1:1-3 still reads as one flowing paragraph, verse
 *  numbers inline), while a poetic verse (Psalms, Proverbs, OT poetry quoted in the NT —
 *  see isPoeticText) always closes whatever's open and gets its own indented row(s), never
 *  sharing a row with a neighboring verse. */
private fun rowGroupsFor(verses: List<Pair<Int, String>>): List<VerseRowGroup> {
    val groups = mutableListOf<VerseRowGroup>()
    var openRow: MutableList<VersePiece>? = null

    fun closeOpenRow() {
        openRow?.let { if (it.isNotEmpty()) groups.add(VerseRowGroup(indentLevel = 0, pieces = it)) }
        openRow = null
    }

    fun wordsOf(text: String) = text.split(Regex("\\s+")).filter { it.isNotEmpty() }

    for ((number, text) in verses) {
        if (isPoeticText(text)) {
            closeOpenRow()
            linesFromVerseText(text).forEachIndexed { lineIndex, line ->
                val pieces = mutableListOf<VersePiece>()
                if (lineIndex == 0) pieces.add(VersePiece.Number(number))
                wordsOf(line.text).forEach { pieces.add(VersePiece.Word(it)) }
                groups.add(VerseRowGroup(indentLevel = line.indentLevel, pieces = pieces))
            }
        } else {
            val row = openRow ?: mutableListOf<VersePiece>().also { openRow = it }
            row.add(VersePiece.Number(number))
            wordsOf(text).forEach { row.add(VersePiece.Word(it)) }
        }
    }
    closeOpenRow()
    return groups
}

/** Renders a passage word-by-word (same FlowRow approach as VerseText, so a verse's own
 *  wrapping never looks different from single-verse displays), sized down from VerseText's
 *  Heading to Paragraph since a multi-verse passage at Heading size gets unwieldy fast.
 *  Each verse's number is marked inline before its first word — smaller and unbolded next
 *  to the reading text so it reads as a marker, not another word. Poetic passages keep
 *  their line breaks and indentation (see [rowGroupsFor]) instead of collapsing into one
 *  paragraph the way a plain multi-verse prose range still does. */
@Composable
private fun NumberedVerseText(verses: List<Pair<Int, String>>, modifier: Modifier = Modifier) {
    val rows = remember(verses) { rowGroupsFor(verses) }
    Column(modifier = modifier) {
        rows.forEachIndexed { index, row ->
            FlowRow(
                modifier = Modifier
                    .padding(top = if (index > 0) VERSE_WORD_VERTICAL_GAP else 0.dp)
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
