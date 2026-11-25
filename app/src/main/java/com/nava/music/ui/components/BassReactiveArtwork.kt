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

/**
 * Centerpiece bass-reactive artwork stage:
 * 1) Concentric harmonic pulse halos radiating behind the cover on every kick drum hit
 * 2) Spring-damped kinetic cover scale & colored ambient glow
 * 3) Optional 28-bar fluid rounded spectrum visualizer synced to live FFT bins
 */
@Composable
fun BassReactiveArtwork(
    track: Track,
    energy: AudioEnergy,
    palette: DynamicPalette,
    isPlaying: Boolean,
    visualizerStyle: VisualizerStyle,
    modifier: Modifier = Modifier
) {
    val activeBeat = if (isPlaying) energy.beatPulse else 0f
    val activeSub = if (isPlaying) energy.subBass else 0f

    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) {
            1f + (activeBeat * 0.042f) + (activeSub * 0.018f)
        } else {
            0.93f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "coverBassScale"
    )

    val haloExpand by animateFloatAsState(
        targetValue = if (isPlaying) activeBeat * 0.9f + activeSub * 0.4f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessLow
        ),
        label = "haloExpand"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Concentric Bass-Reactive Harmonic Halos
            if (visualizerStyle == VisualizerStyle.HALO_AND_WAVE || visualizerStyle == VisualizerStyle.PULSE_HALO) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseR = size.minDimension * 0.44f

                    // Outer soft radial bass bloom
                    val bloomR = baseR * (1.12f + haloExpand * 0.22f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.primary.copy(alpha = 0.36f + activeBeat * 0.24f),
                                palette.secondary.copy(alpha = 0.14f + activeSub * 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = bloomR
                        ),
                        radius = bloomR,
                        center = center
                    )

                    // Crisp harmonic ring 1 (expands on sub-bass)
                    val ring1R = baseR * (1.03f + activeSub * 0.14f)
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                palette.primary.copy(alpha = 0.55f),
                                palette.secondary.copy(alpha = 0.75f),
                                palette.primary.copy(alpha = 0.25f),
                                palette.primary.copy(alpha = 0.55f)
                            ),
                            center = center
                        ),
                        radius = ring1R,
                        center = center,
                        style = Stroke(width = (2.2f + activeBeat * 3.0f).dp.toPx())
                    )

                    // Outer shockwave ring 2 (flashes on kick transient)
                    if (activeBeat > 0.08f) {
                        val ring2R = baseR * (1.08f + haloExpand * 0.22f)
                        drawCircle(
                            color = palette.secondary.copy(alpha = (activeBeat * 0.45f).coerceIn(0f, 0.5f)),
                            radius = ring2R,
                            center = center,
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                    }
                }
            }

            // Layer 2: Kinetic Rounded Album Cover
            TrackArtwork(
                track = track,
                cornerRadius = 28.dp,
                modifier = Modifier
                    .fillMaxSize(0.84f)
                    .graphicsLayer {
                        scaleX = coverScale
                        scaleY = coverScale
                    }
                    .shadow(
                        elevation = (18f + activeBeat * 14f).dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = palette.primary,
                        spotColor = palette.primary
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.32f),
                                palette.secondary.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            )
        }

        // Layer 3: Mirrored Fluid Spectrum Bar Visualizer
        if (visualizerStyle == VisualizerStyle.HALO_AND_WAVE || visualizerStyle == VisualizerStyle.FLUID_SPECTRUM) {
            Spacer(modifier = Modifier.height(14.dp))
            FluidSpectrumStrip(
                spectrum = energy.spectrum,
                palette = palette,
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(42.dp)
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
            // Mirror energy from center outward so bass hits punch symmetrically in the middle
            val distFromCenter = kotlin.math.abs(i - (count - 1) / 2f) / ((count - 1) / 2f)
            val mappedIdx = (distFromCenter * (count - 1)).toInt().coerceIn(0, count - 1)
            val amp = if (isPlaying) spectrum[mappedIdx] else 0.08f
            val barHeight = (size.height * (0.14f + amp * 0.86f)).coerceIn(4.dp.toPx(), size.height)

            val x = i * (barWidth + gap)
            val top = centerY - barHeight / 2f
            val colorMix = Color(
                red = palette.primary.red * (1f - distFromCenter) + palette.secondary.red * distFromCenter,
                green = palette.primary.green * (1f - distFromCenter) + palette.secondary.green * distFromCenter,
                blue = palette.primary.blue * (1f - distFromCenter) + palette.secondary.blue * distFromCenter,
                alpha = 0.45f + amp * 0.55f
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
