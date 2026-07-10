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
 * Read-only view of one saved note, reached from All Notes. Tapping EDIT pushes the
 * existing [TextEditorScreen] and forwards whatever it returns straight back up via
 * goBack, so All Notes (which owns the update call) doesn't need to know this screen
 * exists in between — backing out without editing calls goBack(null), which the SDK
 * treats as "no result" and skips All Notes' resultCallback entirely.
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
                        center = LightTopBarCenter.Text(note.reference),
                        rightButton = LightBarButton.Text(
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
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LightScrollView(
                            modifier = Modifier.fillMaxSize(),
                            scrollBarPosition = LightScrollBarPosition.Inside,
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
                                    lighten = true,
                                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                                )
                                LightText(
                                    text = note.text,
                                    variant = LightTextVariant.Paragraph,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
