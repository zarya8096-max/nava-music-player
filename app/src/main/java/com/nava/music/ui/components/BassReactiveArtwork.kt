package com.nava.music.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.nava.music.data.AudioEnergy
import com.nava.music.data.DynamicPalette
import com.nava.music.data.Track
import com.nava.music.data.VisualizerStyle
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Isolated bass-reactive artwork stage:
 * Collects [energyFlow] locally inside this leaf composable so the rest of the screen
 * NEVER recomposes on audio frames.
 *
 * Visual language: precise hairline frames that step outward on every kick (like a sonar
 * ping on an instrument panel) and a segmented LED analyser with peak-hold caps.
 * No blurred blooms, no coloured glows.
 */
@Composable
fun BassReactiveArtwork(
    track: Track,
    energyFlow: StateFlow<AudioEnergy>,
    palette: DynamicPalette,
    isPlaying: Boolean,
    visualizerStyle: VisualizerStyle,
    modifier: Modifier = Modifier
) {
    // Audio energy is read ONLY inside draw / graphicsLayer lambdas below, so a new FFT frame
    // invalidates the draw phase of these leaves and never recomposes (or re-lays-out) anything.
    val energyState = energyFlow.collectAsState()

    val playFactor = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "coverPlayFactor"
    )

    val coverShape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            if (visualizerStyle == VisualizerStyle.HALO_AND_WAVE || visualizerStyle == VisualizerStyle.PULSE_HALO) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val activeBeat = if (isPlaying) energyState.value.beatPulse else 0f
                    val activeSub = if (isPlaying) energyState.value.subBass else 0f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val coverSide = size.minDimension * 0.84f
                    val baseRadius = 22.dp.toPx()

                    for (i in 0..2) {
                        val rest = (5 + i * 6).dp.toPx()
                        val kick = (3 + i * 1.5f).dp.toPx() * activeBeat + (2f + i).dp.toPx() * activeSub
                        val spread = rest + kick
                        val side = coverSide + spread * 2f
                        val strength = (0.60f + activeBeat * 0.90f).coerceAtMost(1.4f)
                        val alpha = ((0.36f - i * 0.11f) * strength).coerceIn(0f, 0.7f)
                        val tint = if (i == 0) palette.primary else lerp(palette.primary, palette.secondary, i / 2f)
                        drawRoundRect(
                            color = tint.copy(alpha = alpha),
                            topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
                            size = Size(side, side),
                            cornerRadius = CornerRadius(baseRadius + spread),
                            style = Stroke(width = (if (i == 0) 1.2f else 0.8f).dp.toPx())
                        )
                    }
                }
            }

            TrackArtwork(
                track = track,
                cornerRadius = 22.dp,
                decodeSizePx = 640,
                modifier = Modifier
                    .fillMaxSize(0.84f)
                    .graphicsLayer {
                        val e = energyState.value
                        val live = 1f + e.beatPulse * 0.030f + e.subBass * 0.012f
                        val s = 0.95f + (live - 0.95f) * playFactor.value
                        scaleX = s
                        scaleY = s
                    }
                    .shadow(
                        elevation = if (palette.isDark) 26.dp else 18.dp,
                        shape = coverShape,
                        ambientColor = Color.Black.copy(alpha = if (palette.isDark) 0.70f else 0.30f),
                        spotColor = Color.Black.copy(alpha = if (palette.isDark) 0.85f else 0.38f)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.03f))
                        ),
                        shape = coverShape
                    )
            )
        }

        if (visualizerStyle == VisualizerStyle.HALO_AND_WAVE || visualizerStyle == VisualizerStyle.FLUID_SPECTRUM) {
            Spacer(modifier = Modifier.height(16.dp))
            LedSpectrumStrip(
                energyState = energyState,
                palette = palette,
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(34.dp)
            )
        }
    }
}

private const val LED_SEGMENTS = 9
private const val MAX_BARS = 64

/** Segmented analyser: unlit cells stay faintly visible so the instrument reads even when idle. */
@Composable
private fun LedSpectrumStrip(
    energyState: State<AudioEnergy>,
    palette: DynamicPalette,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val peaks = remember { FloatArray(MAX_BARS) }

    Canvas(modifier = modifier.padding(horizontal = 2.dp)) {
        val spectrum = energyState.value.spectrum
        val count = spectrum.size.coerceIn(1, MAX_BARS)
        for (i in 0 until count) {
            val level = if (isPlaying) spectrum.getOrElse(i) { 0f } else 0f
            peaks[i] = max(level, peaks[i] - 0.03f)
        }
        val slot = size.width / count
        val barWidth = (slot * 0.46f).coerceIn(2.dp.toPx(), 5.dp.toPx())
        val cellGap = 2.dp.toPx()
        val cellHeight = (size.height - cellGap * (LED_SEGMENTS - 1)) / LED_SEGMENTS
        val unlit = palette.onBackground.copy(alpha = if (palette.isDark) 0.07f else 0.08f)

        for (i in 0 until count) {
            // Mirror so the low end sits in the middle and the highs fan outward
            val dist = kotlin.math.abs(i - (count - 1) / 2f) / ((count - 1) / 2f).coerceAtLeast(1f)
            val src = (dist * (count - 1)).roundToInt().coerceIn(0, count - 1)
            val level = if (isPlaying) spectrum.getOrElse(src) { 0f } else 0f
            val lit = (level * LED_SEGMENTS).roundToInt().coerceIn(0, LED_SEGMENTS)
            val peakCell = (peaks[src] * LED_SEGMENTS).roundToInt().coerceIn(0, LED_SEGMENTS) - 1
            val x = slot * i + (slot - barWidth) / 2f

            for (c in 0 until LED_SEGMENTS) {
                val y = size.height - (c + 1) * cellHeight - c * cellGap
                val color = when {
                    c < lit -> lerp(palette.secondary, palette.primary, c / (LED_SEGMENTS - 1f)).copy(alpha = 0.92f)
                    c == peakCell && peakCell >= lit -> palette.onBackground.copy(alpha = 0.78f)
                    else -> unlit
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, cellHeight),
                    cornerRadius = CornerRadius(barWidth * 0.3f)
                )
            }
        }
    }
}
