package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.player.components.LeftSideOvalShape
import eu.kanade.presentation.player.components.RightSideOvalShape
import eu.kanade.presentation.util.pluralStringResource
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GestureHandler(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onSingleTap: () -> Unit,
    onDoubleTap: (isRightSide: Boolean) -> Unit,
    onVerticalDragStart: (isRightSide: Boolean) -> Unit,
    onVerticalDrag: (isRightSide: Boolean, dragDistance: Float) -> Unit,
    onHorizontalDragStart: () -> Unit,
    onHorizontalDrag: (dragDistance: Float) -> Unit,
    onDragEnd: () -> Unit,
    seekAmount: Int? = null,
    showSeekTriangles: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()
    var lastTapTime by remember { mutableStateOf(0L) }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapRegion by remember { mutableStateOf<Boolean?>(null) } // true = right, false = left

    val doubleTapTimeout = 250L
    val multiTapContinueWindow = 650L // Window to continue tapping without dropping state

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val downEvent = awaitFirstDown(requireUnconsumed = false)
                    val startX = downEvent.position.x
                    val startY = downEvent.position.y
                    val isRightSide = startX > size.width / 2

                    val screenWidth = size.width
                    val screenHeight = size.height

                    var isDragging = false
                    var gestureType: String? = null
                    var isConsumed = false

                    val touchStartTime = System.currentTimeMillis()

                    do {
                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                        val changes = event.changes

                        if (changes.size > 1) {
                            // Multi-touch detected (e.g. pinch to zoom), cancel single-finger actions
                            if (isDragging) {
                                onDragEnd()
                            }
                            isConsumed = true
                            break
                        }

                        val change = changes.first()
                        val currentX = change.position.x
                        val currentY = change.position.y
                        val deltaX = currentX - startX
                        val deltaY = currentY - startY
                        val distance = Math.hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()
                        val timeSinceStart = System.currentTimeMillis() - touchStartTime

                        if (!isDragging && distance > 20f && timeSinceStart > 100L) {
                            // Determine gesture direction
                            isDragging = true
                            if (abs(deltaX) > abs(deltaY) * 2f) {
                                gestureType = "horizontal"
                                onHorizontalDragStart()
                            } else if (abs(deltaY) > abs(deltaX) * 2f) {
                                gestureType = "vertical"
                                onVerticalDragStart(isRightSide)
                            }
                            tapCount = 0 // Reset tap state when dragging
                        }

                        if (isDragging) {
                            change.consume()
                            if (gestureType == "horizontal") {
                                onHorizontalDrag(deltaX)
                            } else if (gestureType == "vertical") {
                                // Normalized vertical drag percentage relative to screen height
                                val dragPercentage = (deltaY / screenHeight) * -1f
                                onVerticalDrag(isRightSide, dragPercentage)
                            }
                        }

                    } while (event.changes.any { it.pressed })

                    if (isConsumed) return@awaitEachGesture

                    if (isDragging) {
                        onDragEnd()
                    } else if (timeSinceStart < 300L) {
                        // Handle Tap Logic
                        val currentTime = System.currentTimeMillis()
                        val timeSinceLastTap = currentTime - lastTapTime

                        if (timeSinceLastTap > multiTapContinueWindow || lastTapRegion != isRightSide) {
                            // Reset if too much time passed or changed sides
                            tapCount = 1
                            lastTapRegion = isRightSide
                        } else {
                            tapCount++
                        }
                        lastTapTime = currentTime

                        if (tapCount == 1) {
                            coroutineScope.launch {
                                delay(doubleTapTimeout)
                                if (tapCount == 1 && lastTapTime == currentTime) {
                                    onSingleTap()
                                    tapCount = 0
                                }
                            }
                        } else if (tapCount >= 2) {
                            onDoubleTap(isRightSide)
                            // Allow rapid tapping by keeping tapCount > 1 until timeout
                        }
                    }
                }
            }
    ) {
        if (showSeekTriangles && seekAmount != null && seekAmount != 0) {
            val isForward = seekAmount > 0
            val alpha by animateFloatAsState(
                targetValue = if (showSeekTriangles) 0.4f else 0f,
                animationSpec = tween(durationMillis = 200, easing = LinearEasing),
                label = "SeekIndicatorAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f)
                    .align(if (isForward) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(Color.White.copy(alpha), shape = if (isForward) RightSideOvalShape else LeftSideOvalShape)
                    .indication(interactionSource, ripple()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DoubleTapSeekTriangles(isForward = isForward)
                    Text(
                        text = pluralStringResource(R.plurals.seconds, abs(seekAmount), abs(seekAmount)),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
fun DoubleTapSeekTriangles(isForward: Boolean) {
    val animations = remember { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()
    var animCounter by remember { mutableIntStateOf(0) }

    LaunchedEffect(isForward) {
        animations.add(animCounter++)
        scope.launch {
            delay(100)
            animations.add(animCounter++)
        }
    }

    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        // Base static icon
        Icon(
            imageVector = if (isForward) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )

        // Render active moving chevrons
        animations.forEach { animId ->
            key(animId) {
                MovingChevron(
                    isRight = isForward,
                    onFinished = { animations.remove(animId) }
                )
            }
        }
    }
}

@Composable
fun MovingChevron(isRight: Boolean, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(350, easing = LinearEasing)
        )
        onFinished()
    }

    val startOffset = if (isRight) -15f else 15f
    val currentOffset = startOffset * (1f - progress.value)
    val alpha = 1f - progress.value

    Icon(
        imageVector = if (isRight) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowLeft,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier
            .size(48.dp)
            .alpha(alpha)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(x = currentOffset.dp.roundToPx(), y = 0)
                }
            }
    )
}

fun calculateNewVerticalGestureValue(originalValue: Int, startingY: Float, newY: Float, sensitivity: Float): Int {
    return originalValue + ((startingY - newY) * sensitivity).toInt()
}

fun calculateNewVerticalGestureValue(originalValue: Float, startingY: Float, newY: Float, sensitivity: Float): Float {
    return originalValue + ((startingY - newY) * sensitivity)
}

fun calculateNewHorizontalGestureValue(originalValue: Int, startingX: Float, newX: Float, sensitivity: Float): Int {
    return originalValue + ((newX - startingX) * sensitivity).toInt()
}

fun calculateNewHorizontalGestureValue(originalValue: Float, startingX: Float, newX: Float, sensitivity: Float): Float {
    return originalValue + ((newX - startingX) * sensitivity)
}s = false
                        var isHorizontalDrag = false
                        var totalDragDistanceX = 0f

                        val press = PressInteraction.Press(down.position)
                        launch { interactionSource.emit(press) }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break

                                // FIX: Use !pressed to detect lift
                                if (!change.pressed) break

                                val timeElapsed = System.currentTimeMillis() - downTime
                                val dx = change.position.x - change.previousPosition.x
                                totalDragDistanceX += dx

                                // A. Detect Long Press
                                if (!isLongPress &&
                                    !isHorizontalDrag &&
                                    timeElapsed > viewConfiguration.longPressTimeoutMillis
                                ) {
                                    isLongPress = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                                    if (viewModel.paused.value) {
                                        viewModel.sheetShown.update { Sheets.Screenshot }
                                    } else {
                                        val isLeftEdge = down.position.x < size.width * 0.3f
                                        val isRightEdge = down.position.x > size.width * 0.7f
                                        val isCenter = !isLeftEdge && !isRightEdge

                                        if (isCenter) {
                                            viewModel.onHoldSpeedStart()
                                        }
                                    }
                                }

                                // B. Detect Seek Drag
                                if (!isLongPress &&
                                    !isHorizontalDrag &&
                                    abs(totalDragDistanceX) > viewConfiguration.touchSlop
                                ) {
                                    val dyTotal = abs(change.position.y - down.position.y)
                                    if (abs(totalDragDistanceX) > dyTotal) {
                                        if (seekGesture) {
                                            isHorizontalDrag = true
                                            viewModel.pause()
                                        }
                                    }
                                }

                                // C. Handle Speed Drag
                                if (isLongPress && viewModel.isHoldingSpeed) {
                                    viewModel.onHoldSpeedDrag(dx)
                                    change.consume()
                                }

                                // D. Handle Seek Drag
                                if (isHorizontalDrag) {
                                    calculateNewHorizontalGestureValue(
                                        position.toInt(),
                                        down.position.x,
                                        change.position.x,
                                        0.15f,
                                    ).let {
                                        viewModel.gestureSeekAmount.update { _ ->
                                            Pair(
                                                position.toInt(),
                                                (it - position.toInt()).coerceIn(
                                                    0 - position.toInt(),
                                                    (
                                                        duration -
                                                            position.toInt()
                                                        ).toInt(),
                                                ),
                                            )
                                        }
                                        viewModel.seekTo(it.coerceIn(0, duration.toInt()), preciseSeeking)
                                    }
                                    if (showSeekbar) viewModel.showSeekBar()
                                    change.consume()
                                }
                            }
                        } finally {
                            // --- CLEANUP (Guaranteed to run even if gesture is cancelled) ---
                            launch { interactionSource.emit(PressInteraction.Release(press)) }

                            if (isLongPress) {
                                if (viewModel.isHoldingSpeed) viewModel.onHoldSpeedEnd()
                            } else if (isHorizontalDrag) {
                                viewModel.gestureSeekAmount.update { null }
                                viewModel.hideSeekBar()
                                viewModel.unpause()
                            }
                        }
                    }
                }
            }
            // 3. Taps
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (controlsShown) viewModel.hideControls() else viewModel.showControls()
                    },
                    onDoubleTap = {
                        if (areControlsLocked || isDoubleTapSeeking) return@detectTapGestures
                        if (it.x > size.width * 3 / 5) {
                            if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                            viewModel.handleRightDoubleTap()
                            isDoubleTapSeeking = true
                        } else if (it.x < size.width * 2 / 5) {
                            if (isSeekingForwards) viewModel.updateSeekAmount(0)
                            viewModel.handleLeftDoubleTap()
                            isDoubleTapSeeking = true
                        } else {
                            viewModel.handleCenterDoubleTap()
                        }
                    },
                    onPress = {
                        if (panelShown != Panels.None && !allowGesturesInPanels) {
                            viewModel.panelShown.update { Panels.None }
                        }
                    },
                )
            },
    )

    // Ovals for visual feedback
    DoubleTapToSeekOvals(seekAmount, viewModel.seekText.collectAsState().value, interactionSource)
}

