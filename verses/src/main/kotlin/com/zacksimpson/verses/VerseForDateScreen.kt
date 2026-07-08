package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollBarPosition
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import java.time.LocalDate

/** Shows the verse for a specific past date, looked up live (no caching — this is a
 *  low-frequency lookup, unlike the home screen's daily cache). */
class VerseForDateScreen(
    sealedActivity: SealedLightActivity,
    private val dateStr: String,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var state by remember { mutableStateOf<VerseUiState>(VerseUiState.Loading) }

        LaunchedEffect(dateStr) {
            if (BuildConfig.ESV_API_KEY.isBlank()) {
                state = VerseUiState.ConfigError(
                    "Add your ESV API key to local.properties (esvApiKey=...) to use this tool.",
                )
                return@LaunchedEffect
            }
            val reference = VerseSelector.referenceForDate(LocalDate.parse(dateStr))
            val api = EsvApi(apiKey = BuildConfig.ESV_API_KEY)
            val result = api.fetchVerseText(reference)
            api.close()
            state = result.fold(
                onSuccess = { text -> VerseUiState.Loaded(reference = reference, text = text) },
                onFailure = { VerseUiState.ConfigError("Couldn't load that day's verse. Check your connection.") },
            )
        }

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text(formatDisplayDate(dateStr)),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (val mode = state) {
                            is VerseUiState.Loading -> {
                                LightText(
                                    text = "Loading…",
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier.padding(horizontal = 1f.gridUnitsAsDp()),
                                )
                            }

                            is VerseUiState.ConfigError -> {
                                LightText(
                                    text = mode.message,
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier.padding(horizontal = 1f.gridUnitsAsDp()),
                                )
                            }

                            is VerseUiState.Loaded -> {
                                LightScrollView(
                                    modifier = Modifier.fillMaxSize(),
                                    scrollBarPosition = LightScrollBarPosition.Inside,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(
                                            horizontal = 1f.gridUnitsAsDp(),
                                            vertical = 1.5f.gridUnitsAsDp(),
                                        ),
                                    ) {
                                        LightText(
                                            text = mode.text,
                                            variant = LightTextVariant.Heading,
                                            modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                                        )
                                        LightText(
                                            text = "${mode.reference} (ESV)",
                                            variant = LightTextVariant.Copy,
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
