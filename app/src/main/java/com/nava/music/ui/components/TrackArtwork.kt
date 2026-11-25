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
 * Renders album artwork cleanly via Coil, with a rich procedural vinyl-gradient art
 * fallback whenever embedded cover art is absent.
 */
@Composable
fun TrackArtwork(
    track: Track,
    cornerRadius: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val fallbackPalette = remember(track.id) {
        DynamicThemeEngine.fallbackPaletteForSeed("${track.artist}-${track.title}")
    }
    var loadFailed by remember(track.albumArtUri) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        fallbackPalette.primary,
                        fallbackPalette.tertiary,
                        fallbackPalette.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loadFailed) {
            // Artistic geometric vinyl-groove fallback
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                val r = size.minDimension * 0.42f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.28f, size.height * 0.24f),
                        radius = size.maxDimension * 0.7f
                    )
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = r,
                    center = center
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.20f),
                    radius = r * 0.56f,
                    center = center
                )
            }
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(26.dp)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(track.albumArtUri)
                    .crossfade(300)
                    .build(),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                onError = { loadFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
