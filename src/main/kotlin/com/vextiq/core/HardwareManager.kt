package com.vextiq.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import oshi.SystemInfo

/**
 * HardwareManager - SINGLETON
 * FULL DETECTION - бүх арга ашиглана
 */
object HardwareManager {
    
    data class HardwareProfile(
        val cpuName: String = "Unknown",
        val cpuCores: Int = 8,
        val cpuThreads: Int = 16,
        val cpuFreqMHz: Int = 3600,
        val gpuName: String = "Unknown",
        val gpuVendor: String = "Unknown",
        val gpuVramMB: Int = 8192,
        val gpuVramGB: Int = 8,
        val ramGB: Int = 16,
        val ramSpeedMHz: Int = 3200,
        val resWidth: Int = 1920,
        val resHeight: Int = 1080,
        val refreshRate: Int = 60,
        val gpuTier: String = "medium",
        val hwScore: Int = 50,
        val systemDiskType: String = "Unknown", // SSD, HDD
        val systemDiskIsNVMe: Boolean = false
    )
    
    private val _hardware = MutableStateFlow(HardwareProfile())
    val hardware: StateFlow<HardwareProfile> = _hardware
    
    private var isDetected = false
    private val systemInfo by lazy { SystemInfo() }
    
    fun getHardware(): HardwareProfile {
        return _hardware.value
    }
    
    /**
     * Force rescan - Dashboard SCAN товчоор дуудагдана
     */
    suspend fun forceRescan(onLog: (String) -> Unit = {}) {
        isDetected = false
        onLog("[>>] Starting FULL Hardware Scan...")
        onLog("")
        
        val profile = withContext(Dispatchers.IO) {
            detectAllFull(onLog)
        }
        
        _hardware.value = profile
        isDetected = true
    }
    
