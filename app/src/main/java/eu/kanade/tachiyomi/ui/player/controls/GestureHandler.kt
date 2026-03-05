package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.player.components.LeftSideOvalShape
import eu.kanade.presentation.player.components.RightSideOvalShape
import eu.kanade.presentation.theme.playerRippleConfiguration
import eu.kanade.tachiyomi.ui.player.Panels
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import eu.kanade.tachiyomi.ui.player.Sheets
import eu.kanade.tachiyomi.ui.player.controls.components.DoubleTapSeekTriangles
import eu.kanade.tachiyomi.ui.player.settings.AudioPreferences
import eu.kanade.tachiyomi.ui.player.settings.GesturePreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.abs

// Helper to fix "collectAsState" errors on Preferences
@Composable
fun <T> Preference<T>.collectAsState(): State<T> {
    val flow = remember(this) { this.changes() }
    return flow.collectAsState(initial = this.get())
}

@Composable
fun GestureHandler(
    viewModel: PlayerViewModel,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
    val gesturePreferences = remember { Injekt.get<GesturePreferences>() }
    val audioPreferences = remember { Injekt.get<AudioPreferences>() }

    val panelShown by viewModel.panelShown.collectAsState()
    val allowGesturesInPanels by playerPreferences.allowGestures().collectAsState()
    val controlsShown by viewModel.controlsShown.collectAsState()
    val areControlsLocked by viewModel.areControlsLocked.collectAsState()

    val seekAmount: Int by viewModel.doubleTapSeekAmount.collectAsState()
    val isSeekingForwards: Boolean by viewModel.isSeekingForwards.collectAsState()
    var isDoubleTapSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(seekAmount) {
        if (seekAmount != 0) {
            delay(800)
            isDoubleTapSeeking = false
            viewModel.updateSeekAmount(0)
            viewModel.updateSeekText(null)
            delay(100)
            viewModel.hideSeekBar()
        }
    }

    val gestureVolumeBrightness = gesturePreferences.gestureVolumeBrightness().get()
    val swapVolumeBrightness by gesturePreferences.swapVolumeBrightness().collectAsState()
    val seekGesture: Boolean by gesturePreferences.gestureHorizontalSeek().collectAsState()
    val preciseSeeking: Boolean by gesturePreferences.playerSmoothSeek().collectAsState()
    val showSeekbar: Boolean by gesturePreferences.showSeekBar().collectAsState()

    val currentVolume by viewModel.currentVolume.collectAsState()
    val currentMPVVolume by viewModel.currentMPVVolume.collectAsState()
    val currentBrightness by viewModel.currentBrightness.collectAsState()
    val volumeBoostingCap = audioPreferences.volumeBoostCap().get()

    val position by viewModel.pos.collectAsState()
    val duration by viewModel.duration.collectAsState()

    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeGestures)
            // 1. Vertical Drag (Volume/Brightness)
            .pointerInput(areControlsLocked, gestureVolumeBrightness, swapVolumeBrightness) {
                if (!gestureVolumeBrightness || areControlsLocked) return@pointerInput

                var startingY = 0f
                var mpvVolumeStartingY = 0f
                var originalVolume = 0
                var originalMPVVolume = 0
                var originalBrightness = 0f

                val screenWidth = size.width
                val touchSlop = viewConfiguration.touchSlop
                var isEdgeSwipe = false
                var hasPassedSlop = false

                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        startingY = offset.y
                        mpvVolumeStartingY = offset.y
                        originalVolume = currentVolume
                        originalMPVVolume = currentMPVVolume
                        originalBrightness = currentBrightness

                        isEdgeSwipe = offset.x < screenWidth * 0.2f || offset.x > screenWidth * 0.8f
                        hasPassedSlop = false
                    },
                    onDragEnd = { startingY = 0f },
                    onDragCancel = { startingY = 0f },
                ) { change, amount ->
                    if (!isEdgeSwipe) return@detectVerticalDragGestures

                    val currentY = change.position.y
                    val dragDistance = startingY - currentY

                    if (!hasPassedSlop && kotlin.math.abs(dragDistance) > touchSlop) {
                        hasPassedSlop = true
                    }

                    if (!hasPassedSlop) return@detectVerticalDragGestures

                    val isIncreasingVolumeBoost = (
                        volumeBoostingCap > 0 &&
                            currentVolume == viewModel.maxVolume &&
                            currentMPVVolume - 100 < volumeBoostingCap &&
                            amount < 0
                        )

                    val isDecreasingVolumeBoost = (
                        volumeBoostingCap > 0 &&
                            currentVolume == viewModel.maxVolume &&
                            currentMPVVolume - 100 in 1..volumeBoostingCap &&
                            amount > 0
                        )

                    val changeVolume = {
                        if (isIncreasingVolumeBoost || isDecreasingVolumeBoost) {
                            // Already handled dynamically by the gesture limits

                            viewModel.changeMPVVolumeTo(
                                calculateNewVerticalGestureValue(
                                    originalMPVVolume,
                                    mpvVolumeStartingY,
                                    change.position.y,
                                    0.001f * volumeBoostingCap,
                                ).coerceIn(100..volumeBoostingCap + 100),
                            )
                        } else {
                            // Handled correctly
                            viewModel.changeVolumeTo(
                                calculateNewVerticalGestureValue(
                                    originalVolume,
                                    startingY,
                                    change.position.y,
                                    0.001f * viewModel.maxVolume,
                                ),
                            )
                        }
                        viewModel.displayVolumeSlider()
                    }

                    val changeBrightness = {
                        viewModel.changeBrightnessTo(
                            calculateNewVerticalGestureValue(
                                originalBrightness,
                                startingY,
                                change.position.y,
                                0.001f,
                            ),
                        )
                        viewModel.displayBrightnessSlider()
                    }

                    if (swapVolumeBrightness) {
                        if (change.position.x > size.width / 2) changeBrightness() else changeVolume()
                    } else {
                        if (change.position.x < size.width / 2) changeBrightness() else changeVolume()
                    }
                }
            }
            // 2. Custom Detector: Long Press + Drag (Robust with Try-Finally)
            .pointerInput(areControlsLocked, seekGesture) {
                if (areControlsLocked) return@pointerInput

                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        var isLongPress = false
                        var isHorizontalDrag = false
                        var totalDragDistanceX = 0f

                        val screenWidth = size.width
                        val isCenterTouch = down.position.x > screenWidth * 0.2f && down.position.x < screenWidth * 0.8f

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
                                if (isCenterTouch &&
                                    !isLongPress &&
                                    !isHorizontalDrag &&
                                    timeElapsed > viewConfiguration.longPressTimeoutMillis
                                ) {
                                    isLongPress = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                                    if (viewModel.paused.value) {
                                        viewModel.sheetShown.update { Sheets.Screenshot }
                                    } else {
                                        viewModel.onHoldSpeedStart()
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
