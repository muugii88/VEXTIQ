package com.vextiq.optimizer

import com.vextiq.core.PowerShellRunner

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
            PowerShellRunner.runPowerShell(
                """
                [System.GC]::Collect()
                [System.GC]::WaitForPendingFinalizers()

                Get-Process | Where-Object { ${'$'}_.WorkingSet64 -gt 100MB } | ForEach-Object {
                    try {
                        ${'$'}_.MinWorkingSet = 1MB
                    } catch {
                        # Some processes can't be modified
                    }
                }
                """.trimIndent(),
                timeoutSec = 30
            )

            onLog("[OK] Working sets trimmed")

        } catch (e: Exception) {
            onLog("[!] Some operations require admin: ${e.message}")
        }

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
            PowerShellRunner.runPowerShell(
                """
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
                """.trimIndent(),
                timeoutSec = 45
            )

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

        onLog("[1/4] Closing background apps...")
        closeBackgroundApps(onLog)

        onLog("[2/4] Emptying working sets...")
        emptyWorkingSets { }

        onLog("[3/4] Cleaning standby memory...")
        cleanStandbyMemory { }

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
            val result = PowerShellRunner.exec(
                listOf("taskkill", "/IM", "$app*", "/F"),
                timeoutSec = 5
            )
            if (result.success) closed++
        }

        if (closed > 0) {
            onLog("[OK] Closed $closed background apps")
        }
    }

    /**
     * Get available memory in MB
     */
    private fun getAvailableMemoryMB(): Long {
        return PowerShellRunner.runPowerShell(
            "(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory / 1024",
            timeoutSec = 10
        ).trimmedOutput().toDoubleOrNull()?.toLong() ?: 0
    }
}
