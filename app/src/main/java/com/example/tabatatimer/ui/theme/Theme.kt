package com.example.tabatatimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF536694),
    onPrimary = Color.White,
    surface = Color.White,
    onSurface = Color(0xFF2E3D59),
    background = Color.White,
    onBackground = Color(0xFF2E3D59),
    surfaceVariant = Color(0xFFE5E9F2),
    onSurfaceVariant = Color.DarkGray,
    primaryContainer = Color(0xFFC4D1F5),
    onPrimaryContainer = Color(0xFF536694)
)

@Composable
fun TabataTimerTheme(
    content: @Composable () -> Unit
) {
    // We force the LightColorScheme even in dark mode as per user request
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}