package com.nava.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Configures a single lightweight Coil ImageLoader across the entire app
 * with strict RAM and disk caps to prevent bitmap memory bloat.
 */
class NavaApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.14) // Strict 14% RAM budget for thumbnails & covers
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("nava_art_cache"))
                    .maxSizeBytes(32L * 1024L * 1024L) // 32 MB cap
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(180)
            .build()
    }
}
