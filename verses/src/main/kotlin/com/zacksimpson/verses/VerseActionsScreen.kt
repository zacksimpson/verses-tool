package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerseActionsViewModel(
    private val repo: VerseNotesRepository,
    private val bookmarksRepo: VerseBookmarksRepository,
) : LightViewModel<Unit>() {
    val notes = repo.notes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val bookmarks = bookmarksRepo.bookmarks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addNote(date: String, reference: String, text: String, translation: Translation) {
        // caller calls goBack() right after this, which tears down the scope, so
        // without NonCancellable the write could get cancelled mid-flight
        viewModelScope.launch {
            withContext(NonCancellable) {
                repo.addNote(date, reference, text, translation)
            }
        }
    }

    fun toggleBookmark(reference: String, text: String, translation: Translation) {
        // same reasoning as addNote above
        viewModelScope.launch {
            withContext(NonCancellable) {
                bookmarksRepo.toggleBookmark(reference, text, translation)
            }
        }
    }
}

/** long-press action sheet for a verse: copy, memorize, bookmark (or remove, when
 *  already saved), add notes, and view notes when any exist. styled to match
 *  reminders-tool's action sheet. */
class VerseActionsScreen(
    sealedActivity: SealedLightActivity,
    private val date: String,
    private val reference: String,
    private val verseText: String,
    private val translation: Translation,
) : LightScreen<Unit, VerseActionsViewModel>(sealedActivity) {

    override val viewModelClass: Class<VerseActionsViewModel>
        get() = VerseActionsViewModel::class.java

    override fun createViewModel(): VerseActionsViewModel =
        VerseActionsViewModel(
            VerseNotesRepository(lightContext.dataStore),
            VerseBookmarksRepository(lightContext.dataStore),
        )

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val clipboardManager = LocalClipboardManager.current
        val notes by viewModel.notes.collectAsState()
        val hasNotesForVerse = notes.any { it.reference == reference }
        val bookmarks by viewModel.bookmarks.collectAsState()
        val isBookmarked = bookmarks.any { it.reference == reference }

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
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    ActionRow(
                        text = "Copy",
                        onClick = {
                            clipboardManager.setText(
                                AnnotatedString("$verseText\n\n$reference (${translation.abbreviation})"),
                            )
                            goBack(Unit)
                        },
                    )
                    ActionRow(
                        text = "Memorize",
                        onClick = {
                            navigateTo(
                                screenFactory = { MemorizeScreen(it, reference, verseText, translation) },
                                // ignores the result, just closes this sheet once memorize is
                                // backed out of, so returning lands on the verse instead of
                                // leaving the sheet underneath
                                resultCallback = { goBack(Unit) },
                            )
                        },
                    )
                    ActionRow(
                        text = if (isBookmarked) "Remove Bookmark" else "Bookmark",
                        onClick = {
                            viewModel.toggleBookmark(reference, verseText, translation)
                            goBack(Unit)
                        },
                    )
                    ActionRow(
                        text = "Add Notes",
                        onClick = {
                            navigateTo(
                                screenFactory = { TextEditorScreen(it, TextEditorRequest("Add Notes")) },
                                resultCallback = { text ->
                                    viewModel.addNote(date, reference, text, translation)
                                    goBack(Unit)
                                },
                            )
                        },
                    )
                    if (hasNotesForVerse) {
                        ActionRow(
                            text = "View Notes",
                            onClick = {
                                navigateTo(
                                    screenFactory = { AllNotesScreen(it, filterReference = reference) },
                                    resultCallback = { goBack(Unit) },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    LightText(
        text = text,
        variant = LightTextVariant.Heading,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 0.75f.gridUnitsAsDp()),
    )
}
