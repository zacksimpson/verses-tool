package com.zacksimpson.verses

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.gridUnitsAsDp
import kotlin.math.abs

private val EDGE_WIDTH = 30.dp
private val DRAG_THRESHOLD = 80.dp

// matches LightTopBar's own height. every screen using this puts a top bar with a back
// button, so the edge strip below starts right where that bar ends
private const val TOP_BAR_HEIGHT_UNITS = 3f

/**
 * left-edge swipe-to-go-back. LightOS has no os-level back-swipe gesture, so this
 * reimplements it: edge-only start, horizontal-dominant drag, single trigger past a
 * threshold. ported from reminders-tool.
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
            // starts below the top bar instead of y=0. a full-height strip used to
            // overlap the back button and steal its taps, since this box paints on top
            // to win priority over content's own scroll/drag handlers. excluding that
            // band keeps the swipe working everywhere else
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = TOP_BAR_HEIGHT_UNITS.gridUnitsAsDp())
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
