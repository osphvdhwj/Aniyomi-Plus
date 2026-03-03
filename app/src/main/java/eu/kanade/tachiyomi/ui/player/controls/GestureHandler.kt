/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.player.components.LeftSideOvalShape
import eu.kanade.presentation.player.components.RightSideOvalShape
import eu.kanade.presentation.theme.playerRippleConfiguration
import eu.kanade.tachiyomi.ui.player.Panels
import eu.kanade.tachiyomi.ui.player.PlayerUpdates
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import eu.kanade.tachiyomi.ui.player.Sheets
import eu.kanade.tachiyomi.ui.player.controls.components.DoubleTapSeekTriangles
import eu.kanade.tachiyomi.ui.player.settings.AudioPreferences
import eu.kanade.tachiyomi.ui.player.settings.GesturePreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.flow.update
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.abs

@Composable
fun GestureHandler(
    viewModel: PlayerViewModel,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val gesturePreferences = remember { Injekt.get<GesturePreferences>() }
    val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
    val audioPreferences = remember { Injekt.get<AudioPreferences>() }
    val haptics = LocalHapticFeedback.current

    val seekGesture by gesturePreferences.gestureHorizontalSeek().collectAsState()
    val gestureVolumeBrightness by gesturePreferences.gestureVolumeBrightness().collectAsState()
    val preciseSeeking by gesturePreferences.preciseSeeking().collectAsState()
    val swapVolumeBrightness by gesturePreferences.swapVolumeBrightness().collectAsState()
    val volumeBoostingCap by audioPreferences.volumeBoostCap().collectAsState()
    val showSeekbar by playerPreferences.showSeekbar().collectAsState()

    val currentVolume by viewModel.currentVolume.collectAsState()
    val currentMPVVolume by viewModel.currentMPVVolume.collectAsState()
    val currentBrightness by viewModel.currentBrightness.collectAsState()
    val position by viewModel.pos.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val areControlsLocked by viewModel.controlsLocked.collectAsState()

    var isLongPressing by remember { mutableStateOf(false) }
    var isDoubleTapSeeking by remember { mutableStateOf(false) }
    var isSeekingForwards by remember { mutableStateOf(true) }

    val originalSpeed by remember { mutableStateOf(MPVLib.getPropertyDouble("speed") ?: 1.0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeGestures)
            // Zoom gesture (2 fingers) - applied first to see raw events
            .pointerInput(areControlsLocked) {
                if (areControlsLocked) return@pointerInput
                detectZoomGestures { zoomVal, panVal ->
                    val currentZoom = MPVLib.getPropertyDouble("video-zoom") ?: 0.0
                    // video-zoom is log2(scale)
                    // new_scale = old_scale * zoomVal
                    // new_zoom = old_zoom + log2(zoomVal)
                    val zoomChange = kotlin.math.ln(zoomVal.toDouble()) / kotlin.math.ln(2.0)
                    MPVLib.setPropertyDouble("video-zoom", (currentZoom + zoomChange).coerceIn(0.0, 5.0))

                    val currentPanX = MPVLib.getPropertyDouble("video-pan-x") ?: 0.0
                    val currentPanY = MPVLib.getPropertyDouble("video-pan-y") ?: 0.0
                    // Normalize pan by screen size?
                    // Assuming MPV pan 1.0 is roughly half screen width in zoom?
                    // It's safer to use small sensitivity.
                    val panSensX = 1.0 / size.width
                    val panSensY = 1.0 / size.height
                    MPVLib.setPropertyDouble("video-pan-x", currentPanX + panVal.x * panSensX)
                    MPVLib.setPropertyDouble("video-pan-y", currentPanY + panVal.y * panSensY)
                }
            }
            .pointerInput(areControlsLocked) {
                if (!seekGesture || areControlsLocked) return@pointerInput
                var startingPosition = position.toInt()
                var startingX = 0f
                var wasPlayerAlreadyPause = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        startingPosition = position.toInt()
                        startingX = it.x
                        wasPlayerAlreadyPause = viewModel.paused.value
                        viewModel.pause()
                    },
                    onDragEnd = {
                        viewModel.gestureSeekAmount.update { null }
                        viewModel.hideSeekBar()
                        if (!wasPlayerAlreadyPause) viewModel.unpause()
                    },
                ) { change, dragAmount ->
                    if (position <= 0f && dragAmount < 0) return@detectHorizontalDragGestures
                    if (position >= duration && dragAmount > 0) return@detectHorizontalDragGestures
                    calculateNewHorizontalGestureValue(startingPosition, startingX, change.position.x, 0.15f).let {
                        viewModel.gestureSeekAmount.update { _ ->
                            Pair(
                                startingPosition,
                                (it - startingPosition)
                                    .coerceIn(0 - startingPosition, (duration - startingPosition).toInt()),
                            )
                        }
                        viewModel.seekTo(it.coerceIn(0, duration.toInt()), preciseSeeking)
                    }

                    if (showSeekbar) viewModel.showSeekBar()
                }
            }
            .pointerInput(areControlsLocked) {
                if (!gestureVolumeBrightness || areControlsLocked) return@pointerInput
                var startingY = 0f
                var mpvVolumeStartingY = 0f
                var originalVolume = currentVolume
                var originalMPVVolume = currentMPVVolume
                var originalBrightness = currentBrightness
                val brightnessGestureSens = 0.001f
                val volumeGestureSens = 0.001f * viewModel.maxVolume
                val mpvVolumeGestureSens = 0.001f * volumeBoostingCap
                val isIncreasingVolumeBoost: (Float) -> Boolean = {
                    volumeBoostingCap > 0 &&
                        currentVolume == viewModel.maxVolume &&
                        currentMPVVolume - 100 < volumeBoostingCap &&
                        it < 0
                }
                val isDecreasingVolumeBoost: (Float) -> Boolean = {
                    volumeBoostingCap > 0 &&
                        currentVolume == viewModel.maxVolume &&
                        currentMPVVolume - 100 in 1..volumeBoostingCap &&
                        it > 0
                }
                detectVerticalDragGestures(
                    onDragEnd = { startingY = 0f },
                    onDragStart = {
                        startingY = 0f
                        mpvVolumeStartingY = 0f
                        originalVolume = currentVolume
                        originalMPVVolume = currentMPVVolume
                        originalBrightness = currentBrightness
                    },
                ) { change, amount ->
                    val changeVolume: () -> Unit = {
                        if (isIncreasingVolumeBoost(amount) || isDecreasingVolumeBoost(amount)) {
                            if (mpvVolumeStartingY == 0f) {
                                startingY = 0f
                                originalVolume = currentVolume
                                mpvVolumeStartingY = change.position.y
                            }
                            viewModel.changeMPVVolumeTo(
                                calculateNewVerticalGestureValue(
                                    originalMPVVolume,
                                    mpvVolumeStartingY,
                                    change.position.y,
                                    mpvVolumeGestureSens,
                                )
                                    .coerceIn(100..volumeBoostingCap + 100),
                            )
                        } else {
                            if (startingY == 0f) {
                                mpvVolumeStartingY = 0f
                                originalMPVVolume = currentMPVVolume
                                startingY = change.position.y
                            }
                            viewModel.changeVolumeTo(
                                calculateNewVerticalGestureValue(
                                    originalVolume,
                                    startingY,
                                    change.position.y,
                                    volumeGestureSens,
                                ),
                            )
                        }
                        viewModel.displayVolumeSlider()
                    }
                    val changeBrightness: () -> Unit = {
                        if (startingY == 0f) startingY = change.position.y
                        viewModel.changeBrightnessTo(
                            calculateNewVerticalGestureValue(
                                originalBrightness,
                                startingY,
                                change.position.y,
                                brightnessGestureSens,
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
            .pointerInput(areControlsLocked) {
                detectTapGestures(
                    onDoubleTap = {
                        isDoubleTapSeeking = true
                        if (it.x > size.width * 3 / 5) {
                            if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                            viewModel.handleRightDoubleTap()
                        } else if (it.x < size.width * 2 / 5) {
                            if (isSeekingForwards) viewModel.updateSeekAmount(0)
                            viewModel.handleLeftDoubleTap()
                        } else {
                            viewModel.handleCenterDoubleTap()
                        }
                    },
                    onTap = {
                        if (areControlsLocked) {
                            viewModel.showPanel(Panels.None)
                            viewModel.showSheet(Sheets.None)
                            viewModel.showControls()
                            return@detectTapGestures
                        }
                        if (isDoubleTapSeeking) return@detectTapGestures
                        viewModel.showControls()
                    },
                    onPress = { press ->
                        val pressInteraction = PressInteraction.Press(press)
                        if (areControlsLocked) return@detectTapGestures
                        // this press action is weird, we should have a better way to handle this
                        if (isDoubleTapSeeking) {
                            if (press.x > size.width * 3 / 5) {
                                if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                                viewModel.handleRightDoubleTap()
                            } else if (press.x < size.width * 2 / 5) {
                                if (isSeekingForwards) viewModel.updateSeekAmount(0)
                                viewModel.handleLeftDoubleTap()
                            } else {
                                viewModel.handleCenterDoubleTap()
                            }
                        } else {
                            isDoubleTapSeeking = false
                        }
                        interactionSource.emit(pressInteraction)
                        tryAwaitRelease()
                        if (isLongPressing) {
                            isLongPressing = false
                            MPVLib.setPropertyDouble("speed", originalSpeed.toDouble())
                            viewModel.playerUpdate.update { PlayerUpdates.None }
                        }
                        interactionSource.emit(PressInteraction.Release(pressInteraction))
                    },
                    onLongPress = {
                        if (areControlsLocked) return@detectTapGestures
                        if (!isLongPressing) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isLongPressing = true
                            viewModel.pause()
                            viewModel.sheetShown.update { Sheets.Screenshot }
                        }
                    },
                )
            },
    )
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
                        .fillMaxWidth(0.4f), // 2 fifths
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

suspend fun PointerInputScope.detectZoomGestures(
    onZoom: (zoom: Float, pan: Offset) -> Unit,
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    if (event.changes.size > 1) {
                        onZoom(zoomChange, panChange)
                        event.changes.forEach { if (it.positionChange() != Offset.Zero) it.consume() }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}
