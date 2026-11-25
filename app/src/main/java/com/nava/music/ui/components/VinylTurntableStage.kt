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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-craft interactive vinyl turntable stage:
 * - Tilted cardboard album sleeve peeking behind the record
 * - Glossy pressed black vinyl with micro-grooves, anisotropic specular reflections, and rotating artwork label
 * - Articulated brushed-metal tonearm that smoothly pivots onto the record when playing
 * - Delicate bass-reactive Aurora/Cosmos orbital hairline rings & micro-sparks
 */
@Composable
fun VinylTurntableStage(
    track: Track,
    energy: AudioEnergy,
    palette: DynamicPalette,
    isPlaying: Boolean,
    atmosphere: VinylAtmosphere,
    modifier: Modifier = Modifier
) {
    // Continuous smooth rotation angle (retains exact angle when paused — never jumps!)
    var vinylRotationDeg by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dtSec = (frameNanos - lastNanos) / 1_000_000_000f
                    // ~33.3 RPM feels silky & authentic (~38 deg/sec visual cadence)
                    vinylRotationDeg = (vinylRotationDeg + dtSec * 40f) % 360f
                }
                lastNanos = frameNanos
            }
        }
    }

    // Tonearm smoothly engages onto grooves when playing, parks off-disc when paused
    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 23f else 2f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessLow
        ),
        label = "tonearmAngle"
    )

    val activeBass = if (isPlaying) energy.subBass else 0f
    val activeBeat = if (isPlaying) energy.beatPulse else 0f

    val bassPulse by animateFloatAsState(
        targetValue = activeBeat * 0.7f + activeBass * 0.3f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "vinylBassPulse"
    )

    val infinite = rememberInfiniteTransition(label = "orbitFlow")
    val orbitPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitPhase"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.18f),
        contentAlignment = Alignment.Center
    ) {
        val stageW = maxWidth
        val stageH = maxHeight
        val discSize = minOf(stageW * 0.64f, stageH * 0.80f)
        val sleeveSize = discSize * 0.86f

        // 1) Delicate Bass-Reactive Orbital Aurora Filaments (behind & framing vinyl)
        if (atmosphere != VinylAtmosphere.GALLERY) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val discCenter = Offset(size.width * 0.56f, size.height * 0.50f)
                val baseR = discSize.toPx() * 0.52f
                val pulseExpand = 1f + bassPulse * 0.085f

                // Soft warm glow behind record
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.secondary.copy(alpha = 0.24f + bassPulse * 0.16f),
                            palette.primary.copy(alpha = 0.10f + activeBass * 0.08f),
                            Color.Transparent
                        ),
                        center = discCenter,
                        radius = baseR * 1.55f * pulseExpand
                    ),
                    radius = baseR * 1.55f * pulseExpand,
                    center = discCenter
                )

                val orbitCount = if (atmosphere == VinylAtmosphere.COSMOS) 4 else 3
                for (i in 0 until orbitCount) {
                    val tiltDeg = (i * 34f) + (orbitPhase * 18f * (if (i % 2 == 0) 1f else -0.7f))
                    val rx = baseR * (1.16f + i * 0.075f) * pulseExpand
                    val ry = baseR * (0.98f + i * 0.06f) * (1f + bassPulse * 0.05f)
                    val ringColor = if (i % 2 == 0) palette.secondary else palette.primary

                    rotate(degrees = tiltDeg, pivot = discCenter) {
                        drawOval(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    ringColor.copy(alpha = 0.06f),
                                    ringColor.copy(alpha = 0.42f + bassPulse * 0.28f),
                                    Color.White.copy(alpha = 0.55f + bassPulse * 0.25f),
                                    ringColor.copy(alpha = 0.10f),
                                    ringColor.copy(alpha = 0.06f)
                                ),
                                center = discCenter
                            ),
                            topLeft = Offset(discCenter.x - rx, discCenter.y - ry),
                            size = Size(rx * 2f, ry * 2f),
                            style = Stroke(width = (1.0f + (if (i == 0) bassPulse * 0.9f else 0f)).dp.toPx())
                        )

                        // Tiny luminous orbital spark travelling on each filament
                        val sparkAngle = orbitPhase * (1.2f + i * 0.35f) + i * 2.1f
                        val sx = discCenter.x + rx * cos(sparkAngle)
                        val sy = discCenter.y + ry * sin(sparkAngle)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.72f + bassPulse * 0.25f),
                            radius = (2.2f + bassPulse * 1.4f).dp.toPx(),
                            center = Offset(sx, sy)
                        )
                        drawCircle(
                            color = ringColor.copy(alpha = 0.35f),
                            radius = (6.0f + bassPulse * 3.0f).dp.toPx(),
                            center = Offset(sx, sy)
                        )
                    }
                }
            }
        }

        // 2) Tilted Cardboard Album Sleeve peeking out on the left
        Box(
            modifier = Modifier
                .offset(x = -discSize * 0.36f, y = 2.dp)
                .size(sleeveSize)
                .graphicsLayer {
                    rotationZ = -4.2f
                }
                .shadow(
                    elevation = 22.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            TrackArtwork(
                track = track,
                cornerRadius = 14.dp,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3) Glossy Pressed Vinyl Disc with Anisotropic Sheen & Rotating Center Label
        Box(
            modifier = Modifier
                .offset(x = discSize * 0.09f)
                .size(discSize)
                .graphicsLayer {
                    val s = 1f + bassPulse * 0.022f
                    scaleX = s
                    scaleY = s
                }
                .shadow(
                    elevation = 26.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black,
                    spotColor = palette.primary.copy(alpha = 0.65f)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Static specular vinyl surface + concentric micro-grooves
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f

                // Deep pressed vinyl base
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF16161B),
                            Color(0xFF0A0A0D),
                            Color(0xFF141419),
                            Color(0xFF08080A)
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // Realistic diagonal anisotropic light sheen across grooves
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.00f to Color.Transparent,
                        0.10f to Color.White.copy(alpha = 0.13f),
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

                // Subtle concentric record grooves
                val grooveSteps = 18
                for (g in 0 until grooveSteps) {
                    val frac = 0.42f + (g.toFloat() / grooveSteps) * 0.54f
                    drawCircle(
                        color = Color.White.copy(alpha = if (g % 4 == 0) 0.08f else 0.035f),
                        radius = radius * frac,
                        center = center,
                        style = Stroke(width = 0.7.dp.toPx())
                    )
                }

                // Crisp outer vinyl rim highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = radius - 1.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Rotating Center Album-Art Label
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
                    modifier = Modifier.fillMaxSize()
                )
                // Metallic spindle center hole
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawCircle(color = Color(0xFF18181D), radius = size.minDimension * 0.45f)
                    drawCircle(
                        color = Color(0xFFD6D8E0),
                        radius = size.minDimension * 0.28f
                    )
                }
            }
        }

        // 4) Articulated Brushed-Metal Tonearm on Top-Right
        Canvas(
            modifier = Modifier
                .offset(x = discSize * 0.09f)
                .size(discSize)
        ) {
            val pivot = Offset(size.width * 0.92f, size.height * 0.14f)

            // Gimbal mount base shadow & metallic housing
            drawCircle(
                color = Color.Black.copy(alpha = 0.55f),
                radius = 18.dp.toPx(),
                center = Offset(pivot.x + 2.dp.toPx(), pivot.y + 4.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF4B4F5C), Color(0xFF1D1F26)),
                    center = pivot,
                    radius = 16.dp.toPx()
                ),
                radius = 16.dp.toPx(),
                center = pivot
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = 16.dp.toPx(),
                center = pivot,
                style = Stroke(width = 1.dp.toPx())
            )

            // Pivoting Arm Assembly
            rotate(degrees = tonearmAngle, pivot = pivot) {
                // Counterweight behind pivot
                val counterEnd = Offset(pivot.x + 5.dp.toPx(), pivot.y - 18.dp.toPx())
                drawLine(
                    color = Color(0xFF9EA4B3),
                    start = pivot,
                    end = counterEnd,
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Main tapered brushed-chrome shaft
                val elbow = Offset(pivot.x - 10.dp.toPx(), pivot.y + size.height * 0.46f)
                val stylusTip = Offset(elbow.x - 20.dp.toPx(), elbow.y + 22.dp.toPx())

                // Drop shadow under tonearm
                drawLine(
                    color = Color.Black.copy(alpha = 0.40f),
                    start = Offset(pivot.x + 4.dp.toPx(), pivot.y + 5.dp.toPx()),
                    end = Offset(elbow.x + 4.dp.toPx(), elbow.y + 5.dp.toPx()),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Metallic shaft
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFE2E6F0), Color(0xFF8C92A3), Color(0xFFD2D7E5)),
                        start = pivot,
                        end = elbow
                    ),
                    start = pivot,
                    end = elbow,
                    strokeWidth = 3.8.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Angled headshell
                drawLine(
                    color = Color(0xFF2B2E38),
                    start = elbow,
                    end = stylusTip,
                    strokeWidth = 7.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFBFC5D4),
                    start = elbow,
                    end = stylusTip,
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Warm glowing stylus diamond tip when engaged
                if (isPlaying) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.secondary.copy(alpha = 0.85f),
                                palette.secondary.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = stylusTip,
                            radius = (9.dp.toPx() + bassPulse * 4.dp.toPx())
                        ),
                        radius = (9.dp.toPx() + bassPulse * 4.dp.toPx()),
                        center = stylusTip
                    )
                }
                drawCircle(
                    color = if (isPlaying) Color(0xFFFFE5A3) else Color(0xFF888D9B),
                    radius = 2.5.dp.toPx(),
                    center = stylusTip
                )
            }

            // Gimbal top center screw cap
            drawCircle(
                color = Color(0xFFCFD4E0),
                radius = 5.dp.toPx(),
                center = pivot
            )
        }
    }
}
