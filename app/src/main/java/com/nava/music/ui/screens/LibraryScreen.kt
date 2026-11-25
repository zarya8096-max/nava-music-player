package com.nava.music.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nava.music.data.AudioEnergy
import com.nava.music.data.DynamicPalette
import com.nava.music.data.LibraryFilter
import com.nava.music.data.Track
import com.nava.music.playback.PlayerUiState
import com.nava.music.ui.components.MeshBackground
import com.nava.music.ui.components.MiniPlayerBar
import com.nava.music.ui.components.TrackArtwork
import com.nava.music.ui.theme.LocalNavaPalette

@Composable
fun LibraryScreen(
    state: PlayerUiState,
    energy: AudioEnergy,
    onRequestAudioPermission: () -> Unit,
    onRequestRecordPermission: () -> Unit,
    onRefresh: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterSelect: (LibraryFilter) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalNavaPalette.current

    val filteredTracks = remember(state.tracks, state.searchQuery, state.activeFilter, state.favorites) {
        val base = when (state.activeFilter) {
            LibraryFilter.ALL -> state.tracks
            LibraryFilter.FAVORITES -> state.tracks.filter { it.id in state.favorites }
            LibraryFilter.RECENT -> state.tracks.sortedByDescending { it.dateAddedSec }
            LibraryFilter.ARTISTS -> state.tracks.sortedBy { it.artist.lowercase() }
        }
        val q = state.searchQuery.trim().lowercase()
        if (q.isEmpty()) base else base.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
    }

    val recentHighlights = remember(state.tracks) {
        state.tracks.take(10)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshBackground(
            palette = palette,
            bassBoost = if (state.isPlaying) energy.subBass * 0.55f else 0f,
            intensity = 0.65f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Editorial Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Nava",
                        style = MaterialTheme.typography.displaySmall,
                        color = palette.onBackground
                    )
                    Text(
                        text = if (state.tracks.isEmpty()) {
                            "پخش‌کننده هوشمند موسیقی"
                        } else {
                            "${state.tracks.size} قطعه در کتابخانه"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.onBackgroundMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!state.hasRecordPermission) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(palette.primary.copy(alpha = 0.22f))
                                .border(1.dp, palette.primary.copy(alpha = 0.45f), RoundedCornerShape(50))
                                .clickable(onClick = onRequestRecordPermission)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = "Enable Live Bass",
                                    tint = palette.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "بیس زنده",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.onBackground
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = palette.onBackground
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = palette.onBackground
                        )
                    }
                }
            }

            // Glass Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.09f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = palette.onBackgroundMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (state.searchQuery.isEmpty()) {
                        Text(
                            text = "جستجوی آهنگ، خواننده یا آلبوم...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onBackgroundMuted.copy(alpha = 0.72f)
                        )
                    }
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.onBackground),
                        cursorBrush = SolidColor(palette.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = palette.onBackgroundMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onSearchChange("") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Pills + Shuffle All
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(listOf(palette.primary, palette.secondary))
                            )
                            .clickable(enabled = state.tracks.isNotEmpty(), onClick = onShuffleAll)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = null,
                                tint = palette.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "پخش تصادفی",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.onPrimary
                            )
                        }
                    }
                }

                items(LibraryFilter.entries) { filter ->
                    val selected = state.activeFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) palette.primary.copy(alpha = 0.28f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) palette.primary.copy(alpha = 0.75f)
                                else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { onFilterSelect(filter) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) palette.onBackground else palette.onBackgroundMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content Area
            when {
                !state.hasAudioPermission -> {
                    PermissionPromptCard(
                        palette = palette,
                        onGrantClick = onRequestAudioPermission
                    )
                }

                state.isLoadingLibrary -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = palette.primary)
                    }
                }

                state.tracks.isEmpty() -> {
                    EmptyLibraryCard(palette = palette, onRefresh = onRefresh)
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 0.dp,
                            end = 0.dp,
                            top = 4.dp,
                            bottom = if (state.currentTrack != null) 118.dp else 28.dp
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Vibrant Recent Carousel when browsing main list
                        if (state.searchQuery.isEmpty() && state.activeFilter == LibraryFilter.ALL && recentHighlights.size >= 3) {
                            item {
                                Text(
                                    text = "جدیدترین‌ها",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.onBackground,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(recentHighlights, key = { "recent_${it.id}" }) { track ->
                                        RecentTrackCard(
                                            track = track,
                                            isCurrent = state.currentTrack?.id == track.id,
                                            palette = palette,
                                            onClick = { onTrackClick(track, state.tracks) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "همه قطعات",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.onBackground,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )
                            }
                        }

                        items(filteredTracks, key = { it.id }) { track ->
                            val isCurrent = state.currentTrack?.id == track.id
                            TrackRowItem(
                                track = track,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && state.isPlaying,
                                isFavorite = track.id in state.favorites,
                                palette = palette,
                                onClick = { onTrackClick(track, filteredTracks) },
                                onFavoriteClick = { onToggleFavorite(track.id) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Mini Player
        val current = state.currentTrack
        if (current != null) {
            val progress = if (state.durationMs > 0L) {
                (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            MiniPlayerBar(
                track = current,
                isPlaying = state.isPlaying,
                progress = progress,
                palette = palette,
                onTogglePlay = onTogglePlayPause,
                onNext = onSkipNext,
                onExpand = onOpenNowPlaying,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun RecentTrackCard(
    track: Track,
    isCurrent: Boolean,
    palette: DynamicPalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(134.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isCurrent) palette.primary.copy(alpha = 0.24f)
                else Color.White.copy(alpha = 0.07f)
            )
            .border(
                width = 1.dp,
                color = if (isCurrent) palette.primary.copy(alpha = 0.7f)
                else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        TrackArtwork(
            track = track,
            cornerRadius = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(114.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.onBackgroundMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrackRowItem(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    palette: DynamicPalette,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isCurrent) palette.primary.copy(alpha = 0.18f) else Color.Transparent
            )
            .border(
                width = if (isCurrent) 1.dp else 0.dp,
                color = if (isCurrent) palette.primary.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            TrackArtwork(
                track = track,
                cornerRadius = 13.dp,
                modifier = Modifier.size(52.dp)
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    MiniEqualizerGlyph(color = palette.primary)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) palette.primary else palette.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${formatDuration(track.durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onBackgroundMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) palette.primary else palette.onBackgroundMuted.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MiniEqualizerGlyph(color: Color) {
    val infinite = rememberInfiniteTransition(label = "eqGlyph")
    val b1 by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val b2 by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val b3 by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), RepeatMode.Reverse),
        label = "b3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp)
    ) {
        listOf(b1, b2, b3).forEach { frac ->
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height((18 * frac).dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun PermissionPromptCard(
    palette: DynamicPalette,
    onGrantClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, palette.primary.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.LibraryMusic,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "دسترسی به موزیک‌های گوشی",
            style = MaterialTheme.typography.titleLarge,
            color = palette.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "برای نمایش و پخش فایل‌های صوتی و فعال‌سازی انیمیشن بیس زنده، لطفاً مجوز دسترسی به فایل‌های صوتی را تایید کنید.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.onBackgroundMuted
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onGrantClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.primary,
                contentColor = palette.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("فعال‌سازی دسترسی", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    palette: DynamicPalette,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.LibraryMusic,
            contentDescription = null,
            tint = palette.secondary,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "آهنگی در حافظه پیدا نشد",
            style = MaterialTheme.typography.titleLarge,
            color = palette.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "فایل‌های MP3 یا FLAC خود را در پوشه Music قرار دهید و دوباره بررسی کنید.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.onBackgroundMuted
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.primary,
                contentColor = palette.onPrimary
            )
        ) {
            Text("اسکن مجدد")
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}
