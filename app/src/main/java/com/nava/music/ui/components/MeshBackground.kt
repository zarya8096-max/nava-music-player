package com.nava.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.nava.music.data.DynamicPalette

/**
 * Cold studio backdrop: a tight overhead key light, a faint measuring grid that fades out
 * toward the bottom, and a vignette that pulls the eye to the centre.
 *
 * The grid is a single [Path] built once per size inside drawWithCache, so a palette change
 * costs a handful of draw calls instead of rebuilding ~30 lines every frame.
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val step = 36.dp.toPx()
                val fadeEnd = size.height * 0.70f
                val gridPath = Path().apply {
                    var y = step
                    while (y < fadeEnd) {
                        moveTo(0f, y)
                        lineTo(size.width, y)
                        y += step
                    }
                    var x = step / 2f
                    while (x < size.width) {
                        moveTo(x, 0f)
                        lineTo(x, fadeEnd)
                        x += step
                    }
                }
                val gridStroke = Stroke(width = 0.6.dp.toPx())
                val keyCenter = Offset(size.width * 0.5f, size.height * 0.10f)
                val keyRadius = size.maxDimension * 0.55f
                val rimCenter = Offset(size.width * 1.02f, size.height * 0.70f)
                val rimRadius = size.maxDimension * 0.42f
                val vignetteCenter = Offset(size.width / 2f, size.height * 0.45f)
                val vignetteRadius = size.maxDimension * 0.78f

                onDrawBehind {
                    drawRect(color = palette.background)

                    // Measuring grid, then a background-coloured veil that fades it out downward
                    drawPath(
                        path = gridPath,
                        color = palette.onBackground.copy(alpha = gridAlpha),
                        style = gridStroke
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(palette.background.copy(alpha = 0f), palette.background),
                            startY = 0f,
                            endY = fadeEnd
                        ),
                        size = androidx.compose.ui.geometry.Size(size.width, fadeEnd)
                    )

                    // Key light from above, narrow and cool
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.primary.copy(alpha = keyAlpha), Color.Transparent),
                            center = keyCenter,
                            radius = keyRadius
                        ),
                        radius = keyRadius,
                        center = keyCenter
                    )

                    // Faint rim light low on the right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.secondary.copy(alpha = rimAlpha), Color.Transparent),
                            center = rimCenter,
                            radius = rimRadius
                        ),
                        radius = rimRadius,
                        center = rimCenter
                    )

                    // Vignette
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = if (palette.isDark) 0.38f else 0.06f)
                            ),
                            center = vignetteCenter,
                            radius = vignetteRadius
                        )
                    )
                }
            }
    )
}
