package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FaceSwapDarkColorScheme = darkColorScheme(
    primary = MintCyan,
    onPrimary = ObsidianBg,
    primaryContainer = MintCyanDim,
    onPrimaryContainer = MintCyan,
    secondary = TextMuted,
    onSecondary = ObsidianBg,
    error = CoralCrimson,
    onError = TextPrimary,
    errorContainer = CoralCrimsonDim,
    onErrorContainer = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SurfaceSheet,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextMuted,
    outline = SurfaceStroke,
)

@Composable
fun FaceSwapCamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FaceSwapDarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
