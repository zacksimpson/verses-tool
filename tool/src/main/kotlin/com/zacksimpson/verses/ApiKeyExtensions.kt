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
import androidx.datastore.preferences.core.Preferences
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** the key a translation needs, null for the keyless public domain ones. */
internal fun Translation.apiKeyService(): ApiKeyService? = when (source) {
    is TranslationSource.Esv -> ApiKeyService.Esv
    is TranslationSource.YouVersion -> ApiKeyService.YouVersion
    is TranslationSource.PublicDomain -> null
}

internal fun ApiKeyService.isSetIn(prefs: Preferences): Boolean =
    !prefs[preferenceKey].isNullOrBlank()

/** shown when a translation is picked without its key saved. add key walks through the
 *  signup screen, and this one closes itself once a key is saved so the picker can
 *  finish the selection. */
class ApiKeyRequiredScreen(
    sealedActivity: SealedLightActivity,
    private val translation: Translation,
) : SimpleLightScreen<Unit>(sealedActivity) {

    private val service = requireNotNull(translation.apiKeyService())

    // not rememberCoroutineScope, that one is cancelled while the signup screens cover this one
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onScreenDestroy() {
        scope.cancel()
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val keyName = when (service) {
            ApiKeyService.Esv -> "an ESV API key"
            ApiKeyService.YouVersion -> "a YouVersion app key"
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
                        center = LightTopBarCenter.Text("API Key Needed"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LightScrollView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 1.5f.gridUnitsAsDp()),
                        ) {
                            LightText(
                                text = "${translation.abbreviation} needs $keyName. It's free to get, " +
                                    "and the key stays on this device.",
                                variant = LightTextVariant.Paragraph,
                            )
                        }
                    }

                    LightBottomBar(
                        items = listOf(
                            LightBarButton.Text(
                                text = "ADD KEY",
                                onClick = {
                                    navigateTo(
                                        screenFactory = { ApiKeyHelpScreen(it, service) },
                                        resultCallback = {
                                            scope.launch {
                                                if (service.isSetIn(lightContext.dataStore.data.first())) goBack(Unit)
                                            }
                                        },
                                    )
                                },
                            ),
                        ),
                    )
                }
            }
        }
    }
}
