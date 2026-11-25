package com.nava.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nava.music.data.DynamicPalette
import com.nava.music.data.Track
import com.nava.music.playback.PlayerUiState
import com.nava.music.ui.components.MiniPlayerBar
import com.nava.music.ui.components.TrackArtwork
import com.nava.music.ui.theme.LocalNavaPalette
import kotlinx.coroutines.flow.StateFlow

/**
 * Flagship v2.0 Library Screen:
 * - Clean 3-tab bottom navigation (Home / Songs / Favorites) with strict navigationBarsPadding
 * - Uncluttered editorial header (wordmark + Search toggle + Settings)
 * - Bento quick picks, frameless artwork cards, zero harsh list dividers
 */
@Composable
fun LibraryScreen(
    state: PlayerUiState,
    positionFlow: StateFlow<Long>,
    onRequestAudioPermission: () -> Unit,
    onRefresh: () -> Unit,
    onSearchChange: (String) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalNavaPalette.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Home, 1=Songs, 2=Favorites
    var searchExpanded by remember { mutableStateOf(false) }

    val favoriteTracks = remember(state.tracks, state.favorites) {
        state.tracks.filter { it.id in state.favorites }
    }

    val searchResults = remember(state.tracks, state.searchQuery) {
        val q = state.searchQuery.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else state.tracks.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
    }

    val topArtists = remember(state.tracks) {
        state.tracks
            .filter { it.artist != "Unknown Artist" }
            .groupBy { it.artist }
            .entries
            .sortedByDescending { it.value.size }
            .take(8)
            .map { it.key to it.value }
    }

    val quickPicks = remember(state.tracks) { state.tracks.take(4) }
    val recentStrip = remember(state.tracks) {
        if (state.tracks.size > 4) state.tracks.drop(4).take(10) else state.tracks
    }

    // Reserve clean clearance above Bottom Navigation + optional MiniPlayer
    val bottomContentClearance = if (state.currentTrack != null) 168.dp else 94.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Minimal Editorial Header (Wordmark + ONLY 2 clean icons: Search & Settings)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val heading = when {
                        searchExpanded -> "جستجو"
                        selectedTab == 0 -> "Nava"
                        selectedTab == 1 -> "قطعات"
                        else -> "علاقه‌مندی‌ها"
                    }
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.displaySmall,
                        color = palette.onBackground
                    )
                    if (!searchExpanded && state.tracks.isNotEmpty()) {
                        val sub = when (selectedTab) {
                            2 -> "${favoriteTracks.size} قطعه برگزیده"
                            else -> "${state.tracks.size} قطعه در حافظه"
                        }
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onBackgroundMuted
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            searchExpanded = !searchExpanded
                            if (!searchExpanded) onSearchChange("")
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (searchExpanded) palette.onBackground else palette.surface)
                    ) {
                        Icon(
                            imageVector = if (searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = if (searchExpanded) palette.background else palette.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(palette.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = palette.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expandable Inline Search Bar
            AnimatedVisibility(
                visible = searchExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = palette.onBackgroundMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.searchQuery.isEmpty()) {
                            Text(
                                text = "نام آهنگ، خواننده یا آلبوم...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.onBackgroundMuted
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
            }

            // Body Content
            when {
                !state.hasAudioPermission -> {
                    PermissionPromptCard(palette = palette, onGrantClick = onRequestAudioPermission)
                }

                state.isLoadingLibrary -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.primary, strokeWidth = 2.5.dp)
                    }
                }

                state.tracks.isEmpty() -> {
                    EmptyLibraryCard(palette = palette, onRefresh = onRefresh)
                }

                searchExpanded && state.searchQuery.isNotBlank() -> {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 4.dp, bottom = bottomContentClearance),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(searchResults, key = { "search_${it.id}" }) { track ->
                            TrackRowItem(
                                track = track,
                                isCurrent = state.currentTrack?.id == track.id,
                                isPlaying = state.currentTrack?.id == track.id && state.isPlaying,
                                isFavorite = track.id in state.favorites,
                                palette = palette,
                                onClick = { onTrackClick(track, searchResults) },
                                onFavoriteClick = { onToggleFavorite(track.id) }
                            )
                        }
                    }
                }

                selectedTab == 0 -> {
                    // HOME TAB: Action Split + 2x2 Bento Quick Picks + Recent Strip + Top Artists
                    LazyColumn(
                        contentPadding = PaddingValues(top = 4.dp, bottom = bottomContentClearance),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Primary Expressive Split Actions (One Filled Primary + One Tonal Secondary)
                        item(key = "home_actions") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(palette.primary)
                                        .clickable(onClick = onShuffleAll)
                                        .padding(vertical = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = null,
                                            tint = palette.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "پخش تصادفی",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.onPrimary
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(palette.surface)
                                        .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
                                        .clickable(onClick = onPlayAll)
                                        .padding(vertical = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = palette.onBackground,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "پخش همه",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.onBackground
                                        )
                                    }
                                }
                            }
                        }

                        // 2x2 Bento Quick Picks Grid
                        if (quickPicks.size >= 2) {
                            item(key = "quick_picks") {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "پخش سریع",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.onBackground,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                                Column(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    quickPicks.chunked(2).forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowItems.forEach { track ->
                                                BentoQuickTile(
                                                    track = track,
                                                    isCurrent = state.currentTrack?.id == track.id,
                                                    palette = palette,
                                                    onClick = { onTrackClick(track, state.tracks) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (rowItems.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Recently Added Horizontal Showcase
                        if (recentStrip.isNotEmpty()) {
                            item(key = "recent_showcase") {
                                Spacer(modifier = Modifier.height(22.dp))
                                Text(
                                    text = "تازه‌اضافه‌شده",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.onBackground,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(recentStrip, key = { "rec_${it.id}" }) { track ->
                                        RecentShowcaseCard(
                                            track = track,
                                            isCurrent = state.currentTrack?.id == track.id,
                                            palette = palette,
                                            onClick = { onTrackClick(track, state.tracks) }
                                        )
                                    }
                                }
                            }
                        }

                        // Top Artists Strip
                        if (topArtists.isNotEmpty()) {
                            item(key = "top_artists") {
                                Spacer(modifier = Modifier.height(22.dp))
                                Text(
                                    text = "خوانندگان برتر",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.onBackground,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(topArtists, key = { it.first }) { (artist, artistTracks) ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(palette.surfaceElevated)
                                                .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    artistTracks.firstOrNull()?.let { first ->
                                                        onTrackClick(first, artistTracks)
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            TrackArtwork(
                                                track = artistTracks.first(),
                                                cornerRadius = 999.dp,
                                                decodeSizePx = 120,
                                                modifier = Modifier.size(38.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = artist,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = palette.onBackground,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${artistTracks.size} قطعه",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = palette.onBackgroundMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                selectedTab == 1 -> {
                    // SONGS TAB: Clean Full Track List (zero harsh dividers)
                    LazyColumn(
                        contentPadding = PaddingValues(top = 4.dp, bottom = bottomContentClearance),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.tracks, key = { it.id }, contentType = { "track_row" }) { track ->
                            val isCurrent = state.currentTrack?.id == track.id
                            TrackRowItem(
                                track = track,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && state.isPlaying,
                                isFavorite = track.id in state.favorites,
                                palette = palette,
                                onClick = { onTrackClick(track, state.tracks) },
                                onFavoriteClick = { onToggleFavorite(track.id) }
                            )
                        }
                    }
                }

                else -> {
                    // FAVORITES TAB
                    if (favoriteTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = 110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    tint = palette.onBackgroundMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "هنوز آهنگی به علاقه‌مندی‌ها اضافه نشده",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.onBackgroundMuted
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = bottomContentClearance),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(favoriteTracks, key = { "fav_${it.id}" }) { track ->
                                val isCurrent = state.currentTrack?.id == track.id
                                TrackRowItem(
                                    track = track,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && state.isPlaying,
                                    isFavorite = true,
                                    palette = palette,
                                    onClick = { onTrackClick(track, favoriteTracks) },
                                    onFavoriteClick = { onToggleFavorite(track.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Dock: Floating MiniPlayer + Clean Navigation Bar with strict navigationBarsPadding
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    palette.background.copy(alpha = 0.94f)
                )
                .navigationBarsPadding()
        ) {
            val current = state.currentTrack
            if (current != null) {
                MiniPlayerBar(
                    track = current,
                    isPlaying = state.isPlaying,
                    positionFlow = positionFlow,
                    durationMs = state.durationMs,
                    palette = palette,
                    onTogglePlay = onTogglePlayPause,
                    onNext = onSkipNext,
                    onExpand = onOpenNowPlaying
                )
            }

            // Sleek Floating Pill Bottom Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .shadow(
                        elevation = if (palette.isDark) 12.dp else 6.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color.Black.copy(alpha = 0.25f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.surfaceElevated)
                    .border(1.dp, palette.outline, RoundedCornerShape(22.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "خانه",
                    icon = Icons.Rounded.Home,
                    selected = selectedTab == 0 && !searchExpanded,
                    palette = palette,
                    onClick = {
                        searchExpanded = false
                        selectedTab = 0
                    },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    label = "قطعات",
                    icon = Icons.Rounded.MusicNote,
                    selected = selectedTab == 1 && !searchExpanded,
                    palette = palette,
                    onClick = {
                        searchExpanded = false
                        selectedTab = 1
                    },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    label = "علاقه‌مندی‌ها",
                    icon = Icons.Rounded.Favorite,
                    selected = selectedTab == 2 && !searchExpanded,
                    palette = palette,
                    onClick = {
                        searchExpanded = false
                        selectedTab = 2
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    palette: DynamicPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) palette.primary.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) palette.primary else palette.onBackgroundMuted,
            modifier = Modifier.size(19.dp)
        )
        if (selected) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BentoQuickTile(
    track: Track,
    isCurrent: Boolean,
    palette: DynamicPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrent) palette.primary.copy(alpha = 0.14f) else palette.surfaceElevated)
            .border(
                width = if (isCurrent) 1.dp else 0.6.dp,
                color = if (isCurrent) palette.primary.copy(alpha = 0.55f) else palette.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtwork(
            track = track,
            cornerRadius = 11.dp,
            decodeSizePx = 150,
            modifier = Modifier.size(46.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) palette.primary else palette.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.labelMedium,
                color = palette.onBackgroundMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentShowcaseCard(
    track: Track,
    isCurrent: Boolean,
    palette: DynamicPalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick)
    ) {
        TrackArtwork(
            track = track,
            cornerRadius = 18.dp,
            decodeSizePx = 300,
            modifier = Modifier
                .size(130.dp)
                .border(
                    width = if (isCurrent) 2.dp else 0.5.dp,
                    color = if (isCurrent) palette.primary else palette.outline,
                    shape = RoundedCornerShape(18.dp)
                )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isCurrent) palette.primary else palette.onBackground,
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
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrent) palette.primary.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            TrackArtwork(
                track = track,
                cornerRadius = 12.dp,
                decodeSizePx = 150,
                modifier = Modifier.size(50.dp)
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.48f)),
                    contentAlignment = Alignment.Center
                ) {
                    MiniEqualizerGlyph(color = Color.White)
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
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onBackgroundMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = palette.onBackgroundMuted
        )

        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) palette.primary else palette.onBackgroundMuted.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp)
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
        modifier = Modifier.height(16.dp)
    ) {
        listOf(b1, b2, b3).forEach { frac ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16 * frac).dp)
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
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surfaceElevated)
            .border(1.dp, palette.outline, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.LibraryMusic,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "دسترسی به موزیک‌های گوشی",
            style = MaterialTheme.typography.titleLarge,
            color = palette.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "برای نمایش و پخش فایل‌های صوتی و فعال‌سازی انیمیشن بیس زنده، مجوز دسترسی به فایل‌های صوتی را تایید کنید.",
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
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
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
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surfaceElevated)
            .border(1.dp, palette.outline, RoundedCornerShape(22.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.LibraryMusic,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(40.dp)
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