    suspend fun detect(onLog: (String) -> Unit = {}) {
        if (isDetected) {
            return
        }
        
        val profile = withContext(Dispatchers.IO) {
            detectAllFull(onLog)
        }
        
        _hardware.value = profile
        isDetected = true
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FULL DETECTION - ALL METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun detectAllFull(onLog: (String) -> Unit): HardwareProfile {
        var cpuName = "Unknown"
        var cpuCores = 8
        var cpuThreads = 16
        var cpuFreq = 3600
        var gpuName = "Unknown"
        var gpuVendor = "Unknown"
        var gpuVramMB: Int
        var ramGB = 16
        var ramSpeed = 3200
        var resWidth = 1920
        var resHeight = 1080
        var refreshRate = 60
        var systemDiskType = "Unknown"
        var systemDiskIsNVMe = false

        
        // ═══════════════════════════════════════════════════════════════════════
        // CPU DETECTION
        // ═══════════════════════════════════════════════════════════════════════
        onLog("[>>] Detecting CPU...")
        try {
            var process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "(Get-CimInstance Win32_Processor).Name"
            )).redirectErrorStream(true).start()
            cpuName = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            if (cpuName.isNotEmpty() && cpuName != "null") {
                onLog("[OK] CPU: $cpuName")
            }
            
            // Cores
            process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "(Get-CimInstance Win32_Processor).NumberOfCores"
            )).redirectErrorStream(true).start()
            cpuCores = process.inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull() ?: 8
            process.waitFor()
            
            // Threads
            process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "(Get-CimInstance Win32_Processor).NumberOfLogicalProcessors"
            )).redirectErrorStream(true).start()
            cpuThreads = process.inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull() ?: 16
            process.waitFor()
            onLog("[OK] Cores: $cpuCores | Threads: $cpuThreads")
            
            // Frequency - Max of current and max
            process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                ${'$'}cpu = Get-CimInstance Win32_Processor
                ${'$'}current = ${'$'}cpu.CurrentClockSpeed
                ${'$'}max = ${'$'}cpu.MaxClockSpeed
                if (${'$'}current -gt ${'$'}max) { ${'$'}current } else { ${'$'}max }
                """.trimIndent()
            )).redirectErrorStream(true).start()
            cpuFreq = process.inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull() ?: 3600
            process.waitFor()
            onLog("[OK] CPU Frequency: ${cpuFreq}MHz")
            
        } catch (e: Exception) {
            onLog("[!] CPU detection error: ${e.message}")
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // GPU DETECTION
        // ═══════════════════════════════════════════════════════════════════════
        onLog("")
        onLog("[>>] Detecting GPU...")
        try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "Get-CimInstance Win32_VideoController | Where-Object { \$_.Name -notlike '*Microsoft*' -and \$_.Name -notlike '*Basic*' } | Select-Object -First 1 -ExpandProperty Name"
            )).redirectErrorStream(true).start()
            gpuName = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            
            if (gpuName.isNotEmpty() && gpuName != "null") {
                onLog("[OK] GPU: $gpuName")
                
                // Determine vendor
                gpuVendor = when {
                    gpuName.contains("NVIDIA", true) || gpuName.contains("GeForce", true) || 
                    gpuName.contains("RTX", true) || gpuName.contains("GTX", true) -> "NVIDIA"
                    gpuName.contains("AMD", true) || gpuName.contains("Radeon", true) || 
                    gpuName.contains("RX", true) -> "AMD"
                    gpuName.contains("Intel", true) || gpuName.contains("UHD", true) ||
                    gpuName.contains("Iris", true) || gpuName.contains("Arc", true) -> "Intel"
                    else -> "Unknown"
                }
                onLog("[OK] GPU Vendor: $gpuVendor")
            }
        } catch (e: Exception) {
            onLog("[!] GPU detection error: ${e.message}")
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // VRAM DETECTION - MULTI-METHOD (CRITICAL!)
        // ═══════════════════════════════════════════════════════════════════════
        onLog("")
        onLog("[>>] Detecting VRAM (multi-method)...")
        gpuVramMB = detectVramFull(gpuName, gpuVendor, onLog)
        onLog("[OK] VRAM: ${gpuVramMB}MB (${gpuVramMB / 1024}GB)")
        
        // ═══════════════════════════════════════════════════════════════════════
        // RAM DETECTION
        // ═══════════════════════════════════════════════════════════════════════
        onLog("")
        onLog("[>>] Detecting RAM...")
        try {
            var process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "[math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB)"
            )).redirectErrorStream(true).start()
            ramGB = process.inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull() ?: 16
            process.waitFor()
            onLog("[OK] RAM: ${ramGB}GB")
            
            // RAM Speed
            process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "(Get-CimInstance Win32_PhysicalMemory | Select-Object -First 1).Speed"
            )).redirectErrorStream(true).start()
            ramSpeed = process.inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull() ?: 3200
            process.waitFor()
            onLog("[OK] RAM Speed: ${ramSpeed}MHz")
            
        } catch (e: Exception) {
            onLog("[!] RAM detection error: ${e.message}")
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // DISPLAY DETECTION - WMI (tested and works!)
        // ═══════════════════════════════════════════════════════════════════════
        onLog("")
        onLog("[>>] Detecting Display...")
        try {
            // Separate simple queries - most reliable
            val widthProcess = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "(Get-CimInstance Win32_VideoController | Where-Object { \$_.Name -notlike '*Microsoft*' } | Select-Object -First 1).CurrentHorizontalResolution"
            )).redirectErrorStream(true).start()
            resWidth = widthProcess.inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull() ?: 1920
            widthProcess.waitFor()
            
            val heightProcess = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "(Get-CimInstance Win32_VideoController | Where-Object { \$_.Name -notlike '*Microsoft*' } | Select-Object -First 1).CurrentVerticalResolution"
            )).redirectErrorStream(true).start()
            resHeight = heightProcess.inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull() ?: 1080
            heightProcess.waitFor()
            
            val hzProcess = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "(Get-CimInstance Win32_VideoController | Where-Object { \$_.Name -notlike '*Microsoft*' } | Select-Object -First 1).CurrentRefreshRate"
            )).redirectErrorStream(true).start()
            refreshRate = hzProcess.inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull() ?: 60
            hzProcess.waitFor()
            
            onLog("[OK] Resolution: ${resWidth}x${resHeight}")
            onLog("[OK] Refresh Rate: ${refreshRate}Hz")
            
        } catch (e: Exception) {
            onLog("[!] Display detection error: ${e.message}")
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // DISK DETECTION
        // ═══════════════════════════════════════════════════════════════════════
        onLog("")
        onLog("[>>] Detecting System Disk...")
        try {
            val diskProcess = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                ${'$'}drive = Get-CimInstance Win32_LogicalDisk -Filter "DeviceID='C:'" | Get-CimAssociatedInstance -ResultClassName Win32_DiskPartition | Get-CimAssociatedInstance -ResultClassName Win32_DiskDrive
                ${'$'}disk = Get-PhysicalDisk | Where-Object { ${'$'}_.DeviceNumber -eq ${'$'}drive.Index }
                ${'$'}type = ${'$'}disk.MediaType
                ${'$'}isNVMe = ${'$'}disk.BusType -eq 'NVMe'
                "${'$'}type|${'$'}isNVMe"
                """.trimIndent()
            )).redirectErrorStream(true).start()
            
            val output = diskProcess.inputStream.bufferedReader().use { it.readText() }.trim()
            diskProcess.waitFor()
            
            if (output.contains("|")) {
                val parts = output.split("|")
                systemDiskType = parts[0]
                systemDiskIsNVMe = parts[1].toBoolean()
                onLog("[OK] System Drive: $systemDiskType ${if (systemDiskIsNVMe) "(NVMe)" else ""}")
            }
        } catch (e: Exception) {
            onLog("[!] Disk detection error: ${e.message}")
        }
        
        // Calculate tier and score

        val gpuTier = calculateGpuTier(gpuVramMB / 1024, gpuName)
        val hwScore = calculateHwScore(cpuCores, cpuThreads, cpuFreq, gpuVramMB / 1024, ramGB)
        
        onLog("")
        onLog("[OK] GPU Tier: ${gpuTier.uppercase()}")
        onLog("[OK] Hardware Score: $hwScore/100")
        
        return HardwareProfile(
            cpuName = cpuName,
            cpuCores = cpuCores,
            cpuThreads = cpuThreads,
            cpuFreqMHz = cpuFreq,
            gpuName = gpuName,
            gpuVendor = gpuVendor,
            gpuVramMB = gpuVramMB,
            gpuVramGB = gpuVramMB / 1024,
            ramGB = ramGB,
            ramSpeedMHz = ramSpeed,
            resWidth = resWidth,
            resHeight = resHeight,
            refreshRate = refreshRate,
            gpuTier = gpuTier,
            hwScore = hwScore,
            systemDiskType = systemDiskType,
            systemDiskIsNVMe = systemDiskIsNVMe
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VRAM DETECTION - ALL METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun detectVramFull(gpuName: String, gpuVendor: String, onLog: (String) -> Unit): Int {
        
        // METHOD 1: NVIDIA - nvidia-smi (most accurate for NVIDIA)
        if (gpuVendor == "NVIDIA") {
            onLog("  [1] Trying nvidia-smi...")
            try {
                val p = ProcessBuilder("nvidia-smi", "--query-gpu=memory.total", "--format=csv,noheader,nounits")
                    .redirectErrorStream(true).start()
                val vram = p.inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull()
                p.waitFor()
                if (vram != null && vram > 1000) {
                    onLog("  [OK] nvidia-smi: ${vram}MB")
                    return vram
                }
            } catch (_: Exception) {
                onLog("  [!] nvidia-smi not available")
            }
        }
        
        // METHOD 2: Registry qwMemorySize (64-bit, works for AMD >4GB)
        onLog("  [2] Trying Registry qwMemorySize...")
        try {
            val p = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                ${'$'}paths = @(
                    'HKLM:\SYSTEM\CurrentControlSet\Control\Class\{4d36e968-e325-11ce-bfc1-08002be10318}\0000',
                    'HKLM:\SYSTEM\CurrentControlSet\Control\Class\{4d36e968-e325-11ce-bfc1-08002be10318}\0001',
                    'HKLM:\SYSTEM\ControlSet001\Control\Class\{4d36e968-e325-11ce-bfc1-08002be10318}\0000'
                )
                foreach (${'$'}path in ${'$'}paths) {
                    try {
                        ${'$'}reg = Get-ItemProperty -Path ${'$'}path -ErrorAction Stop
                        if (${'$'}reg.DriverDesc -notlike '*Microsoft*' -and ${'$'}reg.DriverDesc -notlike '*Basic*') {
                            ${'$'}qw = ${'$'}reg.'HardwareInformation.qwMemorySize'
                            if (${'$'}qw -and ${'$'}qw -gt 0) {
                                [math]::Round(${'$'}qw / 1MB)
                                exit
                            }
                        }
                    } catch {
                        # Registry key not found or access denied
                    }
                }
                0
                """.trimIndent()
            )).redirectErrorStream(true).start()
            val vram = p.inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull()
            p.waitFor()
            if (vram != null && vram > 1000) {
                onLog("  [OK] Registry qwMemorySize: ${vram}MB")
                return vram
            } else {
                onLog("  [!] Registry method returned: $vram")
            }
        } catch (e: Exception) {
            onLog("  [!] Registry error: ${e.message}")
        }
        
        // METHOD 3: WMI AdapterRAM (may overflow for >4GB)
        onLog("  [3] Trying WMI AdapterRAM...")
        try {
            val p = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                ${'$'}gpu = Get-CimInstance Win32_VideoController | Where-Object { ${'$'}_.Name -notlike '*Microsoft*' } | Select-Object -First 1
                if (${'$'}gpu.AdapterRAM) {
                    [math]::Round(${'$'}gpu.AdapterRAM / 1MB)
                } else { 0 }
                """.trimIndent()
            )).redirectErrorStream(true).start()
            val vram = p.inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull()
            p.waitFor()
            if (vram != null && vram > 1000) {
                onLog("  [OK] WMI AdapterRAM: ${vram}MB")
                // Check for overflow (4GB max in 32-bit)
                if (vram < 4096) {
                    onLog("  [!] WMI value may be overflow, checking GPU name...")
                } else {
                    return vram
                }
            }
        } catch (e: Exception) {
            onLog("  [!] WMI error: ${e.message}")
        }
        
        // METHOD 4: GPU Name Lookup (fallback)
        onLog("  [4] Using GPU name lookup...")
        val vramFromName = getVramFromGpuName(gpuName)
        if (vramFromName > 0) {
            onLog("  [OK] GPU name lookup: ${vramFromName}MB")
            return vramFromName
        }
        
        onLog("  [!] All methods failed, using default 8GB")
        return 8192
    }
    
    private fun getVramFromGpuName(name: String): Int {
        val n = name.uppercase()
        return when {
            // AMD RX 7000
            n.contains("7900 XTX") -> 24576
            n.contains("7900 XT") -> 20480
            n.contains("7900 GRE") -> 16384
            n.contains("7800 XT") -> 16384
            n.contains("7700 XT") -> 12288
            n.contains("7600") -> 8192
            // AMD RX 6000
            n.contains("6950 XT") -> 16384
            n.contains("6900 XT") -> 16384
            n.contains("6900") -> 16384
            n.contains("6800 XT") -> 16384
            n.contains("6800") -> 16384
            n.contains("6750 XT") -> 12288
            n.contains("6700 XT") -> 12288
            n.contains("6700") -> 10240
            n.contains("6650 XT") -> 8192
            n.contains("6600 XT") -> 8192
            n.contains("6600") -> 8192
            n.contains("6500 XT") -> 4096
            // NVIDIA RTX 50
            n.contains("5090") -> 32768
            n.contains("5080") -> 16384
            n.contains("5070 TI") -> 16384
            n.contains("5070") -> 12288
            // NVIDIA RTX 40
            n.contains("4090") -> 24576
            n.contains("4080 SUPER") -> 16384
            n.contains("4080") -> 16384
            n.contains("4070 TI SUPER") -> 16384
            n.contains("4070 TI") -> 12288
            n.contains("4070 SUPER") -> 12288
            n.contains("4070") -> 12288
            n.contains("4060 TI") -> 8192
            n.contains("4060") -> 8192
            // NVIDIA RTX 30
            n.contains("3090 TI") -> 24576
            n.contains("3090") -> 24576
            n.contains("3080 TI") -> 12288
            n.contains("3080") -> 10240
            n.contains("3070 TI") -> 8192
            n.contains("3070") -> 8192
            n.contains("3060 TI") -> 8192
            n.contains("3060") -> 12288
            n.contains("3050") -> 8192
            // NVIDIA RTX 20
            n.contains("2080 TI") -> 11264
            n.contains("2080 SUPER") -> 8192
            n.contains("2080") -> 8192
            n.contains("2070 SUPER") -> 8192
            n.contains("2070") -> 8192
            n.contains("2060 SUPER") -> 8192
            n.contains("2060") -> 6144
            // NVIDIA GTX 16
            n.contains("1660 TI") -> 6144
            n.contains("1660 SUPER") -> 6144
            n.contains("1660") -> 6144
            n.contains("1650 SUPER") -> 4096
            n.contains("1650") -> 4096
            // NVIDIA GTX 10
            n.contains("1080 TI") -> 11264
            n.contains("1080") -> 8192
            n.contains("1070 TI") -> 8192
            n.contains("1070") -> 8192
            n.contains("1060") -> 6144
            n.contains("1050 TI") -> 4096
            n.contains("1050") -> 2048
            // Intel Arc
            n.contains("A770") -> 16384
            n.contains("A750") -> 8192
            n.contains("A380") -> 6144
            else -> 0
        }
    }
    
    private fun calculateGpuTier(vramGB: Int, gpuName: String): String {
        val n = gpuName.uppercase()
        
        // By model first
        return when {
            n.contains("4090") || n.contains("4080") || n.contains("3090") ||
            n.contains("7900 XTX") || n.contains("7900 XT") || n.contains("5090") || n.contains("5080") -> "ultra"
            
            n.contains("4070") || n.contains("3080") || n.contains("3070") ||
            n.contains("6900") || n.contains("6800") || n.contains("7800") || n.contains("5070") -> "high"
            
            n.contains("4060") || n.contains("3060") || n.contains("3050") ||
            n.contains("6700") || n.contains("6600") || n.contains("7600") || n.contains("2070") || n.contains("2080") -> "medium"
            
            else -> when {
                vramGB >= 16 -> "ultra"
                vramGB >= 10 -> "high"
                vramGB >= 6 -> "medium"
                else -> "low"
            }
        }
    }
    
    private fun calculateHwScore(cores: Int, threads: Int, freqMHz: Int, vramGB: Int, ramGB: Int): Int {
        var score = 0
        
        // CPU (max 40)
        score += (cores * 2).coerceAtMost(20)
        score += (threads / 2).coerceAtMost(10)
        score += ((freqMHz - 2000) / 100).coerceIn(0, 10)
        
        // GPU (max 40)
        score += (vramGB * 2).coerceAtMost(30)
        score += 10
        
        // RAM (max 20)
        score += (ramGB / 2).coerceAtMost(20)
        
        return score.coerceIn(0, 100)
    }
}
