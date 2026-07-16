package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

private const val CHAPTER_GRID_COLUMNS = 3

/** Third screen of the verse lookup flow — a grid of chapter numbers for [book]. */
class ChapterPickerScreen(
    sealedActivity: SealedLightActivity,
    private val book: String,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val chapterCount = remember(book) { BibleBooks.all.first { it.name == book }.chapterCount }
        val rows = remember(chapterCount) { (1..chapterCount).chunked(CHAPTER_GRID_COLUMNS) }

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
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp())) {
                                rows.forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        row.forEach { chapter ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        navigateTo(
                                                            screenFactory = { VersePickerScreen(it, book, chapter) },
                                                        )
                                                    }
                                                    .padding(vertical = 1f.gridUnitsAsDp()),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                LightText(text = chapter.toString(), variant = LightTextVariant.Subtitle)
                                            }
                                        }
                                        // Short trailing row (e.g. a book whose chapter count isn't a
                                        // multiple of 3) still gets empty weighted cells so its numbers
                                        // land in the same columns as every row above it.
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
