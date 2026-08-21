package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    primaryContainer = ImmersiveComponent,
    onPrimaryContainer = ImmersivePrimary,
    secondary = ImmersiveStatusAmber,
    onSecondary = ImmersiveBg,
    secondaryContainer = ImmersiveComponent,
    onSecondaryContainer = ImmersiveStatusAmber,
    tertiary = ImmersiveStatusGreen,
    onTertiary = ImmersiveBg,
    background = ImmersiveBg,
    onBackground = ImmersiveTextPrimary,
    surface = ImmersiveSurface,
    onSurface = ImmersiveTextPrimary,
    surfaceVariant = ImmersiveComponent,
    onSurfaceVariant = ImmersiveTextSecondary,
    outline = ImmersiveBorderSubtle,
    outlineVariant = ImmersiveBorderVerySubtle,
    error = ImmersiveStatusRed,
    onError = ImmersiveBg
)

private val LightColorScheme = DarkColorScheme // Preserving deep immersive styling across modes

@Composable
fun BinBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BinBoxTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
