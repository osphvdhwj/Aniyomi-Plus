package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

enum class LibraryQuickFilter {
    All,
    Ongoing,
    Completed,
}

@Composable
fun LibraryQuickFilterBar(
    selected: LibraryQuickFilter,
    onChange: (LibraryQuickFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryQuickFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onChange(filter) },
                label = {
                    Text(
                        text = when (filter) {
                            LibraryQuickFilter.All -> stringResource(MR.strings.all)
                            LibraryQuickFilter.Ongoing -> stringResource(MR.strings.ongoing)
                            LibraryQuickFilter.Completed -> stringResource(MR.strings.completed)
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}
