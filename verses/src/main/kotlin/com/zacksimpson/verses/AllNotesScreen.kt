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
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
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
 * date. Tapping a note reopens it for editing (pre-filled) — there's no separate
 * "view" vs "edit" mode, matching how notes are entered in the first place via
 * VerseActionsScreen's "Add Notes" row.
 */
class AllNotesScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, AllNotesViewModel>(sealedActivity) {

    override val viewModelClass: Class<AllNotesViewModel>
        get() = AllNotesViewModel::class.java

    override fun createViewModel(): AllNotesViewModel =
        AllNotesViewModel(VerseNotesRepository(lightContext.dataStore))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val notes by viewModel.notes.collectAsState()
        val sorted = remember(notes) { notes.sortedByDescending { it.createdAtMillis } }

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text("All Notes"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (sorted.isEmpty()) {
                            LightText(
                                text = "No notes yet — long-press any verse to add one.",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(horizontal = 1f.gridUnitsAsDp()),
                            )
                        } else {
                            LightScrollView(
                                modifier = Modifier.fillMaxSize(),
                                scrollBarPosition = LightScrollBarPosition.Inside,
                            ) {
                                sorted.forEach { note ->
                                    NoteRow(
                                        note = note,
                                        onClick = {
                                            navigateTo(
                                                screenFactory = {
                                                    TextEditorScreen(it, TextEditorRequest(note.reference, note.text))
                                                },
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

@Composable
private fun NoteRow(note: VerseNote, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 1f.gridUnitsAsDp(), vertical = 0.75f.gridUnitsAsDp()),
    ) {
        LightText(
            text = "${note.reference} — ${formatDisplayDate(note.date)}",
            variant = LightTextVariant.Detail,
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )
        LightText(
            text = note.text,
            variant = LightTextVariant.Copy,
        )
    }
}
