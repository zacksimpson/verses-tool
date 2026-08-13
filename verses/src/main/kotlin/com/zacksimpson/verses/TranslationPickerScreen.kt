package com.zacksimpson.verses

import androidx.compose.foundation.background
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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.launch

/** shared by Settings' "Translation" and "Fallback Translation" rows, same selection UI,
 *  differing only in which preference they read/write and the top bar title. both offer
 *  every translation by default, but a caller can pass a narrower list. */
class TranslationPickerScreen(
    sealedActivity: SealedLightActivity,
    private val title: String,
    private val preferenceKey: Preferences.Key<String>,
    private val currentSelection: (Preferences) -> Translation,
    private val options: List<Translation> = Translation.entries,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val scope = rememberCoroutineScope()
        val prefs by lightContext.dataStore.data.collectAsState(initial = null)
        // null (nothing highlighted) instead of guessing a default while prefs is still
        // loading, the right fallback differs per caller and isn't knowable generically here
        val selected = prefs?.let(currentSelection)
        // guards against a fast double-tap firing two goBack() calls, the second would
        // pop whatever screen is now on top
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
                        center = LightTopBarCenter.Text(title),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LightScrollView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 1.5f.gridUnitsAsDp()),
                        ) {
                            Column {
                                options.forEach { translation ->
                                    TranslationRow(
                                        translation = translation,
                                        isSelected = translation == selected,
                                        onClick = {
                                            if (!isSelecting) {
                                                isSelecting = true
                                                scope.launch {
                                                    lightContext.dataStore.edit { p ->
                                                        p[preferenceKey] = translation.name
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
        }
    }
}

@Composable
private fun TranslationRow(translation: Translation, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 1f.gridUnitsAsDp()),
    ) {
        LightText(
            text = translation.abbreviation,
            variant = LightTextVariant.Heading,
        )
        // drawn manually since LightText's underline flag renders too thin to read as a
        // selection indicator. IntrinsicSize.Max keeps the bar tracking the text's width
        // instead of stretching across the row
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
