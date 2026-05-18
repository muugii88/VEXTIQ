package com.vextiq.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import oshi.SystemInfo

/**
 * Real-time system stats for dashboard display
 */
data class SystemStats(
    val cpuUsage: Int = 0,
    val gpuUsage: Int = 0,
    val cpuTemp: Int = 0,
    val gpuTemp: Int = 0,
    val ramUsage: Int = 0,
    val ramUsedGB: Double = 0.0,
    val ramTotalGB: Double = 0.0,
    val fps: Int = 0,
    val frametime: Double = 0.0,
    val latency: Int = 0,
    val activeGame: String = "",
    val isGameRunning: Boolean = false
)

/**
 * Hardware info for UI display (from HardwareManager)
 */
data class HardwareInfo(
    val cpuName: String = "Detecting...",
    val cpuCores: Int = 0,
    val gpuName: String = "Detecting...",
    val gpuVendor: String = "Unknown",
    val gpuVramGB: Int = 0,
    val ramTotalGB: Double = 0.0,
    val osName: String = "Unknown"
)

/**
 * SystemMonitor - Real-time stats collection
 * 
 * Uses HardwareManager singleton for hardware info (no duplicate detection!)
 */
class SystemMonitor {
    private val systemInfo = SystemInfo()
    private val hardware = systemInfo.hardware
    private val processor = hardware.processor
    private val memory = hardware.memory
    
    private var prevTicks: LongArray = processor.systemCpuLoadTicks
    
    private val _stats = MutableStateFlow(SystemStats())
    val stats: StateFlow<SystemStats> = _stats
    
    private val _hardwareInfo = MutableStateFlow(HardwareInfo())
    val hardwareInfo: StateFlow<HardwareInfo> = _hardwareInfo
    
    private var monitorJob: Job? = null
    private var gpuVendor: String = "Unknown"
    private var lastGpuUsage: Int = 0
    private var lastGpuTemp: Int = 0
    private var lastLatency: Int = 0
    private var lastFps: Int = 0
    private var lastFrametime: Double = 0.0
    private var lastActiveGame: String = ""
    private var isGameRunning: Boolean = false
    private var gpuCheckCounter = 0
    
    // FPS Monitors — ETW preferred (native, no .exe), PresentMon fallback
    private val etwFpsMonitor = EtwFpsMonitor()
    private val fpsMonitor = FpsMonitor()
    @Volatile private var etwActive = false
    
    // NOTE: brain/AdaptiveOptimizer.kt exists but is intentionally NOT wired —
    // it mutates an in-memory config map and notifies listeners that nobody
    // subscribes to, so its "Increased shadow resolution" logs had no real
    // effect on any game. Bringing it back requires writing the produced
    // config out to each game's actual config file (CryEngine .cfg / RTSS / etc.)
    // before re-enabling the start/stop calls.

    // Adaptive Tuner - throttles background apps on stutter spikes
    private val settings = SettingsManager()
    private val adaptiveTuner = AdaptiveTuner(
        frametimeProvider = { lastFrametime },
        gameProvider = { lastActiveGame }
    )
    private var adaptiveTunerStarted = false

    // Network Monitor — game-aware latency probing
    private val networkMonitor = NetworkMonitor(settings)
    val networkStats: StateFlow<NetworkMonitor.NetStats> get() = networkMonitor.stats
    
