package com.vextiq.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * VEXTIQ PRO v2.0 - Modern Gaming Theme
 */
object VextiqColors {
    // Brand colors
    val Primary = Color(0xFF00D2FF)
    val Accent = Color(0xFF00FF88)
    val Orange = Color(0xFFFF6B00)
    
    // Status
    val Success = Color(0xFF00FF88)
    val Warning = Color(0xFFFFCC00)
    val Error = Color(0xFFFF4757)
    
    // Backgrounds
    val Background = Color(0xFF0A0A0F)
    val Surface = Color(0xFF12121A)
    val Card = Color(0xFF14141E)
    val CardHover = Color(0xFF1A1A28)
    val Hover = Color(0xFF1E1E2A)
    
    // Text - BRIGHTER for better visibility
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFE0E0E0)  // Much brighter
    val TextMuted = Color(0xFFA0A0B0)      // Brighter gray
    
    // Borders
    val Border = Color(0xFF00D2FF).copy(alpha = 0.2f)
    val BorderHover = Color(0xFF00D2FF).copy(alpha = 0.4f)
    val BorderActive = Color(0xFF00D2FF).copy(alpha = 0.6f)
    
    // Gradients
    val GradientPrimary = Brush.horizontalGradient(listOf(Primary, Accent))
    val GradientOrange = Brush.horizontalGradient(listOf(Color(0xFFFF6B00), Color(0xFFFF8C00)))
}

private val ColorScheme = darkColorScheme(
    primary = VextiqColors.Primary,
    secondary = VextiqColors.Accent,
    background = VextiqColors.Background,
    surface = VextiqColors.Surface,
    error = VextiqColors.Error,
    onPrimary = VextiqColors.Background,
    onBackground = VextiqColors.TextPrimary,
    onSurface = VextiqColors.TextPrimary
)

@Composable
fun VextiqTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, content = content)
}
