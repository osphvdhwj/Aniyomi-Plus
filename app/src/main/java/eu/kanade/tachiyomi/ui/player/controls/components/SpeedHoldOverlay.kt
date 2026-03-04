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
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!isDragMode) {
            // --- MODE 1: Simple Hold ---
            Text(
                // Format: "2.0x"
                text = String.format(Locale.US, "%.1fx", currentSpeed),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            // --- MODE 2: Drag Selector ---
            val listState = rememberLazyListState()

            LaunchedEffect(selectedIndex) {
                listState.animateScrollToItem(
                    index = (selectedIndex - 2).coerceAtLeast(0),
                )
            }

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.widthIn(max = 240.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(availableSpeeds) { index, speed ->
                    val isSelected = index == selectedIndex
                    val color = if (isSelected) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.5f)
                    val size = if (isSelected) 18.sp else 14.sp

                    Text(
                        // Format: "2.0x" even in list
                        text = String.format(Locale.US, "%.1fx", speed),
                        color = color,
                        fontSize = size,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
