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

package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ChaptersSheet(
    chapters: ImmutableList<Segment>,
    currentChapter: Segment,
    onClick: (Segment) -> Unit,
    onDismissRequest: () -> Unit,
    dismissSheet: Boolean,
    coverUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    GenericTracksSheet(
        tracks = chapters,
        header = {
            TrackSheetTitle(
                title = stringResource(AYMR.strings.player_sheets_chapters_title),
                modifier = modifier.padding(top = MaterialTheme.padding.small),
            )
        },
        track = {
            ChapterTrack(
                chapter = it,
                index = chapters.indexOf(it),
                selected = currentChapter == it,
                onClick = { onClick(it) },
                coverUrl = coverUrl,
            )
        },
        onDismissRequest = onDismissRequest,
        dismissEvent = dismissSheet,
        modifier = modifier,
    )
}

@Composable
fun ChapterTrack(
    chapter: Segment,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.padding.small, horizontal = MaterialTheme.padding.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(90.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.BottomEnd,
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.padding(4.dp),
            ) {
                Text(
                    text = Utils.prettyTime(chapter.start.toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }

        // Details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = chapter.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(AYMR.strings.player_sheets_track_title_wo_lang, index + 1, ""),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
