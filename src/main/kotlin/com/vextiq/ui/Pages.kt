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
    netStats: NetworkMonitor.NetStats? = null
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

@Composable
private fun DashStatCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(VextiqColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = VextiqColors.TextMuted, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if (value == "--") VextiqColors.TextMuted else VextiqColors.TextPrimary)
                Spacer(Modifier.width(4.dp))
                Text(unit, fontSize = 13.sp, color = VextiqColors.TextMuted, modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            // Thin color bar
            Box(Modifier.fillMaxWidth().height(3.dp).background(VextiqColors.Surface, RoundedCornerShape(2.dp))) {
                val progress = when {
                    value == "--" -> 0f
                    unit == "%" -> (value.toFloatOrNull() ?: 0f) / 100f
                    unit == "ms" -> ((value.toFloatOrNull() ?: 0f) / 100f).coerceIn(0f, 1f)
                    unit == "FPS" -> ((value.toFloatOrNull() ?: 0f) / 144f).coerceIn(0f, 1f)
                    else -> 0.5f
                }
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(color, RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun DashActionCard(title: String, description: String, accentColor: Color, buttonText: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(VextiqColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(10.dp))
        Text(description, fontSize = 11.sp, color = VextiqColors.TextMuted, lineHeight = 17.sp)
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .clickable { onClick() }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(buttonText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextPrimary, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun ProcessItem(name: String, impact: String, impactColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 11.sp, color = VextiqColors.TextSecondary, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .background(impactColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(impact, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = impactColor, letterSpacing = 1.sp)
        }
    }
}

/**
 * Game Boost Page - Select game and optimize
 */
@Composable
fun GameBoostPage(
    games: List<Game>,
    selectedGame: Game,
    logs: List<Pair<String, String>>,
    onSelectGame: (Game) -> Unit,
    onOptimize: (String) -> Unit,
    onScanGames: () -> Unit
) {
    var selectedPlaystyle by remember { mutableStateOf("none") }
    val gamePaths = remember { GamePaths() }
    var customPath by remember { mutableStateOf("") }
    
    LaunchedEffect(selectedGame) {
        customPath = gamePaths.getPath(selectedGame.id) ?: ""
        val hasPs = selectedGame.playstyles.any { it.id == selectedPlaystyle }
        if (!hasPs) selectedPlaystyle = selectedGame.playstyles.firstOrNull()?.id ?: "none"
    }
    
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // ═══ LEFT: Game Library List ═══
        Column(
            modifier = Modifier.weight(0.38f).fillMaxHeight()
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("GAMES LIBRARY", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = VextiqColors.TextPrimary)
                    Text("${games.size} Scanned titles ready for optimization.", fontSize = 12.sp, color = VextiqColors.TextMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, VextiqColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .clickable { onScanGames() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("SCAN FOR GAMES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary, letterSpacing = 1.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            
            // Game list with thumbnails
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                games.forEach { game ->
                    val isSelected = game == selectedGame
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) VextiqColors.Primary.copy(alpha = 0.08f) else VextiqColors.Card)
                            .border(1.dp, if (isSelected) VextiqColors.Primary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { onSelectGame(game) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Game thumbnail
                        if (game.imagePath.isNotEmpty()) {
                            Image(
                                painter = painterResource(game.imagePath),
                                contentDescription = game.name,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            GameIcon(profile = game.profile, color = game.color, size = 56.dp, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(game.name.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSelected) VextiqColors.Primary else VextiqColors.TextPrimary)
                            Text("${game.genre.uppercase()} ${if (game.publisher.isNotEmpty()) "• ${game.publisher}" else ""}", fontSize = 10.sp, color = VextiqColors.TextMuted, letterSpacing = 0.5.sp)
                        }
                        if (isSelected) {
                            Box(Modifier.size(20.dp).background(VextiqColors.Primary, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                                Text("✓", fontSize = 11.sp, color = VextiqColors.Background, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        
        // ═══ RIGHT: Game Detail + Optimization ═══
        Column(
            modifier = Modifier.weight(0.62f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Game Hero Card with cover image
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VextiqColors.Card)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Cover image
                    if (selectedGame.imagePath.isNotEmpty()) {
                        Image(
                            painter = painterResource(selectedGame.imagePath),
                            contentDescription = selectedGame.name,
                            modifier = Modifier.weight(0.45f).fillMaxHeight().clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Game info
                    Column(
                        modifier = Modifier.weight(0.55f).fillMaxHeight().padding(20.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (selectedGame.publisher.isNotEmpty()) {
                            Text(selectedGame.publisher.uppercase(), fontSize = 10.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(selectedGame.name.uppercase(), fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = VextiqColors.TextPrimary)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TagBadge(selectedGame.genre.uppercase(), Color.White.copy(alpha = 0.1f), VextiqColors.TextSecondary)
                            if (customPath.isNotEmpty()) TagBadge("DETECTED", VextiqColors.Success.copy(alpha = 0.1f), VextiqColors.Success)
                        }
                    }
                }
            }
            
            // Optimization Profile
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(VextiqColors.Primary, androidx.compose.foundation.shape.CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text("OPTIMIZATION PROFILE", fontSize = 11.sp, color = VextiqColors.TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(14.dp))
                
                // Playstyle grid
                val chunked = selectedGame.playstyles.chunked(4)
                chunked.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { ps ->
                            val isSel = selectedPlaystyle == ps.id
                            Box(
                                modifier = Modifier.weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) VextiqColors.Primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, if (isSel) VextiqColors.Primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .clickable { selectedPlaystyle = ps.id }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(ps.name.substringBefore(" (").uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = if (isSel) VextiqColors.Primary else VextiqColors.TextMuted, letterSpacing = 1.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            
            // Installation Path
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text("INSTALLATION PATH", fontSize = 10.sp, color = VextiqColors.TextMuted, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.weight(1f)
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(if (customPath.isNotEmpty()) customPath else "Not detected — click SCAN FOR GAMES", fontSize = 11.sp, color = VextiqColors.TextMuted)
                    }
                }
            }
            
            // Optimize Button
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(VextiqColors.Primary, VextiqColors.Accent)))
                    .clickable { onOptimize(selectedPlaystyle) }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("OPTIMIZE ${selectedGame.name.uppercase()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Background, letterSpacing = 2.sp)
            }
            
            // Status line
            if (logs.isNotEmpty()) {
                Text(
                    logs.last().first.uppercase(),
                    fontSize = 10.sp, color = VextiqColors.TextMuted, letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Text("CORE THREADS PRIORITIZED • LATENCY REDUCTION ACTIVE", fontSize = 10.sp, color = VextiqColors.TextMuted, letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun TagBadge(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier.background(bgColor, RoundedCornerShape(4.dp)).border(1.dp, textColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = 1.sp)
    }
}

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

@Composable
private fun ToolSectionHeader(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.06f)))
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 3.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.06f)))
    }
}

@Composable
private fun ToolToggleCard(title: String, desc: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextPrimary)
            Text(desc, fontSize = 10.sp, color = VextiqColors.TextMuted, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun ToolActionCard(title: String, desc: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(desc, fontSize = 10.sp, color = VextiqColors.TextMuted, lineHeight = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun ToolSection(
    title: String,
    titleColor: Color = VextiqColors.Primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(VextiqColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, fontSize = 11.sp, color = titleColor, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        content()
    }
}

/**
 * Settings Page - System Configuration
 */
@Composable
fun SettingsPage(
    onLanguageChange: (String) -> Unit,
    showFpsOverlay: Boolean = false,
    onToggleFpsOverlay: (Boolean) -> Unit = {}
) {
    val settings = remember { SettingsManager() }
    var selectedLang by remember { mutableStateOf(settings.language) }
    var autoStart by remember { mutableStateOf(settings.startWithWindows) }
    var minimizeToTray by remember { mutableStateOf(settings.minimizeToTray) }
    var checkUpdates by remember { mutableStateOf(settings.checkUpdates) }
    var darkMode by remember { mutableStateOf(settings.darkMode) }
    var autoOptimize by remember { mutableStateOf(settings.autoOptimize) }
    var statusMessage by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Column {
            Text("System Configuration", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = VextiqColors.TextPrimary)
            Text("Manage your global optimization parameters.", fontSize = 13.sp, color = VextiqColors.TextMuted)
        }
        
        // Status
        if (statusMessage.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(VextiqColors.Success.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Text(statusMessage, fontSize = 12.sp, color = VextiqColors.Success)
            }
        }
        
        // ── Top Row: General + Visual Engine ──
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // GENERAL
            Column(
                modifier = Modifier.weight(0.45f)
                    .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("GENERAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary, letterSpacing = 2.sp)
                CfgToggle("Start with Windows", autoStart) {
                    autoStart = it; settings.startWithWindows = it; settings.applyStartWithWindows(it)
                    statusMessage = if (it) "Added to startup" else "Removed from startup"
                }
                CfgToggle("Minimize to Tray", minimizeToTray) {
                    minimizeToTray = it; settings.minimizeToTray = it
                }
                CfgToggle("Auto-Update", checkUpdates) {
                    checkUpdates = it; settings.checkUpdates = it
                }
            }
            
            // VISUAL ENGINE
            Column(
                modifier = Modifier.weight(0.55f)
                    .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("VISUAL ENGINE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Warning, letterSpacing = 2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Dark Mode card
                    Box(
                        modifier = Modifier.weight(1f)
                            .background(if (darkMode) VextiqColors.Primary.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .border(1.dp, if (darkMode) VextiqColors.Primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                            .clickable { darkMode = !darkMode; settings.darkMode = darkMode }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("Dark Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextPrimary)
                            Text(if (darkMode) "Enabled" else "Disabled", fontSize = 11.sp, color = VextiqColors.TextMuted)
                        }
                    }
                }
                CfgToggle("FPS Overlay", showFpsOverlay) {
                    onToggleFpsOverlay(it); settings.fpsOverlayEnabled = it
                }
            }
        }
        
        // ── Middle Row: Optimization Protocol + Localization ──
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // OPTIMIZATION PROTOCOL
            Column(
                modifier = Modifier.weight(0.55f)
                    .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("OPTIMIZATION PROTOCOL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD674FF), letterSpacing = 2.sp)
                CfgToggle("Smart Boost Activation", autoOptimize) {
                    autoOptimize = it; settings.autoOptimize = it
                    statusMessage = if (it) "Auto-optimize enabled" else "Auto-optimize disabled"
                }
                CfgToggle("Auto-Optimize on Game Launch", settings.autoOptimize) {
                    settings.autoOptimize = it
                }
            }
            
            // LOCALIZATION
            Column(
                modifier = Modifier.weight(0.45f)
                    .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("LOCALIZATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary, letterSpacing = 2.sp)
                Text("Select your preferred system interface language.", fontSize = 11.sp, color = VextiqColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("en" to "English", "mn" to "Mongol").forEach { (code, name) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedLang == code) VextiqColors.Primary else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (selectedLang == code) VextiqColors.Primary else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .clickable { selectedLang = code; settings.language = code; onLanguageChange(code) }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = if (selectedLang == code) VextiqColors.Background else VextiqColors.TextSecondary)
                        }
                    }
                }
            }
        }
        
        // ── Bottom: Reset + Apply ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clickable {
                        settings.resetAll()
                        autoStart = false; minimizeToTray = true; checkUpdates = true
                        darkMode = true; autoOptimize = false; selectedLang = "en"
                        statusMessage = "All settings reset to defaults"
                    }
                    .padding(vertical = 12.dp)
            ) {
                Text("RESET TO DEFAULT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextMuted, letterSpacing = 2.sp)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .clickable {
                            val path = settings.exportLogs()
                            statusMessage = if (path != null) "Exported to: $path" else "Export failed"
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text("EXPORT LOGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextSecondary, letterSpacing = 2.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(VextiqColors.Primary, VextiqColors.Accent)))
                        .clickable { statusMessage = "Settings applied" }
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                ) {
                    Text("APPLY CHANGES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Background, letterSpacing = 2.sp)
                }
            }
        }
        
        // About
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("VEXTIQ PRO v2.0", fontSize = 11.sp, color = VextiqColors.TextMuted)
            Text("© 2025 VEXTIQ", fontSize = 11.sp, color = VextiqColors.TextMuted)
        }
    }
}

@Composable
private fun NetDetail(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 8.sp, color = VextiqColors.TextMuted, letterSpacing = 0.5.sp)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = VextiqColors.TextPrimary)
    }
}

@Composable
private fun CfgToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = VextiqColors.TextPrimary)
        // Toggle switch
        Box(
            modifier = Modifier.width(44.dp).height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) VextiqColors.Primary else Color(0xFF3A3A4A))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier.size(20.dp)
                    .offset(x = if (checked) 20.dp else 0.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            )
        }
    }
}

// Keep old components for backward compatibility
@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(VextiqColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary)
        content()
    }
}

/**
 * VPN Page
 */
@Composable
fun VpnPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("🌐 Gaming VPN", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        // VPN Status
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VextiqColors.Card, RoundedCornerShape(14.dp))
                .border(1.dp, VextiqColors.Border, RoundedCornerShape(14.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Status", fontSize = 12.sp, color = VextiqColors.TextMuted)
                    Text("Disconnected", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .background(VextiqColors.Primary, RoundedCornerShape(10.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("CONNECT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Background)
                }
            }
        }
        
        // Server Selection
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VextiqColors.Card, RoundedCornerShape(14.dp))
                .border(1.dp, VextiqColors.Border, RoundedCornerShape(14.dp))
                .padding(20.dp)
        ) {
            Text("🌍 Server Region", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary)
            Spacer(Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    "🇺🇸 US West" to "45ms",
                    "🇪🇺 Europe" to "120ms",
                    "🇯🇵 Asia" to "180ms"
                ).forEach { (region, ping) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VextiqColors.Surface)
                            .border(1.dp, VextiqColors.Border, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(region, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(ping, fontSize = 12.sp, color = VextiqColors.Success)
                        }
                    }
                }
            }
        }
        
        // Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VextiqColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                .border(1.dp, VextiqColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .padding(20.dp)
        ) {
            Text("ℹ️ Gaming VPN Features", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary)
            Spacer(Modifier.height(10.dp))
            Text("• Low latency optimized servers", fontSize = 12.sp, color = VextiqColors.TextSecondary)
            Text("• DDoS protection", fontSize = 12.sp, color = VextiqColors.TextSecondary)
            Text("• No bandwidth limits", fontSize = 12.sp, color = VextiqColors.TextSecondary)
            Text("• WireGuard protocol", fontSize = 12.sp, color = VextiqColors.TextSecondary)
        }
    }
}
