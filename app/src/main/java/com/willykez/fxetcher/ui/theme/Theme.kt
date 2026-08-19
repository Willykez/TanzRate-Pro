package com.willykez.fxetcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.willykez.fxetcher.data.AccentTheme
import com.willykez.fxetcher.data.ThemeMode

private data class AccentColors(val primary: Color, val secondary: Color, val tertiary: Color)

private fun accentColorsForDark(theme: AccentTheme): AccentColors = when (theme) {
    AccentTheme.GOLD -> AccentColors(Gold, Blue, Teal)
    AccentTheme.OCEAN -> AccentColors(Blue, Teal, Gold)
    AccentTheme.EMERALD -> AccentColors(Green, Teal, Gold)
    AccentTheme.SUNSET -> AccentColors(Orange, Red, Gold)
}

private fun accentColorsForLight(theme: AccentTheme): AccentColors = when (theme) {
    AccentTheme.GOLD -> AccentColors(GoldDeep, Blue, Teal)
    AccentTheme.OCEAN -> AccentColors(Blue, Teal, GoldDeep)
    AccentTheme.EMERALD -> AccentColors(EmeraldDeep, Teal, GoldDeep)
    AccentTheme.SUNSET -> AccentColors(SunsetDeep, Red, GoldDeep)
}

private fun darkSchemeFor(accent: AccentColors, amoled: Boolean): ColorScheme = darkColorScheme(
    primary = accent.primary,
    onPrimary = Color(0xFF1A1400),
    secondary = accent.secondary,
    tertiary = accent.tertiary,
    background = if (amoled) Color(0xFF000000) else DarkBg,
    surface = if (amoled) Color(0xFF000000) else DarkSurface,
    surfaceVariant = if (amoled) Color(0xFF121212) else DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = Red
)

private fun lightSchemeFor(accent: AccentColors): ColorScheme = lightColorScheme(
    primary = accent.primary,
    onPrimary = Color(0xFFFFFFFF),
    secondary = accent.secondary,
    tertiary = accent.tertiary,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = Red
)

@Composable
fun useDarkTheme(themeMode: ThemeMode): Boolean {
    val systemDark = isSystemInDarkTheme()
    return when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
}

@Composable
fun FXetcherTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    accentTheme: AccentTheme = AccentTheme.GOLD,
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = useDarkTheme(themeMode)
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        dynamicColor && supportsDynamic && useDark -> dynamicDarkColorScheme(context)
        dynamicColor && supportsDynamic && !useDark -> dynamicLightColorScheme(context)
        useDark -> darkSchemeFor(accentColorsForDark(accentTheme), amoled)
        else -> lightSchemeFor(accentColorsForLight(accentTheme))
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FXetcherTypography,
        content = content
    )
}
