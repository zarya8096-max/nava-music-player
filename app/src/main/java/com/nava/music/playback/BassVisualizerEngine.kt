package com.nava.music.playback

import android.media.audiofx.Visualizer
import android.os.SystemClock
import com.nava.music.data.AudioEnergy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

/**
 * Low-overhead FFT bass & beat engine:
 * - Throttled to ~28fps to save CPU/battery and eliminate GC churn
 * - Automatically suspends when app is backgrounded or Now Playing is closed
 */
class BassVisualizerEngine(
    private val scope: CoroutineScope
) {
    private val _energy = MutableStateFlow(AudioEnergy())
    val energy: StateFlow<AudioEnergy> = _energy.asStateFlow()

    private var visualizer: Visualizer? = null
    private var attachedSessionId: Int = -1
    private var fallbackJob: Job? = null
    private var isUiActive: Boolean = false
    private var lastEmitMs: Long = 0L

    private var smoothSubBass = 0f
    private var smoothMidBass = 0f
    private var smoothMids = 0f
    private var smoothTreble = 0f
    private var beatEnvelope = 0f
    private var rollingBassAvg = 0.15f
    private val smoothSpectrum = FloatArray(BAND_COUNT) { 0f }

    fun setUiActive(active: Boolean, sessionId: Int, hasRecordPermission: Boolean, isPlaying: Boolean) {
        isUiActive = active
        if (!active || !isPlaying) {
            setIdle()
        } else {
            attach(sessionId, hasRecordPermission, isPlaying)
        }
    }

    fun attach(sessionId: Int, hasRecordPermission: Boolean, isPlaying: Boolean) {
        if (!isPlaying || !isUiActive) {
            setIdle()
            return
        }

        if (hasRecordPermission && sessionId > 0) {
            if (visualizer != null && attachedSessionId == sessionId) {
                runCatching { visualizer?.enabled = true }
                stopFallback()
                return
            }
            releaseVisualizer()
            if (tryAttachHardwareVisualizer(sessionId)) {
                stopFallback()
                return
            }
        }

        startOrganicFallback()
    }

    fun setIdle() {
        runCatching { visualizer?.enabled = false }
        stopFallback()
        smoothSubBass = 0f
        smoothMidBass = 0f
        smoothMids = 0f
        smoothTreble = 0f
        beatEnvelope = 0f
        for (i in smoothSpectrum.indices) smoothSpectrum[i] = 0f
        if (_energy.value.subBass != 0f || _energy.value.beatPulse != 0f) {
            _energy.value = AudioEnergy(isLiveAudio = visualizer != null)
        }
    }

    private fun tryAttachHardwareVisualizer(sessionId: Int): Boolean {
        return runCatching {
            val viz = Visualizer(sessionId)
            viz.enabled = false
            val range = Visualizer.getCaptureSizeRange()
            viz.captureSize = 512.coerceIn(range[0], range[1])

            val maxRate = Visualizer.getMaxCaptureRate()
            val captureRate = (maxRate / 2).coerceAtLeast(10000)

            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, w: ByteArray?, r: Int) = Unit
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, r: Int) {
                        if (fft != null && fft.size >= 64) {
                            processFft(fft)
                        }
                    }
                },
                captureRate,
                false,
                true
            )
            viz.enabled = true
            visualizer = viz
            attachedSessionId = sessionId
            true
        }.getOrElse {
            releaseVisualizer()
            false
        }
    }

    private fun processFft(fft: ByteArray) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastEmitMs < EMIT_INTERVAL_MS) return
        lastEmitMs = now

        val binCount = fft.size / 2
        val magnitudes = FloatArray(binCount)
        magnitudes[0] = (fft[0].toInt() and 0xFF) / 255f

        for (k in 1 until binCount) {
            val real = fft[2 * k].toFloat()
            val imag = fft[2 * k + 1].toFloat()
            val mag = hypot(real, imag)
            magnitudes[k] = (ln(1f + mag * 1.4f) / 5.2f).coerceIn(0f, 1f)
        }

        val rawSub = averageRange(magnitudes, 1, 5)
        val rawMidBass = averageRange(magnitudes, 5, 11)
        val rawMids = averageRange(magnitudes, 11, 32)
        val rawTreble = averageRange(magnitudes, 32, minOf(80, binCount))

        smoothSubBass = if (rawSub > smoothSubBass) {
            smoothSubBass * 0.30f + rawSub * 0.70f
        } else {
            smoothSubBass * 0.80f + rawSub * 0.20f
        }
        smoothMidBass = smoothMidBass * 0.65f + rawMidBass * 0.35f
        smoothMids = smoothMids * 0.70f + rawMids * 0.30f
        smoothTreble = smoothTreble * 0.72f + rawTreble * 0.28f

        rollingBassAvg = rollingBassAvg * 0.94f + rawSub * 0.06f
        val delta = (rawSub - rollingBassAvg * 1.12f).coerceAtLeast(0f)
        val instantBeat = (delta * 3.1f).coerceIn(0f, 1f)
        beatEnvelope = max(instantBeat, beatEnvelope * 0.82f)

        val maxUsefulBin = minOf(binCount - 1, 112)
        for (i in 0 until BAND_COUNT) {
            val startBin = (1 + (i * i * maxUsefulBin) / (BAND_COUNT * BAND_COUNT)).coerceIn(1, maxUsefulBin)
            val endBin = (startBin + max(1, (i + 2) / 3)).coerceAtMost(maxUsefulBin)
            val rawBand = averageRange(magnitudes, startBin, endBin + 1)
            val prev = smoothSpectrum[i]
            smoothSpectrum[i] = if (rawBand > prev) {
                prev * 0.28f + rawBand * 0.72f
            } else {
                prev * 0.78f + rawBand * 0.22f
            }
        }

        _energy.value = AudioEnergy(
            subBass = smoothSubBass.coerceIn(0f, 1f),
            midBass = smoothMidBass.coerceIn(0f, 1f),
            mids = smoothMids.coerceIn(0f, 1f),
            treble = smoothTreble.coerceIn(0f, 1f),
            beatPulse = beatEnvelope.coerceIn(0f, 1f),
            spectrum = smoothSpectrum.toList(),
            isLiveAudio = true
        )
    }

    private fun startOrganicFallback() {
        if (fallbackJob?.isActive == true) return
        fallbackJob = scope.launch(Dispatchers.Default) {
            var t = 0.0
            while (isActive) {
                val kick = ((sin(t * 2.0 * PI * 1.95) + 1.0) * 0.5)
                val punch = (kick * kick * kick).toFloat()
                val groove = ((sin(t * 2.0 * PI * 0.97 + 0.6) + 1.0) * 0.5).toFloat()
                val bars = List(BAND_COUNT) { i ->
                    val phase = i * 0.38
                    val wave = ((sin(t * 5.6 + phase) + 1.0) * 0.5).toFloat()
                    val bassBias = if (i < 6) punch * 0.70f else groove * 0.42f
                    (0.14f + wave * 0.34f + bassBias).coerceIn(0.08f, 0.94f)
                }
                _energy.value = AudioEnergy(
                    subBass = (0.20f + punch * 0.64f).coerceIn(0f, 1f),
                    midBass = (0.18f + groove * 0.48f).coerceIn(0f, 1f),
                    mids = (0.24f + groove * 0.34f).coerceIn(0f, 1f),
                    treble = 0.26f,
                    beatPulse = (punch * 0.82f).coerceIn(0f, 1f),
                    spectrum = bars,
                    isLiveAudio = false
                )
                t += 0.036
                delay(EMIT_INTERVAL_MS)
            }
        }
    }

    private fun stopFallback() {
        fallbackJob?.cancel()
        fallbackJob = null
    }

    private fun averageRange(arr: FloatArray, from: Int, toExclusive: Int): Float {
        if (from >= arr.size || toExclusive <= from) return 0f
        var sum = 0f
        val end = minOf(toExclusive, arr.size)
        for (i in from until end) sum += arr[i]
        return sum / (end - from)
    }

    private fun releaseVisualizer() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        attachedSessionId = -1
    }

    fun release() {
        stopFallback()
        releaseVisualizer()
    }

    companion object {
        const val BAND_COUNT = 24
        private const val EMIT_INTERVAL_MS = 36L
    }
}
