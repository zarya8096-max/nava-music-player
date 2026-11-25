package com.nava.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nava.music.data.Track
import com.nava.music.ui.theme.DynamicThemeEngine

/**
 * Memory-efficient album artwork renderer:
 * Decodes bitmaps strictly at [decodeSizePx] (e.g., 160px for list thumbnails, 512px for full cover)
 * to keep RAM usage minimal even with thousands of tracks.
 */
@Composable
fun TrackArtwork(
    track: Track,
    cornerRadius: Dp = 14.dp,
    decodeSizePx: Int = 256,
    modifier: Modifier = Modifier
) {
    val fallbackSeed = remember(track.id) {
        DynamicThemeEngine.fallbackSeedForKey("${track.artist}-${track.title}")
    }
    var loadFailed by remember(track.albumArtUri) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        fallbackSeed.primary,
                        fallbackSeed.tertiary,
                        fallbackSeed.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loadFailed) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                val r = size.minDimension * 0.40f
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = r,
                    center = center
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.18f),
                    radius = r * 0.55f,
                    center = center
                )
            }
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(22.dp)
            )
        } else {
            val context = LocalContext.current
            val request = remember(track.albumArtUri, decodeSizePx) {
                ImageRequest.Builder(context)
                    .data(track.albumArtUri)
                    .size(decodeSizePx, decodeSizePx)
                    .crossfade(180)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                onError = { loadFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
