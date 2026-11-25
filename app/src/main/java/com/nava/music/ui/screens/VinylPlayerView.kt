package com.nava.music.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * Full-screen Hi-Fi Turntable / Vinyl Immersion Mode:
 * - Supports both Portrait (phone) and Landscape (split-stage) layouts automatically
 * - Atmospheric scene presets: Aurora, Cosmos, Gallery
 * - Unified frosted glass control capsule, vertical-tick scrubber, and Up Next pill
 */
@Composable
fun VinylPlayerView(
    state: PlayerUiState,
    energy: AudioEnergy,
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
        // Atmospheric Backdrop tailored to Aurora / Cosmos / Gallery
        VinylAtmosphereBackdrop(
            palette = palette,
            atmosphere = atmosphere,
            bassBoost = if (state.isPlaying) energy.subBass else 0f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Top Studio Status & Atmosphere Switcher Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: NOW SPINNING badge + Mode Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NowSpinningBadge(
                        isPlaying = state.isPlaying,
                        accent = palette.secondary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
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

                // Right: Aurora / Cosmos / Gallery pill + Close button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        VinylAtmosphere.entries.forEach { preset ->
                            val selected = atmosphere == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) palette.secondary.copy(alpha = 0.30f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (selected) 1.dp else 0.dp,
                                        color = if (selected) palette.secondary.copy(alpha = 0.65f) else Color.Transparent,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable { onSelectAtmosphere(preset) }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = preset.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) Color.White else palette.onBackgroundMuted
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.09f))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
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

            // Responsive Stage: Landscape split-screen vs Portrait vertical immersion
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val isLandscape = maxWidth > maxHeight
                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            VinylTurntableStage(
                                track = track,
                                energy = energy,
                                palette = palette,
                                isPlaying = state.isPlaying,
                                atmosphere = atmosphere
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            VinylControlsColumn(
                                track = track,
                                state = state,
                                energy = energy,
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
                                energy = energy,
                                palette = palette,
                                isPlaying = state.isPlaying,
                                atmosphere = atmosphere
                            )
                        }

                        VinylControlsColumn(
                            track = track,
                            state = state,
                            energy = energy,
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
    energy: AudioEnergy,
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
    val playScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f + energy.beatPulse * 0.06f else 1f,
        animationSpec = spring(dampingRatio = 0.68f),
        label = "vinylPlayScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Editorial Track Metadata Block
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
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
                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.09f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                        .padding(horizontal = 11.dp, vertical = 4.dp)
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

        // Studio Vertical-Tick Waveform Scrubber
        TickWaveformScrubber(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            isPlaying = state.isPlaying,
            bassPulse = energy.beatPulse,
            palette = palette,
            onSeekFraction = { frac -> onSeekTo((frac * state.durationMs).toLong()) }
        )

        // Unified Frosted Glass Transport Capsule (Shuffle | Prev | Play | Next | Repeat | Heart)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CapsuleSmallButton(
                active = state.shuffleEnabled,
                accent = palette.secondary,
                onClick = onToggleShuffle
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffleEnabled) palette.secondary else palette.onBackgroundMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            CapsuleSmallButton(
                active = false,
                accent = palette.secondary,
                onClick = onSkipPrevious
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Warm Glowing Center Play/Pause Disc
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        scaleX = playScale
                        scaleY = playScale
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        ambientColor = palette.secondary,
                        spotColor = palette.secondary
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF3D1),
                                palette.secondary
                            )
                        )
                    )
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color(0xFF17131E),
                    modifier = Modifier.size(28.dp)
                )
            }

            CapsuleSmallButton(
                active = false,
                accent = palette.secondary,
                onClick = onSkipNext
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            CapsuleSmallButton(
                active = state.repeatMode != PlayerRepeatMode.OFF,
                accent = palette.secondary,
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
                    tint = if (state.repeatMode != PlayerRepeatMode.OFF) palette.secondary else palette.onBackgroundMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            CapsuleSmallButton(
                active = isFavorite,
                accent = Color(0xFFFF5C7A),
                onClick = { onToggleFavorite(track.id) }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFF5C7A) else palette.onBackgroundMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Bottom Row: UP NEXT Pill + AI Lyrics Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (upNextTrack != null) {
                Row(
                    modifier = Modifier
                        .weight(1.25f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenQueue)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackArtwork(
                        track = upNextTrack,
                        cornerRadius = 9.dp,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UP NEXT",
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.secondary
                        )
                        Text(
                            text = upNextTrack.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                palette.primary.copy(alpha = 0.28f),
                                palette.secondary.copy(alpha = 0.22f)
                            )
                        )
                    )
                    .border(1.dp, palette.secondary.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenLyrics)
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = palette.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "لیریک و ترجمه AI",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
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
    accent: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                if (active) accent.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = 1.dp,
                color = if (active) accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.12f),
                shape = CircleShape
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
    accent: Color
) {
    val infinite = rememberInfiniteTransition(label = "spinDot")
    val dotAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    if (isPlaying) accent.copy(alpha = dotAlpha)
                    else Color.White.copy(alpha = 0.35f)
                )
        )
        Text(
            text = if (isPlaying) "NOW SPINNING" else "PAUSED",
            fontSize = 10.sp,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun VinylAtmosphereBackdrop(
    palette: DynamicPalette,
    atmosphere: VinylAtmosphere,
    bassBoost: Float
) {
    when (atmosphere) {
        VinylAtmosphere.AURORA -> {
            MeshBackground(
                palette = palette,
                bassBoost = bassBoost * 0.9f,
                intensity = 0.92f
            )
        }

        VinylAtmosphere.COSMOS -> {
            Box(modifier = Modifier.fillMaxSize()) {
                MeshBackground(
                    palette = palette.copy(
                        deepBackground = Color(0xFF07070E)
                    ),
                    bassBoost = bassBoost * 0.7f,
                    intensity = 0.65f
                )
                // Subtle drifting starfield dust
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 0 until 36) {
                        val sx = ((i * 197 + 43) % 100) / 100f * size.width
                        val sy = ((i * 313 + 19) % 100) / 100f * size.height
                        val r = if (i % 5 == 0) 2.0.dp.toPx() else 1.1.dp.toPx()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.16f + (i % 4) * 0.09f),
                            radius = r,
                            center = Offset(sx, sy)
                        )
                    }
                }
            }
        }

        VinylAtmosphere.GALLERY -> {
            // Minimalist dark studio plinth with subtle top spotlight
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color(0xFF0E0D12))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.primary.copy(alpha = 0.22f + bassBoost * 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.34f),
                        radius = size.maxDimension * 0.65f
                    ),
                    radius = size.maxDimension * 0.65f,
                    center = Offset(size.width * 0.5f, size.height * 0.34f)
                )
            }
        }
    }
}
