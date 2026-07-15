package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NexusPrimary,
    onPrimary = NexusOnPrimary,
    secondary = NexusSecondary,
    background = NexusBackground,
    onBackground = NexusOnSurface,
    surface = NexusSurface,
    onSurface = NexusOnSurface,
    outline = NexusOutline,
    surfaceVariant = NexusSurfaceVariant,
    error = NexusError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We force dark theme to match the Nexus controller aesthetic
    content: @Composable () -> Unit,
) {
    // We enforce the dark scheme directly to maintain the dark terminal/controller vibe
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
