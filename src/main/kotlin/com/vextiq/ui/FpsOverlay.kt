package com.vextiq.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.vextiq.core.SystemStats

/**
 * FPS/Latency Overlay - Independent always-on-top window
 * Does NOT minimize with main app
 */
@Composable
fun FpsOverlay(
    stats: SystemStats,
    isVisible: Boolean,
    settings: com.vextiq.core.SettingsManager,
    onClose: () -> Unit
) {
    if (!isVisible) return
    
    // Position state - persists across recompositions
    var posX by remember { mutableStateOf(20f) }
    var posY by remember { mutableStateOf(20f) }
    
    // Independent window state
    val windowState = remember {
        WindowState(
            position = WindowPosition(posX.dp, posY.dp),
            width = 140.dp,
            height = 145.dp  // Increased for more content
        )
    }
    
    // Sync position
    LaunchedEffect(posX, posY) {
        windowState.position = WindowPosition(posX.dp, posY.dp)
    }
    
    Window(
        onCloseRequest = onClose,
        title = "VEXTIQ",
        state = windowState,
        alwaysOnTop = true,
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        posX += dragAmount.x
                        posY += dragAmount.y
                    }
                }
                .background(Color(0xDD000000), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF00D2FF).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Header with game name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "VEXTIQ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D2FF),
                        fontFamily = FontFamily.Monospace
                    )
                    if (stats.activeGame.isNotEmpty() && stats.isGameRunning) {
                        Text(
                            stats.activeGame.take(8),
                            fontSize = 8.sp,
                            color = Color(0xFF888888),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                Spacer(Modifier.height(2.dp))
                
                // FPS
                if (settings.overlayShowFps) {
                    StatRow("FPS", if (stats.fps > 0) "${if (stats.fpsIsEstimate) "~" else ""}${stats.fps}" else "--", getFpsColor(stats.fps))
                }
                
                // Frametime
                if (settings.overlayShowFrametime) {
                    val frametimeText = if (stats.frametime > 0) String.format("%.1fms", stats.frametime) else "--"
                    StatRow("FT", frametimeText, getFrametimeColor(stats.frametime))
                }
                
                // GPU
                if (settings.overlayShowGpu) {
                    StatRow("GPU", "${stats.gpuUsage}%", getUsageColor(stats.gpuUsage))
                }
                
                // CPU
                if (settings.overlayShowCpu) {
                    StatRow("CPU", "${stats.cpuUsage}%", getUsageColor(stats.cpuUsage))
                }
                
                // RAM
                if (settings.overlayShowRam) {
                    StatRow("RAM", "${stats.ramUsage}%", getUsageColor(stats.ramUsage))
                }
                
                // Network Latency
                if (settings.overlayShowNet) {
                    StatRow("NET", if (stats.latency > 0) "${stats.latency}ms" else "--", getPingColor(stats.latency))
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = Color(0xFFAAAAAA), fontFamily = FontFamily.Monospace)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
    }
}

private fun getUsageColor(usage: Int) = when {
    usage < 60 -> Color(0xFF00FF88)
    usage < 85 -> Color(0xFFFFCC00)
    else -> Color(0xFFFF4444)
}

private fun getFpsColor(fps: Int) = when {
    fps <= 0 -> Color(0xFF666666)
    fps >= 60 -> Color(0xFF00FF88)
    fps >= 30 -> Color(0xFFFFCC00)
    else -> Color(0xFFFF4444)
}

private fun getFrametimeColor(ft: Double) = when {
    ft <= 0 -> Color(0xFF666666)
    ft < 16.7 -> Color(0xFF00FF88)  // 60+ FPS
    ft < 33.3 -> Color(0xFFFFCC00)  // 30-60 FPS
    else -> Color(0xFFFF4444)       // <30 FPS
}

private fun getPingColor(ping: Int) = when {
    ping <= 0 -> Color(0xFF666666)
    ping < 50 -> Color(0xFF00FF88)
    ping < 100 -> Color(0xFFFFCC00)
    else -> Color(0xFFFF4444)
}

/**
 * FPS Overlay Window wrapper for Main.kt
 */
@Composable
fun FpsOverlayWindow(
    monitor: com.vextiq.core.SystemMonitor,
    onClose: () -> Unit
) {
    val stats by monitor.stats.collectAsState()
    val settings = remember { com.vextiq.core.SettingsManager() }
    
    FpsOverlay(
        stats = stats,
        isVisible = true,
        settings = settings,
        onClose = onClose
    )
}
