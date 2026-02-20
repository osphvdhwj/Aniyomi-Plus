package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    Started,
    NotStarted,
    Unread,
    Downloaded,
}

@Composable
fun LibraryQuickFilterBar(
    selected: LibraryQuickFilter,
    onChange: (LibraryQuickFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        items(LibraryQuickFilter.entries.size) { index ->
            val filter = LibraryQuickFilter.entries[index]
            FilterChip(
                selected = selected == filter,
                onClick = { onChange(filter) },
                label = {
                    Text(
                        text = when (filter) {
                            LibraryQuickFilter.All -> stringResource(MR.strings.all)
                            LibraryQuickFilter.Ongoing -> stringResource(MR.strings.ongoing)
                            LibraryQuickFilter.Completed -> stringResource(MR.strings.completed)
                            LibraryQuickFilter.Started -> stringResource(MR.strings.label_started)
                            LibraryQuickFilter.NotStarted -> stringResource(MR.strings.not_started)
                            LibraryQuickFilter.Unread -> stringResource(MR.strings.unread)
                            LibraryQuickFilter.Downloaded -> stringResource(MR.strings.label_downloaded)
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}
