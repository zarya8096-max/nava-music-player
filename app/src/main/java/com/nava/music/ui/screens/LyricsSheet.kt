package com.nava.music.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nava.music.playback.PlayerUiState
import com.nava.music.ui.components.MeshBackground
import com.nava.music.ui.theme.LocalNavaPalette

@Composable
fun LyricsSheet(
    state: PlayerUiState,
    onClose: () -> Unit,
    onFetchLyrics: (Boolean) -> Unit,
    onTranslateLyrics: () -> Unit,
    onToggleTranslation: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val track = state.currentTrack ?: return
    val palette = LocalNavaPalette.current
    val lyricsState = state.lyricsState
    val payload = lyricsState.payload
    val listState = rememberLazyListState()

    LaunchedEffect(track.id) {
        if (lyricsState.payload == null && !lyricsState.isLoading) {
            onFetchLyrics(false)
        }
    }

    val activeLineIndex = remember(payload, state.positionMs) {
        if (payload == null || !payload.isSynced) -1
        else payload.lines.indexOfLast { line ->
            val ts = line.timestampMs
            ts != null && ts <= state.positionMs + 250L
        }
    }

    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0) {
            val target = (activeLineIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshBackground(palette = palette, animate = false, intensity = 0.55f)

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(track.artist)
                            payload?.sourceLabel?.let { append(" • $it") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.onBackgroundMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { onFetchLyrics(true) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(palette.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Lyrics",
                            tint = palette.onBackground,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(palette.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "AI Settings",
                            tint = palette.onBackground,
                            modifier = Modifier.size(19.dp)
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
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasTranslation = payload?.lines?.any { !it.translation.isNullOrBlank() } == true

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.primary)
                        .clickable(
                            enabled = payload != null && !lyricsState.isTranslating && !lyricsState.isLoading,
                            onClick = onTranslateLyrics
                        )
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (lyricsState.isTranslating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = palette.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "در حال ترجمه با هوش مصنوعی...",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.onPrimary
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Translate,
                                contentDescription = null,
                                tint = palette.onPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = if (hasTranslation) "ترجمه مجدد (${state.aiConfig.targetLanguage})"
                                else "ترجمه با AI به ${state.aiConfig.targetLanguage}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.onPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (hasTranslation) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (lyricsState.showTranslation) palette.onBackground
                                else palette.surface
                            )
                            .border(1.dp, palette.outline, RoundedCornerShape(14.dp))
                            .clickable(onClick = onToggleTranslation)
                            .padding(horizontal = 13.dp, vertical = 11.dp)
                    ) {
                        Text(
                            text = if (lyricsState.showTranslation) "نمایش ترجمه" else "فقط متن اصلی",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (lyricsState.showTranslation) palette.background else palette.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    lyricsState.isLoading -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = palette.primary, strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "در حال جستجو و استخراج لیریک...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.onBackgroundMuted
                            )
                        }
                    }

                    lyricsState.errorMessage != null && payload == null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(22.dp))
                                .background(palette.surfaceElevated)
                                .border(1.dp, palette.outline, RoundedCornerShape(22.dp))
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = lyricsState.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.onBackground
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onOpenSettings,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primary,
                                        contentColor = palette.onPrimary
                                    )
                                ) {
                                    Text("تنظیم کلید API")
                                }
                                Button(
                                    onClick = { onFetchLyrics(true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.surface,
                                        contentColor = palette.onBackground
                                    )
                                ) {
                                    Text("تلاش مجدد")
                                }
                            }
                        }
                    }

                    payload != null -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (lyricsState.errorMessage != null) {
                                Text(
                                    text = lyricsState.errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(palette.surfaceElevated)
                                        .padding(12.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(vertical = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(payload.lines) { idx, line ->
                                    val isActive = payload.isSynced && idx == activeLineIndex
                                    val textColor by animateColorAsState(
                                        targetValue = when {
                                            !payload.isSynced -> palette.onBackground
                                            isActive -> palette.onBackground
                                            else -> palette.onBackgroundMuted.copy(alpha = 0.52f)
                                        },
                                        animationSpec = tween(240),
                                        label = "lyricColor"
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isActive) palette.primary.copy(alpha = 0.14f)
                                                else Color.Transparent
                                            )
                                            .clickable(enabled = line.timestampMs != null) {
                                                line.timestampMs?.let(onSeekTo)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = line.original,
                                            fontSize = if (isActive) 21.sp else 18.sp,
                                            lineHeight = if (isActive) 30.sp else 26.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor
                                        )

                                        if (lyricsState.showTranslation && !line.translation.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = line.translation,
                                                fontSize = if (isActive) 16.sp else 14.sp,
                                                lineHeight = 23.sp,
                                                color = if (isActive) palette.primary else palette.primary.copy(alpha = 0.72f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
