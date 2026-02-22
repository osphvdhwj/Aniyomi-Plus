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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import dev.vivvvek.seeker.Segment
import eu.kanade.tachiyomi.ui.player.Sheets
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton
import eu.kanade.tachiyomi.ui.player.controls.components.CurrentChapter
import eu.kanade.tachiyomi.ui.player.controls.components.SpeedSelectorOverlay
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BottomLeftPlayerControls(
    playbackSpeed: Float,
    currentChapter: Segment?,
    onLockControls: () -> Unit,
    onCycleRotation: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onOpenSheet: (Sheets) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
    val playbackSpeedPresets by playerPreferences.speedPresets().collectAsState()
    val recentSpeedList by playerPreferences.recentSpeedList().collectAsState()
    val sortedPresets = remember(playbackSpeedPresets) {
        playbackSpeedPresets
            .mapNotNull { it.toFloatOrNull() }
            .distinct()
            .sorted()
            .ifEmpty { listOf(1f) }
    }
    val dragSpeeds = remember(recentSpeedList, sortedPresets) {
        val recent = parseSpeeds(recentSpeedList)
        val merged = if (recent.isEmpty()) {
            sortedPresets
        } else {
            recent + sortedPresets
        }
        merged.distinct()
    }
    var showSpeedPresets by remember { mutableStateOf(false) }

    fun applySpeed(speed: Float, closeSheet: Boolean = false) {
        onPlaybackSpeedChange(speed)
        playerPreferences.playerSpeed().set(speed)
        val normalized = speed.normalizeSpeed()
        val updatedRecents = listOf(normalized) + dragSpeeds
            .map { it.normalizeSpeed() }
            .filterNot { abs(it - normalized) < 0.01f }
        playerPreferences.recentSpeedList().set(updatedRecents.take(12).joinToString(",") { it.prettySpeed() })
        if (closeSheet) showSpeedPresets = false
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlsButton(
            Icons.Default.LockOpen,
            onClick = onLockControls,
        )
        ControlsButton(
            icon = Icons.Default.ScreenRotation,
            onClick = onCycleRotation,
        )

        // Speed Control with Drag Gesture
        val overlaySpeeds = remember(dragSpeeds, playbackSpeed) {
            (dragSpeeds + playbackSpeed).map { it.normalizeSpeed() }.distinct().sorted()
        }
        val currentSpeedIndex = overlaySpeeds.indexOfFirst { abs(it - playbackSpeed) < 0.01f }.coerceAtLeast(0)

        var isDraggingSpeed by remember { mutableStateOf(false) }
        var dragOffset by remember { mutableFloatStateOf(0f) }

        Box(
            contentAlignment = Alignment.Center,
        ) {
            ControlsButton(
                text = stringResource(AYMR.strings.player_speed, playbackSpeed),
                onClick = { onOpenSheet(Sheets.PlaybackSpeed) },
                onLongClick = {}, // Disable default long click behavior
                modifier = Modifier.pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            isDraggingSpeed = true
                            dragOffset = 0f
                        },
                        onDragEnd = {
                            isDraggingSpeed = false
                            val itemHeightPx = 40.dp.toPx()
                            val steps = -(dragOffset / itemHeightPx).roundToInt()
                            val newIndex = (currentSpeedIndex + steps).coerceIn(0, overlaySpeeds.lastIndex)
                            applySpeed(overlaySpeeds[newIndex])
                        },
                        onDragCancel = {
                            isDraggingSpeed = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        },
                    )
                },
            )

            if (isDraggingSpeed) {
                val itemHeightPx = 40.dp.toPx()
                val normalizedOffset = -(dragOffset / itemHeightPx)

                Popup(
                    alignment = Alignment.BottomCenter,
                    offset = IntOffset(0, -100),
                ) {
                    SpeedSelectorOverlay(
                        speeds = overlaySpeeds,
                        currentSpeed = playbackSpeed,
                        dragOffset = normalizedOffset,
                    )
                }
            }
        }

        AnimatedVisibility(
            currentChapter != null && playerPreferences.showCurrentChapter().get(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CurrentChapter(
                chapter = currentChapter!!,
                onClick = { onOpenSheet(Sheets.Chapters) },
            )
        }
    }
}

private fun parseSpeeds(value: String): List<Float> = value
    .split(",")
    .mapNotNull { it.trim().toFloatOrNull() }
    .map { it.normalizeSpeed() }
    .distinct()

private fun Float.normalizeSpeed(): Float = ((coerceIn(0.01f, 6f) * 100f).roundToInt()) / 100f

private fun Float.prettySpeed(): String = normalizeSpeed().toString()
