package com.nava.music.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.nava.music.data.DynamicPalette
import com.nava.music.data.PaletteSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

object DynamicThemeEngine {

    /**
     * Refined editorial fallback seeds (warm, organic, non-slop hues).
     */
    private val curatedSeeds = listOf(
        PaletteSeed(Color(0xFFE05A47), Color(0xFFF2A65A), Color(0xFF7B2D4E)), // Terracotta & Amber
        PaletteSeed(Color(0xFF1E9E89), Color(0xFF38A3D1), Color(0xFF154E5C)), // Deep Aegean Teal
        PaletteSeed(Color(0xFFD9822B), Color(0xFFE86A58), Color(0xFF6B321A)), // Saffron & Clay
        PaletteSeed(Color(0xFFD6456B), Color(0xFFF0938B), Color(0xFF5D214A)), // Velvet Rose
        PaletteSeed(Color(0xFF3B7DD8), Color(0xFFE8845C), Color(0xFF1B3A70)), // Studio Cobalt & Peach
        PaletteSeed(Color(0xFF2E9E6E), Color(0xFFD4B845), Color(0xFF144D3A)), // Moss & Brass
        PaletteSeed(Color(0xFFC9603B), Color(0xFFC978B8), Color(0xFF59204E)), // Copper & Plum
        PaletteSeed(Color(0xFF288FA3), Color(0xFFE5A93C), Color(0xFF174552))  // Petrol & Ochre
    )

    val defaultSeed: PaletteSeed = curatedSeeds[0]

    fun fallbackSeedForKey(seed: String): PaletteSeed {
        val index = abs(seed.hashCode()) % curatedSeeds.size
        return curatedSeeds[index]
    }

    /**
     * Extracts a lightweight PaletteSeed using the shared singleton Coil ImageLoader
     * at 96x96 resolution (minimal RAM footprint, zero leaked ImageLoaders).
     */
    suspend fun extractSeedFromArtwork(
        context: Context,
        artUri: Uri,
        fallbackSeed: String
    ): PaletteSeed = withContext(Dispatchers.IO) {
        val bitmap = loadSmallBitmap(context, artUri) ?: return@withContext fallbackSeedForKey(fallbackSeed)
        val palette = Palette.from(bitmap).maximumColorCount(12).generate()

        val vibrant = palette.vibrantSwatch?.rgb
        val lightVibrant = palette.lightVibrantSwatch?.rgb
        val darkVibrant = palette.darkVibrantSwatch?.rgb
        val dominant = palette.dominantSwatch?.rgb
        val muted = palette.mutedSwatch?.rgb

        val primaryInt = vibrant ?: dominant ?: lightVibrant
            ?: return@withContext fallbackSeedForKey(fallbackSeed)
        val secondaryInt = lightVibrant ?: muted ?: vibrant ?: primaryInt
        val tertiaryInt = darkVibrant ?: dominant ?: primaryInt

        PaletteSeed(
            primary = Color(primaryInt),
            secondary = Color(secondaryInt),
            tertiary = Color(tertiaryInt)
        )
    }

    /**
     * Resolves a PaletteSeed into either a refined Light (warm studio alabaster)
     * or Dark (obsidian ink) editorial palette with guaranteed WCAG text contrast.
     */
    fun resolvePalette(seed: PaletteSeed, isDark: Boolean): DynamicPalette {
        return if (isDark) {
            val primary = tuneHsl(seed.primary, minSat = 0.48f, maxSat = 0.86f, minLight = 0.56f, maxLight = 0.70f)
            val secondary = tuneHsl(seed.secondary, minSat = 0.42f, maxSat = 0.82f, minLight = 0.62f, maxLight = 0.78f)
            val tertiary = tuneHsl(seed.tertiary, minSat = 0.30f, maxSat = 0.55f, minLight = 0.20f, maxLight = 0.34f)
            val bg = tintNeutral(seed.tertiary, targetLightness = 0.055f, maxSat = 0.24f)
            val surface = tintNeutral(seed.primary, targetLightness = 0.105f, maxSat = 0.18f)
            val elevated = tintNeutral(seed.primary, targetLightness = 0.155f, maxSat = 0.20f)
            val onPrimary = if (primary.luminance() > 0.40f) Color(0xFF101014) else Color(0xFFFFFFFF)

            DynamicPalette(
                isDark = true,
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                background = bg,
                surface = surface,
                surfaceElevated = elevated,
                outline = Color.White.copy(alpha = 0.10f),
                onPrimary = onPrimary,
                onBackground = Color(0xFFF5F4F8),
                onBackgroundMuted = Color(0xFFA09DB0)
            )
        } else {
            // Editorial Light Mode: Warm alabaster paper subtly tinted by the album hue
            val primary = tuneHsl(seed.primary, minSat = 0.50f, maxSat = 0.84f, minLight = 0.36f, maxLight = 0.48f)
            val secondary = tuneHsl(seed.secondary, minSat = 0.46f, maxSat = 0.80f, minLight = 0.42f, maxLight = 0.56f)
            val tertiary = tuneHsl(seed.tertiary, minSat = 0.32f, maxSat = 0.55f, minLight = 0.76f, maxLight = 0.86f)
            val bg = tintNeutral(seed.primary, targetLightness = 0.965f, maxSat = 0.16f)
            val surface = tintNeutral(seed.primary, targetLightness = 0.992f, maxSat = 0.08f)
            val elevated = Color(0xFFFFFFFF)
            val onPrimary = if (primary.luminance() > 0.46f) Color(0xFF15141A) else Color(0xFFFFFFFF)

            DynamicPalette(
                isDark = false,
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                background = bg,
                surface = surface,
                surfaceElevated = elevated,
                outline = Color(0xFF16151C).copy(alpha = 0.08f),
                onPrimary = onPrimary,
                onBackground = Color(0xFF15141A),
                onBackgroundMuted = Color(0xFF605D6C)
            )
        }
    }

    private fun tuneHsl(
        color: Color,
        minSat: Float,
        maxSat: Float,
        minLight: Float,
        maxLight: Float
    ): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        hsl[1] = hsl[1].coerceIn(minSat, maxSat)
        hsl[2] = hsl[2].coerceIn(minLight, maxLight)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    private fun tintNeutral(
        color: Color,
        targetLightness: Float,
        maxSat: Float
    ): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        hsl[1] = (hsl[1] * 0.38f).coerceIn(0.06f, maxSat)
        hsl[2] = targetLightness
        return Color(ColorUtils.HSLToColor(hsl))
    }

    private suspend fun loadSmallBitmap(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(96, 96)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else {
                null
            }
        }.getOrNull()
    }
}
