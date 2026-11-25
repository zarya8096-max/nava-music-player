package com.nava.music.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Ultra-fast, zero-lag thumbnail & cover art cache designed for 120fps scrolling over 1,000+ tracks:
 * - In-memory LruCache holds ~500 RGB_565 thumbnails (128x128 = 32KB each -> ~16MB cap)
 * - Synchronous O(1) lookup for both cached thumbnails AND known-missing covers (zero recompositions)
 * - Persistent local JPEG thumbnail cache in app cacheDir (avoids MediaProvider IPC on future runs)
 * - List scrolling NEVER invokes MediaMetadataRetriever (zero native mediaserver stalls)
 */
object ArtCacheManager {

    private const val THUMB_SIZE = 128

    // 16 MB in-memory LRU cache (~500 thumbnails at 128x128 RGB_565)
    private val thumbMemoryCache = object : LruCache<Long, ImageBitmap>(16 * 1024) {
        override fun sizeOf(key: Long, value: ImageBitmap): Int {
            // Approximate KB for RGB_565 (2 bytes per pixel)
            return ((value.width * value.height * 2) / 1024).coerceAtLeast(1)
        }
    }

    // Separate tiny cache for full-size Now Playing covers (max 4 covers)
    private val fullMemoryCache = LruCache<Long, ImageBitmap>(4)

    // Synchronous O(1) negative cache for tracks without cover art
    private val missingThumbIds = ConcurrentHashMap<Long, Boolean>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val thumbDispatcher = Dispatchers.IO.limitedParallelism(2)

    private fun thumbFile(context: Context, trackId: Long): File {
        val dir = File(context.cacheDir, "nava_thumbs").apply { if (!exists()) mkdirs() }
        return File(dir, "$trackId.jpg")
    }

    fun getCachedThumbnail(trackId: Long): ImageBitmap? = thumbMemoryCache.get(trackId)

    fun isKnownMissing(trackId: Long): Boolean = missingThumbIds.containsKey(trackId)

    fun getCachedFullCover(trackId: Long): ImageBitmap? = fullMemoryCache.get(trackId)

    /**
     * Loads a lightweight 128x128 RGB_565 thumbnail for scrolling lists.
     * Strictly avoids MediaMetadataRetriever so fast flinging stays silky 120fps.
     */
    suspend fun loadThumbnail(context: Context, track: Track): ImageBitmap? {
        thumbMemoryCache.get(track.id)?.let { return it }
        if (missingThumbIds.containsKey(track.id)) return null

        return withContext(thumbDispatcher) {
            thumbMemoryCache.get(track.id)?.let { return@withContext it }

            val diskFile = thumbFile(context, track.id)

            // 1. Fast local disk cache hit (no Binder IPC)
            if (diskFile.exists()) {
                val bmp = decodeFileRgb565(diskFile)
                if (bmp != null) {
                    val img = bmp.asImageBitmap()
                    thumbMemoryCache.put(track.id, img)
                    return@withContext img
                }
            }

            // 2. OS Scoped-Storage thumbnail (Android 10+) or legacy albumart URI
            val loaded: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    context.contentResolver.loadThumbnail(
                        track.contentUri,
                        Size(THUMB_SIZE, THUMB_SIZE),
                        null
                    )
                }.getOrNull()
            } else {
                loadFromUriSampled(context, track.albumArtUri, THUMB_SIZE)
            }

            if (loaded == null) {
                missingThumbIds[track.id] = true
                return@withContext null
            }

            // Downscale & convert to lightweight RGB_565 and persist tiny JPEG to disk
            val small = scaleToRgb565(loaded, THUMB_SIZE)
            if (small !== loaded) loaded.recycle()

            runCatching {
                FileOutputStream(diskFile).use { out ->
                    small.compress(Bitmap.CompressFormat.JPEG, 78, out)
                }
            }

            val img = small.asImageBitmap()
            thumbMemoryCache.put(track.id, img)
            img
        }
    }

    /**
     * Loads a crisp 512px cover for the single Now Playing / Vinyl screen.
     * Safe to use MediaMetadataRetriever fallback here because only 1 track is loaded at a time.
     */
    suspend fun loadFullCover(context: Context, track: Track): ImageBitmap? {
        fullMemoryCache.get(track.id)?.let { return it }
        return withContext(Dispatchers.IO) {
            val bmp = loadFullBitmap(context, track.contentUri, track.albumArtUri, 512)
                ?: return@withContext null
            val img = bmp.asImageBitmap()
            fullMemoryCache.put(track.id, img)
            img
        }
    }

    /**
     * Loads a raw Android Bitmap (used by DynamicThemeEngine Palette extraction & Full Cover).
     */
    suspend fun loadFullBitmap(
        context: Context,
        contentUri: Uri,
        albumArtUri: Uri,
        targetSize: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                context.contentResolver.loadThumbnail(contentUri, Size(targetSize, targetSize), null)
            }.getOrNull()?.let { return@withContext it }
        }

        // Single-track embedded ID3 fallback
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, contentUri)
                val bytes = retriever.embeddedPicture ?: return@runCatching null
                decodeBytesSampled(bytes, targetSize)
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()?.let { return@withContext it }

        loadFromUriSampled(context, albumArtUri, targetSize)
    }

    private fun decodeFileRgb565(file: File): Bitmap? = runCatching {
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(file.absolutePath, opts)
    }.getOrNull()

    private fun loadFromUriSampled(context: Context, uri: Uri, reqSize: Int): Bitmap? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            decodeBytesSampled(stream.readBytes(), reqSize)
        }
    }.getOrNull()

    private fun decodeBytesSampled(bytes: ByteArray, reqSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (maxDim / (sample * 2) >= reqSize) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun scaleToRgb565(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        val scaled = if (w > maxDim || h > maxDim) {
            val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
            Bitmap.createScaledBitmap(
                src,
                (w * ratio).toInt().coerceAtLeast(1),
                (h * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            src
        }
        if (scaled.config == Bitmap.Config.RGB_565) return scaled
        val converted = scaled.copy(Bitmap.Config.RGB_565, false)
        if (scaled !== src) scaled.recycle()
        return converted ?: scaled
    }
}
