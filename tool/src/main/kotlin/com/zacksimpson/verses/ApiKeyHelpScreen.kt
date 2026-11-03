package com.zacksimpson.verses

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.launch

enum class ApiKeyService(
    val title: String,
    val preferenceKey: Preferences.Key<String>,
    @DrawableRes val qrCode: Int,
    val steps: String,
    val link: String,
) {
    Esv(
        title = "ESV API Key",
        preferenceKey = VersePreferences.ESV_API_KEY,
        qrCode = R.drawable.qr_esv_signup,
        steps = "1. Scan the QR code to open api.esv.org and log in or sign up.\n\n" +
            "2. Create an application to get a key.\n\n" +
            "3. Copy the key it gives you into API Keys.",
        link = "api.esv.org",
    ),
    YouVersion(
        title = "YouVersion App Key",
        preferenceKey = VersePreferences.YOUVERSION_APP_KEY,
        qrCode = R.drawable.qr_youversion_signup,
        steps = "1. Scan the QR code to sign up for a free account.\n\n" +
            "2. Register an app to get an App Key.\n\n" +
            "3. Copy the key into API Keys.",
        link = "platform.youversion.com/platform/get-started",
    ),
}

/** how to sign up for a key, a qr code straight to the right signup page like the
 *  weather example's attribution screen. */
class ApiKeyHelpScreen(
    sealedActivity: SealedLightActivity,
    private val service: ApiKeyService,
) : SimpleLightScreen<Unit>(sealedActivity) {

    // not rememberCoroutineScope, that one is cancelled while the editor covers this screen
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onScreenDestroy() {
        scope.cancel()
    }

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
                        center = LightTopBarCenter.Text(service.title),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LightScrollView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 1.5f.gridUnitsAsDp()),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Image(
                                    painter = painterResource(service.qrCode),
                                    contentDescription = "${service.title} sign up QR code",
                                    modifier = Modifier
                                        .padding(top = 1f.gridUnitsAsDp())
                                        .size(12f.gridUnitsAsDp()),
                                )
                                LightText(
                                    text = service.steps,
                                    variant = LightTextVariant.Fine,
                                    align = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2f.gridUnitsAsDp()),
                                )
                                LightText(
                                    text = service.link,
                                    variant = LightTextVariant.Fine,
                                    underline = true,
                                    align = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 1f.gridUnitsAsDp()),
                                )
                            }
                        }
                    }

                    LightBottomBar(
                        items = listOf(
                            LightBarButton.Text(
                                text = "ENTER KEY",
                                onClick = {
                                    openKeyEditor(
                                        service,
                                        lightContext.dataStore,
                                        scope,
                                        onSaved = { goBack(Unit) },
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
