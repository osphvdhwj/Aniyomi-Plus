package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource

enum class LibraryMediaTab {
    Anime,
    Manga,
}

@Composable
fun LibraryMediaTabs(
    currentTab: LibraryMediaTab,
    onTabChange: (LibraryMediaTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember(currentTab) { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .pointerInput(currentTab) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        dragOffset += amount
                        if (dragOffset <= -40f && currentTab == LibraryMediaTab.Anime) {
                            onTabChange(LibraryMediaTab.Manga)
                            dragOffset = 0f
                        } else if (dragOffset >= 40f && currentTab == LibraryMediaTab.Manga) {
                            onTabChange(LibraryMediaTab.Anime)
                            dragOffset = 0f
                        }
                        change.consume()
                    },
                    onDragEnd = { dragOffset = 0f },
                    onDragCancel = { dragOffset = 0f },
                )
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryMediaTabItem(
            title = stringResource(AYMR.strings.label_anime),
            selected = currentTab == LibraryMediaTab.Anime,
            onClick = { onTabChange(LibraryMediaTab.Anime) },
            modifier = Modifier.weight(1f),
        )
        LibraryMediaTabItem(
            title = stringResource(AYMR.strings.label_manga),
            selected = currentTab == LibraryMediaTab.Manga,
            onClick = { onTabChange(LibraryMediaTab.Manga) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LibraryMediaTabItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
