package com.nava.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nava.music.data.AudioEnergy
import com.nava.music.data.DynamicPalette
import com.nava.music.data.Track
import com.nava.music.data.VinylAtmosphere
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-craft interactive vinyl turntable stage:
 * - Looks equally authentic in Light (studio alabaster plinth) and Dark (obsidian hi-fi) themes
 * - Collects [energyFlow] locally to avoid recomposing the surrounding UI
 */
@Composable
fun VinylTurntableStage(
    track: Track,
    energyFlow: StateFlow<AudioEnergy>,
    palette: DynamicPalette,
    isPlaying: Boolean,
    atmosphere: VinylAtmosphere,
    modifier: Modifier = Modifier
) {
    val energy by energyFlow.collectAsState()

    var vinylRotationDeg by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dtSec = (frameNanos - lastNanos) / 1_000_000_000f
                    vinylRotationDeg = (vinylRotationDeg + dtSec * 38f) % 360f
                }
                lastNanos = frameNanos
            }
        }
    }

    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 23f else 2f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessLow),
        label = "tonearmAngle"
    )

    val bassPulse = if (isPlaying) (energy.beatPulse * 0.7f + energy.subBass * 0.3f) else 0f

    val orbitPhase = if (isPlaying && atmosphere != VinylAtmosphere.GALLERY) {
        val infinite = rememberInfiniteTransition(label = "orbitFlow")
        val p by infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 15000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orbitPhase"
        )
        p
    } else {
        0.5f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.18f),
        contentAlignment = Alignment.Center
    ) {
        val discSize = minOf(maxWidth * 0.64f, maxHeight * 0.80f)
        val sleeveSize = discSize * 0.86f

        // 1) Delicate Orbital Aurora Filaments
        if (atmosphere != VinylAtmosphere.GALLERY) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val discCenter = Offset(size.width * 0.56f, size.height * 0.50f)
                val baseR = discSize.toPx() * 0.52f
                val pulseExpand = 1f + bassPulse * 0.08f
                val glowAlpha = if (palette.isDark) 0.22f else 0.14f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.secondary.copy(alpha = glowAlpha + bassPulse * 0.14f),
                            palette.primary.copy(alpha = (glowAlpha * 0.45f) + bassPulse * 0.06f),
                            Color.Transparent
                        ),
                        center = discCenter,
                        radius = baseR * 1.50f * pulseExpand
                    ),
                    radius = baseR * 1.50f * pulseExpand,
                    center = discCenter
                )

                val orbitCount = if (atmosphere == VinylAtmosphere.COSMOS) 4 else 3
                val sparkCore = if (palette.isDark) Color.White else palette.primary
                for (i in 0 until orbitCount) {
                    val tiltDeg = (i * 34f) + (orbitPhase * 16f * (if (i % 2 == 0) 1f else -0.7f))
                    val rx = baseR * (1.16f + i * 0.07f) * pulseExpand
                    val ry = baseR * (0.98f + i * 0.055f) * (1f + bassPulse * 0.045f)
                    val ringColor = if (i % 2 == 0) palette.secondary else palette.primary

                    rotate(degrees = tiltDeg, pivot = discCenter) {
                        drawOval(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    ringColor.copy(alpha = 0.05f),
                                    ringColor.copy(alpha = 0.40f + bassPulse * 0.25f),
                                    sparkCore.copy(alpha = 0.52f + bassPulse * 0.22f),
                                    ringColor.copy(alpha = 0.08f),
                                    ringColor.copy(alpha = 0.05f)
                                ),
                                center = discCenter
                            ),
                            topLeft = Offset(discCenter.x - rx, discCenter.y - ry),
                            size = Size(rx * 2f, ry * 2f),
                            style = Stroke(width = (1.0f + (if (i == 0) bassPulse * 0.8f else 0f)).dp.toPx())
                        )

                        val sparkAngle = orbitPhase * (1.1f + i * 0.3f) + i * 2.1f
                        val sx = discCenter.x + rx * cos(sparkAngle)
                        val sy = discCenter.y + ry * sin(sparkAngle)
                        drawCircle(
                            color = sparkCore.copy(alpha = 0.72f + bassPulse * 0.22f),
                            radius = (2.0f + bassPulse * 1.2f).dp.toPx(),
                            center = Offset(sx, sy)
                        )
                    }
                }
            }
        }

        // 2) Tilted Cardboard Album Sleeve
        Box(
            modifier = Modifier
                .offset(x = -discSize * 0.36f, y = 2.dp)
                .size(sleeveSize)
                .graphicsLayer { rotationZ = -4.0f }
                .shadow(
                    elevation = if (palette.isDark) 20.dp else 14.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = Color.Black.copy(alpha = 0.55f)
                )
                .border(
                    width = 1.dp,
                    color = palette.outline,
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            TrackArtwork(
                track = track,
                cornerRadius = 14.dp,
                decodeSizePx = 512,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3) Glossy Pressed Vinyl Disc
        Box(
            modifier = Modifier
                .offset(x = discSize * 0.09f)
                .size(discSize)
                .graphicsLayer {
                    val s = 1f + bassPulse * 0.02f
                    scaleX = s
                    scaleY = s
                }
                .shadow(
                    elevation = if (palette.isDark) 24.dp else 18.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.6f),
                    spotColor = Color.Black.copy(alpha = 0.6f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF18181E),
                            Color(0xFF0A0A0D),
                            Color(0xFF15151B),
                            Color(0xFF070709)
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // Diagonal anisotropic vinyl sheen
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.00f to Color.Transparent,
                        0.10f to Color.White.copy(alpha = 0.14f),
                        0.16f to Color.Transparent,
                        0.50f to Color.Transparent,
                        0.60f to Color.White.copy(alpha = 0.11f),
                        0.66f to Color.Transparent,
                        1.00f to Color.Transparent,
                        center = center
                    ),
                    radius = radius * 0.98f,
                    center = center
                )

                val grooveSteps = 16
                for (g in 0 until grooveSteps) {
                    val frac = 0.43f + (g.toFloat() / grooveSteps) * 0.53f
                    drawCircle(
                        color = Color.White.copy(alpha = if (g % 4 == 0) 0.08f else 0.032f),
                        radius = radius * frac,
                        center = center,
                        style = Stroke(width = 0.7.dp.toPx())
                    )
                }

                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = radius - 1.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            Box(
                modifier = Modifier
                    .size(discSize * 0.37f)
                    .graphicsLayer { rotationZ = vinylRotationDeg }
                    .clip(CircleShape)
                    .border(1.5.dp, Color.Black.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                TrackArtwork(
                    track = track,
                    cornerRadius = 999.dp,
                    decodeSizePx = 256,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawCircle(color = Color(0xFF18181D), radius = size.minDimension * 0.45f)
                    drawCircle(color = Color(0xFFD6D8E0), radius = size.minDimension * 0.27f)
                }
            }
        }

        // 4) Articulated Brushed-Metal Tonearm
        Canvas(
            modifier = Modifier
                .offset(x = discSize * 0.09f)
                .size(discSize)
        ) {
            val pivot = Offset(size.width * 0.92f, size.height * 0.14f)

            drawCircle(
                color = Color.Black.copy(alpha = if (palette.isDark) 0.50f else 0.22f),
                radius = 17.dp.toPx(),
                center = Offset(pivot.x + 2.dp.toPx(), pivot.y + 4.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (palette.isDark) {
                        listOf(Color(0xFF4F5462), Color(0xFF1D1F26))
                    } else {
                        listOf(Color(0xFFE5E7EE), Color(0xFFA2A7B5))
                    },
                    center = pivot,
                    radius = 15.dp.toPx()
                ),
                radius = 15.dp.toPx(),
                center = pivot
            )

            rotate(degrees = tonearmAngle, pivot = pivot) {
                val counterEnd = Offset(pivot.x + 5.dp.toPx(), pivot.y - 17.dp.toPx())
                drawLine(
                    color = Color(0xFF9EA4B3),
                    start = pivot,
                    end = counterEnd,
                    strokeWidth = 6.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val elbow = Offset(pivot.x - 10.dp.toPx(), pivot.y + size.height * 0.46f)
                val stylusTip = Offset(elbow.x - 20.dp.toPx(), elbow.y + 22.dp.toPx())

                drawLine(
                    color = Color.Black.copy(alpha = if (palette.isDark) 0.38f else 0.18f),
                    start = Offset(pivot.x + 4.dp.toPx(), pivot.y + 5.dp.toPx()),
                    end = Offset(elbow.x + 4.dp.toPx(), elbow.y + 5.dp.toPx()),
                    strokeWidth = 4.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFE4E8F2), Color(0xFF8C92A3), Color(0xFFD4D9E6)),
                        start = pivot,
                        end = elbow
                    ),
                    start = pivot,
                    end = elbow,
                    strokeWidth = 3.6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = Color(0xFF282B35),
                    start = elbow,
                    end = stylusTip,
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFBFC5D4),
                    start = elbow,
                    end = stylusTip,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                if (isPlaying) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.secondary.copy(alpha = 0.85f),
                                palette.secondary.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = stylusTip,
                            radius = (8.dp.toPx() + bassPulse * 4.dp.toPx())
                        ),
                        radius = (8.dp.toPx() + bassPulse * 4.dp.toPx()),
                        center = stylusTip
                    )
                }
                drawCircle(
                    color = if (isPlaying) Color(0xFFFFE5A3) else Color(0xFF888D9B),
                    radius = 2.4.dp.toPx(),
                    center = stylusTip
                )
            }

            drawCircle(
                color = if (palette.isDark) Color(0xFFCFD4E0) else Color(0xFF525663),
                radius = 4.5.dp.toPx(),
                center = pivot
            )
        }
    }
}
