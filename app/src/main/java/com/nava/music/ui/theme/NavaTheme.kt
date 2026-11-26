package com.nava.music.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nava.music.R
import com.nava.music.data.DynamicPalette

val LocalNavaPalette = staticCompositionLocalOf {
    DynamicThemeEngine.resolvePalette(DynamicThemeEngine.defaultSeed, isDark = true)
}

val VazirmatnFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private val NavaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = VazirmatnFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = VazirmatnFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = VazirmatnFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = VazirmatnFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = VazirmatnFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = VazirmatnFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = VazirmatnFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    )
)

@Composable
fun NavaTheme(
    palette: DynamicPalette,
    content: @Composable () -> Unit
) {
    val animSpec = tween<Color>(durationMillis = 220)

    val primary by animateColorAsState(palette.primary, animSpec, label = "primary")
    val secondary by animateColorAsState(palette.secondary, animSpec, label = "secondary")
    val tertiary by animateColorAsState(palette.tertiary, animSpec, label = "tertiary")
    val background by animateColorAsState(palette.background, animSpec, label = "bg")
    val surface by animateColorAsState(palette.surface, animSpec, label = "surface")
    val surfaceElevated by animateColorAsState(palette.surfaceElevated, animSpec, label = "surfaceElev")
    val onPrimary by animateColorAsState(palette.onPrimary, animSpec, label = "onPrimary")
    val onBg by animateColorAsState(palette.onBackground, animSpec, label = "onBg")
    val onBgMuted by animateColorAsState(palette.onBackgroundMuted, animSpec, label = "onBgMuted")

    val animatedPalette = palette.copy(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        background = background,
        surface = surface,
        surfaceElevated = surfaceElevated,
        onPrimary = onPrimary,
        onBackground = onBg,
        onBackgroundMuted = onBgMuted
    )

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            tertiary = tertiary,
            background = background,
            surface = surface,
            onBackground = onBg,
            onSurface = onBg
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            tertiary = tertiary,
            background = background,
            surface = surface,
            onBackground = onBg,
            onSurface = onBg
        )
    }

    CompositionLocalProvider(LocalNavaPalette provides animatedPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NavaTypography,
            content = content
        )
    }
}
