package eu.kanade.tachiyomi.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
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
    // Material 3 Spring specs for fluid, organic motion
    val springSpecDp = spring<Dp>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)

    val trackWidth = 220.dp
    val shrunkSize = 56.dp
    val padding = 6.dp

    // Dynamic widths
    val pillWidth by animateDpAsState(
        targetValue = if (isShrunk) shrunkSize else trackWidth,
        animationSpec = springSpecDp,
        label = "PillWidth",
    )

    // The "Thumb" is the highlighted background behind the active item
    val thumbWidth by animateDpAsState(
        targetValue = if (isShrunk) shrunkSize else (trackWidth - padding * 2) / 2,
        animationSpec = springSpecDp,
        label = "ThumbWidth",
    )

    // Sliding offset for the thumb (Moves left/right based on selection)
    val thumbOffset by animateDpAsState(
        targetValue = when {
            isShrunk -> 0.dp
            isAnimeMode -> padding // Positioned Left for Anime
            else -> padding + thumbWidth // Positioned Right for Manga
        },
        animationSpec = springSpecDp,
        label = "ThumbOffset",
    )

    var swipeOffset by remember { mutableFloatStateOf(0f) }

    // M3 Surface track container
    Box(
        modifier = modifier
            .width(pillWidth)
            .height(shrunkSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { swipeOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset += dragAmount
                        if (swipeOffset > 80f && !isAnimeMode) {
                            onToggle() // Swipe Right
                            swipeOffset = 0f
                        } else if (swipeOffset < -80f && isAnimeMode) {
                            onToggle() // Swipe Left
                            swipeOffset = 0f
                        }
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // 1. The Sliding Highlight Thumb
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = padding)
                .offset(x = thumbOffset)
                .width(thumbWidth)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        )

        // 2. The Content (Icons or Text)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            if (isShrunk) {
                // When shrunk, morph the icon using M3 Shared-Axis style scaling
                AnimatedContent(
                    targetState = isAnimeMode,
                    transitionSpec = {
                        (
                            fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                scaleIn(initialScale = 0.8f, animationSpec = tween(220, delayMillis = 90))
                            )
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "IconTransition",
                    modifier = Modifier.fillMaxSize(),
                ) { isAnime ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isAnime) Icons.Filled.Movie else Icons.Filled.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            } else {
                // Expanded: Show both clickable labels with smooth color blending
                AniMaLabel(
                    text = "Anime",
                    icon = Icons.Filled.Movie,
                    isSelected = isAnimeMode,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { if (!isAnimeMode) onToggle() },
                )
                AniMaLabel(
                    text = "Manga",
                    icon = Icons.Filled.Book,
                    isSelected = !isAnimeMode,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { if (isAnimeMode) onToggle() },
                )
            }
        }
    }
}

@Composable
private fun AniMaLabel(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    // Animate text and icon color based on whether the thumb is currently behind it
    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "TextColor",
    )

    Row(
        modifier = modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp).padding(end = 4.dp),
        )
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
