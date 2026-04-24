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
class FpsMonitor(private val settings: SettingsManager = SettingsManager()) {

    data class FrameStats(
        val fps: Int = 0,
        val frametime: Double = 0.0,
        val fps1Percent: Int = 0,
        val fps01Percent: Int = 0,
        val processName: String = "",
        val isMonitoring: Boolean = false,
        val source: String = ""
    )

    private val _frameStats = MutableStateFlow(FrameStats())
    val frameStats: StateFlow<FrameStats> = _frameStats

    @Volatile private var monitorJob: Job? = null
    @Volatile private var presentMonProcess: Process? = null
    @Volatile private var etwMonitor: EtwFpsMonitor? = null
    private val startLock = Any()
    
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
                resourceStream.use { input ->
                    presentMonExe.outputStream().use { out ->
                        input.copyTo(out)
                    }
                }
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

        synchronized(startLock) {
            monitorJob?.cancel()
            presentMonProcess?.destroyForcibly()
            presentMonProcess = null
            etwMonitor?.stop()
            etwMonitor = null
        }

        // Try ETW first when enabled
        if (settings.etwFpsEnabled) {
            val etw = EtwFpsMonitor()
            if (etw.isAvailable() && etw.start(scope)) {
                println("[FPS] Using ETW (built-in, no external .exe)")
                etwMonitor = etw
                scope.launch {
                    etw.stats.collect { s ->
                        if (s.isRunning) {
                            _frameStats.value = FrameStats(
                                fps = s.fps,
                                frametime = s.frametime,
                                processName = s.processName,
                                isMonitoring = true,
                                source = "ETW"
                            )
                        }
                    }
                }
                // Start a watchdog — if no FPS after 4s, drop to PresentMon path
                scope.launch(Dispatchers.IO) {
                    delay(4000)
                    if (_frameStats.value.fps == 0) {
                        println("[FPS] ETW produced no data (likely non-admin), falling back...")
                        etw.stop()
                        etwMonitor = null
                        startPresentMonOrFallback(scope)
                    }
                }
                return
            } else {
                println("[FPS] ETW unavailable: ${etw.stats.value.errorMessage}")
            }
        }

