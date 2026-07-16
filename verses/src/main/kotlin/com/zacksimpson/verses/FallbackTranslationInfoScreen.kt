package com.zacksimpson.verses

import androidx.compose.foundation.background
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
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

/** Explains why verse/passage lookups default to KJV and how the per-device daily lookup
 *  limit on copyrighted translations works — reached from Settings. Modal-style (UNDERSTOOD
 *  in the bottom bar to dismiss, no back chevron) matching MemorizeScreen's DONE screen, since
 *  this is a one-off explainer rather than a screen with ongoing navigation. */
class FallbackTranslationInfoScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            SwipeBackContainer(enabled = false, onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        center = LightTopBarCenter.Text("About Fallback Translations"),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1.5f.gridUnitsAsDp()),
                    ) {
                        InfoParagraph(
                            "Most translations carry copyright or API restrictions. KJV and BSB are " +
                                "exceptions to this, as public domain translations. By default, lookups " +
                                "use the KJV to avoid eating into the shared API budget for copyrighted " +
                                "translations.",
                        )
                        InfoParagraph(
                            "You can change this behavior anytime in Settings → Fallback Translation.",
                            bottomPadding = 0f,
                        )
                    }

                    LightBottomBar(
                        items = listOf(
                            LightBarButton.Text(text = "UNDERSTOOD", onClick = { goBack(Unit) }),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoParagraph(text: String, bottomPadding: Float = 1f) {
    LightText(
        text = text,
        variant = LightTextVariant.Paragraph,
        modifier = Modifier.padding(bottom = bottomPadding.gridUnitsAsDp()),
    )
}
