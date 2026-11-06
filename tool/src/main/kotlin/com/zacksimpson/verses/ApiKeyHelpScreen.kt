package com.zacksimpson.verses

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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ApiKeyService(
    val title: String,
    val preferenceKey: Preferences.Key<String>,
) {
    Esv(title = "ESV API Key", preferenceKey = VersePreferences.ESV_API_KEY),
    YouVersion(title = "YouVersion App Key", preferenceKey = VersePreferences.YOUVERSION_APP_KEY),
}

// both services route to the same guide, which lets you pick your translation there,
// so there's only one QR code/steps/link to keep in sync instead of one per service
private const val GUIDE_STEPS = "For a 5-minute step-by-step guide to get your free key, visit:"
private const val GUIDE_LINK = "zacksimpson.github.io/verses-tool/guides"

/** how to sign up for a key, with a qr code to the full setup guide. stays reachable
 *  even after a key is set, so it also offers replacing or removing it.
 *
 *  [initialKeySet] comes from the caller's own already-settled read of the same
 *  preference, shown until this screen's own collectAsState catches up so there's no
 *  flash to a guessed default on the first frame. */
class ApiKeyHelpScreen(
    sealedActivity: SealedLightActivity,
    private val service: ApiKeyService,
    private val initialKeySet: Boolean,
) : SimpleLightScreen<Unit>(sealedActivity) {

    // not rememberCoroutineScope, that one is cancelled while the editor covers this screen
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onScreenDestroy() {
        scope.cancel()
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val prefs by lightContext.dataStore.data.collectAsState(initial = null)
        val keySet = prefs?.let(service::isSetIn) ?: initialKeySet

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text(if (keySet) service.title else "Free API Key Needed"),
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
                                    painter = painterResource(R.drawable.qr_guides),
                                    contentDescription = "${service.title} sign up QR code",
                                    modifier = Modifier
                                        .padding(top = 0.5f.gridUnitsAsDp())
                                        .size(12f.gridUnitsAsDp()),
                                )
                                LightText(
                                    text = GUIDE_STEPS,
                                    variant = LightTextVariant.Fine,
                                    align = TextAlign.Center,
                                    modifier = Modifier.padding(top = 1.5f.gridUnitsAsDp()),
                                )
                                LightText(
                                    text = GUIDE_LINK,
                                    variant = LightTextVariant.Fine,
                                    underline = true,
                                    align = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 0.75f.gridUnitsAsDp()),
                                )
                            }
                        }
                    }

                    val editButton = LightBarButton.Text(
                        text = if (keySet) "EDIT" else "ENTER KEY",
                        onClick = {
                            openKeyEditor(
                                service,
                                lightContext.dataStore,
                                scope,
                                onSaved = { goBack(Unit) },
                            )
                        },
                    )
                    LightBottomBar(
                        items = if (keySet) {
                            listOf(
                                editButton,
                                LightBarButton.Text(
                                    text = "REMOVE",
                                    onClick = {
                                        // a swipe-back right after tapping this tears down
                                        // scope, so without NonCancellable the write could
                                        // get cancelled mid-flight
                                        scope.launch {
                                            withContext(NonCancellable) {
                                                saveApiKey(lightContext.dataStore, service.preferenceKey, "")
                                            }
                                        }
                                    },
                                ),
                            )
                        } else {
                            listOf(editButton)
                        },
                    )
                }
            }
        }
    }
}
