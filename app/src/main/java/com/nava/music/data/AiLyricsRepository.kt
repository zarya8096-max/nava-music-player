package com.nava.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiLyricsRepository(
    private val settingsRepository: SettingsRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val lrcRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?](.*)""")

    /**
     * Fetches lyrics from cache -> LRCLIB (synced/plain) -> AI API fallback.
     */
    suspend fun fetchLyrics(
        track: Track,
        aiConfig: AiConfig,
        forceRefresh: Boolean = false
    ): Result<LyricsPayload> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            settingsRepository.loadCachedLyrics(track.cacheKey)?.let { cached ->
                return@withContext Result.success(cached)
            }
        }

        // 1) Try LRCLIB first for accurate synced timestamps
        val lrcLibResult = fetchFromLrcLib(track)
        if (lrcLibResult != null && lrcLibResult.lines.isNotEmpty()) {
            settingsRepository.saveCachedLyrics(lrcLibResult)
            return@withContext Result.success(lrcLibResult)
        }

        // 2) Fallback to AI API for extracting accurate lyrics
        if (!aiConfig.isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("لیریک این آهنگ در دیتابیس آنلاین پیدا نشد. برای استخراج هوشمند لیریک و ترجمه، کلید API هوش مصنوعی را در تنظیمات وارد کنید.")
            )
        }

        runCatching {
            val prompt = buildString {
                appendLine("You are an expert music lyrics archivist.")
                appendLine("Provide the exact, complete lyrics for this song:")
                appendLine("Title: ${track.title}")
                appendLine("Artist: ${track.artist}")
                if (track.album != "Single" && track.album.isNotBlank()) {
                    appendLine("Album: ${track.album}")
                }
                appendLine()
                appendLine("Rules:")
                appendLine("1. Output ONLY the clean song lyrics, line by line, with blank lines between verses.")
                appendLine("2. Do NOT add introductions, disclaimers, markdown code fences, or commentary.")
                appendLine("3. Keep the original language of the song accurately.")
            }

            val rawLyrics = callAiModel(aiConfig, prompt).trim()
            val cleanedLines = rawLyrics.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("```") }
                .map { LyricLine(timestampMs = null, original = it, translation = null) }

            if (cleanedLines.isEmpty()) {
                error("پاسخ معتبری از سرویس هوش مصنوعی دریافت نشد.")
            }

            val payload = LyricsPayload(
                trackKey = track.cacheKey,
                lines = cleanedLines,
                isSynced = false,
                sourceLabel = "AI (${aiConfig.model})",
                targetLanguage = null
            )
            settingsRepository.saveCachedLyrics(payload)
            payload
        }
    }

    /**
     * Translates an existing LyricsPayload line-by-line while preserving synced timestamps.
     */
    suspend fun translateLyrics(
        track: Track,
        existing: LyricsPayload,
        aiConfig: AiConfig
    ): Result<LyricsPayload> = withContext(Dispatchers.IO) {
        if (!aiConfig.isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("برای ترجمه لیریک، ابتدا کلید API هوش مصنوعی را در بخش تنظیمات وارد کنید.")
            )
        }

        runCatching {
            val indexedInput = existing.lines.mapIndexed { idx, line ->
                "$idx|||${line.original}"
            }.joinToString("\n")

            val prompt = buildString {
                appendLine("You are a poetic, accurate song translator.")
                appendLine("Translate the following song lyrics for \"${track.title}\" by \"${track.artist}\" into ${aiConfig.targetLanguage}.")
                appendLine()
                appendLine("STRICT FORMAT RULES:")
                appendLine("1. Input lines use the format: INDEX|||ORIGINAL_LINE")
                appendLine("2. Return a valid JSON array of objects, one for each index, in exact order:")
                appendLine("""   [{"i": 0, "t": "translated line"}, ...]""")
                appendLine("3. Preserve emotional tone, poetic flow, and accurate meaning.")
                appendLine("4. If a line is purely instrumental or a vocalization (e.g. '♪', 'Ooh', 'Yeah'), keep it minimal or natural.")
                appendLine("5. Output ONLY the JSON array. No markdown fences, no extra text.")
                appendLine()
                appendLine("LYRICS:")
                appendLine(indexedInput)
            }

            val rawResponse = callAiModel(aiConfig, prompt).trim()
            val translations = parseTranslationJson(rawResponse)

            val updatedLines = existing.lines.mapIndexed { index, line ->
                line.copy(translation = translations[index] ?: line.translation)
            }

            val updatedPayload = existing.copy(
                lines = updatedLines,
                targetLanguage = aiConfig.targetLanguage,
                sourceLabel = if (existing.sourceLabel.contains("AI")) {
                    existing.sourceLabel
                } else {
                    "${existing.sourceLabel} + AI"
                }
            )
            settingsRepository.saveCachedLyrics(updatedPayload)
            updatedPayload
        }
    }

    private fun fetchFromLrcLib(track: Track): LyricsPayload? {
        return runCatching {
            val urlBuilder = "https://lrclib.net/api/get".toHttpUrlOrNull()?.newBuilder()
                ?: return null
            urlBuilder.addQueryParameter("track_name", track.title)
            if (track.artist != "Unknown Artist") {
                urlBuilder.addQueryParameter("artist_name", track.artist)
            }
            if (track.album != "Single" && track.album.isNotBlank()) {
                urlBuilder.addQueryParameter("album_name", track.album)
            }
            if (track.durationMs > 0) {
                urlBuilder.addQueryParameter("duration", (track.durationMs / 1000L).toString())
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "NavaMusicPlayer/1.0 (Android)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)

                val synced = json.optString("syncedLyrics", "").trim()
                if (synced.isNotBlank() && synced != "null") {
                    val parsed = parseLrc(synced)
                    if (parsed.isNotEmpty()) {
                        return LyricsPayload(
                            trackKey = track.cacheKey,
                            lines = parsed,
                            isSynced = true,
                            sourceLabel = "LRCLIB Synced"
                        )
                    }
                }

                val plain = json.optString("plainLyrics", "").trim()
                if (plain.isNotBlank() && plain != "null") {
                    val parsed = plain.lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map { LyricLine(timestampMs = null, original = it) }
                    if (parsed.isNotEmpty()) {
                        return LyricsPayload(
                            trackKey = track.cacheKey,
                            lines = parsed,
                            isSynced = false,
                            sourceLabel = "LRCLIB"
                        )
                    }
                }
                null
            }
        }.getOrNull()
    }

    private fun parseLrc(raw: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        raw.lines().forEach { line ->
            val match = lrcRegex.find(line.trim()) ?: return@forEach
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val fracStr = match.groupValues[3]
            val millis = when (fracStr.length) {
                1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                3 -> fracStr.toLongOrNull() ?: 0L
                else -> 0L
            }
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) {
                val ts = (min * 60_000L) + (sec * 1_000L) + millis
                result.add(LyricLine(timestampMs = ts, original = text))
            }
        }
        return result.sortedBy { it.timestampMs ?: Long.MAX_VALUE }
    }

    private fun callAiModel(config: AiConfig, prompt: String): String {
        return when (config.provider) {
            AiProvider.GEMINI -> callGemini(config, prompt)
            AiProvider.OPENAI_COMPATIBLE -> callOpenAiCompatible(config, prompt)
        }
    }

    private fun callGemini(config: AiConfig, prompt: String): String {
        val base = config.baseUrl.trimEnd('/').ifBlank { AiProvider.GEMINI.defaultBaseUrl }
        val model = config.model.ifBlank { AiProvider.GEMINI.defaultModel }
        val url = "$base/models/$model:generateContent?key=${config.apiKey.trim()}"

        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.35)
                }
            )
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("خطای Gemini API (${response.code}): ${extractApiError(bodyStr)}")
            }
            val json = JSONObject(bodyStr)
            val candidates = json.optJSONArray("candidates")
                ?: error("پاسخی از Gemini دریافت نشد.")
            val first = candidates.optJSONObject(0)
                ?: error("پاسخ Gemini خالی است.")
            val parts = first.optJSONObject("content")?.optJSONArray("parts")
                ?: error("محتوای Gemini یافت نشد.")
            return buildString {
                for (i in 0 until parts.length()) {
                    append(parts.optJSONObject(i)?.optString("text").orEmpty())
                }
            }
        }
    }

    private fun callOpenAiCompatible(config: AiConfig, prompt: String): String {
        val base = config.baseUrl.trimEnd('/').ifBlank { AiProvider.OPENAI_COMPATIBLE.defaultBaseUrl }
        val model = config.model.ifBlank { AiProvider.OPENAI_COMPATIBLE.defaultModel }
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"

        val payload = JSONObject().apply {
            put("model", model)
            put("temperature", 0.35)
            put(
                "messages",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }
                )
            )
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey.trim()}")
            .header("HTTP-Referer", "https://nava.music.app")
            .header("X-Title", "Nava Music Player")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("خطای API (${response.code}): ${extractApiError(bodyStr)}")
            }
            val json = JSONObject(bodyStr)
            val choices = json.optJSONArray("choices")
                ?: error("پاسخی از سرور AI دریافت نشد.")
            val msg = choices.optJSONObject(0)?.optJSONObject("message")
                ?: error("ساختار پاسخ AI نامعتبر است.")
            return msg.optString("content", "")
        }
    }

    private fun parseTranslationJson(raw: String): Map<Int, String> {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        if (start == -1 || end <= start) {
            // Graceful fallback: map line by line if model ignored JSON format
            return raw.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("```") }
                .mapIndexed { idx, line -> idx to line.substringAfter("|||").trim() }
                .toMap()
        }
        val slice = raw.substring(start, end + 1)
        val arr = JSONArray(slice)
        val map = mutableMapOf<Int, String>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val idx = obj.optInt("i", i)
            val text = obj.optString("t", "").trim()
            if (text.isNotEmpty()) {
                map[idx] = text
            }
        }
        return map
    }

    private fun extractApiError(body: String): String {
        return runCatching {
            val obj = JSONObject(body)
            obj.optJSONObject("error")?.optString("message")
                ?: obj.optString("message")
                ?: body.take(140)
        }.getOrDefault(body.take(140))
    }
}
