package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
 * read-only view of one saved note, reached from all notes. the header title jumps to
 * the original verse. EDIT pushes [TextEditorScreen] and forwards whatever it returns
 * back up via goBack, so all notes doesn't need to know this screen exists in between.
 * backing out without editing calls goBack(null), which the sdk treats as no result and
 * skips all notes' resultCallback entirely.
 */
class ViewNoteScreen(
    sealedActivity: SealedLightActivity,
    private val note: VerseNote,
) : SimpleLightScreen<String>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(null) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(null) }),
                        center = LightTopBarCenter.Text(
                            text = note.reference,
                            onClick = {
                                navigateTo(screenFactory = { VerseForDateScreen(it, note.date) })
                            },
                        ),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
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
                                LightText(
                                    text = formatDisplayDate(note.date),
                                    variant = LightTextVariant.Detail,
                                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                                )
                                LightText(
                                    text = note.text,
                                    variant = LightTextVariant.Paragraph,
                                )
                            }
                        }
                    }

                    LightBottomBar(
                        items = listOf(
                            LightBarButton.Text(
                                text = "EDIT",
                                onClick = {
                                    navigateTo(
                                        screenFactory = {
                                            TextEditorScreen(it, TextEditorRequest(note.reference, note.text))
                                        },
                                        resultCallback = { text -> goBack(text) },
                                    )
                                },
                            ),
                        ),
                    )
                }
            }
        }
    }
}
