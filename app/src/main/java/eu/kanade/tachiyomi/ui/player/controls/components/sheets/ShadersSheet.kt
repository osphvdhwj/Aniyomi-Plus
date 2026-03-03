package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.player.utils.SHADER_PRESETS
import eu.kanade.tachiyomi.ui.player.utils.ShaderPreset
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.presentation.core.components.material.padding

@Composable
fun ShadersSheet(
    selectedShader: ShaderPreset?,
    onSelectShader: (ShaderPreset) -> Unit,
    onClearShaders: () -> Unit,
    onDismissRequest: () -> Unit,
    dismissSheet: Boolean = false,
) {
    GenericTracksSheet(
        tracks = (listOf(null) + SHADER_PRESETS).toImmutableList(),
        onDismissRequest = onDismissRequest,
        dismissEvent = dismissSheet,
        header = {
            TrackSheetTitle(
                title = "Shaders", // TODO: Add string resource
                modifier = Modifier.padding(top = MaterialTheme.padding.small),
            )
        },
        track = { preset ->
            val isSelected = preset == selectedShader
            val title = preset?.name ?: "None"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (preset == null) onClearShaders() else onSelectShader(preset)
                    }
                    .padding(vertical = MaterialTheme.padding.small, horizontal = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
}
