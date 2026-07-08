package com.thelightphone.sdk.emulator

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.thelightphone.sdk.server.LightSdkServer.queryEnabledClients
import com.thelightphone.sdk.server.LightSdkServer.runningAsSystemApp
import com.thelightphone.sdk.server.LightSdkServerSettings
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController

class MainActivity : ComponentActivity() {

    enum class Nav {
        Toolbox, Settings
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (!runningAsSystemApp) {
            Log.w(
                "LightEmulator",
                "WARNING: LightOS emulator is NOT running as a system app and may not work."
            )
        }
        val serverSettings = LightSdkServerSettings(this)
        setContent {
            val themeColors by LightThemeController.colors.collectAsState()
            var currentNav by remember { mutableStateOf(Nav.Toolbox) }
            LightTheme(colors = themeColors){
                when(currentNav) {
                    Nav.Toolbox -> {
                        ToolList(
                            fetchExternalTools = {
                                queryEnabledClients().map {
                                    val appInfo = it.packageInfo.applicationInfo!!
                                    val label = packageManager.getApplicationLabel(appInfo).toString()
                                    ExternalTool(label, it.packageInfo.packageName)
                                }
                            }, launchPackage = {
                                packageManager.getLaunchIntentForPackage(it)?.let { intent ->
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                    val options =
                                        android.app.ActivityOptions.makeCustomAnimation(this, 0, 0)
                                    startActivity(intent, options.toBundle())
                                }
                            }, launchDefaultTool = {
                                when(it) {
                                    DefaultTool.Settings -> currentNav = Nav.Settings
                                }
                            })
                    }
                    Nav.Settings -> {
                        EmulatorSettings(serverSettings) {
                            currentNav = Nav.Toolbox
                        }
                    }
                }

            }
        }
    }
}

private sealed class Tool(val label: String)
private class ExternalTool(label: String, val packageName: String) : Tool(label)
private sealed class DefaultTool(label: String) : Tool(label) {
    object Settings : DefaultTool("Settings")
}


private val defaultTools: List<Tool> = listOf(
    DefaultTool.Settings
)

@Composable
private fun ToolList(
    fetchExternalTools: suspend () -> List<Tool>,
    launchPackage: (String) -> Unit,
    launchDefaultTool: (DefaultTool) -> Unit
) {
    // TODO page indicator
    val toolPageSize = 6
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var pages by remember { mutableStateOf(defaultTools.chunked(toolPageSize)) }
    val currentPage by remember {
        derivedStateOf {
            pages.getOrNull(currentPageIndex) ?: pages.first()
        }
    }
    LaunchedEffect(Unit) {
        val externalTools = fetchExternalTools()
        pages = (defaultTools + externalTools).chunked(toolPageSize)
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState {},
                onDragStopped = { velocity ->
                    if (velocity < -200f && currentPageIndex < pages.size - 1) {
                        currentPageIndex++
                    } else if (velocity > 200f && currentPageIndex > 0) {
                        currentPageIndex--
                    }
                }
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (tool in currentPage) {
                LightText(
                    text = tool.label,
                    variant = LightTextVariant.Subtitle,
                    modifier = Modifier.clickable {
                        when(tool) {
                            is DefaultTool -> launchDefaultTool(tool)
                            is ExternalTool -> launchPackage(tool.packageName)
                        }
                    }
                )
            }
        }
    }
}
