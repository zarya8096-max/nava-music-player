package com.nava.music.data

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

/**
 * Custom Coil key representing a local audio track's artwork lookup.
 */
data class AudioArtKey(
    val contentUri: Uri,
    val albumArtUri: Uri,
    val sizePx: Int
)

/**
 * Reliable Android 10–16 embedded ID3 album-art fetcher:
 * 1) ContentResolver.loadThumbnail(contentUri) (works natively with Scoped Storage on API 29+)
 * 2) MediaMetadataRetriever.embeddedPicture fallback (downsampled cleanly to avoid RAM bloat)
 * 3) Legacy MediaStore albumArtUri fallback
 */
class AudioArtFetcher(
    private val context: Context,
    private val data: AudioArtKey,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val targetSize = data.sizePx.coerceIn(96, 640)

        // 1. Modern Scoped Storage thumbnail loader (fast, cached by OS, reads embedded ID3 art)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val thumb = runCatching {
                context.contentResolver.loadThumbnail(
                    data.contentUri,
                    Size(targetSize, targetSize),
                    null
                )
            }.getOrNull()
            if (thumb != null) {
                return DrawableResult(
                    drawable = BitmapDrawable(context.resources, thumb),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            }
        }

        // 2. Direct embedded picture extraction via MediaMetadataRetriever (downsampled)
        val embedded = extractEmbeddedBitmap(targetSize)
        if (embedded != null) {
            return DrawableResult(
                drawable = BitmapDrawable(context.resources, embedded),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        }

        // 3. Legacy albumart content URI fallback
        val legacy = runCatching {
            context.contentResolver.openInputStream(data.albumArtUri)?.use { stream ->
                val bytes = stream.readBytes()
                decodeSampledBitmap(bytes, targetSize)
            }
        }.getOrNull()

        if (legacy != null) {
            return DrawableResult(
                drawable = BitmapDrawable(context.resources, legacy),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        }

        return null
    }

    private fun extractEmbeddedBitmap(reqSize: Int) = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, data.contentUri)
            val bytes = retriever.embeddedPicture ?: return@runCatching null
            decodeSampledBitmap(bytes, reqSize)
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull()

    private fun decodeSampledBitmap(bytes: ByteArray, reqSize: Int) = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (maxDim / (sample * 2) >= reqSize) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }.getOrNull()

    class Factory(private val context: Context) : Fetcher.Factory<AudioArtKey> {
        override fun create(
            data: AudioArtKey,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = AudioArtFetcher(context, data, options)
    }
}
