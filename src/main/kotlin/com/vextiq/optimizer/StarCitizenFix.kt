package com.vextiq.optimizer

import com.vextiq.core.PowerShellRunner
import java.io.File

/**
 * Star Citizen Smart Fix — repairs RSI Launcher Error 5007 caused by an
 * interrupted download, corrupt launcher cache, or stale shader data,
 * WITHOUT redownloading the full 150 GB game.
 *
 * Always backs up everything it deletes via [SmartFixBackup] so the
 * operation is fully reversible.
 *
 * Diagnose first, Fix second — this class is only called after the user
 * has run [StarCitizenDiagnostics] and confirmed via a typed "FIX" prompt.
 */
class StarCitizenFix {

    companion object {
        const val FIX_ID = "star-citizen"
    }

    /**
     * Preview what the fix will do — for the confirmation dialog.
     * Does NOT modify anything.
     */
    data class Preview(
        val partialFilesToDelete: List<String>,
        val partialBytes: Long,
        val launcherCacheToClear: String,
        val launcherCacheBytes: Long,
        val shaderCacheToClear: String?,
        val shaderCacheBytes: Long,
        val keybindsToBackup: String?,
        val eacToReset: String?,
        val gpuCachesToClear: List<String>,
        val firewallRulesToAdd: List<String>
    ) {
        val totalDeleteBytes: Long
            get() = partialBytes + launcherCacheBytes + shaderCacheBytes
    }

    fun preview(report: StarCitizenDiagnostics.Report): Preview {
        val localAppData = System.getenv("LOCALAPPDATA") ?: ""
        val gpuCaches = listOf(
            "$localAppData\\NVIDIA\\DXCache",
            "$localAppData\\NVIDIA\\GLCache",
            "$localAppData\\AMD\\DxCache",
            "$localAppData\\AMD\\DxcCache",
            "$localAppData\\AMD\\VkCache",
            "$localAppData\\D3DSCache"
        ).filter { File(it).exists() }

        val shaderDir = File(localAppData, "Star Citizen")
        val shaderBytes = if (shaderDir.exists()) StarCitizenDiagnostics.dirSize(shaderDir) else 0

        val eacPath = report.environments
            .map { File(report.gameInstallPath ?: "", "$it\\EasyAntiCheat") }
            .firstOrNull { it.isDirectory }
            ?.absolutePath

        return Preview(
            partialFilesToDelete = report.partialFilePaths,
            partialBytes = report.partialFileBytes,
            launcherCacheToClear = report.launcherCachePath,
            launcherCacheBytes = report.launcherCacheSizeBytes,
            shaderCacheToClear = if (shaderDir.exists()) shaderDir.absolutePath else null,
            shaderCacheBytes = shaderBytes,
            keybindsToBackup = report.keybindsPath,
            eacToReset = eacPath,
            gpuCachesToClear = gpuCaches,
            firewallRulesToAdd = listOf(
                "StarCitizen.exe (TCP/UDP allow)",
                "RSILauncher.exe (TCP/UDP allow)",
                "TCP 8000-8020, UDP 64090-64110"
            )
        )
    }

