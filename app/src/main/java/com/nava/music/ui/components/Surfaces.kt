package com.nava.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.nava.music.data.DynamicPalette

/**
 * Shared "cold instrument" surface language:
 * a machined panel with a faint top-lit gradient and a hairline rim that is brighter
 * on the top edge than the bottom, like anodised metal catching an overhead light.
 * No coloured glows, no blurry halos.
 */
fun Modifier.instrumentPanel(
    palette: DynamicPalette,
    shape: Shape,
    elevated: Boolean = true
): Modifier {
    val base = if (elevated) palette.surfaceElevated else palette.surface
    val lit = lerp(base, Color.White, if (palette.isDark) 0.045f else 0.55f)
    val rimTop = if (palette.isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.95f)
    val rimBottom = if (palette.isDark) Color.White.copy(alpha = 0.04f) else Color(0xFF0F172A).copy(alpha = 0.07f)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(lit, base)))
        .border(1.dp, Brush.verticalGradient(listOf(rimTop, rimBottom)), shape)
}

/** Primary action fill: a tight vertical gradient of the accent, never a flat slab or a glow. */
fun primaryFill(palette: DynamicPalette): Brush {
    val top = lerp(palette.primary, Color.White, if (palette.isDark) 0.16f else 0.10f)
    val bottom = lerp(palette.primary, Color.Black, if (palette.isDark) 0.14f else 0.12f)
    return Brush.verticalGradient(listOf(top, bottom))
}
