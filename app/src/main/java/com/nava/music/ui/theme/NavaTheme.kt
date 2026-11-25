package com.nava.music.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nava.music.data.DynamicPalette

val LocalNavaPalette = staticCompositionLocalOf { DynamicThemeEngine.defaultPalette }

private val NavaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )
)

@Composable
fun NavaTheme(
    palette: DynamicPalette,
    content: @Composable () -> Unit
) {
    val animSpec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 750)

    val primary by animateColorAsState(palette.primary, animSpec, label = "primary")
    val secondary by animateColorAsState(palette.secondary, animSpec, label = "secondary")
    val tertiary by animateColorAsState(palette.tertiary, animSpec, label = "tertiary")
    val deepBg by animateColorAsState(palette.deepBackground, animSpec, label = "deepBg")
    val surfaceGlass by animateColorAsState(palette.surfaceGlass, animSpec, label = "surfaceGlass")
    val onPrimary by animateColorAsState(palette.onPrimary, animSpec, label = "onPrimary")

    val animatedPalette = palette.copy(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        deepBackground = deepBg,
        surfaceGlass = surfaceGlass,
        onPrimary = onPrimary
    )

    val colorScheme = darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        secondary = secondary,
        tertiary = tertiary,
        background = deepBg,
        surface = deepBg,
        onBackground = animatedPalette.onBackground,
        onSurface = animatedPalette.onBackground
    )

    CompositionLocalProvider(LocalNavaPalette provides animatedPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NavaTypography,
            content = content
        )
    }
}
