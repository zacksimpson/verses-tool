package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

/**
 * First screen of the verse lookup flow, reached from the SEARCH icon on the home
 * screen: choose a testament, then a book ([BibleBookListScreen]), a chapter
 * ([ChapterPickerScreen]), and finally a verse or verse range ([VersePickerScreen]),
 * which hands the resolved passage off to [VerseActionsScreen].
 */
class VerseLookupScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text("Verse Lookup"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Testament.entries.forEach { testament ->
                        LookupRow(
                            text = testament.displayName,
                            onClick = { navigateTo(screenFactory = { BibleBookListScreen(it, testament) }) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LookupRow(text: String, onClick: () -> Unit) {
    LightText(
        text = text,
        variant = LightTextVariant.Heading,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 0.75f.gridUnitsAsDp()),
    )
}
