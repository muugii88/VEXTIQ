package com.vextiq

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.vextiq.core.*
import com.vextiq.optimizer.*
import com.vextiq.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Page { DASHBOARD, GAMEBOOST, TOOLS, VPN, TROUBLESHOOT, SETTINGS }

/**
 * On packaged builds (jpackage sets `jpackage.app-path`), make sure the user's
 * real Desktop has a VEXTIQ shortcut. The installer drops a shortcut in
 * `C:\Users\Public\Desktop`, but OneDrive-redirected desktops hide that, so we
 * top up with a user-level shortcut at first launch.
 *
 * Strictly best-effort: any PowerShell hiccup is swallowed. We run this on a
 * background thread so it never blocks startup.
 */
private fun ensureUserDesktopShortcutAsync() {
    val appPath = System.getProperty("jpackage.app-path") ?: return
    Thread({
        try {
            val getDesktopProc = ProcessBuilder(
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                "[Environment]::GetFolderPath('Desktop')"
            ).redirectErrorStream(true).start()
            val finished = getDesktopProc.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) { getDesktopProc.destroyForcibly(); return@Thread }
            val desktopPath = getDesktopProc.inputStream.bufferedReader().use { it.readText() }.trim()
            val desktop = java.io.File(desktopPath).takeIf { it.exists() && it.isDirectory } ?: return@Thread

            val link = java.io.File(desktop, "VEXTIQ PRO.lnk")
            if (link.exists()) return@Thread
            // Escape paths for single-quoted PowerShell strings (' -> '') so a
            // username/path containing a quote can't break out and inject commands.
            fun ps(value: String) = value.replace("'", "''")
            val linkPath = ps(link.absolutePath)
            val targetPath = ps(appPath)
            val workingDir = ps(java.io.File(appPath).parent ?: "")
            val createProc = ProcessBuilder(
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                "\$s=(New-Object -ComObject WScript.Shell).CreateShortcut('$linkPath'); " +
                    "\$s.TargetPath='$targetPath'; \$s.WorkingDirectory='$workingDir'; " +
                    "\$s.IconLocation='$targetPath'; \$s.Description='VEXTIQ PRO'; \$s.Save()"
            ).start()
            createProc.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Throwable) { /* best-effort */ }
    }, "VEXTIQ-DesktopShortcut").apply { isDaemon = true }.start()
}

fun main() {
    // Install logging FIRST, before any other code can swallow output. Packaged
    // builds run with console=false, so without this, every print and stack
    // trace goes to a detached stream — crashes look invisible to the user.
    CrashLogger.install()
    CrashLogger.log("main() entered, calling application{}")
    try {
        runApp()
    } catch (t: Throwable) {
        CrashLogger.log("[FATAL] main() threw: ${t.javaClass.name}: ${t.message}")
        t.printStackTrace(System.err)
        throw t
    }
}

private fun runApp() = application {
    CrashLogger.log("application{} composable scope entered")
    ensureUserDesktopShortcutAsync()

    val monitor = remember {
        CrashLogger.log("creating SystemMonitor")
        SystemMonitor()
    }
    var showFpsOverlay by remember { mutableStateOf(false) }
    val settings = remember { SettingsManager() }
    var isWindowVisible by remember { mutableStateOf(true) }
    
    // System Tray
    if (!isWindowVisible || settings.minimizeToTray) {
        Tray(
            icon = painterResource("icon.ico"),
            tooltip = "VEXTIQ PRO v2.0",
            onAction = { isWindowVisible = true },  // Double-click tray icon restores window
            menu = {
                Item("Show VEXTIQ PRO") { isWindowVisible = true }
                Item(if (showFpsOverlay) "Disable FPS Overlay" else "Enable FPS Overlay") {
                    showFpsOverlay = !showFpsOverlay
                }
                Separator()
                Item("Exit VEXTIQ") { exitApplication() }
            }
        )
    }
    
    // Main Window
    if (isWindowVisible) {
        Window(
            onCloseRequest = {
                if (settings.minimizeToTray) {
                    // Minimize to tray instead of exiting
                    isWindowVisible = false
                } else {
                    exitApplication()
                }
            },
            title = "VEXTIQ PRO v2.0",
            state = rememberWindowState(size = DpSize(1100.dp, 750.dp)),
            icon = painterResource("icon.ico")
        ) {
            App(
                sharedMonitor = monitor,
                showFpsOverlay = showFpsOverlay,
                onToggleFpsOverlay = { showFpsOverlay = it }
            )
        }
    }
    
    // FPS Overlay Window (independent of main window)
    if (showFpsOverlay) {
        FpsOverlayWindow(monitor) { showFpsOverlay = false }
    }
}

