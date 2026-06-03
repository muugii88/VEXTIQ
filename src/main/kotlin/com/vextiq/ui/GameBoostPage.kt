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

