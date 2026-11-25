package com.nava.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nava.music.data.DynamicPalette
import com.nava.music.ui.screens.formatDuration
import kotlin.math.abs

/**
 * Tactile vertical-tick waveform scrubber inspired by studio hi-fi turntables.
 * Played ticks illuminate in the track's warm accent color; active playhead tick
 * subtly breathes with the music's bass pulse.
 */
@Composable
fun TickWaveformScrubber(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    bassPulse: Float,
    palette: DynamicPalette,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(progress) }
    val displayFraction = if (isDragging) dragFraction else progress

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = formatDuration((displayFraction * durationMs).toLong()),
            style = MaterialTheme.typography.labelMedium,
            color = palette.onBackgroundMuted
        )

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeekFraction(frac)
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
            val tickCount = 54
            val gap = 2.6.dp.toPx()
            val barW = ((size.width - gap * (tickCount - 1)) / tickCount).coerceAtLeast(1.8.dp.toPx())
            val centerY = size.height / 2f
            val activeIdx = (displayFraction * (tickCount - 1)).toInt().coerceIn(0, tickCount - 1)

            for (i in 0 until tickCount) {
                val x = i * (barW + gap)
                val isPlayed = i <= activeIdx
                val distToHead = abs(i - activeIdx)

                // Subtle rhythmic height modulation near playhead when playing
                val headBoost = if (isPlaying && distToHead <= 3) {
                    (1f - distToHead / 4f) * bassPulse * 0.42f
                } else {
                    0f
                }

                val baseH = if (i == activeIdx) 0.92f else 0.52f + ((i * 7) % 5) * 0.04f
                val barH = (size.height * (baseH + headBoost)).coerceIn(8.dp.toPx(), size.height)

                val color = when {
                    i == activeIdx -> Color.White
                    isPlayed -> palette.secondary.copy(alpha = 0.92f)
                    else -> Color.White.copy(alpha = 0.22f)
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, centerY - barH / 2f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
            }
        }

        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = palette.onBackgroundMuted
        )
    }
}
