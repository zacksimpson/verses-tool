package com.zacksimpson.verses

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val EDGE_WIDTH = 30.dp
private val DRAG_THRESHOLD = 80.dp

/**
 * Left-edge swipe-to-go-back gesture. LightOS doesn't provide an OS-level gesture-nav
 * back-swipe, so this reimplements it: edge-only start, horizontal-dominant drag, single
 * trigger past a threshold. Ported from reminders-tool's SwipeBackContainer.
 */
@Composable
fun SwipeBackContainer(
    enabled: Boolean = true,
    onSwipeBack: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val dragThresholdPx = remember(density) { with(density) { DRAG_THRESHOLD.toPx() } }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(EDGE_WIDTH)
                    .pointerInput(onSwipeBack) {
                        var totalX = 0f
                        var totalY = 0f
                        var triggered = false
                        detectHorizontalDragGestures(
                            onDragStart = {
                                totalX = 0f
                                totalY = 0f
                                triggered = false
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                totalX += dragAmount
                                totalY += change.positionChange().y
                                if (!triggered && totalX > dragThresholdPx && abs(totalY) < abs(totalX) * 1.5f) {
                                    triggered = true
                                    onSwipeBack()
                                }
                            },
                        )
                    },
            )
        }
    }
}
