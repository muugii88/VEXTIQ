package com.vextiq.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// VEXTIQ Cyber Colors
object VextiqColors {
    val Primary = Color(0xFF00D2FF)
    val PrimaryDark = Color(0xFF00A8FF)
    val Success = Color(0xFF00E5A0)
    val Warning = Color(0xFFFFB800)
    val Error = Color(0xFFFF3B3B)
    
    val Background = Color(0xFF0A0E14)
    val Surface = Color(0xFF0D1219)
    val Card = Color(0xFF111820)
    val Hover = Color(0xFF1A2332)
    
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8BA3C7)
    val TextMuted = Color(0xFF4A6085)
    
    val Border = Color(0xFF00D2FF).copy(alpha = 0.3f)
    val Glow = Color(0xFF00D2FF).copy(alpha = 0.5f)
}

private val VextiqColorScheme = darkColorScheme(
    primary = VextiqColors.Primary,
    onPrimary = VextiqColors.Background,
    secondary = VextiqColors.Success,
    background = VextiqColors.Background,
    surface = VextiqColors.Surface,
    onBackground = VextiqColors.TextPrimary,
    onSurface = VextiqColors.TextPrimary,
    error = VextiqColors.Error
)

@Composable
fun VextiqTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VextiqColorScheme,
        content = content
    )
}