    /**
     * Execute the fix. [onLog] receives progress messages. Returns true if all
     * mandatory steps succeeded (keybind backup, launcher cache, partial files).
     * Optional steps (firewall, DNS flush) failing will WARN but not fail the fix.
     */
    fun execute(
        report: StarCitizenDiagnostics.Report,
        onLog: (String) -> Unit
    ): Boolean {
        val preview = preview(report)
        val backup = SmartFixBackup.begin(FIX_ID)
        var hadError = false

        onLog("╔══════════════════════════════════════════╗")
        onLog("║  VEXTIQ Star Citizen Smart Fix           ║")
        onLog("║  Backup: ${backup.folder.name}           ║")
        onLog("╚══════════════════════════════════════════╝")
        onLog("")

        // 1. Kill RSI processes
        onLog("[1/8] Stopping RSI processes...")
        killProcesses(onLog)

        // 2. Backup keybinds
        onLog("[2/8] Backing up keybinds...")
        preview.keybindsToBackup?.let {
            if (SmartFixBackup.addSource(backup, File(it), "keybinds")) {
                onLog("[OK] Keybinds archived")
            } else {
                onLog("[!] Keybinds backup skipped (none found)")
            }
        } ?: onLog("[i] No custom keybinds to backup")

        // 3. Backup + clear launcher cache
        onLog("[3/8] Backing up & clearing RSI Launcher cache...")
        val launcherCache = File(preview.launcherCacheToClear, "logs")
        if (launcherCache.exists()) {
            SmartFixBackup.addSource(backup, launcherCache, "launcher-logs")
            deleteLauncherSubdirs(launcherCache, onLog)
        } else {
            onLog("[i] Launcher cache not present")
        }

        // 4. Backup + delete partial patch files
        onLog("[4/8] Removing ${preview.partialFilesToDelete.size} partial patch files (${StarCitizenDiagnostics.formatBytes(preview.partialBytes)})...")
        for (pathStr in preview.partialFilesToDelete) {
            val f = File(pathStr)
            // Back up each partial file individually (small, fast)
            SmartFixBackup.addSource(backup, f, "partial-" + f.name.replace(Regex("[^a-zA-Z0-9._-]"), "_"))
            if (f.delete()) {
                onLog("  [OK] ${f.name}")
            } else {
                onLog("  [!] Could not delete ${f.name}")
                hadError = true
            }
        }

        // 5. Clear Star Citizen shader cache
        preview.shaderCacheToClear?.let { path ->
            onLog("[5/8] Clearing SC shader cache...")
            val dir = File(path)
            if (SmartFixBackup.addSource(backup, dir, "sc-shaders")) {
                deleteRecursively(dir, onLog, label = "SC shaders")
            }
        } ?: onLog("[5/8] SC shader cache absent — skipping")

        // 6. Clear GPU shader caches (non-fatal)
        onLog("[6/8] Clearing GPU shader caches...")
        for (gpuPath in preview.gpuCachesToClear) {
            val dir = File(gpuPath)
            SmartFixBackup.addSource(backup, dir, "gpu-" + dir.name)
            try {
                if (dir.exists()) {
                    dir.listFiles()?.forEach { it.deleteRecursively() }
                    onLog("  [OK] ${dir.name}")
                }
            } catch (e: Exception) {
                onLog("  [!] ${dir.name}: ${e.message}")
            }
        }

        // 7. Firewall rules (best-effort, needs admin)
        onLog("[7/8] Ensuring firewall rules...")
        addFirewallRules(report, onLog)

        // 8. Flush DNS
        onLog("[8/8] Flushing DNS cache...")
        flushDns(onLog)

        // Commit backup manifest
        SmartFixBackup.commit(backup)

        onLog("")
        onLog("════════════════════════════════════════════")
        if (hadError) {
            onLog("[!] Completed with warnings. See log above.")
        } else {
            onLog("[OK] Smart Fix complete.")
        }
        onLog("[i] Launch RSI Launcher as administrator.")
        onLog("[i] It will verify files and redownload only partial data.")
        onLog("[i] Backup: ${backup.folder.absolutePath}")
        onLog("════════════════════════════════════════════")
        return !hadError
    }

    /**
     * Restore the most recent backup, putting the launcher back into the state
     * it was in before the Fix ran.
     */
    fun restoreLatest(onLog: (String) -> Unit): Boolean {
        val backups = SmartFixBackup.listBackups(FIX_ID)
        if (backups.isEmpty()) {
            onLog("[!] No Star Citizen backups found")
            return false
        }
        val latest = backups.first()
        onLog("[>>] Restoring from ${latest.folder.name}...")
        killProcesses(onLog)
        return SmartFixBackup.restore(latest, onLog)
    }

    // ─── private ────────────────────────────────────────────────────────────

