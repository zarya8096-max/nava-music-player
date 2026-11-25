package com.nava.music.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nava.music.data.Track
import com.nava.music.playback.PlayerUiState
import com.nava.music.ui.components.MeshBackground
import com.nava.music.ui.components.TrackArtwork
import com.nava.music.ui.theme.LocalNavaPalette

@Composable
fun QueueSheet(
    state: PlayerUiState,
    onSelectTrack: (Track) -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalNavaPalette.current

    Box(modifier = Modifier.fillMaxSize()) {
        MeshBackground(palette = palette, animate = false, intensity = 0.50f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "صف پخش",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.onBackground
                    )
                    Text(
                        text = "${state.queue.size} قطعه در صف",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.onBackgroundMuted
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(palette.surface)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = palette.onBackground,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(state.queue, key = { idx, t -> "${idx}_${t.id}" }) { idx, track ->
                    val isCurrent = idx == state.currentIndex || track.id == state.currentTrack?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isCurrent) palette.primary.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { onSelectTrack(track) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackArtwork(
                            track = track,
                            cornerRadius = 10.dp,
                            decodeSizePx = 140,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.onBackgroundMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = formatDuration(track.durationMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onBackgroundMuted
                        )
                    }
                    HorizontalDivider(
                        color = palette.outline.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 68.dp)
                    )
                }
            }
        }
    }
}
