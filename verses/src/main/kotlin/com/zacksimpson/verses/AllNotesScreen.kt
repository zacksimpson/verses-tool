package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllNotesViewModel(
    private val repo: VerseNotesRepository,
) : LightViewModel<Unit>() {
    val notes = repo.notes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateNote(id: String, text: String) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                repo.updateNote(id, text)
            }
        }
    }
}

/**
 * Every saved note, most recent first, each shown alongside its verse reference and
 * date. Tapping a note opens a read-only ViewNoteScreen; its EDIT button is what reaches
 * TextEditorScreen. ViewNoteScreen forwards TextEditorScreen's result straight back here,
 * so this is still the only place that calls updateNote.
 *
 * [filterReference], when set, restricts the list to notes on that one verse — this is
 * how VerseActionsScreen's "View Notes" row reuses this screen instead of duplicating the
 * list/row/navigation logic for a single-verse view.
 */
class AllNotesScreen(
    sealedActivity: SealedLightActivity,
    private val filterReference: String? = null,
) : LightScreen<Unit, AllNotesViewModel>(sealedActivity) {

    override val viewModelClass: Class<AllNotesViewModel>
        get() = AllNotesViewModel::class.java

    override fun createViewModel(): AllNotesViewModel =
        AllNotesViewModel(VerseNotesRepository(lightContext.dataStore))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val notes by viewModel.notes.collectAsState()
        val sorted = remember(notes, filterReference) {
            notes.filter { filterReference == null || it.reference == filterReference }
                .sortedByDescending { it.createdAtMillis }
        }

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text(filterReference ?: "All Notes"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (sorted.isEmpty()) {
                            LightText(
                                text = if (filterReference != null) {
                                    "No notes yet for this verse."
                                } else {
                                    "No notes yet — long-press any verse to add one."
                                },
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp()),
                            )
                        } else {
                            LightScrollView(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                sorted.forEach { note ->
                                    NoteRow(
                                        note = note,
                                        onClick = {
                                            navigateTo(
                                                screenFactory = { ViewNoteScreen(it, note) },
                                                resultCallback = { text ->
                                                    viewModel.updateNote(note.id, text)
                                                },
                                            )
                                        },
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

private const val NOTE_PREVIEW_MAX_LINES = 3

@Composable
private fun NoteRow(note: VerseNote, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 1.5f.gridUnitsAsDp(),
                vertical = 0.75f.gridUnitsAsDp(),
            ),
    ) {
        LightText(
            text = "${note.reference} — ${formatDisplayDate(note.date)}",
            variant = LightTextVariant.Detail,
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )
        LightText(
            text = note.text,
            variant = LightTextVariant.Copy,
            maxLines = NOTE_PREVIEW_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            lineHeightMultiplier = 0.85f,
        )
    }
}