@Composable
fun App(
    sharedMonitor: SystemMonitor? = null,
    showFpsOverlay: Boolean = false,
    onToggleFpsOverlay: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val monitor = sharedMonitor ?: remember { SystemMonitor() }
    val optimizer = remember { Optimizer() }
    val settings = remember { SettingsManager() }
    val stats by monitor.stats.collectAsState()
    val netStats by monitor.networkStats.collectAsState()

    // Admin status drives the dashboard warning banner. Detected once off the UI
    // thread (it spawns a PowerShell check) so startup isn't blocked.
    var isAdmin by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        isAdmin = withContext(Dispatchers.IO) { AdminCheck.isAdmin }
    }

    var currentPage by remember { mutableStateOf(Page.DASHBOARD) }
    var selectedGame by remember {
        mutableStateOf(Games.list.firstOrNull { it.id == settings.lastSelectedGame } ?: Games.list.first())
    }
    var dashboardPlaystyle by remember {
        mutableStateOf(
            settings.getLastPlaystyle(selectedGame.id).takeIf { id ->
                selectedGame.playstyles.any { it.id == id }
            } ?: (selectedGame.playstyles.firstOrNull()?.id ?: "none")
        )
    }

    // Load saved playstyle when game changes, fall back to first available
    LaunchedEffect(selectedGame) {
        settings.lastSelectedGame = selectedGame.id
        val saved = settings.getLastPlaystyle(selectedGame.id)
        dashboardPlaystyle = if (selectedGame.playstyles.any { it.id == saved }) {
            saved
        } else {
            selectedGame.playstyles.firstOrNull()?.id ?: "none"
        }
    }
    
    var logs by remember { mutableStateOf(listOf(
        "[OK] VEXTIQ PRO v2.0 initialized" to "ok",
        "[OK] Real-time monitoring active" to "ok",
        "[i] Click HARDWARE SCAN to detect your system" to ""
    )) }
    var boostLogs by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var toolsLogs by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var isOptimizing by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var boostJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    // Always start the monitor - whether shared or local
    // Use DisposableEffect for proper lifecycle management
    var monitorStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!monitorStarted) {
            monitor.start(scope)
            monitorStarted = true
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (sharedMonitor == null) {
                monitor.stop()
            }
        }
    }
    
    val score = (100 - (stats.cpuUsage * 0.4 + stats.ramUsage * 0.3 + stats.gpuUsage * 0.3)).toInt().coerceIn(0, 100)
    
    VextiqTheme {
        Column(modifier = Modifier.fillMaxSize().background(VextiqColors.Background)) {
            // Admin warning strip — thin, only visible when not elevated
            if (!AdminCheck.isAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFFFFB300).copy(alpha = 0.18f))
                        .border(0.dp, Color(0xFFFFB300))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⚠  Not running as Administrator — many optimizations will silently fail. " +
                                "Right-click VEXTIQ → Run as administrator.",
                        fontSize = 11.sp,
                        color = Color(0xFFFFB300)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // ========== SIDEBAR ==========
            Sidebar(
                score = score,
                selectedGame = selectedGame,
                games = Games.list,
                onSelectGame = { selectedGame = it },
                currentPage = currentPage,
                onPageChange = { currentPage = it },
                showFpsOverlay = showFpsOverlay,
                onToggleFpsOverlay = onToggleFpsOverlay,
                activeGame = if (stats.isGameRunning) stats.activeGame else null
            )
            
            // ========== MAIN CONTENT ==========
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(20.dp)
            ) {
                when (currentPage) {
                    Page.DASHBOARD -> DashboardPage(
                        stats = stats,
                        selectedGame = selectedGame,
                        selectedPlaystyle = dashboardPlaystyle,
                        onPlaystyleChange = {
                            dashboardPlaystyle = it
                            settings.setLastPlaystyle(selectedGame.id, it)
                        },
                        logs = logs,
                        isOptimizing = isOptimizing,
                        isScanning = isScanning,
                        onBoost = {
                            boostJob = scope.launch {
                                isOptimizing = true
                                logs = listOf("[>>] Starting optimization for ${selectedGame.name}..." to "")
                                try {
                                    optimizer.runSmartBoost(
                                        gameId = selectedGame.id,
                                        gameName = selectedGame.name,
                                        playstyle = dashboardPlaystyle,
                                        monitor = monitor,
                                        onLog = { msg ->
                                            val type = when {
                                                msg.startsWith("[OK]") -> "ok"
                                                msg.startsWith("[!]") -> "warn"
                                                else -> ""
                                            }
                                            logs = logs + (msg to type)
                                        }
                                    )
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    logs = logs + ("[!] Optimization cancelled by user" to "warn")
                                    throw e
                                } finally {
                                    isOptimizing = false
                                    boostJob = null
                                }
                            }
                        },
                        onCancel = {
                            boostJob?.cancel()
                        },
                        netStats = netStats,
                        onHardwareScan = {
                            scope.launch {
                                isScanning = true
                                logs = listOf("[>>] Starting Hardware Scan..." to "")
                                HardwareManager.forceRescan { msg ->
                                    val type = if (msg.startsWith("[OK]")) "ok" else ""
                                    logs = logs + (msg to type)
                                }
                                logs = logs + ("[OK] Hardware scan complete!" to "ok")
                                isScanning = false
                            }
                        },
                        onQuickAction = { action ->
                            scope.launch {
                                when (action) {
                                    "Cache" -> {
                                        logs = logs + ("[>>] Clearing cache..." to "")
                                        CacheCleaner().clean { logs = logs + (it to "ok") }
                                    }
                                    "RAM" -> {
                                        logs = logs + ("[>>] Freeing RAM..." to "")
                                        RamCleaner().clean { logs = logs + (it to "ok") }
                                    }
                                    "Kill" -> {
                                        logs = logs + ("[>>] Killing background apps..." to "")
                                        ProcessManager().optimizeForGaming(selectedGame.name) { msg ->
                                            val type = when {
                                                msg.startsWith("[OK]") -> "ok"
                                                msg.startsWith("[!]") -> "warn"
                                                else -> ""
                                            }
                                            logs = logs + (msg to type)
                                        }
                                    }
                                    "VPN" -> logs = logs + ("[i] VPN feature coming soon" to "")
                                    else -> logs = logs + ("[i] Action: $action" to "")
                                }
                            }
                        },
                        isAdmin = isAdmin
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
                            settings.setLastPlaystyle(selectedGame.id, playstyle)
                            scope.launch {
                                boostLogs = listOf("[>>] Optimizing ${selectedGame.name}..." to "")
                                GameSpecificOptimizer().optimize(selectedGame.id, playstyle) { msg ->
                                    val t = if (msg.startsWith("[OK]")) "ok" else ""
                                    boostLogs = boostLogs + (msg to t)
                                }
                                boostLogs = boostLogs + ("[OK] Optimization complete!" to "ok")
                            }
                        },
                        onScanGames = {
                            scope.launch {
                                boostLogs = listOf("[>>] Scanning for games..." to "")
                                val found = GamePaths().scanAllLaunchers { msg ->
                                    boostLogs = boostLogs + (msg to if (msg.startsWith("[OK]")) "ok" else "")
                                }
                                boostLogs = boostLogs + ("[OK] Found ${found.size} games!" to "ok")
                            }
                        }
                    )
                    
                    Page.TOOLS -> ToolsPage(
                        logs = toolsLogs,
                        onAction = { action ->
                            scope.launch {
                                toolsLogs = listOf("[>>] Running $action..." to "")
                                val logMsg: (String) -> Unit = { msg ->
                                    val type = when {
                                        msg.startsWith("[OK]") -> "ok"
                                        msg.startsWith("[!]") -> "warn"
                                        msg.startsWith("[ERR]") || msg.startsWith("[X]") -> "err"
                                        else -> ""
                                    }
                                    toolsLogs = toolsLogs + (msg to type)
                                }
                                // Auto-backup before destructive system tweaks if the user has it enabled.
                                // Restore/Backup/Repair actions don't need a pre-backup themselves.
                                val destructiveActions = setOf(
                                    "UltimateBoost", "Timer", "CPU", "PowerPlan", "GameMode", "HAGS",
                                    "Network", "FSO", "GameDVR", "Startup", "Telemetry",
                                    "SearchIndex", "SysMain", "SC_Memory"
                                )
                                if (settings.backupBeforeChanges && action in destructiveActions) {
                                    logMsg("[i] Auto-backup enabled — creating safety backup...")
                                    try {
                                        BackupManager().createBackup { }
                                        logMsg("[OK] Backup created (restore from Tools → Restore)")
                                    } catch (e: Exception) {
                                        logMsg("[!] Backup skipped: ${e.message}")
                                    }
                                }
                                when (action) {
                                    "UltimateBoost" -> optimizer.runUltimateBoost(logMsg, monitor)
                                    "Timer" -> WindowsOptimizer().setTimerResolution(logMsg)
                                    "CPU" -> WindowsOptimizer().disableCoreParking(logMsg)
                                    "PowerPlan" -> WindowsOptimizer().setUltimatePowerPlan(logMsg)
                                    "Mouse" -> WindowsOptimizer().optimizeMouse(logMsg)
                                    "GameMode" -> WindowsOptimizer().enableGameMode(logMsg)
                                    "HAGS" -> WindowsOptimizer().enableHAGS(logMsg)
                                    "Cache" -> CacheCleaner().clean(logMsg)
                                    "GlobalShader" -> CacheCleaner().cleanGlobalShaders(logMsg)
                                    "RAM" -> RamCleaner().clean(logMsg)
                                    "Network" -> NetworkOptimizer().optimize(logMsg)
                                    "WinTemp" -> CacheCleaner().cleanWindowsTemp(logMsg)
                                    "Prefetch" -> CacheCleaner().cleanPrefetch(logMsg)
                                    "NVIDIA" -> GpuOptimizer().optimizeNvidia(logMsg)
                                    "AMD" -> GpuOptimizer().optimizeAmd(logMsg)
                                    "FSO" -> WindowsOptimizer().disableFullscreenOptimizations(logMsg)
                                    "GameDVR" -> WindowsOptimizer().disableGameDVR(logMsg)
                                    "Startup" -> StartupManager().disableStartupApps(logMsg)
                                    "Telemetry" -> WindowsOptimizer().disableTelemetry(logMsg)
                                    "SearchIndex" -> WindowsOptimizer().disableSearchIndexing(logMsg)
                                    "SysMain" -> WindowsOptimizer().disableSysMain(logMsg)
                                    "SC_Shaders" -> CacheCleaner().cleanStarCitizenShaders(logMsg)
                                    "SC_AppData" -> CacheCleaner().openStarCitizenAppData()
                                    "SC_Config" -> GameSpecificOptimizer().generateStarCitizenConfig(logMsg)
                                    "SC_Memory" -> WindowsOptimizer().optimizePagefile(logMsg)
                                    "Backup" -> BackupManager().createBackup(logMsg)
                                    "Restore" -> BackupManager().restoreBackup(logMsg)
                                    "UndoAll" -> optimizer.undoAll(logMsg)
                                    "Repair" -> WindowsOptimizer().repairWindows(logMsg)
                                    else -> toolsLogs = toolsLogs + ("[i] Action: $action" to "")
                                }
                                toolsLogs = toolsLogs + ("[OK] Done!" to "ok")
                            }
                        }
                    )
                    
                    Page.VPN -> VpnPage()

                    Page.TROUBLESHOOT -> TroubleshootPage()

                    Page.SETTINGS -> SettingsPage(
                        onLanguageChange = { lang -> com.vextiq.core.Strings.setLanguage(lang) },
                        showFpsOverlay = showFpsOverlay,
                        onToggleFpsOverlay = onToggleFpsOverlay
                    )
                }
            }
            } // close inner Row
        } // close outer Column
    }
}

