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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** where the user enters their own esv and youversion keys. a saved key is never shown
 *  back, only whether one is set. submitting an empty value clears it. */
class ApiKeysScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    // not rememberCoroutineScope, that one is cancelled while the editor covers this screen
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onScreenDestroy() {
        scope.cancel()
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val prefs by lightContext.dataStore.data.collectAsState(initial = null)

        // an unset key goes to the signup instructions, a saved one straight to the editor
        // so it can be replaced or cleared
        fun openKey(service: ApiKeyService) {
            if (prefs?.get(service.preferenceKey).isNullOrBlank()) {
                navigateTo(screenFactory = { ApiKeyHelpScreen(it, service) })
            } else {
                openKeyEditor(service, lightContext.dataStore, scope)
            }
        }

        fun status(key: Preferences.Key<String>) =
            if (prefs?.get(key).isNullOrBlank()) "Not set" else "Set"

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text("API Keys"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LightScrollView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 1.5f.gridUnitsAsDp()),
                        ) {
                            Column {
                                SettingsValueRow(
                                    label = "ESV API Key",
                                    value = status(VersePreferences.ESV_API_KEY),
                                    onClick = { openKey(ApiKeyService.Esv) },
                                )
                                SettingsValueRow(
                                    label = "YouVersion App Key",
                                    value = status(VersePreferences.YOUVERSION_APP_KEY),
                                    onClick = { openKey(ApiKeyService.YouVersion) },
                                )
                                LightText(
                                    text = "ESV needs its own key. NIV and NASB need a YouVersion key. KJV and " +
                                        "BSB work without one. Keys stay on this device.",
                                    variant = LightTextVariant.Fine,
                                    modifier = Modifier.padding(vertical = 1f.gridUnitsAsDp()),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** true when the service answered and refused the key. a network failure isn't a
 *  rejection, so an offline user can still save a key. */
internal fun isKeyRejection(failure: Throwable): Boolean =
    Regex("""HTTP (401|403)\b""").containsMatchIn(failure.message.orEmpty())

/** looks up one known verse with [key] to see if the service accepts it. */
internal suspend fun apiKeyIsRejected(service: ApiKeyService, key: String): Boolean {
    val result = when (service) {
        ApiKeyService.Esv -> EsvApi().let { api ->
            try { api.fetchVerseText(key, "John 3:16") } finally { api.close() }
        }
        ApiKeyService.YouVersion -> YouVersionApi().let { api ->
            try { api.fetchVerseText(key, 111, "John 3:16") } finally { api.close() }
        }
    }
    return result.exceptionOrNull()?.let(::isKeyRejection) ?: false
}

/** opens the key editor. a key the service rejects is not saved, the editor reopens with
 *  the same text so a typo can be fixed. empty clears the key. [onSaved] runs after. */
internal fun SimpleLightScreen<*>.openKeyEditor(
    service: ApiKeyService,
    dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
    initial: String = "",
    title: String = service.title,
    onSaved: () -> Unit = {},
) {
    navigateTo(
        screenFactory = { TextEditorScreen(it, TextEditorRequest(title, initial, singleLine = true)) },
        resultCallback = { entered ->
            scope.launch {
                val trimmed = entered.trim()
                if (trimmed.isNotEmpty() && apiKeyIsRejected(service, trimmed)) {
                    openKeyEditor(service, dataStore, scope, trimmed, "Key Not Accepted", onSaved)
                } else {
                    saveApiKey(dataStore, service.preferenceKey, entered)
                    onSaved()
                }
            }
        },
    )
}

/** saves [entered] under [key], an empty value clears it. */
internal suspend fun saveApiKey(
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<String>,
    entered: String,
) {
    dataStore.edit { p ->
        val trimmed = entered.trim()
        if (trimmed.isEmpty()) p.remove(key) else p[key] = trimmed
    }
}
