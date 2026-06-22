package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF381E72),
    primaryContainer = DarkSurfaceContainer,
    onPrimaryContainer = Color(0xFFE0D2FF),
    secondary = DarkSecondary,
    onSecondary = Color(0xFF332D41),
    background = DarkBackground,
    onBackground = Color(0xFFE6E1E5),
    surface = DarkSurface,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = DarkOutline,
    error = ErrorColor
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceContainer,
    onPrimaryContainer = Color(0xFF21005D),
    secondary = LightSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF1D1B20),
    surface = LightSurface,
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = Color(0xFF49454F),
    outline = LightOutline,
    error = ErrorColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
