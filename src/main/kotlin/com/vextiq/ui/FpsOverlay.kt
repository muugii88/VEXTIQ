package com.vextiq.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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

    var posX by remember { mutableStateOf(20f) }
    var posY by remember { mutableStateOf(20f) }
    var mode by remember { mutableStateOf(settings.overlayMode) }

    // Dynamic height based on visible rows
    val visibleRows = computeVisibleRowCount(mode, settings, stats)
    val targetHeight = (30 + visibleRows * 16).coerceAtLeast(80)

    val windowState = remember {
        WindowState(
            position = WindowPosition(posX.dp, posY.dp),
            width = 150.dp,
            height = targetHeight.dp
        )
    }

    LaunchedEffect(posX, posY) {
        windowState.position = WindowPosition(posX.dp, posY.dp)
    }
    LaunchedEffect(targetHeight) {
        windowState.size = androidx.compose.ui.unit.DpSize(150.dp, targetHeight.dp)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (mode == "network") "NET" else "VEXTIQ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D2FF),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                mode = if (mode == "network") "fps" else "network"
                                settings.overlayMode = mode
                            })
                        }
                    )
                    if (stats.activeGame.isNotEmpty() && stats.isGameRunning) {
                        Text(
                            stats.activeGame.take(10),
                            fontSize = 8.sp,
                            color = Color(0xFF888888),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                if (mode == "network") {
                    renderNetworkMode(stats, settings)
                } else {
                    renderFpsMode(stats, settings)
                }
            }
        }
    }
}

private fun regionCode(region: String): String = when (region.lowercase()) {
    "singapore" -> "SG"
    "tokyo" -> "JP"
    "hongkong", "hong kong" -> "HK"
    "seoul" -> "KR"
    "sydney" -> "AU"
    "stockholm" -> "SE"
    "us", "america", "n.america", "north america" -> "US"
    "europe", "eu" -> "EU"
    "asia" -> "AS"
    "middle east", "me" -> "ME"
    "brazil", "south america" -> "BR"
    "india" -> "IN"
    else -> region.take(2).uppercase()
}

@Composable
private fun renderFpsMode(stats: SystemStats, settings: com.vextiq.core.SettingsManager) {
    if (settings.overlayShowFps) {
        StatRow("FPS", if (stats.fps > 0) "${stats.fps}" else "--", getFpsColor(stats.fps))
    }
    if (settings.overlayShowFrametime) {
        val frametimeText = if (stats.frametime > 0) String.format("%.1fms", stats.frametime) else "--"
        StatRow("FT", frametimeText, getFrametimeColor(stats.frametime))
    }
    if (settings.overlayShowGpu) {
        StatRow("GPU", "${stats.gpuUsage}%", getUsageColor(stats.gpuUsage))
    }
    if (settings.overlayShowCpu) {
        StatRow("CPU", "${stats.cpuUsage}%", getUsageColor(stats.cpuUsage))
    }
    if (settings.overlayShowRam) {
        StatRow("RAM", "${stats.ramUsage}%", getUsageColor(stats.ramUsage))
    }
    if (settings.overlayShowNet) {
        val netSuffix = if (settings.overlayShowRegion && stats.gameServerRegion.isNotEmpty()) {
            " ${regionCode(stats.gameServerRegion)}"
        } else ""
        StatRow("NET", if (stats.latency > 0) "${stats.latency}ms$netSuffix" else "--", getPingColor(stats.latency))
    }
}

@Composable
private fun renderNetworkMode(stats: SystemStats, settings: com.vextiq.core.SettingsManager) {
    val netSuffix = if (settings.overlayShowRegion && stats.gameServerRegion.isNotEmpty()) {
        " ${regionCode(stats.gameServerRegion)}"
    } else ""
    StatRow("NET", if (stats.latency > 0) "${stats.latency}ms$netSuffix" else "--", getPingColor(stats.latency))

    if (settings.overlayShowWifiBaseline) {
        StatRow("WIFI", if (stats.routerPingMs > 0) "${stats.routerPingMs}ms" else "--", getPingColor(stats.routerPingMs))
    }
    if (settings.overlayShowJitter) {
        StatRow("JIT", if (stats.jitterMs > 0) "${stats.jitterMs}ms" else "--", getJitterColor(stats.jitterMs))
    }
    if (settings.overlayShowPacketLoss) {
        StatRow("PKT", "${stats.packetLossPercent}%", getLossColor(stats.packetLossPercent))
    }
    if (stats.bestRegion.isNotEmpty() && stats.bestRegion != stats.gameServerRegion) {
        StatRow(
            "BEST",
            "${regionCode(stats.bestRegion)} ${stats.bestRegionPingMs}ms",
            getPingColor(stats.bestRegionPingMs)
        )
    }
}

private fun computeVisibleRowCount(
    mode: String,
    settings: com.vextiq.core.SettingsManager,
    stats: SystemStats
): Int {
    return if (mode == "network") {
        var c = 1 // NET always shown in network mode
        if (settings.overlayShowWifiBaseline) c++
        if (settings.overlayShowJitter) c++
        if (settings.overlayShowPacketLoss) c++
        if (stats.bestRegion.isNotEmpty() && stats.bestRegion != stats.gameServerRegion) c++
        c
    } else {
        var c = 0
        if (settings.overlayShowFps) c++
        if (settings.overlayShowFrametime) c++
        if (settings.overlayShowGpu) c++
        if (settings.overlayShowCpu) c++
        if (settings.overlayShowRam) c++
        if (settings.overlayShowNet) c++
        c
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

private fun getJitterColor(jitter: Int) = when {
    jitter <= 0 -> Color(0xFF666666)
    jitter < 5 -> Color(0xFF00FF88)
    jitter < 15 -> Color(0xFFFFCC00)
    else -> Color(0xFFFF4444)
}

private fun getLossColor(loss: Int) = when {
    loss <= 0 -> Color(0xFF00FF88)
    loss < 2 -> Color(0xFFFFCC00)
    else -> Color(0xFFFF4444)
}
