package eu.kanade.tachiyomi.ui.player.controls.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs

@Composable
fun SpeedSelectorOverlay(
    speeds: List<Float>,
    currentSpeed: Float,
    dragOffset: Float, // This represents the fractional index offset from the current speed.
    modifier: Modifier = Modifier,
) {
    val currentIndex = remember(speeds, currentSpeed) {
        speeds.indexOfFirst { abs(it - currentSpeed) < 0.01f }.takeIf { it >= 0 } ?: (speeds.size / 2)
    }

    // Determine the "virtual center" index based on drag.
    // If dragOffset is positive (dragged down), we want to select previous items (lower index).
    // If dragOffset is negative (dragged up), we want to select next items (higher index).
    // Wait, usually dragging UP moves the list UP, revealing items BELOW.
    // Let's align with standard scroll behavior: Drag UP -> Scroll Down (Index Increases).
    // So positive drag (down) -> Index Decreases.

    // However, let's keep it simple: dragOffset is directly added to currentIndex for visual positioning.
    // We will clamp the visual range.

    Box(
        modifier = modifier.size(120.dp, 300.dp),
        contentAlignment = Alignment.Center,
    ) {
        speeds.forEachIndexed { index, speed ->
            // Calculate how far this item is from the "center" of the view.
            // The center is defined by currentIndex + dragOffset.
            // If dragOffset is 0, currentIndex is at center (0 distance).
            val distanceFromCenter = index - (currentIndex + dragOffset)

            // Render only if within visible range
            if (distanceFromCenter in -5f..5f) {
                SpeedItem(
                    speed = speed,
                    distanceFromCenter = distanceFromCenter,
                )
            }
        }
    }
}

@Composable
fun BoxScope.SpeedItem(
    speed: Float,
    distanceFromCenter: Float,
) {
    // Visual parameters
    // We want a stack effect.
    // Items with negative distance (lower index) are "above" visually in the stack?
    // Or positive distance (higher index)?

    // Let's implement iOS recent apps style:
    // Items are stacked vertically.
    // The "focused" item is large and in front.
    // Items further away are smaller, lower alpha, and translated behind.

    val absDistance = abs(distanceFromCenter)
    val scale = (1f - (absDistance * 0.15f)).coerceAtLeast(0.4f)
    val alpha = (1f - (absDistance * 0.4f)).coerceIn(0f, 1f)

    // Translation Y:
    // Positive distance (higher index) -> translated down (positive Y)
    // Negative distance (lower index) -> translated up (negative Y)
    // But we condense them so they overlap.
    val translateY = (distanceFromCenter * 40).dp

    // Z-Index: Closer to center = higher z-index (on top).
    val zIndex = 10f - absDistance

    Box(
        modifier = Modifier
            .size(80.dp, 50.dp)
            .graphicsLayer {
                this.translationY = translateY.toPx()
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
                this.zIndex = zIndex

                // Optional: 3D rotation for more "depth"
                // rotationX = -distanceFromCenter * 5f
            }
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = 2.dp,
                color = if (absDistance <
                    0.5f
                ) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${speed.prettySpeed()}x",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

private fun Float.prettySpeed(): String {
    return if (this == this.toInt().toFloat()) {
        this.toInt().toString()
    } else {
        this.toString()
    }
}
