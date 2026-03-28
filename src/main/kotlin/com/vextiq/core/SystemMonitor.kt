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
        
        // Collect FPS stats
        scope.launch {
            fpsMonitor.frameStats.collect { stats ->
                lastFps = stats.fps
                lastFrametime = stats.frametime
                lastActiveGame = stats.processName
                isGameRunning = stats.isMonitoring && stats.fps > 0
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
            // Priority 1: Game Server (if detected)
            val gameLatency = getGameServerLatency()
            if (gameLatency > 10) return gameLatency
            
            // Priority 2: Regional Master Servers
            val coreServers = listOf(
                "8.8.8.8",                  // Google (Global Baseline)
                "1.1.1.1",                  // Cloudflare
                "ping-iad.ds.ea.com",       // EA East
                "ping-lhr.ds.ea.com",       // EA Europe
                "public.cloudimperiumgames.com" // Star Citizen
            )
            
            var bestLatency = 999
            for (server in coreServers) {
                try {
                    val process = ProcessBuilder(listOf(
                        "ping", "-n", "1", "-w", "500", server
                    )).redirectErrorStream(true).start()
                    val output = process.inputStream.bufferedReader().readText()
                    process.waitFor()
                    
                    // Improved Regex: Handle various languages and formats
                    // Common: "time=56ms", "time<1ms", "хугацаа=56мс", "время=56мс"
                    val match = "(\\d+)\\s*(ms|мс)".toRegex(RegexOption.IGNORE_CASE).find(output)
                    val latency = match?.groupValues?.get(1)?.toIntOrNull()
                    
                    if (latency != null && latency > 0 && latency < bestLatency) {
                        bestLatency = latency
                        if (bestLatency < 100) break // Good enough
                    }
                } catch (e: Exception) {
                    println("Error pinging $server: ${e.message}")
                }
            }
            
            if (bestLatency < 999) bestLatency else 0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Get latency from active game's network connections
     */
    private fun getGameServerLatency(): Int {
        return try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                # 1. Broad game process list
                ${'$'}gameKeywords = 'StarCitizen|starcitizen|BF2042|battlefield|Cyberpunk|cs2|valorant|VALORANT|r5apex|Fortnite|Tarkov|Rust|cod'
                ${'$'}gameProc = Get-Process | Where-Object { ${'$'}_.ProcessName -match ${'$'}gameKeywords } | Select-Object -First 1
                
                if (${'$'}gameProc) {
                    # 2. Look for established connections
                    ${'$'}conns = Get-NetTCPConnection -OwningProcess ${'$'}gameProc.Id -State Established -ErrorAction SilentlyContinue | 
                        Where-Object { 
                            ${'$'}_.RemoteAddress -notmatch '^(127\.|0\.|10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[01])\.|::1|fe80|255\.)' -and
                            ${'$'}_.RemoteAddress -match '^\d+\.\d+\.\d+\.\d+$'
                        }
                    
                    if (${'$'}conns) {
                        ${'$'}serverIP = (${'$'}conns | Select-Object -First 1).RemoteAddress
                        ${'$'}result = Test-Connection -ComputerName ${'$'}serverIP -Count 1 -ErrorAction SilentlyContinue
                        if (${'$'}result) { 
                            # Safe Latency/ResponseTime check (PS 5.1 vs 7)
                            ${'$'}val = ${'$'}result.Latency
                            if (-not ${'$'}val) { ${'$'}val = ${'$'}result.ResponseTime }
                            [math]::Round(${'$'}val)
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
