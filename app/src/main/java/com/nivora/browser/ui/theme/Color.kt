package com.nivora.browser.ui.theme

import androidx.compose.ui.graphics.Color
import com.nivora.browser.data.local.AccentColor

// NIVORA Brand Core Colors
val NivoraNavyDark = Color(0xFF0A0F1D)
val NivoraNavySurface = Color(0xFF111827)
val NivoraNavyCard = Color(0xFF1E293B)
val NivoraBorder = Color(0xFF334155)

// AMOLED True Black Palette
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0A0A)
val AmoledCard = Color(0xFF121212)
val AmoledBorder = Color(0xFF262626)

// Light Palette
val NivoraLightBg = Color(0xFFF8FAFC)
val NivoraLightSurface = Color(0xFFFFFFFF)
val NivoraLightCard = Color(0xFFF1F5F9)
val NivoraLightBorder = Color(0xFFE2E8F0)

// Accent Colors
fun getAccentPrimary(accent: AccentColor): Color {
    return when (accent) {
        AccentColor.CYAN -> Color(0xFF00B4D8)
        AccentColor.ELECTRIC_BLUE -> Color(0xFF0284C7)
        AccentColor.EMERALD -> Color(0xFF10B981)
        AccentColor.PURPLE -> Color(0xFF8B5CF6)
        AccentColor.AMBER -> Color(0xFFF59E0B)
    }
}

fun getAccentSecondary(accent: AccentColor): Color {
    return when (accent) {
        AccentColor.CYAN -> Color(0xFF0077B6)
        AccentColor.ELECTRIC_BLUE -> Color(0xFF0369A1)
        AccentColor.EMERALD -> Color(0xFF059669)
        AccentColor.PURPLE -> Color(0xFF7C3AED)
        AccentColor.AMBER -> Color(0xFFD97706)
    }
}
