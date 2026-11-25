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
     * Cool, architectural Nordic / Hi-Fi fallback seeds — zero pink, coral, or magenta.
     */
    private val curatedSeeds = listOf(
        PaletteSeed(Color(0xFF38BDF8), Color(0xFF81A1C1), Color(0xFF0F2942)), // Glacier Blue & Arctic Slate
        PaletteSeed(Color(0xFF2DD4BF), Color(0xFF38BDF8), Color(0xFF0F3B42)), // Nordic Fjord Teal
        PaletteSeed(Color(0xFF60A5FA), Color(0xFF93C5FD), Color(0xFF172B4D)), // Deep Sapphire Frost
        PaletteSeed(Color(0xFF34D399), Color(0xFF67E8F9), Color(0xFF0E3833)), // Pine Mint & Ice
        PaletteSeed(Color(0xFF7DD3FC), Color(0xFF94A3B8), Color(0xFF1E293B)), // Mist Cyan & Titanium
        PaletteSeed(Color(0xFF22D3EE), Color(0xFF60A5FA), Color(0xFF103344)), // Polar Cyan & Steel
        PaletteSeed(Color(0xFF4ADE80), Color(0xFF2DD4BF), Color(0xFF11352C)), // Boreal Emerald
        PaletteSeed(Color(0xFF94A3B8), Color(0xFF38BDF8), Color(0xFF1B2432))  // Brushed Silver & Sky
    )

    val defaultSeed: PaletteSeed = curatedSeeds[0]

    fun fallbackSeedForKey(seed: String): PaletteSeed {
        val index = abs(seed.hashCode()) % curatedSeeds.size
        return curatedSeeds[index]
    }

    /**
     * Extracts album-art colors at 96x96 via singleton Coil loader (minimal RAM).
     * Filters out pink/magenta hues in favor of cool complementary swatches from the cover
     * (or falls back to cool architectural tones if the cover is overwhelmingly pink).
     */
    suspend fun extractSeedFromArtwork(
        context: Context,
        artUri: Uri,
        fallbackSeed: String
    ): PaletteSeed = withContext(Dispatchers.IO) {
        val bitmap = loadSmallBitmap(context, artUri) ?: return@withContext fallbackSeedForKey(fallbackSeed)
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()

        val candidates = listOfNotNull(
            palette.vibrantSwatch?.rgb,
            palette.lightVibrantSwatch?.rgb,
            palette.dominantSwatch?.rgb,
            palette.mutedSwatch?.rgb,
            palette.darkVibrantSwatch?.rgb,
            palette.darkMutedSwatch?.rgb
        ).map { Color(it) }

        val nonPink = candidates.filterNot { isPinkOrMagenta(it) }
        if (nonPink.isEmpty()) {
            return@withContext fallbackSeedForKey(fallbackSeed)
        }

        val primary = nonPink[0]
        val secondary = nonPink.getOrElse(1) { shiftCool(primary, +18f) }
        val tertiary = nonPink.lastOrNull() ?: shiftCool(primary, -15f)

        PaletteSeed(primary = primary, secondary = secondary, tertiary = tertiary)
    }

    /**
     * Resolves a PaletteSeed into a crisp, cool-toned Light (Nordic Frost) or Dark (Arctic Obsidian)
     * palette with WCAG-compliant contrast.
     */
    fun resolvePalette(seed: PaletteSeed, isDark: Boolean): DynamicPalette {
        return if (isDark) {
            val primary = tuneHsl(seed.primary, minSat = 0.44f, maxSat = 0.78f, minLight = 0.56f, maxLight = 0.70f)
            val secondary = tuneHsl(seed.secondary, minSat = 0.36f, maxSat = 0.70f, minLight = 0.64f, maxLight = 0.78f)
            val tertiary = tuneHsl(seed.tertiary, minSat = 0.24f, maxSat = 0.46f, minLight = 0.16f, maxLight = 0.28f)
            val bg = blendCoolBase(Color(0xFF080C12), seed.tertiary, tintAmount = 0.14f)
            val surface = blendCoolBase(Color(0xFF111824), seed.primary, tintAmount = 0.08f)
            val elevated = blendCoolBase(Color(0xFF17202E), seed.primary, tintAmount = 0.10f)
            val onPrimary = if (primary.luminance() > 0.42f) Color(0xFF090D14) else Color(0xFFF8FAFC)

            DynamicPalette(
                isDark = true,
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                background = bg,
                surface = surface,
                surfaceElevated = elevated,
                outline = Color(0xFFE2E8F0).copy(alpha = 0.10f),
                onPrimary = onPrimary,
                onBackground = Color(0xFFF1F5F9),
                onBackgroundMuted = Color(0xFF94A3B8)
            )
        } else {
            // Crisp Arctic Frost Light Mode — clean cool alabaster & slate ink
            val primary = tuneHsl(seed.primary, minSat = 0.48f, maxSat = 0.80f, minLight = 0.34f, maxLight = 0.46f)
            val secondary = tuneHsl(seed.secondary, minSat = 0.40f, maxSat = 0.72f, minLight = 0.40f, maxLight = 0.54f)
            val tertiary = tuneHsl(seed.tertiary, minSat = 0.22f, maxSat = 0.42f, minLight = 0.82f, maxLight = 0.90f)
            val bg = blendCoolBase(Color(0xFFF3F6FA), seed.primary, tintAmount = 0.05f)
            val surface = blendCoolBase(Color(0xFFEAEFF5), seed.primary, tintAmount = 0.04f)
            val elevated = Color(0xFFFFFFFF)
            val onPrimary = if (primary.luminance() > 0.46f) Color(0xFF0F172A) else Color(0xFFFFFFFF)

            DynamicPalette(
                isDark = false,
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                background = bg,
                surface = surface,
                surfaceElevated = elevated,
                outline = Color(0xFF0F172A).copy(alpha = 0.08f),
                onPrimary = onPrimary,
                onBackground = Color(0xFF0F172A),
                onBackgroundMuted = Color(0xFF526074)
            )
        }
    }

    private fun isPinkOrMagenta(color: Color): Boolean {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        val hue = hsl[0]
        val sat = hsl[1]
        return sat > 0.16f && (hue in 288f..356f)
    }

    private fun shiftCool(color: Color, deltaHue: Float): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        hsl[0] = (hsl[0] + deltaHue + 360f) % 360f
        if (hsl[0] in 288f..356f) hsl[0] = 205f
        return Color(ColorUtils.HSLToColor(hsl))
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
        if (hsl[0] in 288f..356f) hsl[0] = 205f
        hsl[1] = hsl[1].coerceIn(minSat, maxSat)
        hsl[2] = hsl[2].coerceIn(minLight, maxLight)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    private fun blendCoolBase(base: Color, tint: Color, tintAmount: Float): Color {
        return Color(
            red = base.red * (1f - tintAmount) + tint.red * tintAmount,
            green = base.green * (1f - tintAmount) + tint.green * tintAmount,
            blue = base.blue * (1f - tintAmount) + tint.blue * tintAmount,
            alpha = 1f
        )
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
