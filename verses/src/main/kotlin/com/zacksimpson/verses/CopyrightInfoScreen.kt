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
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

/**
 * Renders straight from Translation.entries so a translation's copyright/trademark
 * notice can't be forgotten when a new one is added — see Translation.kt's
 * copyrightNotice/trademarkNotice fields, which the constructor requires for every case.
 */
class CopyrightInfoScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

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
                        center = LightTopBarCenter.Text("Copyright Info"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    LightScrollView(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp())) {
                            Translation.entries.forEach { translation ->
                                NoticeHeader(translation.displayName)
                                NoticeParagraph(translation.copyrightNotice)
                                NoticeParagraph(translation.trademarkNotice)
                            }

                            // Deduplicated by content — NIV and NASB share the same
                            // YouVersion note, so it's shown once, not once per translation.
                            val usageNotes = Translation.entries.map { it.source.usageNote }.distinct()
                            usageNotes.forEachIndexed { index, note ->
                                NoticeParagraph(note, bottomPadding = if (index == usageNotes.lastIndex) 0f else 1f)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeHeader(text: String) {
    LightText(
        text = text,
        variant = LightTextVariant.Subheading,
        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
    )
}

@Composable
private fun NoticeParagraph(text: String, bottomPadding: Float = 1f) {
    LightText(
        text = text,
        variant = LightTextVariant.Paragraph,
        modifier = Modifier.padding(bottom = bottomPadding.gridUnitsAsDp()),
    )
}
