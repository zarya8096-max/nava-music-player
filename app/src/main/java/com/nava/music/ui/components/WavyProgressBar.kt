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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nava.music.data.AudioEnergy
import com.nava.music.data.DynamicPalette
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavyProgressBar(
    progress: Float,
    isPlaying: Boolean,
    energyFlow: StateFlow<AudioEnergy>,
    palette: DynamicPalette,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val energyState = energyFlow.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(progress) }
    val displayFraction = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    // Phase lives in a State that is only read in the draw lambda; the frame clock only runs
    // while playing, so a paused player schedules no frames at all.
    val wavePhaseState = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        val twoPi = (2f * PI).toFloat()
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = (now - lastNanos) / 1_000_000_000f
                    wavePhaseState.floatValue = (wavePhaseState.floatValue + dt * twoPi / 1.5f) % twoPi
                }
                lastNanos = now
            }
        }
    }

    val waveHeightFactorState = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "waveFlatten"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
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
                    onDragCancel = { isDragging = false }
                )
            }
    ) {
        val midY = size.height / 2f
        val thumbX = (size.width * displayFraction).coerceIn(6.dp.toPx(), size.width - 6.dp.toPx())
        val bassAmp = if (isPlaying) energyState.value.midBass else 0f
        val wavePhase = wavePhaseState.floatValue
        val waveHeightFactor = waveHeightFactorState.value

        // Remaining track: a ruler of fine ticks instead of a flat bar
        val tickGap = 7.dp.toPx()
        val tickColor = palette.onBackground.copy(alpha = if (palette.isDark) 0.26f else 0.22f)
        var tx = thumbX + tickGap
        var tickIndex = 0
        while (tx <= size.width) {
            val major = tickIndex % 5 == 4
            val h = if (major) 7.dp.toPx() else 4.dp.toPx()
            drawLine(
                color = if (major) tickColor else tickColor.copy(alpha = tickColor.alpha * 0.65f),
                start = Offset(tx, midY - h / 2f),
                end = Offset(tx, midY + h / 2f),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
            tx += tickGap
            tickIndex++
        }

        val amplitude = (2.6.dp.toPx() + bassAmp * 2.2.dp.toPx()) * waveHeightFactor
        val wavelength = 28.dp.toPx()
        val path = Path()
        val step = 4f
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
            brush = Brush.horizontalGradient(listOf(palette.primary, palette.secondary)),
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Needle thumb: a slim vertical blade, wider while scrubbing
        val needleHalf = if (isDragging) 13.dp.toPx() else 10.dp.toPx()
        val needleWidth = if (isDragging) 4.dp.toPx() else 3.dp.toPx()
        drawLine(
            color = palette.primary.copy(alpha = 0.35f),
            start = Offset(thumbX, midY - needleHalf - 2.dp.toPx()),
            end = Offset(thumbX, midY + needleHalf + 2.dp.toPx()),
            strokeWidth = needleWidth + 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = palette.onBackground,
            start = Offset(thumbX, midY - needleHalf),
            end = Offset(thumbX, midY + needleHalf),
            strokeWidth = needleWidth,
            cap = StrokeCap.Round
        )
    }
}
