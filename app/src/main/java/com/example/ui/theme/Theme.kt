package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkBackground,
    primaryContainer = ElectricBlue,
    onPrimaryContainer = Color.White,
    secondary = NeonViolet,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = NeonMagenta,
    tertiary = NeonMagenta,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = AlertPink,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent futuristic dark theme aligned with icon
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
