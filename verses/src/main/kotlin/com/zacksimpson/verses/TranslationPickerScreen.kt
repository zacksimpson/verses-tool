package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import kotlinx.coroutines.launch

class TranslationPickerScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val scope = rememberCoroutineScope()
        val prefs by lightContext.dataStore.data.collectAsState(initial = null)
        val selected = prefs?.selectedTranslation() ?: Translation.DEFAULT
        // Guards against a fast double-tap (or tapping two rows before the first
        // selection's coroutine finishes) launching two goBack() calls — the second one
        // would pop whatever screen is now on top, not just be a no-op.
        var isSelecting by remember { mutableStateOf(false) }

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text("Translation"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    // Public domain translations (KJV, BSB) are fallbacks for the
                    // (not-yet-built) verse lookup feature, not a choice for the daily
                    // verse — excluded here, not from Translation itself, so
                    // CopyrightInfoScreen still lists them once reachable.
                    Translation.entries.filter { it.source !is TranslationSource.PublicDomain }.forEach { translation ->
                        TranslationRow(
                            translation = translation,
                            isSelected = translation == selected,
                            onClick = {
                                if (!isSelecting) {
                                    isSelecting = true
                                    scope.launch {
                                        lightContext.dataStore.edit { p ->
                                            p[VersePreferences.SELECTED_TRANSLATION] = translation.name
                                        }
                                        goBack(Unit)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationRow(translation: Translation, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
    ) {
        LightText(
            text = translation.abbreviation,
            variant = LightTextVariant.Heading,
        )
        // Drawn manually rather than via LightText's underline flag, which renders a
        // hairline too thin to read as a selection indicator — mirrors the thicker bar
        // reminders-native draws under a selected date in DatePickerScreen.kt.
        // IntrinsicSize.Max sizes this column to the text's natural (unwrapped) width,
        // so the bar below tracks the text instead of stretching across the row.
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            LightText(
                text = translation.displayName,
                variant = LightTextVariant.Paragraph,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(if (isSelected) LightThemeTokens.colors.content else Color.Transparent),
            )
        }
    }
}
