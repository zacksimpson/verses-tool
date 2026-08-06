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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch

/**
 * read-only view of one saved bookmark, reached from all bookmarks. simpler than
 * ViewNoteScreen, no text to edit, so remove writes directly to the repository instead
 * of a resultCallback. all bookmarks' list is a reactive flow, so it reflects the
 * removal automatically.
 */
class ViewBookmarkScreen(
    sealedActivity: SealedLightActivity,
    private val bookmark: VerseBookmark,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val scope = rememberCoroutineScope()
        val repo = remember { VerseBookmarksRepository(lightContext.dataStore) }
        var isRemoving by remember { mutableStateOf(false) }

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text(bookmark.reference),
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
                                VerseText(
                                    text = bookmark.text,
                                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                                )
                                LightText(
                                    text = "(${bookmark.resolvedTranslation().abbreviation})",
                                    variant = LightTextVariant.Copy,
                                )
                            }
                        }
                    }

                    LightBottomBar(
                        items = listOf(
                            LightBarButton.Text(
                                text = "REMOVE BOOKMARK",
                                onClick = {
                                    if (!isRemoving) {
                                        isRemoving = true
                                        scope.launch {
                                            repo.toggleBookmark(
                                                bookmark.reference,
                                                bookmark.text,
                                                bookmark.resolvedTranslation(),
                                            )
                                            goBack(Unit)
                                        }
                                    }
                                },
                            ),
                        ),
                    )
                }
            }
        }
    }
}
