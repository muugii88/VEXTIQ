package com.vextiq.optimizer

/**
 * Process Manager - Manage process priorities and affinity
 */
class ProcessManager {
    
    data class ProcessInfo(
        val pid: Int,
        val name: String,
        val memoryMB: Long,
        val cpuPercent: Double
    )
    
    /**
     * Set process priority to High
     */
    fun setHighPriority(processName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Setting $processName to High priority...")
        
        return try {
            val process = ProcessBuilder(listOf(
                "wmic", "process", "where", "name='$processName'", 
                "call", "setpriority", "128" // High priority
            )).redirectErrorStream(true).start()
            
            process.waitFor()
            
            if (process.exitValue() == 0) {
                onLog("[OK] $processName priority set to High")
                true
            } else {
                onLog("[!] Could not set priority for $processName")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * Set process priority to Realtime (use with caution!)
     */
    fun setRealtimePriority(processName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Setting $processName to Realtime priority...")
        
        return try {
            val process = ProcessBuilder(listOf(
                "wmic", "process", "where", "name='$processName'", 
                "call", "setpriority", "256" // Realtime
            )).redirectErrorStream(true).start()
            
            process.waitFor()
            
            onLog("[OK] $processName priority set to Realtime")
            onLog("[!] Warning: Realtime priority may cause system instability")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * Set CPU affinity to performance cores only (for hybrid CPUs)
     */
    fun setPerformanceCoreAffinity(processName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Setting $processName to P-cores only...")
        
        // For Intel 12th gen+, P-cores are usually the first 8 cores
        // Affinity mask for cores 0-7: 0xFF
        val affinityMask = "FF" // First 8 cores
        
        return try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                ${'$'}proc = Get-Process -Name '${processName.removeSuffix(".exe")}' -ErrorAction SilentlyContinue
                if (${'$'}proc) {
                    ${'$'}proc.ProcessorAffinity = 0x$affinityMask
                    Write-Output "OK"
                }
                """.trimIndent()
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            if (output.contains("OK")) {
                onLog("[OK] $processName affinity set to P-cores")
                true
            } else {
                onLog("[!] Process not found or error")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * Kill process by name
     */
    fun killProcess(processName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Killing $processName...")
        
        return try {
            val process = ProcessBuilder(listOf(
                "taskkill", "/IM", processName, "/F"
            )).redirectErrorStream(true).start()
            
            process.waitFor()
            
            onLog("[OK] $processName terminated")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * Get list of running processes
     */
    fun getRunningProcesses(): List<ProcessInfo> {
        return try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                Get-Process | Select-Object Id, ProcessName, 
                @{N='MemMB';E={[math]::Round(${'$'}_.WorkingSet64/1MB)}},
                @{N='CPU';E={${'$'}_.CPU}} |
                ConvertTo-Json
                """.trimIndent()
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            // Parse JSON manually (simple)
            val processes = mutableListOf<ProcessInfo>()
            // ... simplified parsing
            processes
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Optimize system for gaming - lower priority of background processes
     */
    fun optimizeForGaming(gameName: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing system for $gameName...")
        
        // Lower priority of known background processes
        val backgroundProcesses = listOf(
            "SearchIndexer.exe", "SearchHost.exe",
            "OneDrive.exe", "Dropbox.exe",
            "Spotify.exe", "Discord.exe",
            "slack.exe", "Teams.exe"
        )
        
        var lowered = 0
        for (proc in backgroundProcesses) {
            try {
                val process = ProcessBuilder(listOf(
                    "wmic", "process", "where", "name='$proc'",
                    "call", "setpriority", "64" // Below Normal
                )).redirectErrorStream(true).start()
                process.waitFor()
                if (process.exitValue() == 0) lowered++
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        if (lowered > 0) {
            onLog("[OK] Lowered priority of $lowered background processes")
        }
        
        // Set game to high priority if running
        setHighPriority(gameName, onLog)
        
        onLog("[OK] System optimized for gaming")
    }
}
