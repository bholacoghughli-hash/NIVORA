package com.nivora.browser.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nivora.browser.data.local.AccentColor
import com.nivora.browser.data.local.AppThemeMode

@Composable
fun NivoraTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.CYAN,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }
    val isAmoled = themeMode == AppThemeMode.AMOLED

    val primary = getAccentPrimary(accentColor)
    val secondary = getAccentSecondary(accentColor)

    val colorScheme = when {
        isAmoled -> darkColorScheme(
            primary = primary,
            secondary = secondary,
            background = AmoledBackground,
            surface = AmoledSurface,
            surfaceVariant = AmoledCard,
            outline = AmoledBorder,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFD4D4D8),
            onPrimary = Color.Black
        )
        isDark -> darkColorScheme(
            primary = primary,
            secondary = secondary,
            background = NivoraNavyDark,
            surface = NivoraNavySurface,
            surfaceVariant = NivoraNavyCard,
            outline = NivoraBorder,
            onBackground = Color(0xFFF1F5F9),
            onSurface = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF94A3B8),
            onPrimary = Color.Black
        )
        else -> lightColorScheme(
            primary = primary,
            secondary = secondary,
            background = NivoraLightBg,
            surface = NivoraLightSurface,
            surfaceVariant = NivoraLightCard,
            outline = NivoraLightBorder,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF475569),
            onPrimary = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
