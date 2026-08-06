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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.datastore.preferences.core.edit
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightSurfaceScheme
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
import java.time.LocalDate

@InitialScreen
class VersesHomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, VersesViewModel>(sealedActivity) {

    override val viewModelClass: Class<VersesViewModel>
        get() = VersesViewModel::class.java

    override fun createViewModel(): VersesViewModel =
        VersesViewModel(lightContext.dataStore)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val scope = rememberCoroutineScope()
        val state by viewModel.uiState.collectAsState()
        val notesRepo = remember { VerseNotesRepository(lightContext.dataStore) }
        val notes by notesRepo.notes.collectAsState(initial = emptyList())
        val today = remember { LocalDate.now().toString() }
        val hasNote = remember(notes) { notes.any { it.date == today } }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                val recentsIcon = if (LightThemeTokens.surfaceScheme == LightSurfaceScheme.Dark) {
                    R.drawable.ic_recents_white
                } else {
                    R.drawable.ic_recents_black
                }

                LightTopBar(
                    leftButton = LightBarButton.Icon(
                        painter = painterResource(recentsIcon),
                        onClick = { navigateTo(screenFactory = { VerseDatePickerScreen(it) }) },
                        // ic_recents fills its box edge-to-edge, unlike LightIcons' own icons
                        // which have built-in padding. sized close to the old BACK chevron's
                        // 2-unit box, not reminders-tool's 1.2f, which was tuned for a
                        // thinner icon
                        sizeUnits = 1.5f,
                        contentDescription = "View past verses",
                    ),
                    center = LightTopBarCenter.Text("Verses"),
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
                                            interactionSource = null,
                                            indication = null,
                                            onClick = {},
                                            onLongClick = {
                                                navigateTo(
                                                    screenFactory = {
                                                        VerseActionsScreen(
                                                            it,
                                                            today,
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
                                    if (mode.staleWarning) {
                                        LightText(
                                            text = "Couldn't refresh. Showing last saved verse.",
                                            variant = LightTextVariant.Detail,
                                            modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val bottomBarItems = buildList {
                    val loaded = state as? VerseUiState.Loaded
                    add(
                        LightBarButton.LightIcon(
                            icon = LightIcons.SETTINGS,
                            onClick = { navigateTo(screenFactory = { SettingsScreen(it) }) },
                            contentDescription = "Settings",
                        ),
                    )
                    // sits between settings and memorize so it lands in the visual center
                    // once both are present. entry point for looking up any passage, not
                    // just the daily verse
                    add(
                        LightBarButton.LightIcon(
                            icon = LightIcons.SEARCH,
                            onClick = {
                                scope.launch {
                                    val prefs = lightContext.dataStore.data.first()
                                    val hasSeenInfo = prefs[VersePreferences.HAS_SEEN_FALLBACK_TRANSLATION_INFO] ?: false
                                    if (hasSeenInfo) {
                                        navigateTo(screenFactory = { VerseLookupScreen(it) })
                                    } else {
                                        lightContext.dataStore.edit { p ->
                                            p[VersePreferences.HAS_SEEN_FALLBACK_TRANSLATION_INFO] = true
                                        }
                                        navigateTo(
                                            screenFactory = { FallbackTranslationInfoScreen(it) },
                                            resultCallback = {
                                                navigateTo(screenFactory = { VerseLookupScreen(it) })
                                            },
                                        )
                                    }
                                }
                            },
                            contentDescription = "Look up a verse",
                        ),
                    )
                    if (loaded != null) {
                        val memorizeIcon = if (LightThemeTokens.surfaceScheme == LightSurfaceScheme.Dark) {
                            R.drawable.ic_memorize_white
                        } else {
                            R.drawable.ic_memorize_black
                        }
                        add(
                            LightBarButton.Icon(
                                painter = painterResource(memorizeIcon),
                                onClick = {
                                    navigateTo(
                                        screenFactory = {
                                            MemorizeScreen(it, loaded.reference, loaded.text, loaded.translation)
                                        },
                                    )
                                },
                                sizeUnits = 2.5f,
                                contentDescription = "Memorize this verse",
                            ),
                        )
                    }
                }

                LightBottomBar(items = bottomBarItems)
            }
        }
    }
}
