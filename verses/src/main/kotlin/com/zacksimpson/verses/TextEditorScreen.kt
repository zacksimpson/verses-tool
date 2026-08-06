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
 * reusable multi-line text entry backed by the LightOS keyboard. returns the entered
 * text as the result on submit, nothing if the user backs out. ported from
 * reminders-tool.
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
                    // a fresh instance per push keys each editor uniquely, a fixed key would collide
                    editorKey = this@TextEditorScreen,
                    // smaller than the sdk default so more of a long note stays visible above the keyboard
                    inputTextVariant = LightTextVariant.Paragraph,
                    // only capitalize a fresh note, not existing text where the cursor lands mid-sentence
                    initialCaps = request.initialValue.isBlank(),
                )
            }
        }
    }
}
