package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerseActionsViewModel(
    private val repo: VerseNotesRepository,
) : LightViewModel<Unit>() {
    val notes = repo.notes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addNote(date: String, reference: String, text: String) {
        // NonCancellable: the caller calls goBack() right after this, which tears down
        // this ViewModel's scope — without this the write can get cancelled mid-flight.
        viewModelScope.launch {
            withContext(NonCancellable) {
                repo.addNote(date, reference, text)
            }
        }
    }
}

/** Long-press action sheet for a verse — currently just "Add Notes" — styled to match
 *  reminders-tool's ListActionsScreen. */
class VerseActionsScreen(
    sealedActivity: SealedLightActivity,
    private val date: String,
    private val reference: String,
    private val verseText: String,
) : LightScreen<Unit, VerseActionsViewModel>(sealedActivity) {

    override val viewModelClass: Class<VerseActionsViewModel>
        get() = VerseActionsViewModel::class.java

    override fun createViewModel(): VerseActionsViewModel =
        VerseActionsViewModel(VerseNotesRepository(lightContext.dataStore))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val clipboardManager = LocalClipboardManager.current

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
                            clipboardManager.setText(AnnotatedString("$verseText\n\n$reference (ESV)"))
                            goBack(Unit)
                        },
                    )
                    ActionRow(
                        text = "Add Notes",
                        onClick = {
                            navigateTo(
                                screenFactory = { TextEditorScreen(it, TextEditorRequest("Add Notes")) },
                                resultCallback = { text ->
                                    viewModel.addNote(date, reference, text)
                                    goBack(Unit)
                                },
                            )
                        },
                    )
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
            .clickable(onClick = onClick)
            .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 0.75f.gridUnitsAsDp()),
    )
}
