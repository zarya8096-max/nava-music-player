package com.nava.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nava.music.data.DynamicPalette

/**
 * Clean, architectural studio-light backdrop — zero lava-lamp blobs, zero per-frame CPU overhead.
 * Subtle cool overhead wash tinted softly by the active track's palette.
 */
@Composable
fun MeshBackground(
    palette: DynamicPalette,
    animate: Boolean = false,
    intensity: Float = 1f,
    modifier: Modifier = Modifier
) {
    val topAlpha = if (palette.isDark) 0.22f * intensity else 0.11f * intensity
    val sideAlpha = if (palette.isDark) 0.14f * intensity else 0.07f * intensity

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = palette.background)

        // Subtle overhead architectural spotlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.primary.copy(alpha = topAlpha),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.12f),
                radius = size.maxDimension * 0.68f
            ),
            radius = size.maxDimension * 0.68f,
            center = Offset(size.width * 0.5f, size.height * 0.12f)
        )

        // Secondary cool edge wash
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.secondary.copy(alpha = sideAlpha),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.88f, size.height * 0.62f),
                radius = size.maxDimension * 0.52f
            ),
            radius = size.maxDimension * 0.52f,
            center = Offset(size.width * 0.88f, size.height * 0.62f)
        )
    }
}
