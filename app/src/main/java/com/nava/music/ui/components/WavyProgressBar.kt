package com.nava.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nava.music.data.DynamicPalette
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tactile expressive wavy scrubber:
 * - Animated living sine wave on the played portion while playing
 * - Gently amplifies wave height with mid-bass energy
 * - Smoothly flattens to a crisp line when paused
 */
@Composable
fun WavyProgressBar(
    progress: Float,
    isPlaying: Boolean,
    bassAmplitude: Float,
    palette: DynamicPalette,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(progress) }
    val displayFraction = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    val infinite = rememberInfiniteTransition(label = "waveShift")
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val waveHeightFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 360),
        label = "waveFlatten"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeekFraction(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeekFraction(dragFraction)
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
    ) {
        val midY = size.height / 2f
        val thumbX = (size.width * displayFraction).coerceIn(6.dp.toPx(), size.width - 6.dp.toPx())

        // Remaining unplayed track
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(thumbX, midY),
            end = Offset(size.width, midY),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Played wavy path
        val amplitude = (3.4.dp.toPx() + bassAmplitude * 2.6.dp.toPx()) * waveHeightFactor
        val wavelength = 28.dp.toPx()
        val path = Path()
        val step = 3f
        var x = 0f
        path.moveTo(0f, midY)
        while (x <= thumbX) {
            val y = midY + sin((x / wavelength) * (2f * PI.toFloat()) - wavePhase) * amplitude
            path.lineTo(x, y)
            x += step
        }
        path.lineTo(thumbX, midY)

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(palette.primary, palette.secondary)
            ),
            style = Stroke(width = 3.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Tactile glowing pill thumb
        val thumbRadius = if (isDragging) 8.5.dp.toPx() else 6.5.dp.toPx()
        drawCircle(
            color = palette.secondary.copy(alpha = 0.35f),
            radius = thumbRadius * 1.75f,
            center = Offset(thumbX, midY)
        )
        drawCircle(
            color = Color.White,
            radius = thumbRadius,
            center = Offset(thumbX, midY)
        )
    }
}
