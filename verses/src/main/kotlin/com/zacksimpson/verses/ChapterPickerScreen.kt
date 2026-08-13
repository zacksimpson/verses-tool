package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val CHAPTER_GRID_COLUMNS = 3

/** third screen of the lookup flow, a grid of chapter numbers for [book]. tap a chapter
 *  to pick a specific verse or range within it; long-press a chapter to jump straight
 *  into reading the whole thing. */
class ChapterPickerScreen(
    sealedActivity: SealedLightActivity,
    private val book: String,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val scope = rememberCoroutineScope()
        val chapterCount = remember(book) { BibleBooks.all.first { it.name == book }.chapterCount }
        val rows = remember(chapterCount) { (1..chapterCount).chunked(CHAPTER_GRID_COLUMNS) }
        // guards against a fast double-long-press pushing two PassageScreen instances
        var isJumpingToChapter by remember { mutableStateOf(false) }

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text(book),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LightScrollView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 1.5f.gridUnitsAsDp()),
                        ) {
                            Column {
                                rows.forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        row.forEach { chapter ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .combinedClickable(
                                                        interactionSource = null,
                                                        indication = null,
                                                        onClick = {
                                                            navigateTo(
                                                                screenFactory = { VersePickerScreen(it, book, chapter) },
                                                            )
                                                        },
                                                        onLongClick = {
                                                            if (!isJumpingToChapter) {
                                                                isJumpingToChapter = true
                                                                scope.launch {
                                                                    val translation = lightContext.dataStore.data.first().lookupTranslation()
                                                                    navigateTo(
                                                                        screenFactory = {
                                                                            PassageScreen(it, "$book $chapter", null, translation)
                                                                        },
                                                                        resultCallback = { isJumpingToChapter = false },
                                                                    )
                                                                }
                                                            }
                                                        },
                                                    )
                                                    .padding(vertical = 1f.gridUnitsAsDp()),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                LightText(text = chapter.toString(), variant = LightTextVariant.Subtitle)
                                            }
                                        }
                                        // a short trailing row still gets empty weighted cells so its
                                        // numbers land in the same columns as every row above it
                                        repeat(CHAPTER_GRID_COLUMNS - row.size) {
                                            Box(modifier = Modifier.weight(1f))
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
}
