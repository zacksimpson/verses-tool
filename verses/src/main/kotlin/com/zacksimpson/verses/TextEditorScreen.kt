package com.zacksimpson.verses

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController

data class TextEditorRequest(val title: String, val initialValue: String = "")

/**
 * Reusable multi-line text entry backed by the LightOS keyboard (LightTextInputEditor
 * defaults to multi-line). Returns the entered text as the screen result on submit, or
 * nothing if the user backs out. Ported from reminders-tool's TextEditorScreen.
 */
class TextEditorScreen(
    sealedActivity: SealedLightActivity,
    private val request: TextEditorRequest,
) : SimpleLightScreen<String>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val textState = rememberTextFieldState(request.initialValue)
        val keyboardOptions = rememberKeyboardOptions()

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(null) }) {
                LightTextInputEditor(
                    title = request.title,
                    state = textState,
                    keyboardOptionsFlow = keyboardOptions,
                    onSubmit = { goBack(it.toString()) },
                    onBack = { goBack(null) },
                    // A fresh instance per push, so it keys each editor uniquely — see
                    // reminders-tool's TextEditorScreen for why a fixed key would collide.
                    editorKey = this@TextEditorScreen,
                    // Notes read more naturally at body size than the SDK's default
                    // Heading-sized input text.
                    inputTextVariant = LightTextVariant.Copy,
                )
            }
        }
    }
}