    private fun killProcesses(onLog: (String) -> Unit) {
        val targets = listOf("StarCitizen", "RSILauncher", "StarCitizen_Launcher", "EasyAntiCheat")
        for (name in targets) {
            val r = PowerShellRunner.exec(
                listOf("taskkill", "/IM", "$name.exe", "/F"),
                timeoutSec = 5
            )
            if (r.success) onLog("  [OK] Killed $name.exe")
        }
    }

    private fun deleteLauncherSubdirs(logs: File, onLog: (String) -> Unit) {
        val targets = listOf("Cache", "GPUCache", "Code Cache", "Service Worker", "blob_storage")
        for (name in targets) {
            val sub = File(logs, name)
            if (sub.exists()) {
                try {
                    sub.deleteRecursively()
                    onLog("  [OK] logs\\$name cleared")
                } catch (e: Exception) {
                    onLog("  [!] logs\\$name: ${e.message}")
                }
            }
        }
    }

    private fun deleteRecursively(dir: File, onLog: (String) -> Unit, label: String) {
        try {
            dir.deleteRecursively()
            onLog("  [OK] $label cleared")
        } catch (e: Exception) {
            onLog("  [!] $label: ${e.message}")
        }
    }

    private fun addFirewallRules(report: StarCitizenDiagnostics.Report, onLog: (String) -> Unit) {
        val installPath = report.gameInstallPath ?: return
        val liveExe = File("$installPath\\LIVE\\Bin64\\StarCitizen.exe")
        val launcherExe = findLauncherExe()

        val rules = buildList {
            if (liveExe.isFile) {
                add(listOf("advfirewall", "firewall", "add", "rule",
                    "name=VEXTIQ-StarCitizen", "dir=in", "action=allow",
                    "program=${liveExe.absolutePath}", "enable=yes"))
                add(listOf("advfirewall", "firewall", "add", "rule",
                    "name=VEXTIQ-StarCitizen-Out", "dir=out", "action=allow",
                    "program=${liveExe.absolutePath}", "enable=yes"))
            }
            if (launcherExe != null) {
                add(listOf("advfirewall", "firewall", "add", "rule",
                    "name=VEXTIQ-RSILauncher", "dir=in", "action=allow",
                    "program=${launcherExe.absolutePath}", "enable=yes"))
                add(listOf("advfirewall", "firewall", "add", "rule",
                    "name=VEXTIQ-RSILauncher-Out", "dir=out", "action=allow",
                    "program=${launcherExe.absolutePath}", "enable=yes"))
            }
            // RSI documented ports
            add(listOf("advfirewall", "firewall", "add", "rule",
                "name=VEXTIQ-RSI-TCP", "dir=out", "action=allow", "protocol=TCP",
                "localport=8000-8020"))
            add(listOf("advfirewall", "firewall", "add", "rule",
                "name=VEXTIQ-RSI-UDP", "dir=out", "action=allow", "protocol=UDP",
                "localport=64090-64110"))
        }

        var applied = 0
        for (args in rules) {
            val r = PowerShellRunner.exec(listOf("netsh") + args, timeoutSec = 6)
            if (r.success) applied++
        }
        if (applied > 0) {
            onLog("  [OK] $applied firewall rules applied")
        } else {
            onLog("  [i] Firewall rules skipped (likely needs admin)")
        }
    }

    private fun findLauncherExe(): File? {
        val candidates = listOf(
            "${System.getenv("PROGRAMFILES") ?: "C:\\Program Files"}\\Roberts Space Industries\\RSI Launcher\\RSI Launcher.exe",
            "${System.getenv("LOCALAPPDATA") ?: ""}\\Programs\\rsilauncher\\RSI Launcher.exe"
        )
        return candidates.map(::File).firstOrNull { it.isFile }
    }

    private fun flushDns(onLog: (String) -> Unit) {
        val r = PowerShellRunner.exec(listOf("ipconfig", "/flushdns"), timeoutSec = 5)
        if (r.success) onLog("  [OK] DNS cache flushed")
        else onLog("  [!] DNS flush failed: ${r.trimmedOutput()}")
    }
}
