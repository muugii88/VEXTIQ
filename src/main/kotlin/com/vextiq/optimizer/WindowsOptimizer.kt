package com.vextiq.optimizer

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Windows Optimizer - Real system optimizations
 * Requires Administrator privileges for some operations
 */
class WindowsOptimizer {
    
    data class OptResult(val success: Boolean, val message: String)
    
    /**
     * Enable Windows Game Mode
     */
    fun enableGameMode(onLog: (String) -> Unit): OptResult {
        onLog("[>>] Enabling Game Mode...")
        
        return try {
            // Enable Game Mode
            runReg("add", "HKCU\\Software\\Microsoft\\GameBar", "/v", "AutoGameModeEnabled", "/t", "REG_DWORD", "/d", "1", "/f")
            runReg("add", "HKCU\\Software\\Microsoft\\GameBar", "/v", "AllowAutoGameMode", "/t", "REG_DWORD", "/d", "1", "/f")
            
            onLog("[OK] Game Mode enabled")
            OptResult(true, "Game Mode enabled")
        } catch (e: Exception) {
            onLog("[!] Game Mode: ${e.message}")
            OptResult(false, e.message ?: "Failed")
        }
    }
    
    /**
     * Enable Hardware Accelerated GPU Scheduling (HAGS)
     */
    fun enableHAGS(onLog: (String) -> Unit): OptResult {
        onLog("[>>] Configuring HAGS...")
        
        return try {
            runReg("add", "HKLM\\SYSTEM\\CurrentControlSet\\Control\\GraphicsDrivers", "/v", "HwSchMode", "/t", "REG_DWORD", "/d", "2", "/f")
            onLog("[OK] HAGS enabled (requires restart)")
            OptResult(true, "HAGS enabled")
        } catch (e: Exception) {
            onLog("[!] HAGS requires admin: ${e.message}")
            OptResult(false, e.message ?: "Failed")
        }
    }
    
    /**
     * Disable Fullscreen Optimizations globally
     */
    fun disableFullscreenOptimizations(onLog: (String) -> Unit): OptResult {
        // Delegated to AdvancedBooster — same registry writes lived in both classes.
        val ok = advancedBooster.disableFullscreenOptimizations(onLog)
        return OptResult(ok, if (ok) "FSO disabled" else "Failed")
    }
    
