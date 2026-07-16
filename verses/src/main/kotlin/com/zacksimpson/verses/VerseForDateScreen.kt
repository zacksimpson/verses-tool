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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.first
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcon
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
import java.time.LocalDate

/** Shows the verse for a specific past date, looked up live (no caching — this is a
 *  low-frequency lookup, unlike the home screen's daily cache). */
class VerseForDateScreen(
    sealedActivity: SealedLightActivity,
    private val dateStr: String,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var state by remember { mutableStateOf<VerseUiState>(VerseUiState.Loading) }
        val notesRepo = remember { VerseNotesRepository(lightContext.dataStore) }
        val notes by notesRepo.notes.collectAsState(initial = emptyList())
        val hasNote = remember(notes, dateStr) { notes.any { it.date == dateStr } }

        LaunchedEffect(dateStr) {
            val prefs = lightContext.dataStore.data.first()
            val translation = prefs.selectedTranslation()
            val fetcher = VerseFetcher()

            try {
                if (!fetcher.isConfigured(translation)) {
                    state = VerseUiState.ConfigError(fetcher.missingKeyMessage(translation))
                    return@LaunchedEffect
                }

                // Same daily backstop the verse lookup flow uses — this screen has no
                // caching of its own (a fresh live fetch on every date tapped), so without
                // this a user rapidly browsing VerseDatePickerScreen's calendar could burn
                // through the shared ESV/YouVersion API budget with nothing to stop it.
                val rateLimiter = LookupRateLimiter(lightContext.dataStore)
                if (!rateLimiter.shouldAllowLookup(translation)) {
                    state = VerseUiState.ConfigError(
                        "Today's lookup limit for ${translation.abbreviation} has been reached. " +
                            "Try again tomorrow, or switch translations in Settings.",
                    )
                    return@LaunchedEffect
                }

                val reference = VerseSelector.referenceForDate(LocalDate.parse(dateStr))
                val result = fetcher.fetchVerseText(translation, reference)
                state = result.fold(
                    onSuccess = { text ->
                        rateLimiter.recordLookup(translation)
                        VerseUiState.Loaded(reference = reference, text = text, translation = translation)
                    },
                    onFailure = { VerseUiState.ConfigError("Couldn't load that day's verse. Try again shortly.") },
                )
            } finally {
                // In a finally (not a plain sequential call) so this still runs if the
                // coroutine is cancelled mid-fetch — e.g. the user navigates to another
                // date before this one's request completes — otherwise the two HTTP
                // clients VerseFetcher owns are abandoned without being shut down.
                fetcher.close()
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
                        center = LightTopBarCenter.Text(formatDisplayDate(dateStr)),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (val mode = state) {
                            is VerseUiState.Loading -> {
                                LightText(
                                    text = "Loading…",
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp()),
                                )
                            }

                            is VerseUiState.ConfigError -> {
                                LightText(
                                    text = mode.message,
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp()),
                                )
                            }

                            is VerseUiState.Loaded -> {
                                LightScrollView(
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = {
                                                    navigateTo(
                                                        screenFactory = {
                                                            VerseActionsScreen(
                                                                it,
                                                                dateStr,
                                                                mode.reference,
                                                                mode.text,
                                                                mode.translation,
                                                            )
                                                        },
                                                    )
                                                },
                                            )
                                            .padding(
                                                horizontal = 1.5f.gridUnitsAsDp(),
                                                vertical = 1.5f.gridUnitsAsDp(),
                                            ),
                                    ) {
                                        VerseText(
                                            text = mode.text,
                                            modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            LightText(
                                                text = "${mode.reference} (${mode.translation.abbreviation})",
                                                variant = LightTextVariant.Copy,
                                            )
                                            if (hasNote) {
                                                LightIcon(
                                                    icon = LightIcons.PENCIL,
                                                    size = 1f,
                                                    modifier = Modifier.padding(start = 0.4f.gridUnitsAsDp()),
                                                    contentDescription = "Note saved",
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
        }
    }
}
