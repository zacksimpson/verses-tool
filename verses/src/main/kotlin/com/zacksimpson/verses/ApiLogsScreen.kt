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

/** Read-only view of the app's own tracked usage against each copyrighted translation's API,
 *  for gauging headroom before making future UX calls on those translations (see
 *  LookupRateLimiter). Public domain translations (KJV, BSB) are never rate-limited or
 *  quota-bound, so they're left off entirely.
 *
 *  Deliberately shows this device's own enforced ceiling ([DAILY_LOOKUP_LIMIT]), not the
 *  provider's published API-key limit (ESV's real 5,000/day, for instance) — the two are easy
 *  to conflate, but this app's own, much smaller backstop always kicks in long before a
 *  device could ever approach the provider's real number. Showing the provider's figure next
 *  to "Calls Today" would read as personal headroom ("I have up to 5,000 left"), when the
 *  actual ceiling this device enforces is 100. Verses Cached has no denominator at all for the
 *  same reason: the app has no enforced cap there either, just a single once-a-day cache slot
 *  — a provider figure would be equally misleading. */
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
                            modifier = Modifier.fillMaxSize(),
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

/** "12 / 100" when a limit applies, otherwise just "12" — used for both rows on this screen.
 *  No thousands separators, matching LightOS's own stat rows (e.g. Device Storage's
 *  "65051 / 93691 MB"). */
internal fun formatAgainstLimit(value: Int, limit: Int?): String =
    if (limit != null) "$value / $limit" else "$value"

/** Static label-over-value display row — same shape as every Light SDK tool's settings rows
 *  (label: Paragraph, value: Heading, same horizontal/vertical padding), just without a click
 *  target since this screen is read-only. */
@Composable
private fun StatRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
    ) {
        LightText(text = label, variant = LightTextVariant.Paragraph)
        LightText(text = value, variant = LightTextVariant.Heading)
    }
}