    /**
     * Disable Game DVR/Bar Recording
     */
    fun disableGameDVR(onLog: (String) -> Unit): OptResult {
        onLog("[>>] Disabling Game DVR...")
        
        return try {
            runReg("add", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\GameDVR", "/v", "AppCaptureEnabled", "/t", "REG_DWORD", "/d", "0", "/f")
            runReg("add", "HKCU\\System\\GameConfigStore", "/v", "GameDVR_Enabled", "/t", "REG_DWORD", "/d", "0", "/f")
            
            onLog("[OK] Game DVR disabled")
            OptResult(true, "Game DVR disabled")
        } catch (e: Exception) {
            onLog("[!] Game DVR: ${e.message}")
            OptResult(false, e.message ?: "Failed")
        }
    }
    
    /**
     * Optimize Visual Effects for Performance
     */
    fun optimizeVisualEffects(onLog: (String) -> Unit): OptResult {
        // Delegated to AdvancedBooster — same registry writes lived in both classes.
        val ok = advancedBooster.optimizeVisualEffects(onLog)
        return OptResult(ok, if (ok) "Visual effects optimized" else "Failed")
    }
    
    /**
     * Set Power Plan to High Performance
     */
    fun setHighPerformancePower(onLog: (String) -> Unit): OptResult {
        onLog("[>>] Setting High Performance power plan...")
        
        return try {
            // High Performance GUID
            runCmd("powercfg", "/setactive", "8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c")
            onLog("[OK] High Performance power plan active")
            OptResult(true, "Power plan set")
        } catch (e: Exception) {
            // Try Ultimate Performance
            try {
                runCmd("powercfg", "-duplicatescheme", "e9a42b02-d5df-448d-aa00-03f14749eb61")
                onLog("[OK] Ultimate Performance plan created")
                OptResult(true, "Ultimate Performance created")
            } catch (e2: Exception) {
                onLog("[!] Power plan: ${e.message}")
                OptResult(false, e.message ?: "Failed")
            }
        }
    }
    
    /**
     * Disable Windows Search Indexing (reduces disk I/O)
     */
    fun disableSearchIndexing(onLog: (String) -> Unit): OptResult {
        onLog("[>>] Disabling Search Indexing...")
        
        return try {
            runCmd("sc", "config", "WSearch", "start=", "disabled")
            runCmd("sc", "stop", "WSearch")
            onLog("[OK] Search Indexing disabled")
            OptResult(true, "Indexing disabled")
        } catch (e: Exception) {
            onLog("[!] Indexing (needs admin): ${e.message}")
            OptResult(false, e.message ?: "Failed")
        }
    }
    
    /**
     * Disable Superfetch/SysMain
     */
    fun disableSysMain(onLog: (String) -> Unit): OptResult {
        onLog("[>>] Disabling SysMain/Superfetch...")
        
        return try {
            runCmd("sc", "config", "SysMain", "start=", "disabled")
            runCmd("sc", "stop", "SysMain")
            onLog("[OK] SysMain disabled")
            OptResult(true, "SysMain disabled")
        } catch (e: Exception) {
            onLog("[!] SysMain (needs admin): ${e.message}")
            OptResult(false, e.message ?: "Failed")
        }
    }
    
    /**
     * Apply ALL Windows optimizations
     */
    fun applyAll(onLog: (String) -> Unit): List<OptResult> {
        onLog("[>>] Applying all Windows optimizations...")
        onLog("")
        
        val results = mutableListOf<OptResult>()
        
        results.add(enableGameMode(onLog))
        results.add(disableGameDVR(onLog))
        results.add(disableFullscreenOptimizations(onLog))
        results.add(optimizeVisualEffects(onLog))
        results.add(setHighPerformancePower(onLog))
        
        // These require admin
        results.add(enableHAGS(onLog))
        results.add(disableSearchIndexing(onLog))
        results.add(disableSysMain(onLog))
        
        val success = results.count { it.success }
        onLog("")
        onLog("[OK] Windows optimization: $success/${results.size} applied")
        
        return results
    }
    
    private fun runReg(vararg args: String): String {
        val cmd = listOf("reg") + args.toList()
        return runCommand(cmd)
    }
    
    private fun runCmd(vararg args: String): String {
        return runCommand(args.toList())
    }
    
    private fun runCommand(cmd: List<String>): String {
        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
        
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        
        if (exitCode != 0) {
            throw RuntimeException("Command failed: ${cmd.joinToString(" ")}")
        }
        
        return output
    }
    
    // ─── Delegated to AdvancedBooster ───
    // These three implementations duplicate AdvancedBooster's richer versions.
    // To stay DRY we delegate; the API is unchanged so the Tools page is untouched.
    private val advancedBooster by lazy { AdvancedBooster() }

    fun setTimerResolution(onLog: (String) -> Unit) {
        advancedBooster.setTimerResolution(onLog)
    }

    fun disableCoreParking(onLog: (String) -> Unit) {
        advancedBooster.disableCoreParking(onLog)
    }

    fun setUltimatePowerPlan(onLog: (String) -> Unit) {
        advancedBooster.setUltimatePowerPlan(onLog)
    }
    
    /**
     * Optimize mouse settings
     */
    fun optimizeMouse(onLog: (String) -> Unit) {
        onLog("[>>] Optimizing mouse settings...")
        try {
            // Disable mouse acceleration
            runReg("add", "HKCU\\Control Panel\\Mouse", "/v", "MouseSpeed", "/t", "REG_SZ", "/d", "0", "/f")
            runReg("add", "HKCU\\Control Panel\\Mouse", "/v", "MouseThreshold1", "/t", "REG_SZ", "/d", "0", "/f")
            runReg("add", "HKCU\\Control Panel\\Mouse", "/v", "MouseThreshold2", "/t", "REG_SZ", "/d", "0", "/f")
            onLog("[OK] Mouse acceleration disabled!")
        } catch (e: Exception) {
            onLog("[!] Mouse optimization failed: ${e.message}")
        }
    }
    
    /**
     * Disable Windows Telemetry
     */
    fun disableTelemetry(onLog: (String) -> Unit): OptResult {
        onLog("[>>] Disabling Windows Telemetry...")
        return try {
            runReg("add", "HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\DataCollection", "/v", "AllowTelemetry", "/t", "REG_DWORD", "/d", "0", "/f")
            runReg("add", "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Policies\\DataCollection", "/v", "AllowTelemetry", "/t", "REG_DWORD", "/d", "0", "/f")
            runCmd("sc", "config", "DiagTrack", "start=", "disabled")
            runCmd("sc", "stop", "DiagTrack")
            onLog("[OK] Telemetry disabled!")
            OptResult(true, "Telemetry disabled")
        } catch (e: Exception) {
            onLog("[!] Telemetry disable failed: ${e.message}")
            OptResult(false, e.message ?: "Failed")
        }
    }
    
    /**
     * Optimize pagefile for gaming
     */
    fun optimizePagefile(onLog: (String) -> Unit): OptResult {
        onLog("[>>] Optimizing Pagefile for gaming...")
        return try {
            // Get total RAM
            val totalRam = Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024)
            val pagefileSize = if (totalRam >= 32) 16384 else if (totalRam >= 16) 8192 else 4096
            
            onLog("[i] Recommended pagefile: ${pagefileSize}MB")
            onLog("[i] Note: Manual pagefile configuration recommended")
            onLog("[i] System Properties > Advanced > Performance > Virtual Memory")
            onLog("[OK] Pagefile optimization info provided")
            OptResult(true, "Pagefile info provided")
        } catch (e: Exception) {
            onLog("[!] Pagefile check failed: ${e.message}")
            OptResult(false, e.message ?: "Failed")
        }
    }
    
    /**
     * Repair Windows with DISM and SFC
     */
    fun repairWindows(onLog: (String) -> Unit): OptResult {
        onLog("[>>] Starting Windows repair...")
        onLog("[i] This may take several minutes...")
        return try {
            onLog("[1/2] Running DISM...")
            val dismResult = runCmd("DISM", "/Online", "/Cleanup-Image", "/RestoreHealth")
            if (dismResult.contains("successfully")) {
                onLog("[OK] DISM completed successfully")
            } else {
                onLog("[i] DISM completed")
            }
            
            onLog("[2/2] Running SFC...")
            val sfcResult = runCmd("sfc", "/scannow")
            if (sfcResult.contains("did not find any integrity violations")) {
                onLog("[OK] No integrity violations found")
            } else {
                onLog("[i] SFC scan completed")
            }
            
            onLog("[OK] Windows repair completed!")
            OptResult(true, "Repair completed")
        } catch (e: Exception) {
            onLog("[!] Repair failed: ${e.message}")
            OptResult(false, e.message ?: "Failed")
        }
    }
}
