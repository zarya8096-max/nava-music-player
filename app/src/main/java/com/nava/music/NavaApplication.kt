package com.nava.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.nava.music.data.AudioArtFetcher
import com.nava.music.data.AudioArtKey

class NavaApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AudioArtFetcher.Factory(applicationContext))
                add(coil.key.Keyer<AudioArtKey> { data, _ -> "${data.contentUri}_${data.sizePx}" })
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.14)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("nava_art_cache"))
                    .maxSizeBytes(32L * 1024L * 1024L)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(160)
            .build()
    }
}