@Composable
fun DoubleTapToSeekOvals(
    amount: Int,
    text: String?,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(if (amount == 0) 0f else 0.2f, label = "double_tap_animation_alpha")
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (amount > 0) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides playerRippleConfiguration,
        ) {
            if (amount != 0 || text != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.4f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(if (amount > 0) RightSideOvalShape else LeftSideOvalShape)
                            .background(Color.White.copy(alpha))
                            .indication(interactionSource, ripple()),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DoubleTapSeekTriangles(isForward = amount > 0)
                        Text(
                            text = text ?: pluralStringResource(AYMR.plurals.seconds, amount, amount),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

fun calculateNewVerticalGestureValue(originalValue: Int, startingY: Float, newY: Float, sensitivity: Float): Int {
    return originalValue + ((startingY - newY) * sensitivity).toInt()
}

fun calculateNewVerticalGestureValue(originalValue: Float, startingY: Float, newY: Float, sensitivity: Float): Float {
    return originalValue + ((startingY - newY) * sensitivity)
}

fun calculateNewHorizontalGestureValue(originalValue: Int, startingX: Float, newX: Float, sensitivity: Float): Int {
    return originalValue + ((newX - startingX) * sensitivity).toInt()
}

fun calculateNewHorizontalGestureValue(originalValue: Float, startingX: Float, newX: Float, sensitivity: Float): Float {
    return originalValue + ((newX - startingX) * sensitivity)
}
