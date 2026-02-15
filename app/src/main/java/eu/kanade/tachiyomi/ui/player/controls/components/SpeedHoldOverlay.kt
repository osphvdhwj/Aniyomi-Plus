package eu.kanade.tachiyomi.ui.player.controls.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun SpeedHoldOverlay(
    isDragMode: Boolean,
    currentSpeed: Double,
    availableSpeeds: List<Double>,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp), // Compact Padding
        contentAlignment = Alignment.Center
    ) {
        if (!isDragMode) {
            // --- MODE 1: Simple Hold (Just 2.0x) ---
            Text(
                text = "${String.format(Locale.US, "%.2f", currentSpeed).trimEnd('0').trimEnd('.')}x",
                color = Color.White,
                fontSize = 14.sp, // Compact Font
                fontWeight = FontWeight.Bold
            )
        } else {
            // --- MODE 2: Drag Selector (List) ---
            val listState = rememberLazyListState()

            LaunchedEffect(selectedIndex) {
                // Keep selected item centered
                listState.animateScrollToItem(
                    index = (selectedIndex - 2).coerceAtLeast(0)
                )
            }

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(12.dp), // Closer spacing
                modifier = Modifier.widthIn(max = 200.dp), // Limit max width
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(availableSpeeds) { index, speed ->
                    val isSelected = index == selectedIndex
                    val color = if (isSelected) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.5f) // Blue if selected
                    val size = if (isSelected) 18.sp else 12.sp

                    Text(
                        text = "${String.format(Locale.US, "%.1f", speed).trimEnd('0').trimEnd('.')}",
                        color = color,
                        fontSize = size,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