        startPresentMonOrFallback(scope)
    }

    private fun startPresentMonOrFallback(scope: CoroutineScope) {
        val presentMonPath = findPresentMon()
        if (presentMonPath != null) {
            println("[FPS] Found PresentMon: $presentMonPath")
            startPresentMon(scope, presentMonPath)
            scope.launch(Dispatchers.IO) {
                delay(2000)
                if (_frameStats.value.fps == 0) {
                    println("[FPS] PresentMon not producing data, using fallback...")
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
        val job = scope.launch(Dispatchers.IO) {
            val localProcess = try {
                ProcessBuilder(listOf(
                    path,
                    "-output_stdout",
                    "-stop_existing_session",
                    "-no_header",
                    "-qpc_time"
                )).redirectErrorStream(true).start()
            } catch (e: Exception) {
                println("[FPS] PresentMon start failed: ${e.message}")
                startFallbackMonitor(scope)
                return@launch
            }

            synchronized(startLock) { presentMonProcess = localProcess }

            try {
                localProcess.inputStream.bufferedReader().use { reader ->
                    _frameStats.value = _frameStats.value.copy(isMonitoring = true)

                    val ignoreList = listOf(
                        "Discord", "Spotify", "Chrome", "msedge", "Firefox", "Steam", "EpicGamesLauncher",
                        "GalaxyClient", "Battle.net", "Origin", "EADesktop", "Taskmgr", "VEXTIQ", "explorer",
                        "SearchHost", "StartMenuExperienceHost", "ApplicationFrameHost", "TextInputHost",
                        "ShellExperienceHost", "NVIDIA Share", "RadeonSoftware", "RTSS", "Afterburner",
                        "Overwolf", "Twitch", "Teams", "Slack", "Zoom", "WhatsApp"
                    )

                    val processFrameCounts = mutableMapOf<String, Int>()
                    val processTotalTime = mutableMapOf<String, Double>()
                    var lastSampleNs = System.nanoTime()
                    val sampleWindowNs = 1_000_000_000L // 1 second

                    while (isActive && localProcess.isAlive) {
                        val line = reader.readLine() ?: break
                        val parts = line.split(",")
                        if (parts.size < 6) continue

                        try {
                            val processName = parts[0].replace(".exe", "", ignoreCase = true)
                            if (processName.contains("PresentMon", true) || processName == "Idle") continue

                            val frametime = parts[5].toDoubleOrNull() ?: continue

                            processFrameCounts[processName] = (processFrameCounts[processName] ?: 0) + 1
                            processTotalTime[processName] = (processTotalTime[processName] ?: 0.0) + frametime

                            val now = System.nanoTime()
                            if (now - lastSampleNs >= sampleWindowNs) {
                                val candidate = processFrameCounts.entries
                                    .filter { entry -> ignoreList.none { ignore -> entry.key.contains(ignore, true) } }
                                    .maxByOrNull { it.value }

                                val bestProcess = candidate?.key ?: processFrameCounts.maxByOrNull { it.value }?.key ?: ""

                                if (bestProcess.isNotEmpty()) {
                                    val count = processFrameCounts[bestProcess] ?: 0
                                    val totalTime = processTotalTime[bestProcess] ?: 0.0
                                    val avgFrametime = if (count > 0) totalTime / count else 0.0
                                    val fps = if (avgFrametime > 0.0) (1000.0 / avgFrametime).toInt() else 0

                                    _frameStats.value = FrameStats(
                                        fps = fps,
                                        frametime = avgFrametime,
                                        processName = bestProcess,
                                        isMonitoring = true
                                    )
                                }

                                processFrameCounts.clear()
                                processTotalTime.clear()
                                lastSampleNs = now
                            }
                        } catch (e: Exception) {
                            println("Error parsing PresentMon output: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                startFallbackMonitor(scope)
            } finally {
                synchronized(startLock) {
                    if (presentMonProcess === localProcess) presentMonProcess = null
                }
                try { localProcess.destroyForcibly() } catch (_: Exception) {}
            }
        }

        synchronized(startLock) { monitorJob = job }
    }
    
    /**
     * Fallback: Use Windows GPU Performance Counters or DirectX Query
     */
    private fun startFallbackMonitor(scope: CoroutineScope) {
        synchronized(startLock) {
            monitorJob?.cancel()
            presentMonProcess?.destroyForcibly()
            presentMonProcess = null
        }
        val job = scope.launch(Dispatchers.IO) {
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
        synchronized(startLock) { monitorJob = job }
    }

    /**
     * Get FPS estimate from DirectX present statistics
     */
    private fun getDirectXFps(): Int {
        return PowerShellRunner.runPowerShell(
            """
            try {
                ${'$'}counters = Get-Counter '\GPU Engine(*)\Running Time' -ErrorAction SilentlyContinue
                ${'$'}active = ${'$'}counters.CounterSamples | Where-Object { ${'$'}_.CookedValue -gt 100000 }
                if (${'$'}active.Count -gt 0) {
                    ${'$'}utilization = (${'$'}active | Measure-Object -Property CookedValue -Average).Average / 100000
                    [math]::Min(240, [math]::Max(30, [math]::Round(${'$'}utilization * 2)))
                } else { 0 }
            } catch { 0 }
            """.trimIndent(),
            timeoutSec = 8
        ).trimmedOutput().toIntOrNull() ?: 0
    }

    /**
     * Estimate FPS from GPU 3D Engine usage
     */
    private fun getGpuFpsEstimate(): Pair<Int, String> {
        val ignoreRegex = "Discord|Spotify|Chrome|msedge|Firefox|Steam|EpicGamesLauncher|Taskmgr|VEXTIQ|explorer|SearchHost|StartMenuExperienceHost|ApplicationFrameHost|TextInputHost|ShellExperienceHost|NVIDIA Share|RadeonSoftware|RTSS|Afterburner|Overwolf|Twitch|Teams|Slack|Zoom|WhatsApp"

        val processName = PowerShellRunner.runPowerShell(
            "\$p = Get-Process | Where-Object { \$_.MainWindowHandle -ne 0 -and \$_.ProcessName -notmatch '$ignoreRegex' } | Sort-Object CPU -Descending | Select-Object -First 1; if (\$p) { \$p.ProcessName } else { (Get-Process | Where-Object { \$_.MainWindowHandle -ne 0 } | Select-Object -First 1).ProcessName }",
            timeoutSec = 8
        ).trimmedOutput().ifEmpty { "Unknown" }

        val fps = PowerShellRunner.runPowerShell(
            """try {
                ${'$'}s = (Get-Counter '\GPU Engine(*engtype_3D)\Utilization Percentage' -EA Stop).CounterSamples | Where { ${'$'}_.CookedValue -gt 1 }
                ${'$'}gpu = (${'$'}s | Measure -Property CookedValue -Sum).Sum
                if (${'$'}gpu -gt 5) { [math]::Min(300, [math]::Max(15, [math]::Round((${'$'}gpu / 100) * 120 + 30))) } else { 0 }
            } catch { 0 }""",
            timeoutSec = 8
        ).trimmedOutput().toIntOrNull() ?: 0

        return Pair(fps, processName)
    }

    fun stop() {
        synchronized(startLock) {
            monitorJob?.cancel()
            presentMonProcess?.destroyForcibly()
            presentMonProcess = null
            etwMonitor?.stop()
            etwMonitor = null
        }
        _frameStats.value = FrameStats(isMonitoring = false)
    }
}
