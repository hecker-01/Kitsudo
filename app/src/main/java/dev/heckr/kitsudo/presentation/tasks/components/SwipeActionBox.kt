package dev.heckr.kitsudo.presentation.tasks.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SwipeDirection { Left, Right }

/**
 * Swipe gesture container that fires actions **only when the user lifts their
 * finger** past [thresholdFraction] of the row width.
 *
 * Key design decisions:
 * - `pointerInput(Unit)` — the gesture block is created once and never
 *   restarted due to lambda re-creation on recomposition.  Callbacks are read
 *   through [rememberUpdatedState] so they're always current.
 * - Left swipe (destructive): content slides fully off-screen FIRST, then the
 *   action fires.  The background is never visible after the item disappears.
 * - Right swipe (non-destructive): action fires immediately, then the row
 *   springs back to rest.
 * - Threshold haptic fires once per drag direction-cross; action haptic fires
 *   on release.
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

    // Always hold the latest callbacks without restarting pointerInput.
    val latestOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val latestOnSwipeRight by rememberUpdatedState(onSwipeRight)
    val latestSwipeLeftHaptic by rememberUpdatedState(swipeLeftHaptic)
    val latestSwipeRightHaptic by rememberUpdatedState(swipeRightHaptic)
    val latestThreshold by rememberUpdatedState(thresholdFraction)

    // Background recomposes only when direction bucket changes, not every frame.
    val swipeDirection by remember {
        derivedStateOf {
            when {
                offsetX.value < -10f -> SwipeDirection.Left
                offsetX.value > 10f -> SwipeDirection.Right
                else -> null
            }
        }
    }

    Box(modifier = modifier.onSizeChanged { widthPx = it.width }) {

        // matchParentSize() defers measurement until after the foreground is
        // laid out, so the background always matches the row's actual height.
        // fillMaxSize() would collapse to 0 in an unbounded LazyColumn item.
        Box(modifier = Modifier.matchParentSize()) {
            backgroundContent(swipeDirection)
        }

        // Foreground — follows the finger; its opaque surface always masks
        // the background until it slides completely off screen.
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                // Unit key → gesture block is NEVER restarted by recomposition.
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val thresholdPx = widthPx * latestThreshold
                            val cur = offsetX.value
                            when {
                                abs(cur) >= thresholdPx && cur < 0 &&
                                    latestOnSwipeLeft != null -> {
                                    // Fire haptic immediately, slide off first, then delete.
                                    view.performHapticFeedback(latestSwipeLeftHaptic)
                                    scope.launch {
                                        offsetX.animateTo(
                                            targetValue = -widthPx.toFloat(),
                                            animationSpec = tween(durationMillis = 150),
                                        )
                                        latestOnSwipeLeft?.invoke()
                                    }
                                }
                                abs(cur) >= thresholdPx && cur > 0 &&
                                    latestOnSwipeRight != null -> {
                                    // Fire haptic + action, then spring back.
                                    view.performHapticFeedback(latestSwipeRightHaptic)
                                    latestOnSwipeRight?.invoke()
                                    scope.launch {
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.75f,
                                                stiffness = 500f,
                                            ),
                                        )
                                    }
                                }
                                else -> {
                                    // Below threshold — spring back, no action.
                                    scope.launch {
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.75f,
                                                stiffness = 500f,
                                            ),
                                        )
                                    }
                                }
                            }
                            crossedThreshold = false
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.75f,
                                        stiffness = 500f,
                                    ),
                                )
                            }
                            crossedThreshold = false
                        },
                        onHorizontalDrag = { _, delta ->
                            val max = widthPx.toFloat()
                            val new = (offsetX.value + delta).coerceIn(
                                if (latestOnSwipeLeft != null) -max else 0f,
                                if (latestOnSwipeRight != null) max else 0f,
                            )
                            scope.launch { offsetX.snapTo(new) }

                            // Tick when threshold is first crossed in this drag.
                            val nowCrossed = abs(new) >= widthPx * latestThreshold
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
