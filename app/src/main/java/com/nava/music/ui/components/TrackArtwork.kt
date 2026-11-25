package com.nava.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nava.music.data.ArtCacheManager
import com.nava.music.data.Track
import com.nava.music.ui.theme.LocalNavaPalette
import com.nava.music.ui.theme.VazirmatnFamily
import kotlin.math.abs

/**
 * Zero-lag album artwork component:
 * - Reads ArtCacheManager synchronously on composition (0ms hit for cached covers & missing covers)
 * - Cancels off-screen coroutine work automatically during fast fling
 * - Uses solid matte slate surface (zero per-row gradient shaders or Coil painter allocations)
 */
@Composable
fun TrackArtwork(
    track: Track,
    cornerRadius: Dp = 12.dp,
    decodeSizePx: Int = 150,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalNavaPalette.current
    val isFullSize = decodeSizePx >= 360

    val initialBitmap = remember(track.id, isFullSize) {
        if (isFullSize) {
            ArtCacheManager.getCachedFullCover(track.id) ?: ArtCacheManager.getCachedThumbnail(track.id)
        } else {
            ArtCacheManager.getCachedThumbnail(track.id)
        }
    }
    val knownMissing = remember(track.id, isFullSize) {
        !isFullSize && ArtCacheManager.isKnownMissing(track.id)
    }

    var bitmap by remember(track.id, isFullSize) { mutableStateOf(initialBitmap) }

    LaunchedEffect(track.id, isFullSize) {
        if (isFullSize) {
            if (ArtCacheManager.getCachedFullCover(track.id) == null) {
                val loaded = ArtCacheManager.loadFullCover(context, track)
                if (loaded != null) bitmap = loaded
            }
        } else if (bitmap == null && !knownMissing) {
            val loaded = ArtCacheManager.loadThumbnail(context, track)
            if (loaded != null) bitmap = loaded
        }
    }

    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val bgColor = remember(track.id, palette.isDark) {
        val h = abs(track.id.hashCode()) % 4
        if (palette.isDark) {
            when (h) {
                0 -> Color(0xFF161E2B)
                1 -> Color(0xFF142128)
                2 -> Color(0xFF192030)
                else -> Color(0xFF172026)
            }
        } else {
            when (h) {
                0 -> Color(0xFFE3EAF2)
                1 -> Color(0xFFDFECEF)
                2 -> Color(0xFFE5EAF4)
                else -> Color(0xFFE4EAEF)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        val img = bitmap
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val monogram = remember(track.title) {
                track.title.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "N"
            }
            Text(
                text = monogram,
                fontFamily = VazirmatnFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (isFullSize) 40.sp else 19.sp,
                color = palette.onBackground.copy(alpha = if (palette.isDark) 0.25f else 0.22f)
            )
        }
    }
}
