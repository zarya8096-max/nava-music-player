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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.nava.music.data.AudioEnergy
import com.nava.music.data.DynamicPalette
import com.nava.music.data.Track
import com.nava.music.data.VisualizerStyle
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

/**
 * Isolated bass-reactive artwork stage:
 * Collects [energyFlow] locally inside this leaf composable so the rest of the screen
 * NEVER recomposes on audio frames.
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
    val energy by energyFlow.collectAsState()
    val activeBeat = if (isPlaying) energy.beatPulse else 0f
    val activeSub = if (isPlaying) energy.subBass else 0f

    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f + (activeBeat * 0.036f) + (activeSub * 0.015f) else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "coverBassScale"
    )

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
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseR = size.minDimension * 0.44f
                    val haloExpand = activeBeat * 0.85f + activeSub * 0.35f

                    val bloomR = baseR * (1.10f + haloExpand * 0.18f)
                    val bloomAlpha = if (palette.isDark) 0.30f else 0.20f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.primary.copy(alpha = bloomAlpha + activeBeat * 0.18f),
                                palette.secondary.copy(alpha = (bloomAlpha * 0.4f) + activeSub * 0.10f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = bloomR
                        ),
                        radius = bloomR,
                        center = center
                    )

                    val ring1R = baseR * (1.03f + activeSub * 0.12f)
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                palette.primary.copy(alpha = 0.48f),
                                palette.secondary.copy(alpha = 0.65f),
                                palette.primary.copy(alpha = 0.18f),
                                palette.primary.copy(alpha = 0.48f)
                            ),
                            center = center
                        ),
                        radius = ring1R,
                        center = center,
                        style = Stroke(width = (1.6f + activeBeat * 2.2f).dp.toPx())
                    )
                }
            }

            TrackArtwork(
                track = track,
                cornerRadius = 26.dp,
                decodeSizePx = 640,
                modifier = Modifier
                    .fillMaxSize(0.84f)
                    .graphicsLayer {
                        scaleX = coverScale
                        scaleY = coverScale
                    }
                    .shadow(
                        elevation = if (palette.isDark) 20.dp else 16.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = palette.primary.copy(alpha = 0.55f),
                        spotColor = palette.primary.copy(alpha = 0.55f)
                    )
                    .border(
                        width = 1.dp,
                        color = palette.outline,
                        shape = RoundedCornerShape(26.dp)
                    )
            )
        }

        if (visualizerStyle == VisualizerStyle.HALO_AND_WAVE || visualizerStyle == VisualizerStyle.FLUID_SPECTRUM) {
            Spacer(modifier = Modifier.height(14.dp))
            FluidSpectrumStrip(
                spectrum = energy.spectrum,
                palette = palette,
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(36.dp)
            )
        }
    }
}

@Composable
private fun FluidSpectrumStrip(
    spectrum: List<Float>,
    palette: DynamicPalette,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(horizontal = 4.dp)) {
        val count = spectrum.size.coerceAtLeast(1)
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2.dp.toPx())
        val centerY = size.height / 2f

        for (i in 0 until count) {
            val distFromCenter = abs(i - (count - 1) / 2f) / ((count - 1) / 2f).coerceAtLeast(1f)
            val mappedIdx = (distFromCenter * (count - 1)).toInt().coerceIn(0, count - 1)
            val amp = if (isPlaying) spectrum[mappedIdx] else 0.08f
            val barHeight = (size.height * (0.14f + amp * 0.86f)).coerceIn(3.5.dp.toPx(), size.height)

            val x = i * (barWidth + gap)
            val top = centerY - barHeight / 2f
            val colorMix = Color(
                red = palette.primary.red * (1f - distFromCenter) + palette.secondary.red * distFromCenter,
                green = palette.primary.green * (1f - distFromCenter) + palette.secondary.green * distFromCenter,
                blue = palette.primary.blue * (1f - distFromCenter) + palette.secondary.blue * distFromCenter,
                alpha = 0.42f + amp * 0.56f
            )

            drawRoundRect(
                color = colorMix,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
