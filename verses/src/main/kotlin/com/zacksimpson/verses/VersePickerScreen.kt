package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

private const val VERSE_GRID_COLUMNS = 4

private sealed class ResolveState {
    data object Idle : ResolveState()
    data object Resolving : ResolveState()
    data class Error(val message: String) : ResolveState()
}

/** Given the verse a range was anchored on and the verse just tapped to close it, returns
 *  the resolved "Book Chapter:Verse" or "Book Chapter:Start-End" reference — order doesn't
 *  matter (tapping backwards from the anchor still resolves correctly) and tapping the
 *  anchor again resolves to a single verse rather than a same-start-and-end range. */
internal fun resolvePassageReference(book: String, chapter: Int, anchor: Int, tapped: Int): String {
    val lo = minOf(anchor, tapped)
    val hi = maxOf(anchor, tapped)
    return if (lo == hi) "$book $chapter:$lo" else "$book $chapter:$lo-$hi"
}

/**
 * Fourth screen of the verse lookup flow — a grid of verse numbers for [book] [chapter].
 * The grid itself is static (BibleBooks' baked-in per-chapter verse counts), so it renders
 * instantly with no network round trip. Only once a verse or range is actually confirmed
 * does a single fetchVerses call resolve its verses (translation from the lookup
 * preference, defaulting to KJV — user-configurable among any translation, see Settings'
 * Fallback Translation and VersePreferences.lookupTranslation), then hands off to
 * PassageScreen to read it. LookupRateLimiter's daily backstop is consulted before every
 * fetch here — a no-op for public domain sources, a real backstop for copyrighted ones.
 *
 * Tapping a verse number anchors a range start (underlined); tapping another verse number
 * closes the range and resolves it. Tapping the anchor again resolves to that single verse
 * instead of a same-verse range.
 */
class VersePickerScreen(
    sealedActivity: SealedLightActivity,
    private val book: String,
    private val chapter: Int,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val scope = rememberCoroutineScope()
        // Scoped to the screen (not per-tap) so picking multiple verses from this chapter
        // reuses the same HTTP client instead of building and tearing one down every time.
        val fetcher = remember { VerseFetcher() }
        DisposableEffect(Unit) {
            onDispose { fetcher.close() }
        }
        val verseCount = remember(book, chapter) {
            BibleBooks.all.first { it.name == book }.versesPerChapter[chapter - 1]
        }
        val rows = remember(verseCount) { (1..verseCount).chunked(VERSE_GRID_COLUMNS) }
        var rangeStart by remember { mutableStateOf<Int?>(null) }
        var resolveState by remember { mutableStateOf<ResolveState>(ResolveState.Idle) }

        fun confirmSelection(anchor: Int, tapped: Int) {
            val reference = resolvePassageReference(book, chapter, anchor, tapped)
            resolveState = ResolveState.Resolving
            scope.launch {
                val prefs = lightContext.dataStore.data.first()
                val translation = prefs.lookupTranslation()
                if (!fetcher.isConfigured(translation)) {
                    resolveState = ResolveState.Error(fetcher.missingKeyMessage(translation))
                    return@launch
                }
                val rateLimiter = LookupRateLimiter(lightContext.dataStore)
                if (!rateLimiter.shouldAllowLookup(translation)) {
                    resolveState = ResolveState.Error(
                        "Today's lookup limit for ${translation.abbreviation} has been reached. " +
                            "Try again tomorrow, or switch translations in Settings.",
                    )
                    return@launch
                }
                val result = fetcher.fetchVerses(translation, reference)
                result.fold(
                    onSuccess = { verses ->
                        rateLimiter.recordLookup(translation)
                        navigateTo(
                            screenFactory = { PassageScreen(it, reference, verses, translation) },
                        )
                        resolveState = ResolveState.Idle
                    },
                    onFailure = {
                        resolveState = ResolveState.Error("Couldn't load $reference. Try again shortly.")
                    },
                )
            }
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
                        center = LightTopBarCenter.Text("$book $chapter"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (val resolving = resolveState) {
                            is ResolveState.Resolving -> LightText(
                                text = "Loading…",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp()),
                            )

                            is ResolveState.Error -> LightText(
                                text = "${resolving.message}\n\nTap to try again.",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier
                                    .padding(horizontal = 1.5f.gridUnitsAsDp())
                                    .clickable { resolveState = ResolveState.Idle },
                            )

                            is ResolveState.Idle -> {
                                LightScrollView(
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp())) {
                                        rows.forEach { row ->
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                row.forEach { number ->
                                                    VerseCell(
                                                        number = number,
                                                        isAnchor = number == rangeStart,
                                                        onClick = {
                                                            val anchor = rangeStart
                                                            if (anchor == null) {
                                                                rangeStart = number
                                                            } else {
                                                                rangeStart = null
                                                                confirmSelection(anchor, number)
                                                            }
                                                        },
                                                    )
                                                }
                                                repeat(VERSE_GRID_COLUMNS - row.size) {
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
    }
}

@Composable
private fun RowScope.VerseCell(number: Int, isAnchor: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 0.75f.gridUnitsAsDp()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LightText(text = number.toString(), variant = LightTextVariant.Subtitle)
            Box(
                modifier = Modifier
                    .padding(top = 0.2f.gridUnitsAsDp())
                    .width(14.dp)
                    .height(2.dp)
                    .background(if (isAnchor) LightThemeTokens.colors.content else Color.Transparent),
            )
        }
    }
}