/**
 * Sidebar Component
 */
@Composable
fun Sidebar(
    score: Int,
    selectedGame: Game,
    games: List<Game>,
    onSelectGame: (Game) -> Unit,
    currentPage: Page,
    onPageChange: (Page) -> Unit,
    showFpsOverlay: Boolean,
    onToggleFpsOverlay: (Boolean) -> Unit,
    activeGame: String?
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(VextiqColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
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
            Text("PRO v2.0", fontSize = 11.sp, color = VextiqColors.TextMuted)
        }

        Spacer(Modifier.height(24.dp))

        // Score Gauge
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedGauge(value = score)
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Game Selector
        Text("TARGET GAME", fontSize = 10.sp, color = VextiqColors.TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VextiqColors.Primary),
                border = BorderStroke(1.dp, VextiqColors.Border)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameIcon(profile = selectedGame.profile, color = selectedGame.color, size = 24.dp, fontSize = 9.sp)
                        Text(selectedGame.name, fontSize = 12.sp, color = VextiqColors.TextPrimary)
                    }
                    Text("▼", fontSize = 10.sp, color = VextiqColors.TextMuted)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(VextiqColors.Card)
            ) {
                games.forEach { game ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GameIcon(profile = game.profile, color = game.color, size = 28.dp, fontSize = 10.sp)
                                Text(game.name, color = VextiqColors.TextPrimary, fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            onSelectGame(game)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Navigation
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ImageNavItem("icons/dashboard.png", "Dashboard", currentPage == Page.DASHBOARD) { onPageChange(Page.DASHBOARD) }
            ImageNavItem("icons/games.png", "Games", currentPage == Page.GAMEBOOST) { onPageChange(Page.GAMEBOOST) }
            ImageNavItem("icons/tools.png", "Tools", currentPage == Page.TOOLS) { onPageChange(Page.TOOLS) }
            ImageNavItem("icons/vpn.png", "VPN", currentPage == Page.VPN) { onPageChange(Page.VPN) }
            ImageNavItem("icons/tools.png", "Troubleshoot", currentPage == Page.TROUBLESHOOT) { onPageChange(Page.TROUBLESHOOT) }
            ImageNavItem("icons/settings.png", "Settings", currentPage == Page.SETTINGS) { onPageChange(Page.SETTINGS) }
        }
        
        Spacer(Modifier.height(20.dp))
        
        // FPS Overlay Toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (showFpsOverlay) VextiqColors.Success.copy(alpha = 0.15f) else VextiqColors.Card)
                .border(1.dp, if (showFpsOverlay) VextiqColors.Success else VextiqColors.Border, RoundedCornerShape(12.dp))
                .clickable { onToggleFpsOverlay(!showFpsOverlay) }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource("icons/fps_overlay.png"),
                    contentDescription = "FPS Overlay",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "FPS Overlay",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
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
        
        // Active Game / Online Status
        if (activeGame != null && activeGame.isNotEmpty()) {
            ActiveGameCard(gameName = activeGame)
        } else {
            OnlineIndicator()
        }
    }
}
