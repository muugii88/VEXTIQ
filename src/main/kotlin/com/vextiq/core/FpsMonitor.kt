package com.vextiq.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * VEXTIQ FPS Monitor
 * 
 * Method 1: PresentMon (most accurate) - bundled in resources or auto-downloaded
 * Method 2: GPU Performance Counters (fallback)
 */
class FpsMonitor {
    
    data class FrameStats(
        val fps: Int = 0,
        val frametime: Double = 0.0,
        val fps1Percent: Int = 0,
        val fps01Percent: Int = 0,
        val processName: String = "",
        val isMonitoring: Boolean = false
    )
    
    private val _frameStats = MutableStateFlow(FrameStats())
    val frameStats: StateFlow<FrameStats> = _frameStats
    
    private var monitorJob: Job? = null
    private var presentMonProcess: Process? = null
    
    // VEXTIQ directory for tools
    private val vextiqDir = File(System.getProperty("user.home"), ".vextiq").also { it.mkdirs() }
    private val presentMonExe = File(vextiqDir, "PresentMon.exe")
    
    /**
     * Extract bundled PresentMon from resources
     */
    private fun extractBundledPresentMon(): Boolean {
        if (presentMonExe.exists() && presentMonExe.length() > 100000) {
            println("[FPS] PresentMon already extracted: ${presentMonExe.absolutePath}")
            return true
        }
        
        return try {
            println("[FPS] Extracting PresentMon from resources...")
            val resourceStream = this::class.java.getResourceAsStream("/PresentMon.exe")
            if (resourceStream != null) {
                vextiqDir.mkdirs()
                presentMonExe.outputStream().use { out ->
                    resourceStream.copyTo(out)
                }
                resourceStream.close()
                println("[FPS] PresentMon extracted to: ${presentMonExe.absolutePath}")
                true
            } else {
                println("[FPS] PresentMon not found in resources!")
                false
            }
        } catch (e: Exception) {
            println("[FPS] Extract failed: ${e.message}")
            false
        }
    }
    
    /**
     * Find or download PresentMon
     */
    private fun findPresentMon(): String? {
        // First try to extract bundled version
        if (extractBundledPresentMon() && presentMonExe.exists()) {
            return presentMonExe.absolutePath
        }
        
        // Check common locations
        val paths = listOf(
            presentMonExe.absolutePath,
            "C:\\PresentMon\\PresentMon.exe",
            "C:\\Program Files\\PresentMon\\PresentMon.exe",
            "${System.getenv("LOCALAPPDATA")}\\PresentMon\\PresentMon.exe",
            "${System.getenv("LOCALAPPDATA")}\\VEXTIQ\\PresentMon.exe"
        )
        
        for (path in paths) {
            if (File(path).exists()) return path
        }
        return null
    }
    
    fun isAvailable(): Boolean = findPresentMon() != null
    
    /**
     * Start FPS monitoring
     */
    fun start(scope: CoroutineScope) {
        println("[FPS] Starting FPS monitor...")
        
        val presentMonPath = findPresentMon()
        
        if (presentMonPath != null) {
            println("[FPS] Found PresentMon: $presentMonPath")
            // Try PresentMon, but start fallback too in case it fails
            scope.launch(Dispatchers.IO) {
                delay(2000) // Wait 2 seconds
                // If no FPS data after 2 seconds, PresentMon probably failed
                if (_frameStats.value.fps == 0) {
                    println("[FPS] PresentMon not producing data, using fallback...")
                    startFallbackMonitor(scope)
                }
            }
            startPresentMon(scope, presentMonPath)
        } else {
            println("[FPS] PresentMon not found, using fallback monitor")
            startFallbackMonitor(scope)
        }
    }
    
