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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nava.music.data.AudioEnergy
import com.nava.music.data.DynamicPalette
import com.nava.music.ui.screens.formatDuration
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

@Composable
fun TickWaveformScrubber(
    positionFlow: StateFlow<Long>,
    durationMs: Long,
    isPlaying: Boolean,
    energyFlow: StateFlow<AudioEnergy>,
    palette: DynamicPalette,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Energy is read only inside the Canvas draw lambda (no recomposition at audio rate);
    // position ticks at 4Hz and only recomposes this small composable, not the whole controls column.
    val energyState = energyFlow.collectAsState()
    val positionMs by positionFlow.collectAsState()
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
                .height(28.dp)
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
                        onDragCancel = { isDragging = false }
                    )
                }
        ) {
            val tickCount = 52
            val gap = 2.6.dp.toPx()
            val barW = ((size.width - gap * (tickCount - 1)) / tickCount).coerceAtLeast(1.8.dp.toPx())
            val centerY = size.height / 2f
            val activeIdx = (displayFraction * (tickCount - 1)).toInt().coerceIn(0, tickCount - 1)
            val bassPulse = if (isPlaying) energyState.value.beatPulse else 0f

            for (i in 0 until tickCount) {
                val x = i * (barW + gap)
                val isPlayed = i <= activeIdx
                val distToHead = abs(i - activeIdx)
                val headBoost = if (isPlaying && distToHead <= 3) {
                    (1f - distToHead / 4f) * bassPulse * 0.40f
                } else {
                    0f
                }
                val baseH = if (i == activeIdx) 0.90f else 0.50f + ((i * 7) % 5) * 0.04f
                val barH = (size.height * (baseH + headBoost)).coerceIn(7.dp.toPx(), size.height)

                val color = when {
                    i == activeIdx -> palette.onBackground
                    isPlayed -> palette.primary
                    else -> palette.onBackground.copy(alpha = if (palette.isDark) 0.20f else 0.16f)
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
