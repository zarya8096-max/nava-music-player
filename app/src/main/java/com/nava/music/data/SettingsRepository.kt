package com.nava.music.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SettingsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("nava_prefs", Context.MODE_PRIVATE)
    private val lyricsCacheDir = File(context.filesDir, "lyrics_cache").apply { mkdirs() }

    fun loadAiConfig(): AiConfig {
        val providerName = prefs.getString("ai_provider", AiProvider.GEMINI.name) ?: AiProvider.GEMINI.name
        val provider = runCatching { AiProvider.valueOf(providerName) }.getOrDefault(AiProvider.GEMINI)
        return AiConfig(
            provider = provider,
            apiKey = prefs.getString("ai_api_key", "") ?: "",
            baseUrl = prefs.getString("ai_base_url", provider.defaultBaseUrl) ?: provider.defaultBaseUrl,
            model = prefs.getString("ai_model", provider.defaultModel) ?: provider.defaultModel,
            targetLanguage = prefs.getString("ai_target_lang", "Persian (فارسی)") ?: "Persian (فارسی)"
        )
    }

    fun saveAiConfig(config: AiConfig) {
        prefs.edit()
            .putString("ai_provider", config.provider.name)
            .putString("ai_api_key", config.apiKey.trim())
            .putString("ai_base_url", config.baseUrl.trim())
            .putString("ai_model", config.model.trim())
            .putString("ai_target_lang", config.targetLanguage.trim())
            .apply()
    }

    fun loadVisualizerStyle(): VisualizerStyle {
        val name = prefs.getString("viz_style", VisualizerStyle.HALO_AND_WAVE.name)
        return runCatching { VisualizerStyle.valueOf(name ?: "") }
            .getOrDefault(VisualizerStyle.HALO_AND_WAVE)
    }

    fun saveVisualizerStyle(style: VisualizerStyle) {
        prefs.edit().putString("viz_style", style.name).apply()
    }

    fun loadPlayerViewMode(): PlayerViewMode {
        val name = prefs.getString("player_view_mode", PlayerViewMode.VINYL.name)
        return runCatching { PlayerViewMode.valueOf(name ?: "") }
            .getOrDefault(PlayerViewMode.VINYL)
    }

    fun savePlayerViewMode(mode: PlayerViewMode) {
        prefs.edit().putString("player_view_mode", mode.name).apply()
    }

    fun loadVinylAtmosphere(): VinylAtmosphere {
        val name = prefs.getString("vinyl_atmosphere", VinylAtmosphere.AURORA.name)
        return runCatching { VinylAtmosphere.valueOf(name ?: "") }
            .getOrDefault(VinylAtmosphere.AURORA)
    }

    fun saveVinylAtmosphere(atmosphere: VinylAtmosphere) {
        prefs.edit().putString("vinyl_atmosphere", atmosphere.name).apply()
    }

    fun loadFavorites(): Set<Long> {
        val raw = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun saveFavorites(ids: Set<Long>) {
        prefs.edit().putStringSet("favorite_ids", ids.map { it.toString() }.toSet()).apply()
    }

    fun loadCachedLyrics(trackKey: String): LyricsPayload? {
        val file = File(lyricsCacheDir, "${trackKey.hashCode()}.json")
        if (!file.exists()) return null
        return runCatching {
            val json = JSONObject(file.readText())
            val arr = json.getJSONArray("lines")
            val lines = buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val ts = if (obj.isNull("ts")) null else obj.getLong("ts")
                    val orig = obj.getString("orig")
                    val tr = if (obj.isNull("tr")) null else obj.optString("tr").takeIf { it.isNotBlank() }
                    add(LyricLine(timestampMs = ts, original = orig, translation = tr))
                }
            }
            LyricsPayload(
                trackKey = json.getString("trackKey"),
                lines = lines,
                isSynced = json.optBoolean("isSynced", false),
                sourceLabel = json.optString("sourceLabel", "Cached"),
                targetLanguage = json.optString("targetLanguage").takeIf { it.isNotBlank() }
            )
        }.getOrNull()
    }

    fun saveCachedLyrics(payload: LyricsPayload) {
        runCatching {
            val file = File(lyricsCacheDir, "${payload.trackKey.hashCode()}.json")
            val arr = JSONArray()
            payload.lines.forEach { line ->
                val obj = JSONObject()
                if (line.timestampMs != null) obj.put("ts", line.timestampMs) else obj.put("ts", JSONObject.NULL)
                obj.put("orig", line.original)
                if (line.translation != null) obj.put("tr", line.translation) else obj.put("tr", JSONObject.NULL)
                arr.put(obj)
            }
            val root = JSONObject().apply {
                put("trackKey", payload.trackKey)
                put("isSynced", payload.isSynced)
                put("sourceLabel", payload.sourceLabel)
                put("targetLanguage", payload.targetLanguage ?: "")
                put("lines", arr)
            }
            file.writeText(root.toString())
        }
    }

    fun clearLyricsCache(trackKey: String) {
        val file = File(lyricsCacheDir, "${trackKey.hashCode()}.json")
        if (file.exists()) file.delete()
    }
}
