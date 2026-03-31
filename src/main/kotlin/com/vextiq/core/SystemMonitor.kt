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
    
    // FPS Monitor
    private val fpsMonitor = FpsMonitor()
    
    // Adaptive Optimizer - auto-tunes quality based on real-time FPS
    private val adaptiveOptimizer = com.vextiq.brain.AdaptiveOptimizer(
        fpsProvider = { lastFps }
    )
    private var adaptiveStarted = false
    
    fun start(scope: CoroutineScope) {
        // Get hardware from singleton (ONE-TIME detection, shared with all modules)
        scope.launch(Dispatchers.IO) {
            HardwareManager.detect()
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
        
        // Start FPS monitoring
        fpsMonitor.start(scope)
        
        // Collect FPS stats + manage AdaptiveOptimizer lifecycle
        scope.launch {
            fpsMonitor.frameStats.collect { stats ->
                lastFps = stats.fps
                lastFrametime = stats.frametime
                lastActiveGame = stats.processName
                isGameRunning = stats.isMonitoring && stats.fps > 0
                
                // Auto-start/stop AdaptiveOptimizer based on game state
                if (isGameRunning && !adaptiveStarted) {
                    println("[ADAPTIVE] Game detected: $lastActiveGame - Starting adaptive optimizer")
                    adaptiveOptimizer.startMonitoring(scope, targetFps = 60)
                    adaptiveStarted = true
                } else if (!isGameRunning && adaptiveStarted) {
                    println("[ADAPTIVE] Game stopped - Stopping adaptive optimizer")
                    adaptiveOptimizer.stopMonitoring()
                    adaptiveStarted = false
                }
            }
        }
        
        monitorJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                updateStats()
                delay(1000)
            }
        }
    }
    
    fun stop() {
        monitorJob?.cancel()
        fpsMonitor.stop()
        adaptiveOptimizer.stopMonitoring()
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
            lastLatency = getNetworkLatency()
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
            // Build process search: active game from FPS monitor + known games
            var gameKeywords = "StarCitizen|starcitizen|BF2042|battlefield|Cyberpunk|cs2|valorant|VALORANT|r5apex|Fortnite|Tarkov|Rust|cod|DeltaForce|deltaforce|DFBarracks|XDefiant|xdefiant|HellLetLoose|Squad|Overwatch|PUBG|TslGame|destiny2|TheFinals|EscapeFrom|ModernWarfare|Warzone|BlackOps"
            
            if (!activeGameProcess.isNullOrEmpty() && activeGameProcess.length > 2) {
                gameKeywords = "$activeGameProcess|$gameKeywords"
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
