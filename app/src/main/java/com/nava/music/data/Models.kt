package com.nava.music.data

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri,
    val dateAddedSec: Long = 0L
) {
    val cacheKey: String
        get() = "${artist.trim().lowercase()}::${title.trim().lowercase()}"
}

@Immutable
data class LyricLine(
    val timestampMs: Long?,
    val original: String,
    val translation: String? = null
)

@Immutable
data class LyricsPayload(
    val trackKey: String,
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val sourceLabel: String,
    val targetLanguage: String? = null
)

enum class AiProvider(val displayName: String, val defaultModel: String, val defaultBaseUrl: String) {
    GEMINI(
        displayName = "Google Gemini",
        defaultModel = "gemini-2.5-flash",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta"
    ),
    OPENAI_COMPATIBLE(
        displayName = "OpenAI / OpenRouter / Custom",
        defaultModel = "gpt-4o-mini",
        defaultBaseUrl = "https://api.openai.com/v1"
    )
}

@Immutable
data class AiConfig(
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = "",
    val baseUrl: String = AiProvider.GEMINI.defaultBaseUrl,
    val model: String = AiProvider.GEMINI.defaultModel,
    val targetLanguage: String = "Persian (فارسی)"
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank()
}

enum class VisualizerStyle(val label: String) {
    HALO_AND_WAVE("هاله + طیف بیس"),
    PULSE_HALO("فقط هاله نبض‌دار"),
    FLUID_SPECTRUM("طیف فرکانسی"),
    MINIMAL("مینیمال")
}

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class LibraryFilter(val label: String) {
    ALL("همه"),
    FAVORITES("علاقه‌مندی‌ها"),
    RECENT("جدیدترین‌ها"),
    ARTISTS("خواننده‌ها")
}

@Immutable
data class AudioEnergy(
    val subBass: Float = 0f,
    val midBass: Float = 0f,
    val mids: Float = 0f,
    val treble: Float = 0f,
    val beatPulse: Float = 0f,
    val spectrum: List<Float> = List(28) { 0f },
    val isLiveAudio: Boolean = false
)

@Immutable
data class DynamicPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val deepBackground: Color,
    val surfaceGlass: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val onBackgroundMuted: Color
)
