package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import eu.kanade.presentation.player.components.PlayerSheet
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.presentation.core.components.material.padding

@Composable
fun IdentifyAnimeSheet(
    searchResults: List<Anime>,
    onSearch: (String) -> Unit,
    onAnimeSelected: (Anime) -> Unit,
    onDismissRequest: () -> Unit,
    dismissSheet: Boolean = false,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    PlayerSheet(onDismissRequest, dismissEvent = dismissSheet) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
        ) {
            Text(
                text = "Identify Anime",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = MaterialTheme.padding.small),
            )

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                label = { Text("Search Library") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            ) {
                items(searchResults) { anime ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAnimeSelected(anime) }
                            .padding(vertical = MaterialTheme.padding.small),
                    ) {
                        Text(text = anime.title)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
