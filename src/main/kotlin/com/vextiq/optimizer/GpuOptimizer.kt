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
            // 1. Set Persistence Mode (keeps driver loaded, lowers latency)
            val pmResult = runCommand(listOf("nvidia-smi", "-pm", "1"))
            if (pmResult.contains("Enabled", ignoreCase = true)) {
                onLog("[OK] NVIDIA Persistence Mode: Enabled")
            }
            
            // 2. Set Performance Mode (Max Clocks) - note: requires admin
            runCommand(listOf("nvidia-smi", "-pl", "MAX")) // Max Power Limit
            
            // 3. Registry Fallbacks (Legacy)
            runReg("add", "HKCU\\Software\\NVIDIA Corporation\\Global\\NVTweak", "/v", "PowerMizer", "/t", "REG_DWORD", "/d", "1", "/f")
            onLog("[OK] Power mode: Maximum Performance (Registry)")
            
            onLog("[OK] NVIDIA optimizations complete")
            onLog("[i] Recommended: Use 'NVIDIA Profile Inspector' for deeper tweaks")
            return true
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
            
            for ((key, value) in tweaks) {
                runReg("add", amdRegPath, "/v", key, "/t", if (key.contains("Quality")) "REG_SZ" else "REG_DWORD", "/d", value.toString(), "/f")
            }
            
            onLog("[OK] AMD Anti-Lag & Boost: Enabled")
            onLog("[OK] AMD Stutter-Free: Enabled")
            onLog("[OK] AMD Texture Filtering: Performance")
            
            onLog("[OK] AMD optimizations complete")
            return true
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
            // Hardware-accelerated GPU scheduling
            runReg("add", "HKLM\\SYSTEM\\CurrentControlSet\\Control\\GraphicsDrivers", "/v", "HwSchMode", "/t", "REG_DWORD", "/d", "2", "/f")
            onLog("[OK] Hardware-accelerated GPU scheduling: Enabled")
            
            // Disable GPU power saving
            runReg("add", "HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences", "/v", "DirectXUserGlobalSettings", "/t", "REG_SZ", "/d", "VRROptimizeEnable=0;SwapEffectUpgradeEnable=1;", "/f")
            onLog("[OK] DirectX settings optimized")
            
            return true
        } catch (e: Exception) {
            onLog("[!] Generic optimization error: ${e.message}")
            return false
        }
    }
    
    private fun runReg(vararg args: String): String {
        val cmd = listOf("reg") + args.toList()
        return runCommand(cmd)
    }
    
    private fun runCommand(cmd: List<String>): String {
        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
        
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output
    }
}
