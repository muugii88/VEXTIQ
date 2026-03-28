package com.vextiq

import androidx.compose.foundation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.vextiq.core.*
import com.vextiq.core.Strings
import com.vextiq.optimizer.Optimizer
import com.vextiq.ui.*
import kotlinx.coroutines.launch

enum class Page { DASHBOARD, GAMEBOOST, TOOLS, VPN, SETTINGS }

@Composable
fun App(
    sharedMonitor: SystemMonitor? = null,
    showFpsOverlay: Boolean = false,
    onToggleFpsOverlay: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    // Use shared monitor if provided, otherwise create new (for preview)
    val monitor = sharedMonitor ?: remember { SystemMonitor() }
    val optimizer = remember { Optimizer() }
    
    val stats by monitor.stats.collectAsState()
    
    var currentPage by remember { mutableStateOf(Page.DASHBOARD) }
    var selectedGame by remember { mutableStateOf(Games.list.first()) }
    var dashboardPlaystyle by remember { mutableStateOf("none") }
    var logs by remember { mutableStateOf(listOf(
        "[OK] VEXTIQ PRO V1 initialized" to "ok",
        "[OK] Real-time monitoring active" to "ok",
        "[i] Click HARDWARE SCAN to detect your system" to ""
    )) }
    var boostLogs by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var toolsLogs by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var isOptimizing by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    
    // History for charts
    var cpuHistory by remember { mutableStateOf(List(20) { 0 }) }
    var gpuHistory by remember { mutableStateOf(List(20) { 0 }) }
    var ramHistory by remember { mutableStateOf(List(20) { 0 }) }
    var latencyHistory by remember { mutableStateOf(List(20) { 0 }) }
    
    // Start monitoring only if not using shared monitor
    LaunchedEffect(Unit) {
        if (sharedMonitor == null) {
            monitor.start(scope)
        }
    }
    
    // Update history
    LaunchedEffect(stats) {
        cpuHistory = cpuHistory.drop(1) + stats.cpuUsage
        gpuHistory = gpuHistory.drop(1) + stats.gpuUsage
        ramHistory = ramHistory.drop(1) + stats.ramUsage
        latencyHistory = latencyHistory.drop(1) + stats.frametime.toInt()
    }
    
    val score = (100 - (stats.cpuUsage * 0.4 + stats.ramUsage * 0.3 + stats.gpuUsage * 0.3)).toInt().coerceIn(0, 100)
    
    VextiqTheme {
        Row(modifier = Modifier.fillMaxSize().background(VextiqColors.Background)) {
            // Sidebar
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .background(VextiqColors.Surface)
                    .border(width = 1.dp, color = VextiqColors.Border, shape = RoundedCornerShape(0.dp))
                    .padding(15.dp)
            ) {
                // Logo
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource("logo.png"),
                        contentDescription = "VEXTIQ Logo",
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("PRO V1", fontSize = 11.sp, color = VextiqColors.TextMuted)
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Gauge
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularGauge(score)
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Game selector
                Text("target_game".localized(), fontSize = 10.sp, color = VextiqColors.TextMuted)
                Spacer(Modifier.height(4.dp))
                
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth().height(40.dp), // Slightly taller
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = VextiqColors.Primary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        border = BorderStroke(1.dp, VextiqColors.Border)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${selectedGame.icon} ${selectedGame.name}", fontSize = 12.sp)
                            Text("▼", fontSize = 10.sp, color = VextiqColors.TextMuted)
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded, 
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(VextiqColors.Card).border(1.dp, VextiqColors.Border, RoundedCornerShape(6.dp))
                    ) {
                        Games.list.forEach { game ->
                            DropdownMenuItem(
                                text = { Text("${game.icon} ${game.name}", color = VextiqColors.TextPrimary) },
                                onClick = {
                                    selectedGame = game
                                    expanded = false
                                    logs = logs + ("[i] Target: ${game.name}" to "")
                                }
                            )
                        }
                    }
                }
                
                
                Spacer(Modifier.height(20.dp))
                
                // Navigation
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    NavItem("◉", "nav_dashboard".localized(), currentPage == Page.DASHBOARD) { currentPage = Page.DASHBOARD }
                    NavItem("🎮", "nav_boost".localized(), currentPage == Page.GAMEBOOST) { currentPage = Page.GAMEBOOST }
                    NavItem("🔧", "nav_tools".localized(), currentPage == Page.TOOLS) { currentPage = Page.TOOLS }
                    NavItem("🌐", "VPN", currentPage == Page.VPN) { currentPage = Page.VPN } // VPN is Global
                    NavItem("⚙", "nav_settings".localized(), currentPage == Page.SETTINGS) { currentPage = Page.SETTINGS }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // FPS Overlay Toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (showFpsOverlay) VextiqColors.Success.copy(alpha = 0.2f) 
                            else VextiqColors.Card,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (showFpsOverlay) VextiqColors.Success else VextiqColors.Border,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleFpsOverlay(!showFpsOverlay) }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "sidebar_overlay".localized(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showFpsOverlay) VextiqColors.Success else VextiqColors.TextPrimary
                            )
                            Text(
                                if (showFpsOverlay) "ON" else "OFF",
                                fontSize = 10.sp,
                                color = if (showFpsOverlay) VextiqColors.Success else VextiqColors.TextMuted
                            )
                        }
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                // Status
                Divider(color = VextiqColors.Border)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(VextiqColors.Success, RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("sidebar_status".localized(), fontSize = 11.sp, color = VextiqColors.Success)
                }
            }
            
            // Main Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                when (currentPage) {
                    Page.DASHBOARD -> DashboardPage(
                        stats = stats,
                        cpuHistory = cpuHistory,
                        gpuHistory = gpuHistory,
                        _ramHistory = ramHistory,
                        latencyHistory = latencyHistory,
                        selectedGame = selectedGame,
                        selectedPlaystyle = dashboardPlaystyle,
                        onPlaystyleChange = { dashboardPlaystyle = it },
                        logs = logs,
                        isOptimizing = isOptimizing,
                        isScanning = isScanning,
                        onBoost = {
                            scope.launch {
                                isOptimizing = true
                                logs = listOf(
                                    "[>>] Starting Full Optimization for ${selectedGame.name}..." to "",
                                    "[i] Playstyle: ${dashboardPlaystyle.uppercase()}" to "",
                                    "" to ""
                                )
                                
                                // Full optimization with selected playstyle
                                optimizer.runSmartBoost(selectedGame.id, selectedGame.name, dashboardPlaystyle) { msg ->
                                    val type = when {
                                        msg.startsWith("[OK]") -> "ok"
                                        msg.contains("════") || msg.contains("SAVED") || msg.contains("CONFIG") -> "ok"
                                        msg.startsWith("[!]") -> "warn"
                                        else -> ""
                                    }
                                    logs = logs + (msg to type)
                                }
                                isOptimizing = false
                            }
                        },
                        onHardwareScan = {
                            scope.launch {
                                isScanning = true
                                logs = listOf(
                                    "[>>] Starting Hardware Scan..." to "",
                                    "" to ""
                                )
                                
                                // Force fresh detection
                                com.vextiq.core.HardwareManager.forceRescan { msg ->
                                    val type = when {
                                        msg.startsWith("[OK]") -> "ok"
                                        msg.startsWith("[!]") -> "warn"
                                        msg.startsWith("[>>]") -> ""
                                        else -> ""
                                    }
                                    logs = logs + (msg to type)
                                }
                                
                                logs = logs + ("" to "")
                                logs = logs + ("[OK] Hardware scan complete!" to "ok")
                                isScanning = false
                            }
                        }
                    )
                    
                    Page.GAMEBOOST -> GameBoostPage(
                        games = Games.list,
                        selectedGame = selectedGame,
                        logs = boostLogs,
                        onSelectGame = { game ->
                            selectedGame = game
                            boostLogs = listOf("[i] Selected: ${game.name}" to "")
                        },
                        onOptimize = { playstyle ->
                            scope.launch {
                                boostLogs = listOf("[>>] Optimizing ${selectedGame.name}..." to "")
                                val gameSpecific = com.vextiq.optimizer.GameSpecificOptimizer()
                                gameSpecific.optimize(selectedGame.id, playstyle) { msg ->
                                    val t = when {
                                        msg.startsWith("[OK]") -> "ok"
                                        msg.contains("════") || msg.contains("SAVED") -> "ok"
                                        msg.startsWith("[!]") -> "warn"
                                        else -> ""
                                    }
                                    boostLogs = boostLogs + (msg to t)
                                }
                                boostLogs = boostLogs + ("" to "") + ("[OK] Optimization complete!" to "ok")
                            }
                        },

                        onScanGames = {
                            scope.launch {
                                boostLogs = listOf("[>>] Scanning for games..." to "")
                                val gamePaths = com.vextiq.core.GamePaths()
                                val found = gamePaths.scanAllLaunchers { msg ->
                                    val t = if (msg.startsWith("[OK]")) "ok" else ""
                                    boostLogs = boostLogs + (msg to t)
                                }
                                boostLogs = boostLogs + ("" to "") + ("[OK] Found ${found.size} games!" to "ok")
                            }
                        }
                    )
                    
                    Page.TOOLS -> ToolsPage(
                        logs = toolsLogs,
                        onAction = { action ->
                            scope.launch {
                                val logFn: (String) -> Unit = { msg ->
                                    val type = when {
                                        msg.startsWith("[OK]") -> "ok"
                                        msg.startsWith("[!]") -> "warn"
                                        else -> ""
                                    }
                                    toolsLogs = toolsLogs + (msg to type)
                                }
                                
                                when (action) {
                                    "UltimateBoost" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.ultimateBoost(logFn)
                                    }
                                    "Timer" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.setTimerResolution(logFn)
                                    }
                                    "CPU" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.disableCoreParking(logFn)
                                    }
                                    "PowerPlan" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.setUltimatePowerPlan(logFn)
                                    }
                                    "Mouse" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.optimizeUsbPolling(logFn)
                                    }
                                    "Cache" -> optimizer.cleanCache(logFn)
                                    "Network" -> optimizer.optimizeNetwork(logFn)
                                    "RAM" -> {
                                        val ramCleaner = com.vextiq.optimizer.RamCleaner()
                                        ramCleaner.optimizeForGaming(logFn)
                                    }
                                    "SC_Shaders" -> {
                                        val cacheCleaner = com.vextiq.optimizer.CacheCleaner()
                                        cacheCleaner.cleanStarCitizenCache(logFn)
                                    }
                                    "SC_AppData" -> {
                                        logFn("[>>] Opening Star Citizen AppData...")
                                        val scPath = "${System.getenv("LOCALAPPDATA")}\\Star Citizen"
                                        try {
                                            ProcessBuilder("explorer", scPath).start()
                                            logFn("[OK] Opened: $scPath")
                                        } catch (e: Exception) {
                                            logFn("[!] Failed to open: ${e.message}")
                                        }
                                    }
                                    "SC_Config" -> {
                                        val brain = com.vextiq.optimizer.VextiqBrain()
                                        val result = brain.optimize("star_citizen", "performance", "none", 60, logFn)
                                        logFn("")
                                        logFn("[OK] USER.cfg generated! Score: ${result.score}")
                                        logFn("[i] Copy the config from above to your USER folder")
                                    }
                                    "MMCSS" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.optimizeMMCSS(logFn)
                                    }
                                    "GamePriority" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.setGamePriority(logFn)
                                    }
                                    "Backup" -> optimizer.createBackup(logFn)
                                    "Restore" -> optimizer.restoreBackup(logFn)
                                    "Pagefile" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.setOptimizedPagefile(logFn)
                                    }
                                    "MSI" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.enableMsiMode(logFn)
                                    }
                                    "GlobalShader" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.cleanGlobalShaders(logFn)
                                    }
                                    "Debloat" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.optimizeServices(logFn)
                                    }
                                    "USB" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.optimizeUsbLatency(logFn)
                                    }
                                    "Affinity" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.setCpuAffinity(logFn)
                                    }
                                    "Standby" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.cleanStandbyList(logFn)
                                    }
                                    "Repair" -> {
                                        val booster = com.vextiq.optimizer.AdvancedBooster()
                                        booster.repairSystem(logFn)
                                    }
                                }
                            }
                        }
                    )
                    
                    Page.VPN -> VpnPage()
                    
                    Page.SETTINGS -> SettingsPage(
                        onLanguageChange = { lang ->
                            Strings.setLanguage(lang)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardPage(
    stats: SystemStats,
    cpuHistory: List<Int>,
    gpuHistory: List<Int>,
    _ramHistory: List<Int>,
    latencyHistory: List<Int>,
    selectedGame: Game,
    selectedPlaystyle: String,
    onPlaystyleChange: (String) -> Unit,
    logs: List<Pair<String, String>>,
    isOptimizing: Boolean,
    isScanning: Boolean,
    onBoost: () -> Unit,
    onHardwareScan: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        // Stats row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("CPU", "${stats.cpuUsage}%", cpuHistory, Modifier.weight(1f))
            StatCard("GPU", "${stats.gpuUsage}%", gpuHistory, Modifier.weight(1f))
            val fpsText = if(stats.isGameRunning) "${stats.fps} FPS" else "0 FPS"
            StatCard("FPS", fpsText, listOf(stats.fps), Modifier.weight(1f))
            StatCard("LATENCY", "${String.format("%.1f", stats.frametime)}ms", latencyHistory, Modifier.weight(1f))
        }
        
        // Hardware Scan Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VextiqColors.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, VextiqColors.Border, RoundedCornerShape(10.dp))
                .clickable(enabled = !isScanning) { onHardwareScan() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔍", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (isScanning) "SCANNING HARDWARE..." else "HARDWARE SCAN",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isScanning) VextiqColors.Primary else VextiqColors.TextPrimary
                        )
                        Text(
                            "Detect CPU, GPU, RAM, Display",
                            fontSize = 11.sp,
                            color = VextiqColors.TextMuted
                        )
                    }
                }
                if (!isScanning) {
                    Box(
                        modifier = Modifier
                            .background(VextiqColors.Primary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("SCAN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        
        // Hero - Optimize section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                .border(2.dp, VextiqColors.Border, RoundedCornerShape(12.dp))
                .padding(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (stats.isGameRunning) {
                    Text("ACTIVE: ${stats.activeGame.uppercase()}", fontSize = 12.sp, color = VextiqColors.Success, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                }
                Text("Smart Boost", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                // Game info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedGame.icon, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(selectedGame.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Playstyle selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Playstyle:", fontSize = 12.sp, color = VextiqColors.TextSecondary)
                    selectedGame.playstyles.forEach { ps ->
                        val isSelected = selectedPlaystyle == ps.id
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) VextiqColors.Primary else VextiqColors.Surface,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) VextiqColors.Primary else VextiqColors.Border,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onPlaystyleChange(ps.id) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                ps.name.replace(" (", "\n(").substringBefore("\n"),
                                fontSize = 10.sp,
                                color = if (isSelected) Color.White else VextiqColors.TextSecondary
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                BoostButton(
                    text = if (isOptimizing) "OPTIMIZING..." else "OPTIMIZE NOW",
                    enabled = !isOptimizing && !isScanning,
                    onClick = onBoost
                )
            }
        }
        
        // Terminal
        Terminal("System Log", logs, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
fun GameBoostPage(
    games: List<Game>,
    selectedGame: Game,
    logs: List<Pair<String, String>>,
    onSelectGame: (Game) -> Unit,
    onOptimize: (String) -> Unit,
    onScanGames: () -> Unit
) {
    var customPath by remember { mutableStateOf("") }
    var selectedPlaystyle by remember { mutableStateOf("none") }
    val gamePaths = remember { com.vextiq.core.GamePaths() }

    
    // Load saved path when game changes
    LaunchedEffect(selectedGame) {
        customPath = gamePaths.getPath(selectedGame.id) ?: ""
    }
    
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Left: Game selection
        Column(
            modifier = Modifier.weight(0.35f),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Game List Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1A1D23), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SELECT GAME", 
                        fontSize = 12.sp, 
                        color = Color(0xFF00D9FF),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Button(
                        onClick = onScanGames,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                        modifier = Modifier.height(30.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) { 
                        Text("SCAN", fontSize = 11.sp, fontWeight = FontWeight.Bold) 
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Game list
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    games.forEach { game ->
                        val isSelected = game == selectedGame
                        val hasPath = gamePaths.getPath(game.id) != null
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color(0xFF00D9FF).copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    if (isSelected) 1.dp else 0.dp,
                                    if (isSelected) Color(0xFF00D9FF) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectGame(game) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(game.icon, fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    game.name,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color(0xFF00D9FF) else Color.White,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                            if (hasPath) {
                                Text("✓", fontSize = 14.sp, color = Color(0xFF4ADE80))
                            }
                        }
                    }
                }
            }
            
            // Playstyle section
            Column(
                modifier = Modifier
                    .background(Color(0xFF1A1D23), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    "⚡ PLAYSTYLE", 
                    fontSize = 11.sp, 
                    color = Color(0xFF00D9FF), 
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(10.dp))
                
                // Reset playstyle if current selection is not in the new game's list
                if (selectedGame.playstyles.none { it.id == selectedPlaystyle }) {
                    selectedPlaystyle = selectedGame.playstyles.firstOrNull()?.id ?: "none"
                }
                
                selectedGame.playstyles.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlaystyle = style.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPlaystyle == style.id,
                            onClick = { selectedPlaystyle = style.id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF00D9FF),
                                unselectedColor = Color(0xFF4B5563)
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            style.name,
                            fontSize = 12.sp,
                            color = if (selectedPlaystyle == style.id) Color.White else Color(0xFF9CA3AF)
                        )
                    }
                }
            }

            
            // Path section with Browse button
            Column(
                modifier = Modifier
                    .background(Color(0xFF1A1D23), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PATH", fontSize = 10.sp, color = Color(0xFF00D9FF), fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { 
                            val chooser = javax.swing.JFileChooser()
                            chooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                            chooser.dialogTitle = "Select ${selectedGame.name} folder"
                            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                                customPath = chooser.selectedFile.absolutePath
                                gamePaths.setPath(selectedGame.id, customPath)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                        modifier = Modifier.height(26.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) { 
                        Text("Browse", fontSize = 10.sp) 
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (customPath.isNotEmpty()) customPath else "No path set - click Browse or Scan",
                    fontSize = 10.sp,
                    color = if (customPath.isNotEmpty()) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                    maxLines = 2
                )
            }

            
            // Optimize Button
            Button(
                onClick = {
                    // Auto-save path before optimize
                    if (customPath.isNotEmpty()) {
                        gamePaths.setPath(selectedGame.id, customPath)
                    }
                    onOptimize(selectedPlaystyle)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D9FF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "OPTIMIZE ${selectedGame.name.uppercase()}", 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1117)
                )
            }
        }
        
        // Right: Output Log
        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
                .background(Color(0xFF1A1D23), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                "OUTPUT", 
                fontSize = 12.sp, 
                color = Color(0xFF00D9FF),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(12.dp))
            
            // Log content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                logs.forEach { (msg, type) ->
                    if (msg.isNotEmpty()) {
                        val color = when (type) {
                            "ok" -> Color(0xFF4ADE80)
                            "warn" -> Color(0xFFFBBF24)
                            else -> Color(0xFF9CA3AF)
                        }
                        val isHighlight = msg.contains("════") || msg.contains("SAVED") || msg.contains("CONFIG")
                        Text(
                            msg,
                            fontSize = if (isHighlight) 11.sp else 10.sp,
                            color = if (isHighlight) Color(0xFF00D9FF) else color,
                            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolsPage(
    logs: List<Pair<String, String>>,
    onAction: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        // Left: Tool buttons with scroll
        Column(
            modifier = Modifier
                .weight(0.4f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ultimate Boost
            Button(
                onClick = { onAction("UltimateBoost") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("🚀 ULTIMATE BOOST", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(4.dp))
            
            // System Optimizations
            Column(
                modifier = Modifier
                    .background(VextiqColors.Card, RoundedCornerShape(10.dp))
                    .border(1.dp, VextiqColors.Border, RoundedCornerShape(10.dp))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("SYSTEM", fontSize = 11.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
                
                ToolButton("⏱️ Timer Resolution", "0.5ms latency") { onAction("Timer") }
                ToolButton("💻 Disable Core Parking", "All cores active") { onAction("CPU") }
                ToolButton("⚡ Ultimate Power Plan", "Max performance") { onAction("PowerPlan") }
                ToolButton("🖱️ Mouse Optimization", "Raw input") { onAction("Mouse") }
            }
            
            // Cleanup
            Column(
                modifier = Modifier
                    .background(VextiqColors.Card, RoundedCornerShape(10.dp))
                    .border(1.dp, VextiqColors.Border, RoundedCornerShape(10.dp))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("CLEANUP", fontSize = 11.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
                
                ToolButton("🗑️ Clear Cache", "Shader & temp files") { onAction("Cache") }
                ToolButton("✨ Global Shader Clean", "DX & NVIDIA cache") { onAction("GlobalShader") }
                ToolButton("🧹 Free RAM", "Release memory") { onAction("RAM") }
                ToolButton("🌐 Network Boost", "TCP/IP optimize") { onAction("Network") }
            }
            
            // Star Citizen
            Column(
                modifier = Modifier
                    .background(VextiqColors.Card, RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF00B4D8), RoundedCornerShape(10.dp))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🌌 STAR CITIZEN", fontSize = 11.sp, color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                
                ToolButton("🗑️ Clear SC Shaders", "Delete shader cache") { onAction("SC_Shaders") }
                ToolButton("📁 Open SC AppData", "Local cache folder") { onAction("SC_AppData") }
                ToolButton("⚙️ Generate USER.cfg", "Optimized config") { onAction("SC_Config") }
            }
            
            // Backup
            Column(
                modifier = Modifier
                    .background(VextiqColors.Card, RoundedCornerShape(10.dp))
                    .border(1.dp, VextiqColors.Border, RoundedCornerShape(10.dp))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("MAINTENANCE", fontSize = 11.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
                
                ToolButton("💾 ${"backup_settings".localized()}", "Save configuration") { onAction("Backup") }
                ToolButton("↩️ ${"restore_backup".localized()}", "Revert app changes") { onAction("Restore") }
                ToolButton("🛠️ ${"tools_repair".localized()}", "tools_repair_desc".localized()) { onAction("Repair") }
            }
            
            // Expert tweaks
            Column(
                modifier = Modifier
                    .background(VextiqColors.Card, RoundedCornerShape(10.dp))
                    .border(1.dp, VextiqColors.Border, RoundedCornerShape(10.dp))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("tools_expert".localized(), fontSize = 11.sp, color = Color(0xFFFF6B00), fontWeight = FontWeight.Bold)
                
                ToolButton("🎯 MMCSS Priority", "Optimize task scheduler") { onAction("MMCSS") }
                ToolButton("🚀 Game Priority", "Set global priority") { onAction("GamePriority") }
                ToolButton("📄 Virtual Memory", "Optimized Pagefile") { onAction("Pagefile") }
                ToolButton("🏎️ MSI Mode", "GPU Latency Fix") { onAction("MSI") }
                ToolButton("🧹 Windows Debloat", "Deep service clean") { onAction("Debloat") }
                ToolButton("🖱️ USB Zero-Lag", "No power saving") { onAction("USB") }
                ToolButton("🧠 CPU Affinity", "P-Core Pinner") { onAction("Affinity") }
                ToolButton("🧼 Standby Purge", "ISLC-style RAM clean") { onAction("Standby") }
            }
        }
        
        // Right: Log output
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .background(VextiqColors.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, VextiqColors.Border, RoundedCornerShape(10.dp))
                .padding(15.dp)
        ) {
            Text("📋 TOOLS LOG", fontSize = 11.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                logs.forEach { (msg, type) ->
                    if (msg.isNotEmpty()) {
                        val color = when (type) {
                            "ok" -> VextiqColors.Success
                            "warn" -> VextiqColors.Warning
                            else -> VextiqColors.TextSecondary
                        }
                        Text(
                            msg,
                            fontSize = 10.sp,
                            color = color,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolButton(title: String, subtitle: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, VextiqColors.Border),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title, fontSize = 11.sp, color = VextiqColors.TextPrimary)
            Text(subtitle, fontSize = 9.sp, color = VextiqColors.TextMuted)
        }
    }
}

@Composable
fun SmallActionBtn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
        border = BorderStroke(1.dp, VextiqColors.Border)
    ) {
        Text(text, fontSize = 9.sp)
    }
}

@Composable
fun HwRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = VextiqColors.TextMuted)
        Text(value, fontSize = 12.sp, color = VextiqColors.Primary, fontWeight = FontWeight.SemiBold)
    }
    Divider(color = VextiqColors.Border)
}

@Composable
fun SettingsPage(
    onLanguageChange: (String) -> Unit
) {
    val settings = remember { com.vextiq.core.SettingsManager() }
    var selectedLanguage by remember { mutableStateOf(settings.language) }
    var darkMode by remember { mutableStateOf(settings.darkMode) }
    var minimizeToTray by remember { mutableStateOf(settings.minimizeToTray) }
    var startWithWindows by remember { mutableStateOf(settings.startWithWindows) }
    var autoOptimize by remember { mutableStateOf(settings.autoOptimize) }
    var checkUpdates by remember { mutableStateOf(settings.checkUpdates) }
    
    // Game paths
    var scPath by remember { mutableStateOf("") }
    var bf6Path by remember { mutableStateOf("") }
    var pathStatus by remember { mutableStateOf("") }
    val gamePaths = remember { com.vextiq.core.GamePaths() }
    
    // Load saved paths
    LaunchedEffect(Unit) {
        scPath = gamePaths.getPath("star_citizen") ?: ""
        bf6Path = gamePaths.getPath("battlefield_6") ?: ""
    }
    
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        // Header
        Text(
            "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = VextiqColors.Primary
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Left column - General settings
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                    .border(1.dp, VextiqColors.Border, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Text("GENERAL", fontSize = 12.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
                
                // Language
                SettingRow("Language / Хэл") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingChip("English", selectedLanguage == "en") { 
                            selectedLanguage = "en"
                            settings.language = "en"
                            onLanguageChange("en")
                        }
                        SettingChip("Монгол", selectedLanguage == "mn") { 
                            selectedLanguage = "mn"
                            settings.language = "mn"
                            onLanguageChange("mn")
                        }
                    }
                }
                
                // Theme
                SettingRow("Theme") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingChip("🌙 Dark", darkMode) { darkMode = true; settings.darkMode = true }
                        SettingChip("☀️ Light", !darkMode) { darkMode = false; settings.darkMode = false }
                    }
                }
                
                Divider(color = VextiqColors.Border)
                
                Text("settings_overlay_content".localized(), fontSize = 12.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
                
                var showGpu by remember { mutableStateOf(settings.overlayShowGpu) }
                var showCpu by remember { mutableStateOf(settings.overlayShowCpu) }
                var showRam by remember { mutableStateOf(settings.overlayShowRam) }
                var showNet by remember { mutableStateOf(settings.overlayShowNet) }
                var showFps by remember { mutableStateOf(settings.overlayShowFps) }
                var showFt by remember { mutableStateOf(settings.overlayShowFrametime) }

                SettingToggle("Show FPS Counter", showFps) { showFps = it; settings.overlayShowFps = it }
                SettingToggle("Show Frametime (FT)", showFt) { showFt = it; settings.overlayShowFrametime = it }
                SettingToggle("Show GPU Usage", showGpu) { showGpu = it; settings.overlayShowGpu = it }
                SettingToggle("Show CPU Usage", showCpu) { showCpu = it; settings.overlayShowCpu = it }
                SettingToggle("Show RAM Usage", showRam) { showRam = it; settings.overlayShowRam = it }
                SettingToggle("Show Network (Ping)", showNet) { showNet = it; settings.overlayShowNet = it }

                Divider(color = VextiqColors.Border)
                
                Text("settings_behavior".localized(), fontSize = 12.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
                
                // Toggles
                SettingToggle("minimize_tray".localized(), minimizeToTray) { minimizeToTray = it; settings.minimizeToTray = it }
                SettingToggle("start_windows".localized(), startWithWindows) { startWithWindows = it; settings.startWithWindows = it }
                SettingToggle("auto_optimize".localized(), autoOptimize) { autoOptimize = it; settings.autoOptimize = it }
                SettingToggle("check_updates".localized(), checkUpdates) { checkUpdates = it; settings.checkUpdates = it }
            }
            
            // Right column - Game Paths & About
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                // Game Paths
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                        .border(1.dp, VextiqColors.Border, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GAME PATHS", fontSize = 12.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
                        
                        // Scan button
                        Button(
                            onClick = {
                                pathStatus = "Scanning..."
                                val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                                scope.launch {
                                    val found = gamePaths.scanAllLaunchers { }
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        scPath = gamePaths.getPath("star_citizen") ?: ""
                                        bf6Path = gamePaths.getPath("battlefield_6") ?: ""
                                        pathStatus = "✓ Found ${found.size} games!"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                            modifier = Modifier.height(32.dp)
                        ) { Text("🔍 Scan Games", fontSize = 11.sp) }
                    }
                    
                    Text("Auto-detect from Steam, Epic, EA, RSI", fontSize = 10.sp, color = VextiqColors.TextMuted)
                    
                    Divider(color = VextiqColors.Border)
                    
                    // Status message
                    if (pathStatus.isNotEmpty()) {
                        Text(
                            pathStatus, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Medium,
                            color = if (pathStatus.startsWith("✓") || pathStatus.contains("Found")) VextiqColors.Success 
                                   else if (pathStatus == "Scanning...") VextiqColors.Primary
                                   else VextiqColors.Error
                        )
                    }
                    
                    // Show all found games
                    val allPaths = gamePaths.getAllPaths()
                    if (allPaths.isNotEmpty()) {
                        Text("FOUND GAMES (${allPaths.size})", fontSize = 11.sp, color = VextiqColors.Success, fontWeight = FontWeight.Bold)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(VextiqColors.Surface, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            allPaths.forEach { (gameId, path) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "✓ ${gameId.replace("_", " ").uppercase()}",
                                        fontSize = 10.sp,
                                        color = VextiqColors.Success,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    path,
                                    fontSize = 9.sp,
                                    color = VextiqColors.TextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Text(
                            "Click 'Scan Games' to detect installed games",
                            fontSize = 11.sp,
                            color = VextiqColors.TextMuted
                        )
                    }
                }
                
                // About
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                        .border(1.dp, VextiqColors.Border, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    Text("ABOUT", fontSize = 12.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("VEXTIQ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary)
                        Text("PRO V1", fontSize = 14.sp, color = VextiqColors.TextSecondary)
                        Spacer(Modifier.height(10.dp))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(VextiqColors.Surface, RoundedCornerShape(8.dp))
                                .padding(15.dp)
                        ) {
                            AboutRow("Version", "1.0.0")
                            AboutRow("Build", "2026.03")
                            AboutRow("Engine", "VEXTIQ BRAIN")
                        }
                    }
                    
                    Text(
                        "Made with ❤️ for gamers",
                        fontSize = 11.sp,
                        color = VextiqColors.TextMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = VextiqColors.TextPrimary)
        content()
    }
}

@Composable
fun SettingChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) VextiqColors.Primary.copy(alpha = 0.2f) else VextiqColors.Surface)
            .border(1.dp, if (selected) VextiqColors.Primary else VextiqColors.Border, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (selected) VextiqColors.Primary else VextiqColors.TextSecondary
        )
    }
}

@Composable
fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High contrast label
        Text(label, fontSize = 13.sp, color = VextiqColors.TextPrimary, fontWeight = FontWeight.Medium)
        
        Box(
            modifier = Modifier
                .size(44.dp, 24.dp) // Slightly bigger
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) VextiqColors.Success else Color(0xFF2D3748)) // Darker background when off
                .border(1.dp, if (checked) Color.Transparent else VextiqColors.Border, RoundedCornerShape(12.dp))
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .offset(x = if (checked) 20.dp else 0.dp)
                    .background(Color.White, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = VextiqColors.TextMuted)
        Text(value, fontSize = 12.sp, color = VextiqColors.TextPrimary)
    }
}

fun main() = application {
    val settings = remember { com.vextiq.core.SettingsManager() }
    var isWindowVisible by remember { mutableStateOf(true) }
    val windowState = rememberWindowState(width = 1100.dp, height = 750.dp)
    
    // SINGLE SystemMonitor - shared between App and FpsOverlay
    val sharedMonitor = remember { SystemMonitor() }
    val sharedScope = rememberCoroutineScope()
    val sharedStats by sharedMonitor.stats.collectAsState()
    
    // FPS Overlay state
    var showFpsOverlay by remember { mutableStateOf(false) }
    
    // Start monitor ONCE
    LaunchedEffect(Unit) {
        // Load correct language immediately
        Strings.setLanguage(settings.language)
        
        // Detect hardware first (singleton) - will log to console
        com.vextiq.core.HardwareManager.detect { msg ->
            println(msg) // Console log for debugging
        }
        // Then start monitoring
        sharedMonitor.start(sharedScope)
    }
    
    // Watch for language changes
    LaunchedEffect(settings.language) {
        Strings.setLanguage(settings.language)
    }

    LaunchedEffect(windowState.isMinimized) {
        if (windowState.isMinimized && settings.minimizeToTray) {
            isWindowVisible = false
            windowState.isMinimized = false
        }
    }

    if (isWindowVisible) {
        Window(
            onCloseRequest = {
                if (settings.minimizeToTray) {
                    isWindowVisible = false
                } else {
                    exitApplication()
                }
            },
            title = "VEXTIQ PRO V1",
            state = windowState,
            icon = painterResource("logo.png")
        ) {
            App(
                sharedMonitor = sharedMonitor,
                showFpsOverlay = showFpsOverlay,
                onToggleFpsOverlay = { showFpsOverlay = it }
            )
        }
    }

    // FPS Overlay - OUTSIDE main window, uses same monitor
    FpsOverlay(
        stats = sharedStats,
        isVisible = showFpsOverlay,
        settings = settings,
        onClose = { showFpsOverlay = false }
    )

    if (isTraySupported) {
        Tray(
            icon = painterResource("logo.png"),
            tooltip = "VEXTIQ PRO V1",
            onAction = { isWindowVisible = true },
            menu = {
                Item("tray_show".localized(), onClick = { isWindowVisible = true })
                Item("tray_toggle_fps".localized(), onClick = { showFpsOverlay = !showFpsOverlay })
                Separator()
                Item("tray_exit".localized(), onClick = ::exitApplication)
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// VPN PAGE
// ═══════════════════════════════════════════════════════════════

@Composable
fun VpnPage() {
    val scope = rememberCoroutineScope()
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var selectedRegion by remember { mutableStateOf("USA") }
    var vpnLatency by remember { mutableStateOf(0) }
    var logs by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    
    // VPN Regions (сервер нэмэх бүрт энд нэмнэ)
    val regions = listOf(
        Triple("USA", "34.171.9.109", "🇺🇸"),
        // Triple("EU", "xx.xx.xx.xx", "🇪🇺"),  // Дараа нэмнэ
        // Triple("ASIA", "xx.xx.xx.xx", "🇸🇬"), // Дараа нэмнэ
    )
    
    // Check VPN status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isConnected = checkWireGuardStatus()
            if (isConnected) {
                vpnLatency = pingVpnServer(regions.find { it.first == selectedRegion }?.second ?: "")
            }
            kotlinx.coroutines.delay(3000)
        }
    }
    
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        // Left: VPN Controls
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Connection Status Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                    .border(
                        2.dp,
                        if (isConnected) Color(0xFF00FF88) else VextiqColors.Border,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isConnected) "🟢 CONNECTED" else "🔴 DISCONNECTED",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) Color(0xFF00FF88) else Color(0xFFFF4444)
                )
                
                Spacer(Modifier.height(10.dp))
                
                if (isConnected) {
                    Text(
                        "VPN Latency: ${vpnLatency}ms",
                        fontSize = 14.sp,
                        color = if (vpnLatency < 50) Color(0xFF00FF88) else if (vpnLatency < 100) Color(0xFFFFCC00) else Color(0xFFFF4444)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Region: $selectedRegion",
                        fontSize = 12.sp,
                        color = VextiqColors.TextSecondary
                    )
                }
                
                Spacer(Modifier.height(15.dp))
                
                // Connect/Disconnect Button
                Button(
                    onClick = {
                        scope.launch {
                            isConnecting = true
                            val logFn: (String) -> Unit = { msg ->
                                val type = when {
                                    msg.contains("[OK]") -> "ok"
                                    msg.contains("[!]") -> "error"
                                    else -> ""
                                }
                                logs = logs + (msg to type)
                            }
                            
                            if (isConnected) {
                                disconnectWireGuard(logFn)
                                isConnected = false
                            } else {
                                connectWireGuard(logFn)
                                kotlinx.coroutines.delay(2000)
                                isConnected = checkWireGuardStatus()
                            }
                            isConnecting = false
                        }
                    },
                    enabled = !isConnecting,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color(0xFFFF4444) else Color(0xFF00D2FF)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isConnecting) {
                        Text("⏳ CONNECTING...", fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            if (isConnected) "🔌 DISCONNECT" else "🚀 CONNECT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            
            // Region Selection
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                    .border(1.dp, VextiqColors.Border, RoundedCornerShape(12.dp))
                    .padding(15.dp)
            ) {
                Text("🌍 SELECT REGION", fontSize = 12.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
                
                Spacer(Modifier.height(10.dp))
                
                regions.forEach { (name, _, flag) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selectedRegion == name) VextiqColors.Primary.copy(alpha = 0.2f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedRegion = name }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(flag, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(name, fontSize = 14.sp, color = VextiqColors.TextPrimary, fontWeight = FontWeight.Medium)
                        }
                        if (selectedRegion == name) {
                            Text("✓", fontSize = 16.sp, color = VextiqColors.Primary)
                        }
                    }
                }
                
                Spacer(Modifier.height(10.dp))
                Text(
                    "💡 More regions coming soon!",
                    fontSize = 11.sp,
                    color = VextiqColors.TextSecondary
                )
            }
        }
        
        // Right: Log output
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .background(VextiqColors.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, VextiqColors.Border, RoundedCornerShape(10.dp))
                .padding(15.dp)
        ) {
            Text("📋 VPN LOG", fontSize = 11.sp, color = VextiqColors.Primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (logs.isEmpty()) {
                    Text(
                        "[i] Click CONNECT to start VPN",
                        fontSize = 11.sp,
                        color = VextiqColors.TextSecondary
                    )
                }
                logs.forEach { (msg, type) ->
                    Text(
                        msg,
                        fontSize = 11.sp,
                        color = when (type) {
                            "ok" -> Color(0xFF00FF88)
                            "error" -> Color(0xFFFF4444)
                            else -> VextiqColors.TextSecondary
                        },
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// WireGuard Helper Functions
private fun checkWireGuardStatus(): Boolean {
    return try {
        val process = ProcessBuilder(listOf(
            "powershell", "-NoProfile", "-Command",
            "Get-Service -Name 'WireGuardTunnel*' -ErrorAction SilentlyContinue | Where-Object { \$_.Status -eq 'Running' } | Measure-Object | Select-Object -ExpandProperty Count"
        )).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        (output.toIntOrNull() ?: 0) > 0
    } catch (e: Exception) { false }
}

private fun pingVpnServer(ip: String): Int {
    if (ip.isEmpty()) return 0
    return try {
        val process = ProcessBuilder(listOf(
            "ping", "-n", "1", "-w", "1000", "10.0.0.1"
        )).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        val match = "time[<=](\\d+)".toRegex().find(output)
        match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    } catch (e: Exception) { 0 }
}

private fun connectWireGuard(onLog: (String) -> Unit) {
    onLog("[>>] Connecting to VPN...")
    try {
        // Try to activate WireGuard tunnel
        val process = ProcessBuilder(listOf(
            "powershell", "-NoProfile", "-Command",
            "Start-Process 'C:\\Program Files\\WireGuard\\wireguard.exe' -ArgumentList '/installtunnelservice', 'C:\\Users\\\$env:USERNAME\\Documents\\WireGuard\\VEXTIQ-VPN.conf' -Verb RunAs -Wait -WindowStyle Hidden; Start-Sleep 2"
        )).redirectErrorStream(true).start()
        process.waitFor()
        
        // Alternative: Use wg-quick style
        val altProcess = ProcessBuilder(listOf(
            "powershell", "-NoProfile", "-Command",
            "& 'C:\\Program Files\\WireGuard\\wireguard.exe' /installtunnelservice 'C:\\Users\\\$env:USERNAME\\Documents\\WireGuard\\VEXTIQ-VPN.conf'"
        )).redirectErrorStream(true).start()
        altProcess.waitFor()
        
        onLog("[OK] VPN connection initiated")
        onLog("[i] Check WireGuard app for status")
    } catch (e: Exception) {
        onLog("[!] Error: ${e.message}")
        onLog("[i] Make sure WireGuard is installed and config exists")
    }
}

private fun disconnectWireGuard(onLog: (String) -> Unit) {
    onLog("[>>] Disconnecting VPN...")
    try {
        val process = ProcessBuilder(listOf(
            "powershell", "-NoProfile", "-Command",
            "& 'C:\\Program Files\\WireGuard\\wireguard.exe' /uninstalltunnelservice 'VEXTIQ-VPN'"
        )).redirectErrorStream(true).start()
        process.waitFor()
        onLog("[OK] VPN disconnected")
    } catch (e: Exception) {
        onLog("[!] Error: ${e.message}")
    }
}
