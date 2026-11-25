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
 * Lightweight GPU-accelerated living mesh backdrop tinted by the active song's palette.
 * Gently expands and brightens with sub-bass energy without expensive software blurs.
 */
@Composable
fun MeshBackground(
    palette: DynamicPalette,
    bassBoost: Float = 0f,
    intensity: Float = 1f,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "meshDrift")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "meshPhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val bassScale = 1f + bassBoost * 0.24f

        // Deep tinted base canvas
        drawRect(color = palette.deepBackground)

        // Orb 1: Primary vibrant glow drifting near top-left
        val c1 = Offset(
            x = w * (0.24f + 0.14f * cos(phase)),
            y = h * (0.20f + 0.08f * sin(phase * 1.1f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.primary.copy(alpha = (0.42f + bassBoost * 0.16f) * intensity),
                    palette.primary.copy(alpha = 0.12f * intensity),
                    Color.Transparent
                ),
                center = c1,
                radius = w * 0.85f * bassScale
            ),
            radius = w * 0.85f * bassScale,
            center = c1
        )

        // Orb 2: Secondary luminous accent drifting near mid-right
        val c2 = Offset(
            x = w * (0.82f + 0.12f * sin(phase * 0.85f)),
            y = h * (0.44f + 0.10f * cos(phase * 0.9f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.secondary.copy(alpha = (0.34f + bassBoost * 0.14f) * intensity),
                    palette.secondary.copy(alpha = 0.08f * intensity),
                    Color.Transparent
                ),
                center = c2,
                radius = w * 0.78f * bassScale
            ),
            radius = w * 0.78f * bassScale,
            center = c2
        )

        // Orb 3: Deep atmospheric tertiary anchor at bottom
        val c3 = Offset(
            x = w * (0.42f + 0.16f * cos(phase * 0.7f + 1.5f)),
            y = h * (0.84f + 0.06f * sin(phase))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.tertiary.copy(alpha = (0.48f + bassBoost * 0.12f) * intensity),
                    Color.Transparent
                ),
                center = c3,
                radius = w * 0.92f
            ),
            radius = w * 0.92f,
            center = c3
        )
    }
}
