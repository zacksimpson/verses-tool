package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

/** one level under settings, the daily-verse translation and the lookup fallback
 *  translation, each showing its current selection under the label, plus the explainer
 *  for why lookups default to a public domain translation.
 *
 *  [initialTranslationAbbreviation]/[initialFallbackAbbreviation] come from
 *  SettingsScreen's own already-settled read of the same preferences, shown until this
 *  screen's own collectAsState catches up so there's no flash to a guessed default on
 *  the first frame. */
class TranslationsSettingsScreen(
    sealedActivity: SealedLightActivity,
    private val initialTranslationAbbreviation: String,
    private val initialFallbackAbbreviation: String,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val prefs by lightContext.dataStore.data.collectAsState(initial = null)

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text("Translations"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Column(modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp())) {
                        SettingsValueRow(
                            label = "Translation",
                            value = prefs?.selectedTranslation()?.abbreviation ?: initialTranslationAbbreviation,
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        TranslationPickerScreen(
                                            it,
                                            title = "Translation",
                                            preferenceKey = VersePreferences.SELECTED_TRANSLATION,
                                            currentSelection = { p -> p.selectedTranslation() },
                                        )
                                    },
                                )
                            },
                        )
                        SettingsValueRow(
                            label = "Fallback Translation",
                            value = prefs?.lookupTranslation()?.abbreviation ?: initialFallbackAbbreviation,
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        TranslationPickerScreen(
                                            it,
                                            title = "Fallback Translation",
                                            preferenceKey = VersePreferences.LOOKUP_TRANSLATION,
                                            currentSelection = { p -> p.lookupTranslation() },
                                        )
                                    },
                                )
                            },
                        )
                        SettingsLinkRow(
                            label = "Fallback Translation Info",
                            onClick = { navigateTo(screenFactory = { FallbackTranslationInfoScreen(it) }) },
                        )
                    }
                }
            }
        }
    }
}
