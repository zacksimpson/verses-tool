package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.emptyPreferences
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class SettingsScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        // Read here (not in TranslationsSettingsScreen) so by the time someone actually taps
        // the "Translations" row, this has had a moment to resolve past its first-frame
        // initial value — passed down as constructor args, it lets that screen show the
        // real current translation immediately instead of flashing a guessed default.
        val prefs by lightContext.dataStore.data.collectAsState(initial = emptyPreferences())

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text("Settings"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LightScrollView(modifier = Modifier.fillMaxSize()) {
                            Column {
                                SettingsLinkRow(
                                    label = "Translations",
                                    onClick = {
                                        navigateTo(
                                            screenFactory = {
                                                TranslationsSettingsScreen(
                                                    it,
                                                    initialTranslationAbbreviation =
                                                        prefs.selectedTranslation().abbreviation,
                                                    initialFallbackAbbreviation =
                                                        prefs.lookupTranslation().abbreviation,
                                                )
                                            },
                                        )
                                    },
                                )
                                SettingsLinkRow(
                                    label = "View All Notes",
                                    onClick = { navigateTo(screenFactory = { AllNotesScreen(it) }) },
                                )
                                SettingsLinkRow(
                                    label = "Advanced",
                                    onClick = { navigateTo(screenFactory = { AdvancedSettingsScreen(it) }) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A settings row that's just a label leading to another screen — no current-value line.
 *  Shared with [AdvancedSettingsScreen], which is structured identically one level deeper. */
@Composable
internal fun SettingsLinkRow(label: String, onClick: () -> Unit) {
    LightText(
        text = label,
        variant = LightTextVariant.Heading,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
    )
}

/** A settings row with its current value shown under the label — same label-over-value
 *  shape as ApiLogsScreen's StatRow, but clickable through to a picker instead of read-only.
 *  Shared with [TranslationsSettingsScreen]. */
@Composable
internal fun SettingsValueRow(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
    ) {
        LightText(text = label, variant = LightTextVariant.Paragraph)
        LightText(text = value, variant = LightTextVariant.Heading)
    }
}
