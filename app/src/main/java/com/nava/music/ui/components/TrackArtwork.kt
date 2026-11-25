package com.nava.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nava.music.data.AudioArtKey
import com.nava.music.data.Track
import com.nava.music.ui.theme.LocalNavaPalette
import com.nava.music.ui.theme.VazirmatnFamily
import kotlin.math.abs

/**
 * Refined album artwork component:
 * 1) Uses [AudioArtKey] -> [AudioArtFetcher] to extract real embedded ID3 cover art on Android 10–16
 * 2) When a file genuinely lacks cover art, renders a calm Swiss-minimalist matte slate monogram tile
 *    (zero loud bullseye gradients or generic music-note icons).
 */
@Composable
fun TrackArtwork(
    track: Track,
    cornerRadius: Dp = 12.dp,
    decodeSizePx: Int = 256,
    modifier: Modifier = Modifier
) {
    val palette = LocalNavaPalette.current
    var showMonogram by remember(track.id) { mutableStateOf(false) }

    val monogram = remember(track.title) {
        track.title.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "N"
    }

    // Subtle cool-slate tonal variation per track (never loud neon or bullseyes)
    val tileColors = remember(track.id, palette.isDark) {
        val h = abs(track.id.hashCode()) % 4
        if (palette.isDark) {
            when (h) {
                0 -> listOf(Color(0xFF17202D), Color(0xFF101620))
                1 -> listOf(Color(0xFF15242B), Color(0xFF0E171D))
                2 -> listOf(Color(0xFF1A2233), Color(0xFF111622))
                else -> listOf(Color(0xFF192329), Color(0xFF11181C))
            }
        } else {
            when (h) {
                0 -> listOf(Color(0xFFE6ECF3), Color(0xFFDCE4EE))
                1 -> listOf(Color(0xFFE3EEF0), Color(0xFFD6E6E9))
                2 -> listOf(Color(0xFFE8ECF5), Color(0xFFDCE2EF))
                else -> listOf(Color(0xFFE7ECEF), Color(0xFFDBE2E7))
            }
        }
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(tileColors))
            .border(0.6.dp, palette.outline, shape),
        contentAlignment = Alignment.Center
    ) {
        if (showMonogram) {
            Text(
                text = monogram,
                fontFamily = VazirmatnFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (decodeSizePx >= 360) 40.sp else 20.sp,
                color = palette.onBackground.copy(alpha = if (palette.isDark) 0.26f else 0.24f)
            )
        } else {
            val context = LocalContext.current
            val request = remember(track.contentUri, track.albumArtUri, decodeSizePx) {
                ImageRequest.Builder(context)
                    .data(AudioArtKey(track.contentUri, track.albumArtUri, decodeSizePx))
                    .crossfade(160)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                onError = { showMonogram = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
