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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AllBookmarksViewModel(
    repo: VerseBookmarksRepository,
) : LightViewModel<Unit>() {
    val bookmarks = repo.bookmarks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}

/**
 * every saved bookmark, most recent first. tapping one opens ViewBookmarkScreen, whose
 * remove button writes straight to the same repository this screen reads from, so no
 * resultCallback is needed since bookmarks is a reactive flow. unlike all notes, there's
 * no filterReference variant, a bookmark's state is already visible on the action sheet.
 */
class AllBookmarksScreen(
    sealedActivity: SealedLightActivity,
) : LightScreen<Unit, AllBookmarksViewModel>(sealedActivity) {

    override val viewModelClass: Class<AllBookmarksViewModel>
        get() = AllBookmarksViewModel::class.java

    override fun createViewModel(): AllBookmarksViewModel =
        AllBookmarksViewModel(VerseBookmarksRepository(lightContext.dataStore))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val bookmarks by viewModel.bookmarks.collectAsState()
        val sorted = remember(bookmarks) { bookmarks.sortedByDescending { it.createdAtMillis } }

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text("Bookmarks"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (sorted.isEmpty()) {
                            LightText(
                                text = "No bookmarks yet — long-press any verse to save one.",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp()),
                            )
                        } else {
                            LightScrollView(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                sorted.forEach { bookmark ->
                                    BookmarkRow(
                                        bookmark = bookmark,
                                        onClick = {
                                            navigateTo(screenFactory = { ViewBookmarkScreen(it, bookmark) })
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

private const val BOOKMARK_PREVIEW_MAX_LINES = 3

@Composable
private fun BookmarkRow(bookmark: VerseBookmark, onClick: () -> Unit) {
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
            text = "${bookmark.reference} (${bookmark.resolvedTranslation().abbreviation})",
            variant = LightTextVariant.Detail,
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )
        LightText(
            text = bookmark.text,
            variant = LightTextVariant.Copy,
            maxLines = BOOKMARK_PREVIEW_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            lineHeightMultiplier = 0.85f,
        )
    }
}