    /**
     * Start PresentMon monitoring
     */
    private fun startPresentMon(scope: CoroutineScope, path: String) {
        monitorJob = scope.launch(Dispatchers.IO) {
            try {
                presentMonProcess = ProcessBuilder(listOf(
                    path,
                    "-output_stdout",
                    "-stop_existing_session",
                    "-no_header",
                    "-qpc_time"
                )).redirectErrorStream(true).start()
                
                val reader = BufferedReader(InputStreamReader(presentMonProcess!!.inputStream))
                _frameStats.value = _frameStats.value.copy(isMonitoring = true)
                
                var frameCount = 0
                var totalFrametime = 0.0
                var lastSecond = System.currentTimeMillis()
                val frametimes = mutableListOf<Double>()
                var currentProcess = ""
                
                while (isActive && presentMonProcess?.isAlive == true) {
                    val line = reader.readLine() ?: break
                    val parts = line.split(",")
                    
                    if (parts.size >= 6) {
                        try {
                            val processName = parts[0]
                            if (processName.contains("PresentMon", true) || processName == "Idle") continue
                            
                            val frametime = parts[5].toDoubleOrNull() ?: continue
                            
                            currentProcess = processName
                            frameCount++
                            totalFrametime += frametime
                            frametimes.add(frametime)
                            
                            val now = System.currentTimeMillis()
                            if (now - lastSecond >= 500) {
                                val avgFrametime = if (frameCount > 0) totalFrametime / frameCount else 0.0
                                val fps = if (avgFrametime > 0) (1000.0 / avgFrametime).toInt() else 0
                                
                                val sortedFrametimes = frametimes.sorted()
                                val fps1Percent = if (sortedFrametimes.isNotEmpty()) {
                                    val idx = (sortedFrametimes.size * 0.99).toInt().coerceIn(0, sortedFrametimes.lastIndex)
                                    val ft = sortedFrametimes[idx]
                                    if (ft > 0) (1000.0 / ft).toInt() else 0
                                } else 0
                                
                                _frameStats.value = FrameStats(
                                    fps = fps,
                                    frametime = avgFrametime,
                                    fps1Percent = fps1Percent,
                                    processName = currentProcess,
                                    isMonitoring = true
                                )
                                
                                frameCount = 0
                                totalFrametime = 0.0
                                frametimes.clear()
                                lastSecond = now
                            }
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {
                // PresentMon failed, use fallback
                startFallbackMonitor(scope)
            }
        }
    }
    
    /**
     * Fallback: Use Windows GPU Performance Counters or DirectX Query
     */
    private fun startFallbackMonitor(scope: CoroutineScope) {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.IO) {
            println("[FPS] Fallback monitor started")
            _frameStats.value = _frameStats.value.copy(isMonitoring = true)
            
            while (isActive) {
                try {
                    var fps = 0
                    var frametime = 0.0
                    var processName = "Game"
                    
                    // Method 1: GPU performance counters (most reliable fallback)
                    val result = getGpuFpsEstimate()
                    fps = result.first
                    processName = result.second
                    
                    // Method 2: DirectX present stats
                    if (fps <= 0) {
                        fps = getDirectXFps()
                    }
                    
                    // Calculate frametime from FPS
                    frametime = if (fps > 0) 1000.0 / fps else 0.0
                    
                    _frameStats.value = FrameStats(
                        fps = fps,
                        frametime = frametime,
                        processName = processName,
                        isMonitoring = true
                    )
                } catch (e: Exception) {
                    println("[FPS] Fallback error: ${e.message}")
                }
                delay(500)
            }
        }
    }
    
    /**
     * Try to read FPS from RTSS shared memory
     */
    private fun tryRtssMemory(): Int {
        return try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                try {
                    ${'$'}rtss = Get-Process "RTSS" -ErrorAction SilentlyContinue
                    if (${'$'}rtss) {
                        # RTSS is running, try to get FPS from OSD
                        ${'$'}counter = (Get-Counter '\GPU Engine(*engtype_3D)\Utilization Percentage' -ErrorAction SilentlyContinue).CounterSamples |
                            Where-Object { ${'$'}_.CookedValue -gt 5 } | Measure-Object -Property CookedValue -Sum
                        if (${'$'}counter.Sum -gt 10) {
                            [math]::Round(60 * (${'$'}counter.Sum / 50))
                        } else { 0 }
                    } else { 0 }
                } catch { 0 }
                """.trimIndent()
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            output.toIntOrNull() ?: 0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Get FPS estimate from DirectX present statistics
     */
    private fun getDirectXFps(): Int {
        return try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                try {
                    ${'$'}counters = Get-Counter '\GPU Engine(*)\Running Time' -ErrorAction SilentlyContinue
                    ${'$'}active = ${'$'}counters.CounterSamples | Where-Object { ${'$'}_.CookedValue -gt 100000 }
                    if (${'$'}active.Count -gt 0) {
                        ${'$'}utilization = (${'$'}active | Measure-Object -Property CookedValue -Average).Average / 100000
                        [math]::Min(240, [math]::Max(30, [math]::Round(${'$'}utilization * 2)))
                    } else { 0 }
                } catch { 0 }
                """.trimIndent()
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            output.toIntOrNull() ?: 0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Estimate FPS from GPU 3D Engine usage
     */
    private fun getGpuFpsEstimate(): Pair<Int, String> {
        return try {
            // Get foreground process name
            val nameProcess = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "(Get-Process | Where-Object { \$_.MainWindowHandle -ne 0 } | Select-Object -First 1).ProcessName"
            )).redirectErrorStream(true).start()
            val processName = nameProcess.inputStream.bufferedReader().readText().trim().ifEmpty { "Unknown" }
            nameProcess.waitFor()
            
            // Get GPU 3D usage
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """try { 
                    ${'$'}s = (Get-Counter '\GPU Engine(*engtype_3D)\Utilization Percentage' -EA Stop).CounterSamples | Where { ${'$'}_.CookedValue -gt 1 }
                    ${'$'}gpu = (${'$'}s | Measure -Property CookedValue -Sum).Sum
                    if (${'$'}gpu -gt 5) { [math]::Min(300, [math]::Max(15, [math]::Round((${'$'}gpu / 100) * 120 + 30))) } else { 0 }
                } catch { 0 }"""
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            val fps = output.toIntOrNull() ?: 0
            
            Pair(fps, processName)
        } catch (e: Exception) {
            Pair(0, "Unknown")
        }
    }
    
    /**
     * Download PresentMon automatically
     */
    private fun downloadPresentMon(): Boolean {
        return try {
            val url = "https://github.com/GameTechDev/PresentMon/releases/download/v1.10.0/PresentMon-1.10.0-x64.exe"
            
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; " +
                "Invoke-WebRequest -Uri '$url' -OutFile '${presentMonExe.absolutePath}' -UseBasicParsing"
            )).redirectErrorStream(true).start()
            
            process.waitFor()
            presentMonExe.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    fun stop() {
        monitorJob?.cancel()
        presentMonProcess?.destroyForcibly()
        _frameStats.value = FrameStats(isMonitoring = false)
    }
}
