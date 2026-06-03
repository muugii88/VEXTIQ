package com.vextiq.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vextiq.core.*

/**
 * Tools Page - System utilities
 */
@Composable
fun ToolsPage(
    logs: List<Pair<String, String>>,
    onAction: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // ═══ LEFT: Tool Categories ═══
        Column(
            modifier = Modifier.weight(0.6f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Hero: System Potential ──
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VextiqColors.Card)
                    .border(1.dp, VextiqColors.Warning.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text("SYSTEM POTENTIAL:", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, color = VextiqColors.TextPrimary)
                    Text("LIMITED", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Warning, letterSpacing = (-1).sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Execute global optimization protocols to synchronize hardware clocks and purge background latency bottlenecks.", fontSize = 12.sp, color = VextiqColors.TextMuted, lineHeight = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(VextiqColors.Warning, VextiqColors.Orange)))
                            .clickable { onAction("UltimateBoost") }
                            .padding(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        Text("ULTIMATE BOOST", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Background, letterSpacing = 2.sp)
                    }
                }
            }
            
            // ── System Protocols (2-col grid) ──
            ToolSectionHeader("SYSTEM PROTOCOLS", VextiqColors.Primary)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolToggleCard("Timer Resolution", "Forces 0.5ms system timer for input accuracy.", Modifier.weight(1f)) { onAction("Timer") }
                    ToolToggleCard("Disable Core Parking", "Prevent CPU cores from entering sleep states.", Modifier.weight(1f)) { onAction("CPU") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolToggleCard("Ultimate Power Plan", "Bypass power throttling on all hardware.", Modifier.weight(1f)) { onAction("PowerPlan") }
                    ToolToggleCard("Mouse Optimization", "Disable MarkC acceleration and enhance raw input.", Modifier.weight(1f)) { onAction("Mouse") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolToggleCard("Enable Game Mode", "Windows internal prioritization for active focus.", Modifier.weight(1f)) { onAction("GameMode") }
                    ToolToggleCard("Enable HAGS", "Hardware-accelerated GPU scheduling protocol.", Modifier.weight(1f)) { onAction("HAGS") }
                }
            }
            
            // ── Cleanup Matrix (3-col) ──
            ToolSectionHeader("CLEANUP MATRIX", Color(0xFFD674FF))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolActionCard("Clear Cache", "Purge system temp and shader cache data.", Modifier.weight(1f)) { onAction("Cache") }
                ToolActionCard("Global Shader", "Clean DX and NVIDIA shader pipeline cache.", Modifier.weight(1f)) { onAction("GlobalShader") }
                ToolActionCard("Free RAM", "Flush standby list and free working memory.", Modifier.weight(1f)) { onAction("RAM") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolActionCard("Network Boost", "Optimize TCP/IP stack and Nagle algorithm.", Modifier.weight(1f)) { onAction("Network") }
                ToolActionCard("Windows Temp", "Remove Windows temporary folder contents.", Modifier.weight(1f)) { onAction("WinTemp") }
                ToolActionCard("Prefetch", "Clear prefetch data for fresh loading.", Modifier.weight(1f)) { onAction("Prefetch") }
            }
            
            // ── GPU Optimization ──
            ToolSectionHeader("GPU OPTIMIZATION", VextiqColors.Accent)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolToggleCard("NVIDIA Optimize", "Apply optimal Control Panel profile.", Modifier.weight(1f)) { onAction("NVIDIA") }
                ToolToggleCard("AMD Optimize", "Configure Radeon settings for gaming.", Modifier.weight(1f)) { onAction("AMD") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolToggleCard("Disable Fullscreen Opt", "Remove DWM overhead on game windows.", Modifier.weight(1f)) { onAction("FSO") }
                ToolToggleCard("Disable Game DVR", "Turn off Xbox Game Bar recording.", Modifier.weight(1f)) { onAction("GameDVR") }
            }
            
            // ── Services ──
            ToolSectionHeader("STARTUP & SERVICES", VextiqColors.Primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolToggleCard("Disable Startup Apps", "Reduce boot time and background load.", Modifier.weight(1f)) { onAction("Startup") }
                ToolToggleCard("Disable Telemetry", "Remove data collection overhead.", Modifier.weight(1f)) { onAction("Telemetry") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolToggleCard("Disable Search Index", "Free disk I/O from indexing service.", Modifier.weight(1f)) { onAction("SearchIndex") }
                ToolToggleCard("Disable SysMain", "Turn off Superfetch memory preloading.", Modifier.weight(1f)) { onAction("SysMain") }
            }
            
            // ── Maintenance ──
            ToolSectionHeader("MAINTENANCE", VextiqColors.Warning)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolActionCard("Backup", "Save current system configuration.", Modifier.weight(1f)) { onAction("Backup") }
                ToolActionCard("Restore", "Revert to saved backup state.", Modifier.weight(1f)) { onAction("Restore") }
                ToolActionCard("Repair Windows", "Run DISM and SFC integrity check.", Modifier.weight(1f)) { onAction("Repair") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolActionCard("Undo All", "Revert every VEXTIQ change: registry, services, power, network and game configs.", Modifier.weight(1f)) { onAction("UndoAll") }
            }
        }
        
        // ═══ RIGHT: Live Output Terminal ═══
        Column(
            modifier = Modifier.weight(0.4f).fillMaxHeight()
                .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(VextiqColors.Success, androidx.compose.foundation.shape.CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("LIVE OUTPUT TERMINAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = VextiqColors.TextPrimary)
                }
            }
            Divider(color = Color.White.copy(alpha = 0.05f))
            
            // Log content
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (logs.isEmpty()) {
                    Text("[--:--:--] Awaiting commands...", fontSize = 11.sp, color = VextiqColors.TextMuted, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                } else {
                    logs.forEach { (msg, type) ->
                        val c = when (type) { "ok" -> VextiqColors.Success; "warn" -> VextiqColors.Warning; "err" -> VextiqColors.Error; else -> VextiqColors.TextSecondary }
                        Text(msg, fontSize = 11.sp, color = c, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, lineHeight = 16.sp)
                    }
                }
            }
            
            Divider(color = Color.White.copy(alpha = 0.05f))
            // System Stability bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SYSTEM STABILITY", fontSize = 9.sp, color = VextiqColors.TextMuted, letterSpacing = 1.sp)
                Text("OPTIMIZED", fontSize = 9.sp, color = VextiqColors.Success, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp).height(4.dp).background(VextiqColors.Surface, RoundedCornerShape(2.dp))) {
                Box(Modifier.fillMaxWidth(0.78f).fillMaxHeight().background(VextiqColors.Primary, RoundedCornerShape(2.dp)))
            }
        }
    }
}

// ═══ NEW TOOLS COMPONENTS ═══

