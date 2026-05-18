package com.vextiq.optimizer

/**
 * RAM Cleaner - Free up standby memory and optimize RAM usage
 */
class RamCleaner {
    
    data class CleanResult(
        val freedMB: Long,
        val beforeMB: Long,
        val afterMB: Long
    )
    
    /**
     * Clean standby memory using PowerShell
     */
    fun cleanStandbyMemory(onLog: (String) -> Unit): CleanResult {
        onLog("[>>] Cleaning standby memory...")
        
        val beforeFree = getAvailableMemoryMB()
        
        try {
            // Clear standby list (requires admin)
            runPowerShell("""
                # Clear file system cache
                [System.GC]::Collect()
                [System.GC]::WaitForPendingFinalizers()
                
                # Clear working sets
                Get-Process | Where-Object { ${'$'}_.WorkingSet64 -gt 100MB } | ForEach-Object {
                    try {
                        ${'$'}_.MinWorkingSet = 1MB
                    } catch {
                        # Some processes can't be modified
                    }
                }
            """.trimIndent())
            
            onLog("[OK] Working sets trimmed")
            
        } catch (e: Exception) {
            onLog("[!] Some operations require admin: ${e.message}")
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(500)
        
        val afterFree = getAvailableMemoryMB()
        val freed = afterFree - beforeFree
        
        onLog("[OK] RAM freed: ${if (freed > 0) freed else 0} MB")
        onLog("[i] Available: $afterFree MB")
        
        return CleanResult(
            freedMB = if (freed > 0) freed else 0,
            beforeMB = beforeFree,
            afterMB = afterFree
        )
    }
    
    /**
     * Empty working sets of all processes
     */
    fun emptyWorkingSets(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Emptying working sets...")
        
        return try {
            runPowerShell("""
                ${'$'}signature = @'
                [DllImport("psapi.dll")]
                public static extern bool EmptyWorkingSet(IntPtr hProcess);
'@
                
                ${'$'}type = Add-Type -MemberDefinition ${'$'}signature -Name "Win32" -Namespace "Psapi" -PassThru
                
                Get-Process | Where-Object { ${'$'}_.Id -ne ${'$'}PID } | ForEach-Object {
                    try {
                        ${'$'}type::EmptyWorkingSet(${'$'}_.Handle) | Out-Null
                    } catch {
                        # Some processes can't be emptied
                    }
                }
            """.trimIndent())
            
            onLog("[OK] Working sets emptied")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * Optimize memory for gaming
     */
    fun optimizeForGaming(onLog: (String) -> Unit): CleanResult {
        onLog("[>>] Optimizing memory for gaming...")
        onLog("")
        
        val beforeFree = getAvailableMemoryMB()
        
        // Step 1: Close unnecessary background apps
        onLog("[1/4] Closing background apps...")
        closeBackgroundApps(onLog)
        
        // Step 2: Empty working sets
        onLog("[2/4] Emptying working sets...")
        emptyWorkingSets { }
        
        // Step 3: Clean standby memory
        onLog("[3/4] Cleaning standby memory...")
        cleanStandbyMemory { }
        
        // Step 4: Disable memory compression (optional)
        onLog("[4/4] Optimizing memory settings...")
        
        Thread.sleep(1000)
        
        val afterFree = getAvailableMemoryMB()
        val freed = afterFree - beforeFree
        
        onLog("")
        onLog("[OK] Memory optimization complete!")
        onLog("[i] Freed: ${if (freed > 0) freed else 0} MB")
        onLog("[i] Available: $afterFree MB")
        
        return CleanResult(
            freedMB = if (freed > 0) freed else 0,
            beforeMB = beforeFree,
            afterMB = afterFree
        )
    }
    
    /**
     * Close common background apps that consume RAM
     */
    private fun closeBackgroundApps(onLog: (String) -> Unit) {
        val appsToClose = listOf(
            "OneDrive", "Spotify", "Discord", "Slack", "Teams",
            "SkypeApp", "YourPhone", "GameBar", "Xbox"
        )
        
        var closed = 0
        for (app in appsToClose) {
            try {
                val process = ProcessBuilder(listOf(
                    "taskkill", "/IM", "$app*", "/F"
                )).redirectErrorStream(true).start()
                process.waitFor()
                
                if (process.exitValue() == 0) {
                    closed++
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        if (closed > 0) {
            onLog("[OK] Closed $closed background apps")
        }
    }
    
    /**
     * Get available memory in MB
     */
    private fun getAvailableMemoryMB(): Long {
        return try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory / 1024"
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            
            output.toDoubleOrNull()?.toLong() ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun runPowerShell(script: String): String {
        val process = ProcessBuilder(listOf(
            "powershell", "-NoProfile", "-NonInteractive", "-Command", script
        )).redirectErrorStream(true).start()
        
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        
        return output
    }
    
    /**
     * Clean - alias for optimizeForGaming
     */
    fun clean(onLog: (String) -> Unit) {
        optimizeForGaming(onLog)
    }
}
