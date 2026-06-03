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

