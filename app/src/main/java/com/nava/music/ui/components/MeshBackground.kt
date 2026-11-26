package com.nava.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nava.music.data.DynamicPalette

/**
 * Cold studio backdrop: a tight overhead key light, a faint measuring grid that fades out
 * toward the bottom, and a vignette that pulls the eye to the centre.
 * Everything is static per palette, so there is zero per-frame cost.
 */
@Composable
fun MeshBackground(
    palette: DynamicPalette,
    animate: Boolean = false,
    intensity: Float = 1f,
    modifier: Modifier = Modifier
) {
    val keyAlpha = if (palette.isDark) 0.20f * intensity else 0.10f * intensity
    val rimAlpha = if (palette.isDark) 0.10f * intensity else 0.05f * intensity
    val gridAlpha = if (palette.isDark) 0.045f else 0.05f
    val gridColor = palette.onBackground

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = palette.background)

        // Key light from above, narrow and cool
        val keyCenter = Offset(size.width * 0.5f, size.height * 0.10f)
        val keyRadius = size.maxDimension * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.primary.copy(alpha = keyAlpha), Color.Transparent),
                center = keyCenter,
                radius = keyRadius
            ),
            radius = keyRadius,
            center = keyCenter
        )

        // Faint rim light low on the right, from the secondary swatch
        val rimCenter = Offset(size.width * 1.02f, size.height * 0.70f)
        val rimRadius = size.maxDimension * 0.42f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.secondary.copy(alpha = rimAlpha), Color.Transparent),
                center = rimCenter,
                radius = rimRadius
            ),
            radius = rimRadius,
            center = rimCenter
        )

        // Measuring grid: hairlines every 36dp, fading to nothing by 70% height
        val step = 36.dp.toPx()
        val fadeEnd = size.height * 0.70f
        var y = step
        while (y < fadeEnd) {
            val a = gridAlpha * (1f - y / fadeEnd)
            drawLine(
                color = gridColor.copy(alpha = a),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.6.dp.toPx()
            )
            y += step
        }
        var x = step / 2f
        while (x < size.width) {
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(gridColor.copy(alpha = gridAlpha), Color.Transparent),
                    startY = 0f,
                    endY = fadeEnd
                ),
                start = Offset(x, 0f),
                end = Offset(x, fadeEnd),
                strokeWidth = 0.6.dp.toPx()
            )
            x += step
        }

        // Vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (palette.isDark) 0.38f else 0.06f)),
                center = Offset(size.width / 2f, size.height * 0.45f),
                radius = size.maxDimension * 0.78f
            )
        )
    }
}
