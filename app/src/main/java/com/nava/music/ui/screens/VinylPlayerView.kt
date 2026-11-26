package com.nava.music.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nava.music.data.AudioEnergy
import com.nava.music.data.DynamicPalette
import com.nava.music.data.RepeatMode as PlayerRepeatMode
import com.nava.music.data.Track
import com.nava.music.data.VinylAtmosphere
import com.nava.music.playback.PlayerUiState
import com.nava.music.ui.components.MeshBackground
import com.nava.music.ui.components.TickWaveformScrubber
import com.nava.music.ui.components.TrackArtwork
import com.nava.music.ui.components.VinylTurntableStage
import com.nava.music.ui.theme.LocalNavaPalette
import kotlinx.coroutines.flow.StateFlow

@Composable
fun VinylPlayerView(
    state: PlayerUiState,
    positionFlow: StateFlow<Long>,
    energyFlow: StateFlow<AudioEnergy>,
    onCollapse: () -> Unit,
    onSwitchToModern: () -> Unit,
    onSelectAtmosphere: (VinylAtmosphere) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val track = state.currentTrack ?: return
    val palette = LocalNavaPalette.current
    val isFavorite = track.id in state.favorites
    val atmosphere = state.vinylAtmosphere

    val upNextTrack = remember(state.queue, state.currentIndex) {
        if (state.queue.size <= 1) null
        else state.queue.getOrNull((state.currentIndex + 1) % state.queue.size)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VinylAtmosphereBackdrop(
            palette = palette,
            atmosphere = atmosphere,
            isPlaying = state.isPlaying
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NowSpinningBadge(
                        isPlaying = state.isPlaying,
                        palette = palette
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.surface)
                            .border(1.dp, palette.outline, RoundedCornerShape(50))
                            .clickable(onClick = onSwitchToModern)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Modern Mode",
                                tint = palette.onBackgroundMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "مدرن",
                                fontSize = 11.sp,
                                color = palette.onBackgroundMuted
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.surface)
                            .border(1.dp, palette.outline, RoundedCornerShape(50))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        VinylAtmosphere.entries.forEach { preset ->
                            val selected = atmosphere == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) palette.onBackground
                                        else Color.Transparent
                                    )
                                    .clickable { onSelectAtmosphere(preset) }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = preset.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) palette.background else palette.onBackgroundMuted
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(palette.surface)
                            .border(1.dp, palette.outline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = palette.onBackground,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val isLandscape = maxWidth > maxHeight
                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            VinylTurntableStage(
                                track = track,
                                energyFlow = energyFlow,
                                palette = palette,
                                isPlaying = state.isPlaying,
                                atmosphere = atmosphere
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            VinylControlsColumn(
                                track = track,
                                state = state,
                                positionFlow = positionFlow,
                                energyFlow = energyFlow,
                                palette = palette,
                                isFavorite = isFavorite,
                                upNextTrack = upNextTrack,
                                onTogglePlayPause = onTogglePlayPause,
                                onSkipNext = onSkipNext,
                                onSkipPrevious = onSkipPrevious,
                                onSeekTo = onSeekTo,
                                onToggleShuffle = onToggleShuffle,
                                onCycleRepeat = onCycleRepeat,
                                onToggleFavorite = onToggleFavorite,
                                onOpenLyrics = onOpenLyrics,
                                onOpenQueue = onOpenQueue
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            VinylTurntableStage(
                                track = track,
                                energyFlow = energyFlow,
                                palette = palette,
                                isPlaying = state.isPlaying,
                                atmosphere = atmosphere
                            )
                        }

                        VinylControlsColumn(
                            track = track,
                            state = state,
                            positionFlow = positionFlow,
                            energyFlow = energyFlow,
                            palette = palette,
                            isFavorite = isFavorite,
                            upNextTrack = upNextTrack,
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipNext = onSkipNext,
                            onSkipPrevious = onSkipPrevious,
                            onSeekTo = onSeekTo,
                            onToggleShuffle = onToggleShuffle,
                            onCycleRepeat = onCycleRepeat,
                            onToggleFavorite = onToggleFavorite,
                            onOpenLyrics = onOpenLyrics,
                            onOpenQueue = onOpenQueue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VinylControlsColumn(
    track: Track,
    state: PlayerUiState,
    positionFlow: StateFlow<Long>,
    energyFlow: StateFlow<AudioEnergy>,
    palette: DynamicPalette,
    isFavorite: Boolean,
    upNextTrack: Track?,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.displaySmall,
                color = palette.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.titleMedium,
                color = palette.onBackgroundMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (track.album.isNotBlank() && track.album != "Single") {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.surface)
                        .border(1.dp, palette.outline, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = track.album,
                        fontSize = 11.sp,
                        color = palette.onBackgroundMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        val positionMs by positionFlow.collectAsState()
        TickWaveformScrubber(
            positionMs = positionMs,
            durationMs = state.durationMs,
            isPlaying = state.isPlaying,
            energyFlow = energyFlow,
            palette = palette,
            onSeekFraction = { frac -> onSeekTo((frac * state.durationMs).toLong()) }
        )

        // Unified Studio Control Capsule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(palette.surfaceElevated)
                .border(1.dp, palette.outline, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CapsuleSmallButton(
                active = state.shuffleEnabled,
                palette = palette,
                onClick = onToggleShuffle
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffleEnabled) palette.primary else palette.onBackgroundMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            CapsuleSmallButton(active = false, palette = palette, onClick = onSkipPrevious) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = palette.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f),
                        spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f)
                    )
                    .clip(CircleShape)
                    .background(com.nava.music.ui.components.primaryFill(palette))
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = palette.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            CapsuleSmallButton(active = false, palette = palette, onClick = onSkipNext) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = palette.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }

            CapsuleSmallButton(
                active = state.repeatMode != PlayerRepeatMode.OFF,
                palette = palette,
                onClick = onCycleRepeat
            ) {
                val icon = if (state.repeatMode == PlayerRepeatMode.ONE) {
                    Icons.Rounded.RepeatOne
                } else {
                    Icons.Rounded.Repeat
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode != PlayerRepeatMode.OFF) palette.primary else palette.onBackgroundMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            CapsuleSmallButton(
                active = isFavorite,
                palette = palette,
                onClick = { onToggleFavorite(track.id) }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) palette.primary else palette.onBackgroundMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Bottom Row: UP NEXT + AI Lyrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (upNextTrack != null) {
                Row(
                    modifier = Modifier
                        .weight(1.25f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenQueue)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackArtwork(
                        track = upNextTrack,
                        cornerRadius = 8.dp,
                        decodeSizePx = 128,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UP NEXT",
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                        Text(
                            text = upNextTrack.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.surfaceElevated)
                    .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenLyrics)
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "لیریک و ترجمه AI",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CapsuleSmallButton(
    active: Boolean,
    palette: DynamicPalette,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                if (active) palette.primary.copy(alpha = 0.14f)
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun NowSpinningBadge(
    isPlaying: Boolean,
    palette: DynamicPalette
) {
    val dotAlpha = if (isPlaying) {
        val infinite = rememberInfiniteTransition(label = "spinDot")
        val a by infinite.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotAlpha"
        )
        a
    } else {
        0.35f
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    if (isPlaying) palette.primary.copy(alpha = dotAlpha)
                    else palette.onBackgroundMuted.copy(alpha = 0.45f)
                )
        )
        Text(
            text = if (isPlaying) "NOW SPINNING" else "PAUSED",
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.onBackground
        )
    }
}

@Composable
private fun VinylAtmosphereBackdrop(
    palette: DynamicPalette,
    atmosphere: VinylAtmosphere,
    isPlaying: Boolean
) {
    when (atmosphere) {
        VinylAtmosphere.AURORA -> {
            MeshBackground(palette = palette, animate = isPlaying, intensity = 0.85f)
        }

        VinylAtmosphere.COSMOS -> {
            Box(modifier = Modifier.fillMaxSize()) {
                MeshBackground(
                    palette = palette.copy(
                        background = if (palette.isDark) Color(0xFF07070D) else palette.background
                    ),
                    animate = isPlaying,
                    intensity = 0.65f
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val starColor = if (palette.isDark) Color.White else palette.primary
                    for (i in 0 until 28) {
                        val sx = ((i * 197 + 43) % 100) / 100f * size.width
                        val sy = ((i * 313 + 19) % 100) / 100f * size.height
                        val r = if (i % 5 == 0) 1.8.dp.toPx() else 1.0.dp.toPx()
                        drawCircle(
                            color = starColor.copy(alpha = if (palette.isDark) 0.24f else 0.16f),
                            radius = r,
                            center = Offset(sx, sy)
                        )
                    }
                }
            }
        }

        VinylAtmosphere.GALLERY -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = palette.background)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.primary.copy(alpha = if (palette.isDark) 0.16f else 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.34f),
                        radius = size.maxDimension * 0.60f
                    ),
                    radius = size.maxDimension * 0.60f,
                    center = Offset(size.width * 0.5f, size.height * 0.34f)
                )
            }
        }
    }
}
