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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.vivvvek.seeker.Segment
import eu.kanade.tachiyomi.ui.player.Sheets
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton
import eu.kanade.tachiyomi.ui.player.controls.components.CurrentChapter
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
        merged.distinct().take(12)
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
        ControlsButton(
            text = stringResource(AYMR.strings.player_speed, playbackSpeed),
            onClick = {
                val currentIndex = sortedPresets.indexOfFirst { abs(it - playbackSpeed) < 0.01f }
                val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % sortedPresets.size else 0
                applySpeed(sortedPresets[nextIndex])
            },
            onLongClick = { showSpeedPresets = !showSpeedPresets },
        )
        AnimatedVisibility(
            visible = showSpeedPresets,
            enter = fadeIn() + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            exit = fadeOut() + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
        ) {
            val itemHeight = 48.dp
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .pointerInput(showSpeedPresets, dragSpeeds) {
                        val itemHeightPx = itemHeight.toPx()
                        detectVerticalDragGestures(
                            onDragStart = { startOffset ->
                                val index = (startOffset.y / itemHeightPx).toInt().coerceIn(0, dragSpeeds.lastIndex)
                                applySpeed(dragSpeeds[index])
                            },
                            onVerticalDrag = { change, _ ->
                                val index = (change.position.y / itemHeightPx).toInt().coerceIn(0, dragSpeeds.lastIndex)
                                applySpeed(dragSpeeds[index])
                                change.consume()
                            },
                            onDragEnd = { showSpeedPresets = false },
                            onDragCancel = { showSpeedPresets = false },
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                dragSpeeds.forEach { speed ->
                    ControlsButton(
                        text = stringResource(AYMR.strings.player_speed, speed),
                        onClick = { applySpeed(speed, closeSheet = true) },
                        onLongClick = { onOpenSheet(Sheets.PlaybackSpeed) },
                        color = if (abs(playbackSpeed - speed) < 0.01f) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White
                        },
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
