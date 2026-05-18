package com.vextiq.optimizer

class ProcessManager {

    data class ProcessInfo(
        val pid: Int,
        val name: String,
        val memoryMB: Long,
        val cpuPercent: Double
    )

    private fun runPowerShell(script: String): Pair<Int, String> {
        val proc = ProcessBuilder(
            listOf("powershell", "-NoProfile", "-NonInteractive", "-Command", script)
        ).redirectErrorStream(true).start()
        val output = proc.inputStream.bufferedReader().readText()
        val exit = proc.waitFor()
        return exit to output
    }

    private fun baseName(name: String) = name.removeSuffix(".exe").removeSuffix(".EXE")

    fun setHighPriority(processName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Setting $processName to High priority...")
        return setPriority(processName, "High", onLog)
    }

    fun setRealtimePriority(processName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Setting $processName to Realtime priority...")
        val ok = setPriority(processName, "RealTime", onLog)
        if (ok) onLog("[!] Warning: Realtime priority may cause system instability")
        return ok
    }

    private fun setPriority(processName: String, priorityClass: String, onLog: (String) -> Unit): Boolean {
        val script = """
            ${'$'}targets = Get-Process -Name '${baseName(processName)}' -ErrorAction SilentlyContinue
            if (-not ${'$'}targets) { Write-Output 'NOT_FOUND'; exit 0 }
            ${'$'}count = 0
            foreach (${'$'}p in ${'$'}targets) {
                try { ${'$'}p.PriorityClass = '$priorityClass'; ${'$'}count++ } catch {}
            }
            Write-Output "OK:${'$'}count"
        """.trimIndent()
        return try {
            val (_, output) = runPowerShell(script)
            when {
                output.contains("NOT_FOUND") -> {
                    onLog("[!] $processName not running")
                    false
                }
                output.contains("OK:") -> {
                    val count = output.substringAfter("OK:").trim().toIntOrNull() ?: 0
                    onLog("[OK] $processName priority set to $priorityClass ($count process${if (count == 1) "" else "es"})")
                    count > 0
                }
                else -> {
                    onLog("[!] Could not set priority — admin rights may be required")
                    false
                }
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }

    fun setPerformanceCoreAffinity(processName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Setting $processName to P-cores only...")
        val script = """
            ${'$'}targets = Get-Process -Name '${baseName(processName)}' -ErrorAction SilentlyContinue
            if (-not ${'$'}targets) { Write-Output 'NOT_FOUND'; exit 0 }
            ${'$'}count = 0
            foreach (${'$'}p in ${'$'}targets) {
                try { ${'$'}p.ProcessorAffinity = 0xFF; ${'$'}count++ } catch {}
            }
            Write-Output "OK:${'$'}count"
        """.trimIndent()
        return try {
            val (_, output) = runPowerShell(script)
            if (output.contains("OK:")) {
                onLog("[OK] $processName affinity set to P-cores")
                true
            } else {
                onLog("[!] Process not found or admin required")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }

    fun killProcess(processName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Killing $processName...")
        return try {
            ProcessBuilder(listOf("taskkill", "/IM", processName, "/F"))
                .redirectErrorStream(true).start().waitFor()
            onLog("[OK] $processName terminated")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }

    fun getRunningProcesses(): List<ProcessInfo> {
        val script = """
            Get-Process | ForEach-Object {
                "${'$'}(${'$'}_.Id)|${'$'}(${'$'}_.ProcessName)|${'$'}([math]::Round(${'$'}_.WorkingSet64/1MB))|${'$'}(if (${'$'}_.CPU) { [math]::Round(${'$'}_.CPU, 2) } else { 0 })"
            }
        """.trimIndent()
        return try {
            val (_, output) = runPowerShell(script)
            output.lineSequence()
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size == 4) {
                        val pid = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
                        val name = parts[1].trim()
                        val memMB = parts[2].trim().toLongOrNull() ?: 0L
                        val cpu = parts[3].trim().toDoubleOrNull() ?: 0.0
                        ProcessInfo(pid, name, memMB, cpu)
                    } else null
                }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun optimizeForGaming(gameName: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing system for $gameName...")

        val backgroundProcesses = listOf(
            "SearchIndexer", "SearchHost",
            "OneDrive", "Dropbox",
            "Spotify", "Discord",
            "slack", "Teams"
        )

        var lowered = 0
        val script = """
            ${'$'}names = @(${backgroundProcesses.joinToString(",") { "'$it'" }})
            ${'$'}count = 0
            foreach (${'$'}n in ${'$'}names) {
                ${'$'}procs = Get-Process -Name ${'$'}n -ErrorAction SilentlyContinue
                foreach (${'$'}p in ${'$'}procs) {
                    try { ${'$'}p.PriorityClass = 'BelowNormal'; ${'$'}count++ } catch {}
                }
            }
            Write-Output "LOWERED:${'$'}count"
        """.trimIndent()
        try {
            val (_, output) = runPowerShell(script)
            lowered = output.substringAfter("LOWERED:", "0").trim().toIntOrNull() ?: 0
        } catch (e: Exception) {
            // Ignore — non-fatal
        }

        if (lowered > 0) {
            onLog("[OK] Lowered priority of $lowered background processes")
        }

        setHighPriority(gameName, onLog)
        onLog("[OK] System optimized for gaming")
    }
}
