package eu.kanade.tachiyomi.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab

// Global state so the Navigation Bar can react to the Pill changes
object AniMaState {
    var isAnimeMode by mutableStateOf(true)
}

object AniMaTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isAnime = AniMaState.isAnimeMode
            val title = if (isAnime) "Anime" else "Manga"

            // ANIMATION 1: Morphing the Navigation Bar Icon
            val moviePainter = rememberVectorPainter(Icons.Filled.Movie)
            val bookPainter = rememberVectorPainter(Icons.Filled.Book)

            val transition = updateTransition(targetState = isAnime, label = "NavIconMorph")
            val movieAlpha by transition.animateFloat(label = "MovieAlpha") { if (it) 1f else 0f }
            val bookAlpha by transition.animateFloat(label = "BookAlpha") { if (it) 0f else 1f }

            val animatedIconPainter = remember(movieAlpha, bookAlpha) {
                object : Painter() {
                    override val intrinsicSize = moviePainter.intrinsicSize
                    override fun DrawScope.onDraw() {
                        with(moviePainter) { draw(size, alpha = movieAlpha) }
                        with(bookPainter) { draw(size, alpha = bookAlpha) }
                    }
                }
            }

            return TabOptions(
                index = 0u,
                title = title,
                icon = animatedIconPainter, // Injects the animated crossfade painter directly into the Nav Bar
            )
        }

    @Composable
    override fun Content() {
        var isPillHidden by remember { mutableStateOf(false) }

        // Intercept scrolling to trigger the Pill slide-down animation
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -15f && !isPillHidden) {
                        isPillHidden = true
                    } else if (available.y > 15f && isPillHidden) {
                        isPillHidden = false
                    }
                    return Offset.Zero
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
        ) {
            // ANIMATION 2: Content sliding horizontally like a ViewPager
            AnimatedContent(
                targetState = AniMaState.isAnimeMode,
                transitionSpec = {
                    if (targetState) {
                        // Manga -> Anime: Slide in from Left
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                    } else {
                        // Anime -> Manga: Slide in from Right
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                    }
                },
                label = "LibraryContentSlide",
            ) { isAnime ->
                if (isAnime) AnimeLibraryTab.Content() else MangaLibraryTab.Content()
            }

            // ANIMATION 3: Pill physically slides Up and Down out of view
            val pillOffsetY by animateDpAsState(
                targetValue = if (isPillHidden) 120.dp else 0.dp, // 120dp completely removes it from the screen
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                label = "PillSlideVertical",
            )

            AniMaPill(
                isAnimeMode = AniMaState.isAnimeMode,
                onToggle = { AniMaState.isAnimeMode = !AniMaState.isAnimeMode },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = pillOffsetY) // Applies the fluid translation
                    .padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
fun AniMaPill(
    isAnimeMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val springSpecDp = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)

    val trackWidth = 220.dp
    val pillHeight = 56.dp
    val padding = 6.dp

    val thumbWidth = (trackWidth - padding * 2) / 2
    val thumbOffset by animateDpAsState(
        targetValue = if (isAnimeMode) padding else padding + thumbWidth,
        animationSpec = springSpecDp,
        label = "ThumbOffset",
    )

    var swipeOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(pillHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { swipeOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset += dragAmount
                        if (swipeOffset > 80f && !isAnimeMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggle() // Swipe Right
                            swipeOffset = 0f
                        } else if (swipeOffset < -80f && isAnimeMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggle() // Swipe Left
                            swipeOffset = 0f
                        }
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // The Sliding Highlight Thumb
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = padding)
                .offset(x = thumbOffset)
                .width(thumbWidth)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        )

        // The Clickable Labels
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AniMaLabel(
                text = "Anime",
                icon = Icons.Filled.Movie,
                isSelected = isAnimeMode,
                modifier = Modifier
                    .weight(1f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (!isAnimeMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggle()
                        }
                    },
            )
            AniMaLabel(
                text = "Manga",
                icon = Icons.Filled.Book,
                isSelected = !isAnimeMode,
                modifier = Modifier
                    .weight(1f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (isAnimeMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggle()
                        }
                    },
            )
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
