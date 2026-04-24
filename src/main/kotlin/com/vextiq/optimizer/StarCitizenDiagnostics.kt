package com.vextiq.optimizer

import com.vextiq.core.PowerShellRunner
import java.io.File

/**
 * Read-only diagnostic scan for Star Citizen installation.
 *
 * Produces a [Report] describing the state of the RSI Launcher, game files,
 * shader caches, EAC, keybinds, and interrupted download state. Never writes.
 *
 * The UI uses the report to decide whether the Smart Fix button should be
 * enabled at all — if nothing's wrong, the Fix button stays disabled to
 * prevent accidental destruction.
 */
object StarCitizenDiagnostics {

    private val TRAILING_SLASH = Regex("""[\\/]+$""")

    data class Check(
        val id: String,
        val label: String,
        val status: Status,
        val detail: String = "",
        val actionable: Boolean = false
    )

    enum class Status { OK, WARN, PROBLEM, INFO }

    data class Report(
        val gameInstallPath: String?,
        val environments: List<String>, // LIVE, PTU, EPTU, TECH-PREVIEW
        val checks: List<Check>,
        val totalGameSizeBytes: Long,
        val partialFileBytes: Long,
        val partialFilePaths: List<String>,
        val launcherCachePath: String,
        val launcherCacheSizeBytes: Long,
        val hasKeybinds: Boolean,
        val keybindsPath: String?,
        val problemCount: Int
    ) {
        val recommendFix: Boolean get() = problemCount > 0
    }

    fun diagnose(): Report {
        val checks = mutableListOf<Check>()
        val installPath = findInstallPath()

        if (installPath == null) {
            checks.add(Check("install", "Game installation", Status.PROBLEM,
                detail = "Star Citizen install folder not found in common locations"))
            return Report(
                gameInstallPath = null,
                environments = emptyList(),
                checks = checks,
                totalGameSizeBytes = 0,
                partialFileBytes = 0,
                partialFilePaths = emptyList(),
                launcherCachePath = launcherCacheDir().absolutePath,
                launcherCacheSizeBytes = 0,
                hasKeybinds = false,
                keybindsPath = null,
                problemCount = 1
            )
        }

        val installDir = File(installPath)
        checks.add(Check("install", "Game installation", Status.OK, detail = installPath))

        val environments = listOf("LIVE", "PTU", "EPTU", "TECH-PREVIEW")
            .filter { File(installDir, it).isDirectory }

        if (environments.isEmpty()) {
            checks.add(Check("environments", "Environments", Status.PROBLEM,
                detail = "No LIVE/PTU folders found under install path"))
        } else {
            checks.add(Check("environments", "Environments", Status.OK,
                detail = environments.joinToString(", ")))
        }

        // Launcher cache
        val launcherCacheDir = launcherCacheDir()
        val cacheSize = if (launcherCacheDir.exists()) dirSize(launcherCacheDir) else 0
        if (!launcherCacheDir.exists()) {
            checks.add(Check("launcherCache", "RSI Launcher cache", Status.INFO,
                detail = "Not present — launcher may not be installed or already clean"))
        } else {
            val suspicious = hasSuspiciousLauncherState(launcherCacheDir)
            val status = if (suspicious) Status.PROBLEM else Status.OK
            checks.add(Check("launcherCache", "RSI Launcher cache",
                status,
                detail = "${formatBytes(cacheSize)} @ ${launcherCacheDir.absolutePath}" +
                    if (suspicious) " — stale/corrupt signature detected" else "",
                actionable = suspicious))
        }

        // Partial patch files (.part, .tmp, .download) under environments
        val partialFiles = mutableListOf<File>()
        var partialBytes = 0L
        for (env in environments) {
            val envDir = File(installDir, env)
            collectPartialFiles(envDir, partialFiles)
        }
        partialBytes = partialFiles.sumOf { it.length() }
        if (partialFiles.isNotEmpty()) {
            checks.add(Check(
                "partial", "Interrupted download", Status.PROBLEM,
                detail = "${partialFiles.size} partial files, ${formatBytes(partialBytes)} — download was interrupted",
                actionable = true
            ))
        } else {
            checks.add(Check("partial", "Interrupted download", Status.OK,
                detail = "No partial .p4k/.part files found"))
        }

        // Shader cache
        val scShaderDir = File(System.getenv("LOCALAPPDATA") ?: "", "Star Citizen")
        val scShaderSize = if (scShaderDir.exists()) dirSize(scShaderDir) else 0
        val shaderOld = scShaderDir.exists() &&
            (System.currentTimeMillis() - scShaderDir.lastModified()) > 30L * 24 * 3600 * 1000
        when {
            !scShaderDir.exists() -> checks.add(Check("shader", "SC shader cache",
                Status.OK, "Not present"))
            shaderOld -> checks.add(Check("shader", "SC shader cache", Status.WARN,
                detail = "${formatBytes(scShaderSize)} — ${daysAgo(scShaderDir.lastModified())} days old", actionable = true))
            else -> checks.add(Check("shader", "SC shader cache",
                Status.OK, formatBytes(scShaderSize)))
        }

        // EAC
        val eacPresent = environments.any { File(File(installDir, it), "EasyAntiCheat").isDirectory }
        if (eacPresent) {
            checks.add(Check("eac", "EasyAntiCheat", Status.OK, "Installed"))
        } else {
            checks.add(Check("eac", "EasyAntiCheat", Status.WARN,
                detail = "Not found in any environment — may need reinstall"))
        }

        // Keybinds
        val keybindsFile = environments.asSequence()
            .map { File(File(installDir, it), "USER\\Client\\0\\Profiles\\default\\controls") }
            .firstOrNull { it.isDirectory }
        val hasKeybinds = keybindsFile != null
        if (hasKeybinds) {
            checks.add(Check("keybinds", "Keybinds backup target",
                Status.OK, detail = keybindsFile!!.absolutePath))
        } else {
            checks.add(Check("keybinds", "Keybinds", Status.INFO,
                detail = "No custom keybind profile found"))
        }

        // Error log signature for 5007
        val errorSignature = detect5007Signature(installDir, launcherCacheDir)
        if (errorSignature != null) {
            checks.add(Check("error5007", "Recent error signature", Status.PROBLEM,
                detail = errorSignature, actionable = true))
        }

        val totalSize = environments.sumOf { dirSize(File(installDir, it)) }

        val problemCount = checks.count { it.status == Status.PROBLEM }

        return Report(
            gameInstallPath = installPath,
            environments = environments,
            checks = checks,
            totalGameSizeBytes = totalSize,
            partialFileBytes = partialBytes,
            partialFilePaths = partialFiles.map { it.absolutePath },
            launcherCachePath = launcherCacheDir.absolutePath,
            launcherCacheSizeBytes = cacheSize,
            hasKeybinds = hasKeybinds,
            keybindsPath = keybindsFile?.absolutePath,
            problemCount = problemCount
        )
    }

