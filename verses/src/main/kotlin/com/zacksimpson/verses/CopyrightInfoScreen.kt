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

private const val ESV_COPYRIGHT_NOTICE = "Scripture quotations are from the ESV® Bible " +
    "(The Holy Bible, English Standard Version®), copyright © 2001 by Crossway, a publishing " +
    "ministry of Good News Publishers. Used by permission. All rights reserved."

private const val ESV_TRADEMARK_NOTICE = "\"ESV\" and \"English Standard Version\" are " +
    "registered trademarks of Crossway."

private const val ESV_USAGE_NOTE = "Verse text is fetched from the official ESV API " +
    "(api.esv.org) once per day and cached on-device so the tool works offline; only the " +
    "current day's verse is kept, and it's replaced the next time a new verse is fetched, " +
    "under Crossway's terms for free, non-commercial use."

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
                        scrollBarPosition = LightScrollBarPosition.Inside,
                    ) {
                        LightText(
                            text = ESV_COPYRIGHT_NOTICE,
                            variant = LightTextVariant.Paragraph,
                            modifier = Modifier
                                .padding(horizontal = 1f.gridUnitsAsDp())
                                .padding(bottom = 1f.gridUnitsAsDp()),
                        )
                        LightText(
                            text = ESV_TRADEMARK_NOTICE,
                            variant = LightTextVariant.Paragraph,
                            modifier = Modifier
                                .padding(horizontal = 1f.gridUnitsAsDp())
                                .padding(bottom = 1f.gridUnitsAsDp()),
                        )
                        LightText(
                            text = ESV_USAGE_NOTE,
                            variant = LightTextVariant.Paragraph,
                            modifier = Modifier.padding(horizontal = 1f.gridUnitsAsDp()),
                        )
                    }
                }
            }
        }
    }
}
