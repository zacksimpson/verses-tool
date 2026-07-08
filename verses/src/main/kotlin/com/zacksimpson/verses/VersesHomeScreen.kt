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
import androidx.compose.ui.res.painterResource
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollBarPosition
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightSurfaceScheme
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

@InitialScreen
class VersesHomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, VersesViewModel>(sealedActivity) {

    override val viewModelClass: Class<VersesViewModel>
        get() = VersesViewModel::class.java

    override fun createViewModel(): VersesViewModel =
        VersesViewModel(lightContext.dataStore)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                val recentsIcon = if (LightThemeTokens.surfaceScheme == LightSurfaceScheme.Dark) {
                    R.drawable.ic_recents_white
                } else {
                    R.drawable.ic_recents_black
                }

                LightTopBar(
                    leftButton = LightBarButton.Icon(
                        painter = painterResource(recentsIcon),
                        onClick = { navigateTo(screenFactory = { VerseDatePickerScreen(it) }) },
                        // ic_recents' artwork fills its box edge-to-edge (unlike LightIcons'
                        // own icons, which have built-in padding), but the previous BACK
                        // chevron still rendered in a full 2-grid-unit box by default — this
                        // stays close to that rather than reminders-tool's 1.2f (which was
                        // tuned for a much thinner plus-sign icon, not a comparable shape).
                        sizeUnits = 1.5f,
                        contentDescription = "View past verses",
                    ),
                    center = LightTopBarCenter.Text("Verses"),
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
                                    if (mode.staleWarning) {
                                        LightText(
                                            text = "Couldn't refresh — showing last saved verse.",
                                            variant = LightTextVariant.Detail,
                                            modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LightBottomBar(
                    items = listOf(
                        LightBarButton.LightIcon(
                            icon = LightIcons.SETTINGS,
                            onClick = { navigateTo(screenFactory = { SettingsScreen(it) }) },
                            contentDescription = "Settings",
                        ),
                    ),
                )
            }
        }
    }
}
