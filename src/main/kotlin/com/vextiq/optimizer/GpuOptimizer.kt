package com.vextiq.optimizer

import java.io.File

/**
 * GPU Optimizer - NVIDIA and AMD optimizations
 */
class GpuOptimizer {
    
    data class GpuInfo(
        val vendor: String,  // "NVIDIA", "AMD", "Intel", "Unknown"
        val name: String,
        val isNvidia: Boolean,
        val isAmd: Boolean
    )
    
    /**
     * Detect GPU vendor using OSHI (Native)
     */
    fun detectGpu(): GpuInfo {
        return try {
            val si = oshi.SystemInfo()
            val gpu = si.hardware.graphicsCards.firstOrNull { 
                !it.name.contains("Microsoft") && !it.name.contains("Basic") 
            }
            
            val name = gpu?.name ?: "Unknown GPU"
            val vendor = when {
                name.contains("NVIDIA", ignoreCase = true) || 
                name.contains("GeForce", ignoreCase = true) ||
                name.contains("RTX", ignoreCase = true) -> "NVIDIA"
                
                name.contains("AMD", ignoreCase = true) || 
                name.contains("Radeon", ignoreCase = true) -> "AMD"
                
                name.contains("Intel", ignoreCase = true) -> "Intel"
                else -> "Unknown"
            }
            
            GpuInfo(
                vendor = vendor,
                name = name,
                isNvidia = vendor == "NVIDIA",
                isAmd = vendor == "AMD"
            )
        } catch (e: Exception) {
            GpuInfo("Unknown", "Unknown", false, false)
        }
    }
    
    /**
     * Apply NVIDIA optimizations
     */
    fun optimizeNvidia(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Applying NVIDIA optimizations...")
        
        try {
            // 1. Set Persistence Mode (keeps driver loaded, lowers latency).
            // Often "Not Supported" on consumer GeForce cards, so this is best-effort
            // and never the success criterion.
            val pmResult = runCommand(listOf("nvidia-smi", "-pm", "1"))
            if (pmResult.output.contains("Enabled", ignoreCase = true)) {
                onLog("[OK] NVIDIA Persistence Mode: Enabled")
            }

            // NOTE: the old code ran `nvidia-smi -pl MAX` here. `-pl` takes a numeric
            // power limit in watts — "MAX" is rejected as invalid, so the call always
            // errored and did nothing. Power-limit tuning needs a card-specific watt
            // value, so it's dropped rather than faked.

            // 2. Registry Power mode (Maximum Performance) — HKCU, reliable.
            val reg = runReg("add", "HKCU\\Software\\NVIDIA Corporation\\Global\\NVTweak", "/v", "PowerMizer", "/t", "REG_DWORD", "/d", "1", "/f")
            if (reg.ok) {
                onLog("[OK] Power mode: Maximum Performance (Registry)")
                onLog("[OK] NVIDIA optimizations complete")
                onLog("[i] Recommended: Use 'NVIDIA Profile Inspector' for deeper tweaks")
                return true
            } else {
                onLog("[!] NVIDIA registry write failed")
                return false
            }
        } catch (e: Exception) {
            onLog("[!] NVIDIA optimization error: ${e.message}")
            return false
        }
    }

    
    /**
     * Apply AMD optimizations via registry (Modern Adrenalin Edition)
     */
    fun optimizeAmd(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Applying AMD optimizations...")
        
        try {
            // AMD Radeon Settings - Main path
            val amdRegPath = "HKCU\\Software\\AMD\\CN"
            
            val tweaks = mapOf(
                "AntiLag" to 1,
                "RadeonBoost" to 1,
                "Chill" to 0,
                "EnhancedSync" to 0,
                "TextureFilterQuality" to 1, // 0: High, 1: Perf
                "StutterFree" to 1,
                "SurfaceFormatReplacements" to 0,
                "TF_AnisotropicFiltering" to 0,
                "TF_TextureFilteringQuality" to 1
            )
            
            var ok = true
            for ((key, value) in tweaks) {
                val r = runReg("add", amdRegPath, "/v", key, "/t", if (key.contains("Quality")) "REG_SZ" else "REG_DWORD", "/d", value.toString(), "/f")
                if (!r.ok) ok = false
            }

            if (ok) {
                onLog("[OK] AMD Anti-Lag & Boost: Enabled")
                onLog("[OK] AMD Stutter-Free: Enabled")
                onLog("[OK] AMD Texture Filtering: Performance")
                onLog("[OK] AMD optimizations complete")
                return true
            } else {
                onLog("[!] AMD optimization: some registry writes failed")
                return false
            }
        } catch (e: Exception) {
            onLog("[!] AMD optimization error: ${e.message}")
            return false
        }
    }

    
    /**
     * Auto-detect and optimize GPU
     */
    fun autoOptimize(onLog: (String) -> Unit): Boolean {
        val gpu = detectGpu()
        onLog("[i] Detected GPU: ${gpu.name}")
        
        return when {
            gpu.isNvidia -> optimizeNvidia(onLog)
            gpu.isAmd -> optimizeAmd(onLog)
            else -> {
                onLog("[!] Unknown GPU vendor, applying generic optimizations")
                applyGenericOptimizations(onLog)
            }
        }
    }
    
    /**
     * Generic GPU optimizations via Windows settings
     */
    fun applyGenericOptimizations(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Applying generic GPU optimizations...")
        
        try {
            // Hardware-accelerated GPU scheduling (HKLM — needs admin + reboot)
            val hags = runReg("add", "HKLM\\SYSTEM\\CurrentControlSet\\Control\\GraphicsDrivers", "/v", "HwSchMode", "/t", "REG_DWORD", "/d", "2", "/f")
            if (hags.ok) onLog("[OK] Hardware-accelerated GPU scheduling: Enabled (reboot to apply)")
            else onLog("[!] HAGS failed (needs admin)")

            // DirectX preferences (HKCU)
            val dx = runReg("add", "HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences", "/v", "DirectXUserGlobalSettings", "/t", "REG_SZ", "/d", "VRROptimizeEnable=0;SwapEffectUpgradeEnable=1;", "/f")
            if (dx.ok) onLog("[OK] DirectX settings optimized")
            else onLog("[!] DirectX settings failed")

            return hags.ok && dx.ok
        } catch (e: Exception) {
            onLog("[!] Generic optimization error: ${e.message}")
            return false
        }
    }
    
    private data class CmdResult(val exitCode: Int, val output: String) {
        val ok: Boolean get() = exitCode == 0
    }

    private fun runReg(vararg args: String): CmdResult = runCommand(listOf("reg") + args.toList())

    private fun runCommand(cmd: List<String>): CmdResult {
        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        return CmdResult(exit, output)
    }
}
