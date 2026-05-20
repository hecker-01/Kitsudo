package dev.heckr.kitsudo.presentation.tasks.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SwipeDirection { Left, Right }

/**
 * A swipe-gesture container that fires actions **only when the user lifts their
 * finger** past [thresholdFraction] of the row width.
 *
 * While dragging:
 *  - The foreground [content] follows the finger.
 *  - [backgroundContent] is revealed behind it, receiving the current direction.
 *  - A [HapticFeedbackConstants.CLOCK_TICK] fires the first time the threshold
 *    is crossed in a given drag.
 *
 * On release:
 *  - Past threshold → the appropriate haptic fires, then [onSwipeLeft] or
 *    [onSwipeRight] is called. The row always snaps back.
 *  - Before threshold → row snaps back, nothing is called.
 */
@Composable
fun SwipeActionBox(
    modifier: Modifier = Modifier,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    swipeLeftHaptic: Int = HapticFeedbackConstants.REJECT,
    swipeRightHaptic: Int = HapticFeedbackConstants.CONFIRM,
    thresholdFraction: Float = 0.35f,
    backgroundContent: @Composable (direction: SwipeDirection?) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val offsetX = remember { Animatable(0f) }
    var widthPx by remember { mutableIntStateOf(1) }
    var crossedThreshold by remember { mutableStateOf(false) }

    // Recompose background only when direction changes — not every drag frame.
    val swipeDirection by remember {
        derivedStateOf {
            when {
                offsetX.value < -10f -> SwipeDirection.Left
                offsetX.value > 10f -> SwipeDirection.Right
                else -> null
            }
        }
    }

    Box(
        modifier = modifier.onSizeChanged { widthPx = it.width },
    ) {
        // Background — always below the foreground
        Box(modifier = Modifier.fillMaxSize()) {
            backgroundContent(swipeDirection)
        }

        // Foreground — slides with the finger
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(onSwipeLeft, onSwipeRight) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val threshold = widthPx * thresholdFraction
                            val cur = offsetX.value
                            if (abs(cur) >= threshold) {
                                when {
                                    cur < 0 && onSwipeLeft != null -> {
                                        view.performHapticFeedback(swipeLeftHaptic)
                                        onSwipeLeft()
                                    }
                                    cur > 0 && onSwipeRight != null -> {
                                        view.performHapticFeedback(swipeRightHaptic)
                                        onSwipeRight()
                                    }
                                }
                            }
                            // Always snap back, regardless of whether action fired.
                            scope.launch {
                                offsetX.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.75f, stiffness = 500f),
                                )
                            }
                            crossedThreshold = false
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f) }
                            crossedThreshold = false
                        },
                        onHorizontalDrag = { _, delta ->
                            val max = widthPx.toFloat()
                            val new = (offsetX.value + delta).coerceIn(
                                if (onSwipeLeft != null) -max else 0f,
                                if (onSwipeRight != null) max else 0f,
                            )
                            scope.launch { offsetX.snapTo(new) }

                            // Haptic tick the first time threshold is crossed per drag.
                            val nowCrossed = abs(new) >= widthPx * thresholdFraction
                            if (nowCrossed && !crossedThreshold) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            crossedThreshold = nowCrossed
                        },
                    )
                },
        ) {
            content()
        }
    }
}