    fun findInstallPath(): String? {
        // 1. Running process
        val fromProcess = PowerShellRunner.runPowerShell(
            "(Get-Process -Name 'StarCitizen','RSILauncher' -ErrorAction SilentlyContinue | Select-Object -First 1).Path",
            timeoutSec = 5
        ).trimmedOutput()
        if (fromProcess.isNotEmpty()) {
            // walk up until we find the "Roberts Space Industries" dir
            var f: File? = File(fromProcess).parentFile
            while (f != null) {
                if (f.name.equals("Roberts Space Industries", ignoreCase = true)) {
                    val sc = File(f, "StarCitizen")
                    if (sc.isDirectory) return sc.absolutePath
                }
                f = f.parentFile
            }
        }

        // 2. Common install paths
        val candidates = listOf(
            "C:\\Program Files\\Roberts Space Industries\\StarCitizen",
            "D:\\Program Files\\Roberts Space Industries\\StarCitizen",
            "E:\\Program Files\\Roberts Space Industries\\StarCitizen",
            "D:\\Games\\Roberts Space Industries\\StarCitizen",
            "E:\\Games\\Roberts Space Industries\\StarCitizen",
            "C:\\Games\\Roberts Space Industries\\StarCitizen"
        )
        return candidates.firstOrNull { File(it).isDirectory }
    }

    private fun launcherCacheDir(): File =
        File(System.getenv("APPDATA") ?: "", "rsilauncher")

    private fun collectPartialFiles(root: File, out: MutableList<File>) {
        if (!root.isDirectory) return
        val stack = ArrayDeque<File>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val cur = stack.removeFirst()
            val children = cur.listFiles() ?: continue
            for (c in children) {
                if (c.isDirectory) {
                    stack.add(c)
                } else {
                    val name = c.name.lowercase()
                    if (name.endsWith(".p4k.part") ||
                        name.endsWith(".p4k.tmp") ||
                        name.endsWith(".download") ||
                        name.endsWith(".crdownload") ||
                        name.endsWith(".partial") ||
                        (name.startsWith("~") && name.endsWith(".tmp"))
                    ) {
                        out.add(c)
                    }
                }
            }
        }
    }

    private fun hasSuspiciousLauncherState(cacheDir: File): Boolean {
        val logs = File(cacheDir, "logs")
        if (!logs.isDirectory) return false
        val cacheSub = File(logs, "Cache")
        val gpuCacheSub = File(logs, "GPUCache")
        // Stale browser cache >200 MB often indicates queued-update corruption
        val bigCache = (cacheSub.exists() && dirSize(cacheSub) > 200L * 1024 * 1024) ||
            (gpuCacheSub.exists() && dirSize(gpuCacheSub) > 200L * 1024 * 1024)
        return bigCache || hasRecentErrorLog(logs)
    }

    private fun hasRecentErrorLog(logs: File): Boolean {
        val cutoff = System.currentTimeMillis() - (7L * 24 * 3600 * 1000)
        val files = logs.listFiles { f -> f.isFile && f.name.endsWith(".log") } ?: return false
        return files.any { it.lastModified() > cutoff && looksLikeErrorLog(it) }
    }

    private fun looksLikeErrorLog(file: File): Boolean {
        return try {
            file.useLines { lines ->
                lines.take(200).any { line ->
                    line.contains("5007") ||
                        line.contains("ECONNRESET") ||
                        line.contains("ETIMEDOUT") ||
                        line.contains("patch failed", ignoreCase = true)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun detect5007Signature(installDir: File, launcherCacheDir: File): String? {
        if (!launcherCacheDir.isDirectory) return null
        val logs = File(launcherCacheDir, "logs")
        if (!logs.isDirectory) return null
        val files = logs.listFiles { f -> f.isFile && f.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            ?: return null
        for (f in files.take(5)) {
            val hit = try {
                f.useLines { it.take(500).firstOrNull { l -> l.contains("5007") } }
            } catch (_: Exception) { null }
            if (hit != null) return "5007 logged in ${f.name} (${daysAgo(f.lastModified())}d ago)"
        }
        return null
    }

    fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0
        var total = 0L
        val stack = ArrayDeque<File>()
        stack.add(dir)
        while (stack.isNotEmpty()) {
            val cur = stack.removeFirst()
            val ch = cur.listFiles() ?: continue
            for (c in ch) {
                if (c.isDirectory) stack.add(c) else total += c.length()
            }
        }
        return total
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }

    private fun daysAgo(timestamp: Long): Int =
        ((System.currentTimeMillis() - timestamp) / (24L * 3600 * 1000)).toInt()
}
