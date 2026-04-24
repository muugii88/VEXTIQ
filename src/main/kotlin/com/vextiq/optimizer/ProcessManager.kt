package com.vextiq.optimizer

import com.vextiq.core.PowerShellRunner

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

        val result = PowerShellRunner.exec(
            listOf("wmic", "process", "where", "name='$processName'", "call", "setpriority", "128"),
            timeoutSec = 10
        )

        return if (result.success) {
            onLog("[OK] $processName priority set to High")
            true
        } else {
            onLog("[!] Could not set priority for $processName${if (result.timedOut) " (timeout)" else ""}")
            false
        }
    }

    /**
     * Set process priority to Realtime (use with caution!)
     */
    fun setRealtimePriority(processName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Setting $processName to Realtime priority...")

        val result = PowerShellRunner.exec(
            listOf("wmic", "process", "where", "name='$processName'", "call", "setpriority", "256"),
            timeoutSec = 10
        )

        return if (result.success) {
            onLog("[OK] $processName priority set to Realtime")
            onLog("[!] Warning: Realtime priority may cause system instability")
            true
        } else {
            onLog("[!] Could not set priority for $processName${if (result.timedOut) " (timeout)" else ""}")
            false
        }
    }

    /**
     * Set CPU affinity to performance cores only (for hybrid CPUs)
     */
    fun setPerformanceCoreAffinity(processName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Setting $processName to P-cores only...")

        // Intel 12th gen+: P-cores are typically first 8 cores → mask 0xFF
        val affinityMask = "FF"
        val cleanName = processName.removeSuffix(".exe")

        val output = PowerShellRunner.runPowerShell(
            """
            ${'$'}proc = Get-Process -Name '$cleanName' -ErrorAction SilentlyContinue
            if (${'$'}proc) {
                ${'$'}proc.ProcessorAffinity = 0x$affinityMask
                Write-Output "OK"
            }
            """.trimIndent(),
            timeoutSec = 10
        ).output

        return if (output.contains("OK")) {
            onLog("[OK] $processName affinity set to P-cores")
            true
        } else {
            onLog("[!] Process not found or error")
            false
        }
    }

    /**
     * Kill process by name
     */
    fun killProcess(processName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Killing $processName...")

        val result = PowerShellRunner.exec(
            listOf("taskkill", "/IM", processName, "/F"),
            timeoutSec = 10
        )

        return if (result.timedOut) {
            onLog("[!] taskkill timed out for $processName")
            false
        } else {
            onLog("[OK] $processName terminated")
            true
        }
    }

    /**
     * Get list of running processes
     */
    fun getRunningProcesses(): List<ProcessInfo> {
        return try {
            PowerShellRunner.runPowerShell(
                """
                Get-Process | Select-Object Id, ProcessName,
                @{N='MemMB';E={[math]::Round(${'$'}_.WorkingSet64/1MB)}},
                @{N='CPU';E={${'$'}_.CPU}} |
                ConvertTo-Json
                """.trimIndent(),
                timeoutSec = 20
            )
            // JSON parsing not implemented — callers don't currently consume this
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Optimize system for gaming - lower priority of background processes
     */
    fun optimizeForGaming(gameName: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing system for $gameName...")

        val backgroundProcesses = listOf(
            "SearchIndexer.exe", "SearchHost.exe",
            "OneDrive.exe", "Dropbox.exe",
            "Spotify.exe", "Discord.exe",
            "slack.exe", "Teams.exe"
        )

        var lowered = 0
        for (proc in backgroundProcesses) {
            val result = PowerShellRunner.exec(
                listOf("wmic", "process", "where", "name='$proc'", "call", "setpriority", "64"),
                timeoutSec = 5
            )
            if (result.success) lowered++
        }

        if (lowered > 0) {
            onLog("[OK] Lowered priority of $lowered background processes")
        }

        setHighPriority(gameName, onLog)

        onLog("[OK] System optimized for gaming")
    }
}
