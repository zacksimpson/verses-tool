package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

/** read-only view of this app's own usage against each copyrighted translation's api.
 *  public domain translations are never rate-limited, so they're left off.
 *
 *  shows this device's own enforced ceiling ([DAILY_LOOKUP_LIMIT]), not the provider's
 *  published limit. the two are easy to conflate, but our own backstop is much smaller
 *  and kicks in first, so showing the provider's number next to "Calls Today" would read
 *  as more headroom than actually exists. verses cached has no denominator for the same
 *  reason, there's no enforced cap there either, just one cache slot a day. */
class ApiLogsScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

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
                        center = LightTopBarCenter.Text("API Logs"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LightScrollView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 1.5f.gridUnitsAsDp()),
                        ) {
                            Column {
                                val translations = Translation.entries.filter { it.source !is TranslationSource.PublicDomain }
                                val currentPrefs = prefs
                                if (currentPrefs != null) {
                                    translations.forEach { translation ->
                                        StatRow(
                                            label = "${translation.abbreviation} Calls Today",
                                            value = formatAgainstLimit(
                                                LookupRateLimit.countToday(currentPrefs, translation),
                                                DAILY_LOOKUP_LIMIT,
                                            ),
                                        )
                                        StatRow(
                                            label = "${translation.abbreviation} Verses Cached",
                                            value = formatAgainstLimit(
                                                currentPrefs.cachedVerseCount(translation),
                                                limit = null,
                                            ),
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

/** "12 / 100" when a limit applies, otherwise just "12". no thousands separators,
 *  matching LightOS's own stat rows. */
internal fun formatAgainstLimit(value: Int, limit: Int?): String =
    if (limit != null) "$value / $limit" else "$value"

/** static label-over-value row, same shape as a settings row but without a click
 *  target since this screen is read-only. */
@Composable
private fun StatRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1f.gridUnitsAsDp()),
    ) {
        LightText(text = label, variant = LightTextVariant.Paragraph)
        LightText(text = value, variant = LightTextVariant.Heading)
    }
}