    fun start(scope: CoroutineScope) {
        CrashLogger.log("SystemMonitor.start() begin")
        // Get hardware from singleton (ONE-TIME detection, shared with all modules)
        scope.launch(Dispatchers.IO) {
            CrashLogger.log("HardwareManager.detect() launching")
            HardwareManager.detect()
            CrashLogger.log("HardwareManager.detect() complete")
            val hw = HardwareManager.hardware.value
            gpuVendor = hw.gpuVendor
            
            _hardwareInfo.value = HardwareInfo(
                cpuName = hw.cpuName,
                cpuCores = hw.cpuCores,
                gpuName = hw.gpuName,
                gpuVendor = hw.gpuVendor,
                gpuVramGB = hw.gpuVramGB,
                ramTotalGB = hw.ramGB.toDouble(),
                osName = "Windows"
            )
        }
        
        // ETW is currently DISABLED. The EventTraceLogfile JNA mapping declares
        // CurrentEvent and LogfileHeader as Pointer? (8 bytes each), but the real
        // Windows EVENT_TRACE_LOGFILEW has them as inline EVENT_TRACE (~184 B) and
        // TRACE_LOGFILE_HEADER (~280 B) structs. As a result the EventRecordCallback
        // ends up at the wrong byte offset, and when admin lets StartTraceW succeed
        // (non-admin returns ACCESS_DENIED and we never get this far), OpenTraceW
        // reads a garbage function pointer and ProcessTrace dispatches into it →
        // EXCEPTION_ACCESS_VIOLATION on launch. Re-enable only after the struct
        // layout is properly mapped.
        // ETW (native, ~1ms, no extra process) is the primary FPS source. The
        // EventTraceLogfile struct now uses byte-array placeholders so the
        // EventRecordCallback offset matches Windows EVENT_TRACE_LOGFILEW.
        // Without that fix this used to crash on admin launch.
        CrashLogger.log("EtwFpsMonitor.start() begin")
        etwActive = try {
            etwFpsMonitor.start(scope)
        } catch (t: Throwable) {
            CrashLogger.log("[ERR] EtwFpsMonitor.start() threw: ${t.javaClass.name}: ${t.message}")
            false
        }
        CrashLogger.log("EtwFpsMonitor.start() returned active=$etwActive")
        if (etwActive) {
            println("[FPS] ETW monitor active (Microsoft-Windows-DXGI)")
            scope.launch {
                etwFpsMonitor.stats.collect { s ->
                    if (!s.isRunning) return@collect
                    lastFps = s.fps
                    lastFrametime = s.frametime
                    lastActiveGame = s.processName
                    isGameRunning = s.fps > 0 && s.processName.isNotEmpty()
                    handleGameLifecycle(scope)
                }
            }
        } else {
            println("[FPS] ETW unavailable (${etwFpsMonitor.stats.value.errorMessage}); using PresentMon fallback")
            fpsMonitor.start(scope)
        }

        // Collect FPS stats + manage AdaptiveOptimizer lifecycle
        scope.launch {
            fpsMonitor.frameStats.collect { stats ->
                if (etwActive) return@collect // ETW is authoritative
                lastFps = stats.fps
                lastFrametime = stats.frametime
                lastActiveGame = stats.processName
                isGameRunning = stats.isMonitoring && stats.fps > 0
                handleGameLifecycle(scope)
            }
        }
        
        monitorJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                updateStats()
                delay(1000)
            }
        }

        // Game-aware network latency probing
        CrashLogger.log("NetworkMonitor.start() begin")
        networkMonitor.start(scope)
        CrashLogger.log("SystemMonitor.start() complete")
    }
    
    private fun handleGameLifecycle(scope: CoroutineScope) {
        if (isGameRunning && !adaptiveTunerStarted && settings.adaptiveTuningEnabled) {
            adaptiveTuner.start(scope) { println(it) }
            adaptiveTunerStarted = true
        } else if (!isGameRunning && adaptiveTunerStarted) {
            adaptiveTuner.stop()
            adaptiveTunerStarted = false
        }

        // Keep NetworkMonitor's active game in sync with FPS detection.
        // Empty string means "no game" → NetworkMonitor will skip game-server probing.
        networkMonitor.setActiveGame(if (isGameRunning) lastActiveGame else "")
    }

    fun stop() {
        monitorJob?.cancel()
        etwFpsMonitor.stop()
        fpsMonitor.stop()
        networkMonitor.stop()
        adaptiveTuner.stop()
    }
    
    fun isFpsMonitorAvailable(): Boolean = fpsMonitor.isAvailable()
    
    private fun updateStats() {
        // CPU from OSHI
        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks)
        prevTicks = processor.systemCpuLoadTicks
        val cpuUsage = (cpuLoad * 100).toInt().coerceIn(0, 100)
        
        // RAM from OSHI
        val ramUsed = memory.total - memory.available
        val ramUsage = ((ramUsed.toDouble() / memory.total) * 100).toInt()
        val ramUsedGB = ramUsed / (1024.0 * 1024.0 * 1024.0)
        val ramTotalGB = memory.total / (1024.0 * 1024.0 * 1024.0)
        
        // GPU - check every 2 seconds to reduce overhead
        gpuCheckCounter++
        if (gpuCheckCounter % 2 == 0) {
            lastGpuUsage = getGpuUsage()
            lastGpuTemp = getGpuTemperature()
            // Latency: prefer NetworkMonitor's game-aware ping estimate when available.
            // Fall back to the legacy in-house probe only if NetworkMonitor has no data yet.
            val netStats = networkMonitor.stats.value
            lastLatency = when {
                netStats.gamePingEstimateMs > 0 -> netStats.gamePingEstimateMs
                netStats.gameServerPingMs > 0 -> netStats.gameServerPingMs
                else -> getNetworkLatency()
            }
        }
        
        _stats.value = SystemStats(
            cpuUsage = cpuUsage,
            gpuUsage = lastGpuUsage,
            cpuTemp = 0,
            gpuTemp = lastGpuTemp,
            ramUsage = ramUsage,
            ramUsedGB = ramUsedGB,
            ramTotalGB = ramTotalGB,
            fps = lastFps,
            frametime = lastFrametime,
            latency = lastLatency,
            activeGame = lastActiveGame,
            isGameRunning = isGameRunning
        )
    }
    
    private fun getGpuUsage(): Int {
        return try {
            when (gpuVendor) {
                "NVIDIA" -> getNvidiaGpuUsage()
                "AMD" -> getAmdGpuUsage()
                else -> getAmdGpuUsage()
            }
        } catch (e: Exception) { 0 }
    }
    
    private fun getNvidiaGpuUsage(): Int {
        return try {
            val process = ProcessBuilder(listOf(
                "nvidia-smi", "--query-gpu=utilization.gpu", "--format=csv,noheader,nounits"
            )).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            val exitCode = process.waitFor()
            process.destroy()
            if (exitCode == 0) output.toIntOrNull() ?: 0 else 0
        } catch (e: Exception) { 0 }
    }
    
    private fun getAmdGpuUsage(): Int {
        return try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                "try { \$sum = (Get-Counter '\\GPU Engine(*engtype_3D)\\Utilization Percentage' -ErrorAction Stop).CounterSamples | Measure-Object -Property CookedValue -Sum | Select-Object -ExpandProperty Sum; [math]::Min(100, [math]::Round(\$sum)) } catch { 0 }"
            )).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            val exitCode = process.waitFor()
            process.destroy()
            if (exitCode == 0) output.toIntOrNull()?.coerceIn(0, 100) ?: 0 else 0
        } catch (e: Exception) { 0 }
    }
    
    private fun getNetworkLatency(): Int {
        return try {
            // Priority 1: Game Server (use active game from FPS monitor)
            val activeProcess = lastActiveGame.ifEmpty { null }
            val gameLatency = getGameServerLatency(activeProcess)
            if (gameLatency > 0) return gameLatency
            
            // Priority 2: Broad search via netstat
            val broadLatency = getBroadGameLatency()
            if (broadLatency > 0) return broadLatency
            
            // Priority 3: Simple ping to reliable low-latency servers
            // This gives baseline network latency (always works, no admin needed)
            val fallbackServers = listOf("1.1.1.1", "8.8.8.8")
            for (server in fallbackServers) {
                try {
                    val process = ProcessBuilder(listOf(
                        "ping", "-n", "1", "-w", "500", server
                    )).redirectErrorStream(true).start()
                    val output = process.inputStream.bufferedReader().readText()
                    process.waitFor()
                    
                    val match = "(\\d+)\\s*(ms|мс|мил|мсек)".toRegex(RegexOption.IGNORE_CASE).find(output)
                    val latency = match?.groupValues?.get(1)?.toIntOrNull()
                    if (latency != null && latency > 0) return latency
                } catch (e: Exception) { /* skip */ }
            }
            0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Get latency from active game's network connections
     * Uses FPS monitor's detected process name for accurate detection
     * Checks BOTH TCP and UDP connections (games use UDP for real-time data!)
     */
    private fun getGameServerLatency(activeGameProcess: String?): Int {
        return try {
            // Only trust FPS-monitor-detected name if it passes the game filter.
            // Without this guard a brief misidentification (Discord, browser) would
            // get appended to the keyword regex and the next Get-Process call would
            // happily pick Discord again. The keyword fallback below uses only the
            // hardcoded whitelist, never the live process name.
            val trustedActive = activeGameProcess
                ?.takeIf { it.length > 2 && !GameDetection.isIgnored(it) }
            val gameKeywords = if (trustedActive != null) {
                "$trustedActive|StarCitizen|starcitizen|BF2042|battlefield|Cyberpunk|cs2|valorant|VALORANT|r5apex|Fortnite|Tarkov|Rust|cod|DeltaForce|deltaforce|DFBarracks|XDefiant|xdefiant|HellLetLoose|Squad|Overwatch|PUBG|TslGame|destiny2|TheFinals|EscapeFrom|ModernWarfare|Warzone|BlackOps"
            } else {
                "StarCitizen|starcitizen|BF2042|battlefield|Cyberpunk|cs2|valorant|VALORANT|r5apex|Fortnite|Tarkov|Rust|cod|DeltaForce|deltaforce|DFBarracks|XDefiant|xdefiant|HellLetLoose|Squad|Overwatch|PUBG|TslGame|destiny2|TheFinals|EscapeFrom|ModernWarfare|Warzone|BlackOps"
            }
            
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                ${'$'}gameKeywords = '$gameKeywords'
                ${'$'}gameProc = Get-Process | Where-Object { ${'$'}_.ProcessName -match ${'$'}gameKeywords } | Select-Object -First 1
                
                if (${'$'}gameProc) {
                    ${'$'}pid = ${'$'}gameProc.Id
                    ${'$'}serverIP = ${'$'}null
                    
                    # Method 1: TCP connections (lobby, matchmaking)
                    ${'$'}tcp = Get-NetTCPConnection -OwningProcess ${'$'}pid -State Established -ErrorAction SilentlyContinue |
                        Where-Object { 
                            ${'$'}_.RemoteAddress -notmatch '^(127\.|0\.|10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[01])\.|::1|fe80|255\.)' -and
                            ${'$'}_.RemoteAddress -match '^\d+\.\d+\.\d+\.\d+${'$'}'
                        }
                    if (${'$'}tcp) {
                        ${'$'}gameConn = ${'$'}tcp | Where-Object { ${'$'}_.RemotePort -notin @(80, 443, 8080) } | Select-Object -First 1
                        if (-not ${'$'}gameConn) { ${'$'}gameConn = ${'$'}tcp | Select-Object -First 1 }
                        ${'$'}serverIP = ${'$'}gameConn.RemoteAddress
                    }
                    
                    # Method 2: UDP via netstat (game servers use UDP for real-time!)
                    if (-not ${'$'}serverIP) {
                        ${'$'}udpLines = netstat -ano -p UDP 2>${'$'}null | Where-Object { ${'$'}_ -match "${'$'}pid${'$'}" }
                        foreach (${'$'}line in ${'$'}udpLines) {
                            if (${'$'}line -match '(\d+\.\d+\.\d+\.\d+):(\d+)\s') {
                                ${'$'}ip = ${'$'}Matches[1]
                                if (${'$'}ip -notmatch '^(127\.|0\.|10\.|192\.168\.|172\.|255\.|0\.0\.0\.0|\*|::)') {
                                    ${'$'}serverIP = ${'$'}ip
                                    break
                                }
                            }
                        }
                    }
                    
                    # Method 3: ALL netstat connections for this PID (TCP + UDP)
                    if (-not ${'$'}serverIP) {
                        ${'$'}allLines = netstat -ano 2>${'$'}null | Where-Object { ${'$'}_ -match "${'$'}pid${'$'}" -and ${'$'}_ -match 'ESTABLISHED|UDP' }
                        foreach (${'$'}line in ${'$'}allLines) {
                            if (${'$'}line -match '\s+(\d+\.\d+\.\d+\.\d+):(\d+)\s') {
                                ${'$'}ip = ${'$'}Matches[1]
                                ${'$'}port = [int]${'$'}Matches[2]
                                if (${'$'}ip -notmatch '^(127\.|0\.|10\.|192\.168\.|172\.|255\.|0\.0\.0\.0)' -and ${'$'}port -notin @(80,443)) {
                                    ${'$'}serverIP = ${'$'}ip
                                    break
                                }
                            }
                        }
                    }
                    
                    # Ping the found server IP
                    if (${'$'}serverIP) {
                        ${'$'}result = Test-Connection -ComputerName ${'$'}serverIP -Count 1 -ErrorAction SilentlyContinue
                        if (${'$'}result) {
                            ${'$'}val = ${'$'}result.Latency
                            if (-not ${'$'}val) { ${'$'}val = ${'$'}result.ResponseTime }
                            if (${'$'}val) { [math]::Round(${'$'}val) } else { 0 }
                        }
                    }
                }
                """.trimIndent()
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            output.toIntOrNull() ?: 0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Broad search: Find ANY non-system process with game-like connections
     * Checks both TCP AND UDP via netstat
     */
    private fun getBroadGameLatency(): Int {
        return try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                ${'$'}ignore = 'chrome|msedge|firefox|Discord|Spotify|Steam|EpicGames|Teams|Slack|Zoom|svchost|lsass|csrss|wininit|services|SearchHost|OneDrive|Dropbox|Code|idea|python|node|VEXTIQ|explorer|Taskmgr|RuntimeBroker|dwm|conhost|RTSS|Afterburner|audiodg|WmiPrvSE|dllhost|sihost|fontdrvhost'
                
                # Use netstat for both TCP+UDP
                ${'$'}lines = netstat -ano 2>${'$'}null | Where-Object { ${'$'}_ -match 'ESTABLISHED|UDP' }
                ${'$'}checked = @{}
                
                foreach (${'$'}line in ${'$'}lines) {
                    if (${'$'}line -match '\s+(\d+\.\d+\.\d+\.\d+):(\d+)\s+.*\s+(\d+)${'$'}') {
                        ${'$'}ip = ${'$'}Matches[1]
                        ${'$'}port = [int]${'$'}Matches[2]
                        ${'$'}pid = [int]${'$'}Matches[3]
                        
                        if (${'$'}ip -match '^(127\.|0\.|10\.|192\.168\.|172\.|255\.|0\.0\.0\.0)') { continue }
                        if (${'$'}port -in @(80, 443, 8080, 8443)) { continue }
                        if (${'$'}checked.ContainsKey(${'$'}pid)) { continue }
                        ${'$'}checked[${'$'}pid] = ${'$'}true
                        
                        ${'$'}proc = Get-Process -Id ${'$'}pid -ErrorAction SilentlyContinue
                        if (${'$'}proc -and ${'$'}proc.ProcessName -notmatch ${'$'}ignore) {
                            ${'$'}result = Test-Connection -ComputerName ${'$'}ip -Count 1 -ErrorAction SilentlyContinue
                            if (${'$'}result) {
                                ${'$'}val = ${'$'}result.Latency
                                if (-not ${'$'}val) { ${'$'}val = ${'$'}result.ResponseTime }
                                if (${'$'}val -and ${'$'}val -gt 0) { [math]::Round(${'$'}val); exit }
                            }
                        }
                    }
                }
                0
                """.trimIndent()
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            output.toIntOrNull() ?: 0
        } catch (e: Exception) { 0 }
    }
    
    private fun getGpuTemperature(): Int {
        return try {
            when (gpuVendor) {
                "NVIDIA" -> {
                    val process = ProcessBuilder(listOf(
                        "nvidia-smi", "--query-gpu=temperature.gpu", "--format=csv,noheader,nounits"
                    )).redirectErrorStream(true).start()
                    val output = process.inputStream.bufferedReader().readText().trim()
                    if (process.waitFor() == 0) output.toIntOrNull() ?: 0 else 0
                }
                "AMD" -> {
                    val process = ProcessBuilder(listOf(
                        "powershell", "-NoProfile", "-NonInteractive", "-Command",
                        """
                        try {
                            ${'$'}temp = (Get-Counter '\GPU Adapter Memory(*)\Temperature' -ErrorAction Stop).CounterSamples | 
                                Where-Object { ${'$'}_.InstanceName -notlike '*Microsoft*' } |
                                Select-Object -First 1 -ExpandProperty CookedValue
                            [math]::Round(${'$'}temp)
                        } catch { 0 }
                        """.trimIndent()
                    )).redirectErrorStream(true).start()
                    val output = process.inputStream.bufferedReader().readText().trim()
                    process.waitFor()
                    output.toIntOrNull()?.coerceIn(0, 120) ?: 0
                }
                else -> 0
            }
        } catch (e: Exception) { 0 }
    }
}
