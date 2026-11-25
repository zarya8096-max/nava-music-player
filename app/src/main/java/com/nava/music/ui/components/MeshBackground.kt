package com.nava.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nava.music.data.DynamicPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Refined, ultra-lightweight atmospheric backdrop supporting both Light and Dark themes.
 * When [animate] is false (e.g., on Library screen or while paused), uses zero animation frames.
 */
@Composable
fun MeshBackground(
    palette: DynamicPalette,
    animate: Boolean = false,
    intensity: Float = 1f,
    modifier: Modifier = Modifier
) {
    val phase = if (animate) {
        val infinite = rememberInfiniteTransition(label = "meshDrift")
        val p by infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "meshPhase"
        )
        p
    } else {
        0.4f
    }

    val alphaScale = if (palette.isDark) intensity else intensity * 0.52f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(color = palette.background)

        val c1 = Offset(
            x = w * (0.22f + 0.12f * cos(phase)),
            y = h * (0.18f + 0.06f * sin(phase))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.primary.copy(alpha = 0.28f * alphaScale),
                    palette.primary.copy(alpha = 0.06f * alphaScale),
                    Color.Transparent
                ),
                center = c1,
                radius = w * 0.85f
            ),
            radius = w * 0.85f,
            center = c1
        )

        val c2 = Offset(
            x = w * (0.82f + 0.10f * sin(phase * 0.8f)),
            y = h * (0.48f + 0.08f * cos(phase * 0.9f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.secondary.copy(alpha = 0.24f * alphaScale),
                    palette.secondary.copy(alpha = 0.05f * alphaScale),
                    Color.Transparent
                ),
                center = c2,
                radius = w * 0.78f
            ),
            radius = w * 0.78f,
            center = c2
        )
    }
}
