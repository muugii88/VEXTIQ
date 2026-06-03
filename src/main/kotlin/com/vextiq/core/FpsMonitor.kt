package com.vextiq.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * VEXTIQ FPS Monitor
 * 
 * Method 1: PresentMon (most accurate) - bundled in resources, extracted on first use
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
    @Volatile private var presentMonProcess: Process? = null
    
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
            startPresentMon(scope, presentMonPath)
            // PresentMon legitimately emits nothing until a game actually presents
            // frames, so fps==0 while idle is NORMAL — not a failure. The old check
            // (`fps == 0`) tripped 2s after launch whenever no game was running yet,
            // permanently cancelling the accurate PresentMon job and locking the UI
            // onto the rough GPU-utilisation estimate. Only fall back if the
            // PresentMon process itself failed to start or died early (no admin,
            // blocked by AV, bad binary).
            scope.launch(Dispatchers.IO) {
                delay(2000)
                if (presentMonProcess?.isAlive != true) {
                    println("[FPS] PresentMon process not alive, using fallback...")
                    startFallbackMonitor(scope)
                }
            }
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
                
                presentMonProcess!!.inputStream.bufferedReader().use { reader ->
                    _frameStats.value = _frameStats.value.copy(isMonitoring = true)

                    val processFrameCounts = mutableMapOf<String, Int>()
                    val processTotalTime = mutableMapOf<String, Double>()
                    var lastSecond = System.currentTimeMillis()
                    
                    while (isActive && presentMonProcess?.isAlive == true) {
                        val line = reader.readLine() ?: break
                        val parts = line.split(",")
                        
                        if (parts.size >= 6) {
                            try {
                                val processName = parts[0].replace(".exe", "", ignoreCase = true)
                                if (processName.contains("PresentMon", true) || processName == "Idle") continue
                                
                                val frametime = parts[5].toDoubleOrNull() ?: continue
                                
                                // Track frames for this process
                                processFrameCounts[processName] = (processFrameCounts[processName] ?: 0) + 1
                                processTotalTime[processName] = (processTotalTime[processName] ?: 0.0) + frametime
                                
                                val now = System.currentTimeMillis()
                                if (now - lastSecond >= 800) {
                                    val bestProcess = GameDetection.pickActiveGame(processFrameCounts)

                                    if (bestProcess.isNotEmpty()) {
                                        val count = processFrameCounts[bestProcess] ?: 0
                                        val totalTime = processTotalTime[bestProcess] ?: 0.0
                                        val avgFrametime = if (count > 0) totalTime / count else 0.0
                                        val fps = if (avgFrametime > 0) (1000.0 / avgFrametime).toInt() else 0
                                        
                                        _frameStats.value = FrameStats(
                                            fps = fps,
                                            frametime = avgFrametime,
                                            processName = bestProcess,
                                            isMonitoring = true
                                        )
                                    }
                                    
                                    processFrameCounts.clear()
                                    processTotalTime.clear()
                                    lastSecond = now
                                }
                            } catch (e: Exception) {
                                println("Error parsing PresentMon output: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // PresentMon failed, use fallback
                startFallbackMonitor(scope)
            } finally {
                presentMonProcess?.destroy()
                presentMonProcess = null
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
            
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            output.toIntOrNull() ?: 0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Estimate FPS from GPU 3D Engine usage
     */
    private fun getGpuFpsEstimate(): Pair<Int, String> {
        return try {
            // Get foreground process name - smarter detection
            val ignoreRegex = "Discord|Spotify|Chrome|msedge|Firefox|Steam|EpicGamesLauncher|Taskmgr|VEXTIQ|explorer|SearchHost|StartMenuExperienceHost|ApplicationFrameHost|TextInputHost|ShellExperienceHost|NVIDIA Share|RadeonSoftware|RTSS|Afterburner|Overwolf|Twitch|Teams|Slack|Zoom|WhatsApp"
            val nameProcess = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "\$p = Get-Process | Where-Object { \$_.MainWindowHandle -ne 0 -and \$_.ProcessName -notmatch '$ignoreRegex' } | Sort-Object CPU -Descending | Select-Object -First 1; if (\$p) { \$p.ProcessName } else { (Get-Process | Where-Object { \$_.MainWindowHandle -ne 0 } | Select-Object -First 1).ProcessName }"
            )).redirectErrorStream(true).start()
            val processName = nameProcess.inputStream.bufferedReader().use { it.readText() }.trim().ifEmpty { "Unknown" }
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
            
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            val fps = output.toIntOrNull() ?: 0
            
            Pair(fps, processName)
        } catch (e: Exception) {
            Pair(0, "Unknown")
        }
    }
    
    fun stop() {
        monitorJob?.cancel()
        presentMonProcess?.destroyForcibly()
        _frameStats.value = FrameStats(isMonitoring = false)
    }
}
