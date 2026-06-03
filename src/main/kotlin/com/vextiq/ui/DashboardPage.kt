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
 * Dashboard Page - Main overview with stats and quick boost
 */
@Composable
fun DashboardPage(
    stats: SystemStats,
    selectedGame: Game,
    selectedPlaystyle: String,
    onPlaystyleChange: (String) -> Unit,
    logs: List<Pair<String, String>>,
    isOptimizing: Boolean,
    isScanning: Boolean,
    onBoost: () -> Unit,
    onHardwareScan: () -> Unit,
    onQuickAction: (String) -> Unit,
    onCancel: () -> Unit = {},
    netStats: NetworkMonitor.NetStats? = null,
    isAdmin: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ═══ LEFT: Main Content ═══
        Column(
            modifier = Modifier.weight(0.65f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Admin warning: without admin, HKLM/registry/power tweaks silently fail ──
            if (!isAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(VextiqColors.Error.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .border(1.dp, VextiqColors.Error.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⚠  Not running as Administrator — many optimizations (registry, power plan, services) won't take effect. Restart VEXTIQ as admin.",
                        color = VextiqColors.Error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Stat Cards Row ──
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashStatCard("CPU LOAD", "${stats.cpuUsage}", "%", VextiqColors.Primary, Modifier.weight(1f))
                DashStatCard("GPU", "${stats.gpuUsage}", "%", VextiqColors.Warning, Modifier.weight(1f))
                DashStatCard("NET PING", if (stats.latency > 0) "${stats.latency}" else "--", "ms",
                    when { stats.latency <= 0 -> VextiqColors.TextMuted; stats.latency < 50 -> VextiqColors.Primary; stats.latency < 100 -> VextiqColors.Warning; else -> VextiqColors.Error },
                    Modifier.weight(1f))
                DashStatCard("FPS", if (stats.fps > 0) "${if (stats.fpsIsEstimate) "~" else ""}${stats.fps}" else "--", "FPS",
                    when { stats.fps <= 0 -> VextiqColors.TextMuted; stats.fps >= 60 -> VextiqColors.Accent; stats.fps >= 30 -> VextiqColors.Warning; else -> VextiqColors.Error },
                    Modifier.weight(1f))
            }

            // ── Network detail strip (visible when game-aware probe has data) ──
            if (netStats != null && netStats.gameServerPingMs > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(VextiqColors.Card.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NetDetail("ROUTE",
                        if (netStats.gameServerIp.isNotEmpty()) netStats.gameServerIp else "—")
                    NetDetail("NET RTT", "${netStats.gameServerPingMs}ms")
                    if (netStats.tcpPingMs > 0) NetDetail("TCP", "${netStats.tcpPingMs}ms")
                    NetDetail("JITTER", "${netStats.jitterMs}ms")
                    NetDetail("LOSS", "${netStats.packetLossPercent}%")
                    if (netStats.bestRegion.isNotEmpty() && netStats.bestRegionPingMs > 0) {
                        NetDetail("BEST", "${netStats.bestRegion} ${netStats.bestRegionPingMs}ms")
                    }
                }
            }

            // ── Hero: Intelligent Optimization ──
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VextiqColors.Card)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(28.dp)
            ) {
                Column {
                    // Tag
                    Box(
                        modifier = Modifier
                            .border(1.dp, VextiqColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("INTELLIGENT OPTIMIZATION", fontSize = 9.sp, color = VextiqColors.Primary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    // Title
                    Text("MAXIMIZE YOUR", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextPrimary, letterSpacing = (-1).sp)
                    Text("POTENTIAL", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextPrimary, letterSpacing = (-1).sp)
                    Spacer(Modifier.height(12.dp))
                    
                    // Game info + description
                    if (stats.isGameRunning && stats.activeGame.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(VextiqColors.Success, androidx.compose.foundation.shape.CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text("ACTIVE: ${stats.activeGame.uppercase()}", fontSize = 11.sp, color = VextiqColors.Success, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    Text(
                        "Deploy VEXTIQ's neural processing to reallocate system resources for peak gaming performance.",
                        fontSize = 13.sp, color = VextiqColors.TextMuted, lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    // Playstyle row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        selectedGame.playstyles.take(4).forEach { ps ->
                            PlaystyleTag(
                                label = ps.name.substringBefore(" ("),
                                isSelected = selectedPlaystyle == ps.id,
                                onClick = { onPlaystyleChange(ps.id) }
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    
                    // Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Engage Optimizer (primary glow button) — doubles as Cancel when running
                        val engageBg = if (isOptimizing)
                            Brush.horizontalGradient(listOf(VextiqColors.Error, VextiqColors.Warning))
                        else
                            Brush.horizontalGradient(listOf(VextiqColors.Primary, VextiqColors.Accent))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(engageBg)
                                .clickable(enabled = !isScanning) {
                                    if (isOptimizing) onCancel() else onBoost()
                                }
                                .padding(horizontal = 28.dp, vertical = 12.dp)
                        ) {
                            Text(
                                if (isOptimizing) "CANCEL" else "ENGAGE OPTIMIZER",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Background, letterSpacing = 2.sp
                            )
                        }
                        // System Scan
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .clickable(enabled = !isScanning) { onHardwareScan() }
                                .padding(horizontal = 28.dp, vertical = 12.dp)
                        ) {
                            Text(
                                if (isScanning) "SCANNING..." else "SYSTEM SCAN",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextPrimary, letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
            
            // ── Quick Action Cards (3-column) ──
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashActionCard("CLEAR CACHE", "Remove temporary files and shader cache to reclaim storage space.", VextiqColors.Primary, "EXECUTE CLEANUP", { onQuickAction("Cache") }, Modifier.weight(1f))
                DashActionCard("BOOST RAM", "Flush standby memory and prioritize active game process threads.", VextiqColors.Accent, "FLUSH MEMORY", { onQuickAction("RAM") }, Modifier.weight(1f))
                DashActionCard("KILL APPS", "Suspend background processes and free CPU for gaming performance.", VextiqColors.Warning, "SUSPEND APPS", { onQuickAction("Kill") }, Modifier.weight(1f))
            }
        }
        
        // ═══ RIGHT: Process Cluster + Status ═══
        Column(
            modifier = Modifier.weight(0.35f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Process Cluster
            Column(
                modifier = Modifier.weight(1f)
                    .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PROCESS CLUSTER", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = VextiqColors.TextPrimary)
                        Text("ACTIVE MONITORING", fontSize = 9.sp, color = VextiqColors.TextMuted, letterSpacing = 1.sp)
                    }
                    Box(
                        modifier = Modifier.background(VextiqColors.Surface, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${stats.cpuUsage + stats.gpuUsage + 80} TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextPrimary)
                    }
                }
                
                Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                
                // Scrollable process list
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // System log entries as process items
                    if (logs.isNotEmpty()) {
                        logs.takeLast(8).forEach { (msg, type) ->
                            val procName = msg.removePrefix("[OK] ").removePrefix("[>>] ").removePrefix("[!] ").removePrefix("[i] ").take(24)
                            val impact = when (type) { "ok" -> "DONE"; "warn" -> "WARN"; "err" -> "ERROR"; else -> "INFO" }
                            val impactColor = when (type) { "ok" -> VextiqColors.Success; "warn" -> VextiqColors.Warning; "err" -> VextiqColors.Error; else -> VextiqColors.Primary }
                            ProcessItem(procName, impact, impactColor)
                        }
                    } else {
                        ProcessItem("System Idle", "OK", VextiqColors.Success)
                        ProcessItem("VEXTIQ Monitor", "ACTIVE", VextiqColors.Primary)
                        ProcessItem("Click SCAN to detect", "INFO", VextiqColors.TextMuted)
                    }
                }
                
                Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                
                // VPN button at bottom
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { onQuickAction("VPN") }.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("GAMING VPN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary, letterSpacing = 2.sp)
                }
            }
            
            // PRO STATUS card
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(VextiqColors.Primary.copy(alpha = 0.1f), Color.Transparent)),
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, VextiqColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("PRO STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    val score = (100 - (stats.cpuUsage * 0.4 + stats.ramUsage * 0.3 + stats.gpuUsage * 0.3)).toInt().coerceIn(0, 100)
                    Text(
                        buildString {
                            append("Your hardware is running at ")
                        },
                        fontSize = 12.sp, color = VextiqColors.TextMuted
                    )
                    Row {
                        Text("$score% efficiency", fontSize = 12.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
                        Text(". Ultimate mode available.", fontSize = 12.sp, color = VextiqColors.TextMuted)
                    }
                }
            }
        }
    }
}

// ═══ NEW DASHBOARD COMPONENTS ═══

