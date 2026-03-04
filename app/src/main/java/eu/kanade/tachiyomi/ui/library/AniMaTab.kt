package eu.kanade.tachiyomi.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab

object AniMaTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = "AniMa",
            icon = rememberVectorPainter(Icons.Filled.VideoLibrary),
        )

    @Composable
    override fun Content() {
        // rememberSaveable handles RAM/ROM lifecycle optimization naturally
        var isAnimeMode by rememberSaveable { mutableStateOf(true) }
        var isPillShrunk by remember { mutableStateOf(false) }

        // Battery Optimization: Throttle scroll event interceptors
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    // Y < -15 is a downward scroll on the screen (user swiping up)
                    if (available.y < -15f && !isPillShrunk) {
                        isPillShrunk = true
                    } else if (available.y > 15f && isPillShrunk) {
                        isPillShrunk = false
                    }
                    return Offset.Zero // Don't consume scroll, just read it
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
        ) {
            // AnimatedContent ensures the inactive library is unmounted from RAM
            AnimatedContent(targetState = isAnimeMode, label = "AniMa Switch") { isAnime ->
                if (isAnime) AnimeLibraryTab.Content() else MangaLibraryTab.Content()
            }

            AniMaPill(
                isAnimeMode = isAnimeMode,
                isShrunk = isPillShrunk,
                onToggle = { isAnimeMode = !isAnimeMode },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
fun AniMaPill(
    isAnimeMode: Boolean,
    isShrunk: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Great UX Animation: Bouncy fluid physics when shrinking/growing
    val pillWidth by animateDpAsState(
        targetValue = if (isShrunk) 56.dp else 160.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "PillWidth",
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (isShrunk) 0.6f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "PillAlpha",
    )

    var swipeOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .width(pillWidth)
            .height(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = pillAlpha))
            .clickable { onToggle() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { swipeOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset += dragAmount
                        if (swipeOffset > 80f && !isAnimeMode) {
                            onToggle() // Swipe Right for Anime
                            swipeOffset = 0f
                        } else if (swipeOffset < -80f && isAnimeMode) {
                            onToggle() // Swipe Left for Manga
                            swipeOffset = 0f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!isShrunk) {
            Text(
                text = if (isAnimeMode) "Anime" else "Manga",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelLarge,
            )
        } else {
            Icon(
                imageVector = if (isAnimeMode) Icons.Filled.Movie else Icons.Filled.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
