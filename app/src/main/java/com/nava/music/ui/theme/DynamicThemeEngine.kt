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
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.nava.music.data.DynamicPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

object DynamicThemeEngine {

    /**
     * Hand-crafted editorial fallback palettes (never dull gray or generic neon purple).
     */
    private val curatedPalettes = listOf(
        // Warm Sunset Coral & Amber
        Triple(Color(0xFFFF6B6B), Color(0xFFFFB84D), Color(0xFF9B3D6A)),
        // Aegean Teal & Electric Mint
        Triple(Color(0xFF2DD4BF), Color(0xFF38BDF8), Color(0xFF1E5F74)),
        // Saffron Gold & Terracotta
        Triple(Color(0xFFF59E0B), Color(0xFFFB7185), Color(0xFF7C2D12)),
        // Velvet Rose & Warm Peach
        Triple(Color(0xFFF43F5E), Color(0xFFFDA4AF), Color(0xFF6B21A8)),
        // Cobalt Sky & Coral Spark
        Triple(Color(0xFF60A5FA), Color(0xFFFF8A65), Color(0xFF1D4ED8)),
        // Emerald Moss & Lime Glow
        Triple(Color(0xFF34D399), Color(0xFFFDE047), Color(0xFF065F46)),
        // Tangerine & Deep Magenta
        Triple(Color(0xFFFF7A45), Color(0xFFE879F9), Color(0xFF701A75)),
        // Turquoise Lagoon & Warm Sand
        Triple(Color(0xFF22D3EE), Color(0xFFFBBF24), Color(0xFF155E75))
    )

    val defaultPalette: DynamicPalette = buildPaletteFromTriad(
        primary = Color(0xFFFF6B81),
        secondary = Color(0xFFFFB84D),
        tertiary = Color(0xFF5B3A8C)
    )

    fun fallbackPaletteForSeed(seed: String): DynamicPalette {
        val index = abs(seed.hashCode()) % curatedPalettes.size
        val (p, s, t) = curatedPalettes[index]
        return buildPaletteFromTriad(p, s, t)
    }

    suspend fun extractFromArtwork(
        context: Context,
        artUri: Uri,
        fallbackSeed: String
    ): DynamicPalette = withContext(Dispatchers.IO) {
        val bitmap = loadBitmap(context, artUri) ?: return@withContext fallbackPaletteForSeed(fallbackSeed)
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()

        val vibrant = palette.vibrantSwatch?.rgb
        val lightVibrant = palette.lightVibrantSwatch?.rgb
        val darkVibrant = palette.darkVibrantSwatch?.rgb
        val dominant = palette.dominantSwatch?.rgb
        val muted = palette.mutedSwatch?.rgb

        val primaryInt = vibrant ?: lightVibrant ?: dominant
            ?: return@withContext fallbackPaletteForSeed(fallbackSeed)
        val secondaryInt = lightVibrant ?: muted ?: vibrant ?: primaryInt
        val tertiaryInt = darkVibrant ?: dominant ?: primaryInt

        val primaryColor = boostVibrance(Color(primaryInt), minSat = 0.52f, minLight = 0.54f, maxLight = 0.72f)
        val secondaryColor = boostVibrance(Color(secondaryInt), minSat = 0.45f, minLight = 0.58f, maxLight = 0.78f)
        val tertiaryColor = boostVibrance(Color(tertiaryInt), minSat = 0.38f, minLight = 0.24f, maxLight = 0.42f)

        buildPaletteFromTriad(primaryColor, secondaryColor, tertiaryColor)
    }

    private fun buildPaletteFromTriad(
        primary: Color,
        secondary: Color,
        tertiary: Color
    ): DynamicPalette {
        val deepBg = darkenAndTint(tertiary, targetLightness = 0.08f, saturationFactor = 0.65f)
        val surfaceGlass = darkenAndTint(primary, targetLightness = 0.16f, saturationFactor = 0.42f)
            .copy(alpha = 0.52f)
        val onPrimary = if (primary.luminance() > 0.42f) Color(0xFF100E14) else Color(0xFFFFFFFF)

        return DynamicPalette(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            deepBackground = deepBg,
            surfaceGlass = surfaceGlass,
            onPrimary = onPrimary,
            onBackground = Color(0xFFFAF8FC),
            onBackgroundMuted = Color(0xFFC8C2D4)
        )
    }

    private fun boostVibrance(
        color: Color,
        minSat: Float,
        minLight: Float,
        maxLight: Float
    ): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        hsl[1] = hsl[1].coerceIn(minSat, 0.92f)
        hsl[2] = hsl[2].coerceIn(minLight, maxLight)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    private fun darkenAndTint(
        color: Color,
        targetLightness: Float,
        saturationFactor: Float
    ): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        hsl[1] = (hsl[1] * saturationFactor).coerceIn(0.18f, 0.58f)
        hsl[2] = targetLightness.coerceIn(0.05f, 0.22f)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    private suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(160, 160)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else {
                null
            }
        }.getOrNull()
    }
}
