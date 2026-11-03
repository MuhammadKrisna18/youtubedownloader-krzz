package com.personal.ytmusicdownloader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = LightSurface,
    primaryContainer = RedLight,
    onPrimaryContainer = RedDark,
    secondary = RedDark,
    onSecondary = LightSurface,
    background = LightBackground,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun YTDownloaderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
