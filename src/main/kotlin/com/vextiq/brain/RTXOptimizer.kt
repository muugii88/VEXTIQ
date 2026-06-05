package com.vextiq.brain

/**
 * RTXOptimizer - GPU Feature Optimizer
 * 
 * Detects GPU capabilities (RTX, DLSS, FSR, Ray Tracing)
 * and provides optimal settings based on hardware tier.
 */
class RTXOptimizer {

    data class GpuCapabilities(
        val vendor: String = "Unknown",        // NVIDIA, AMD, Intel
        val supportsRayTracing: Boolean = false,
        val supportsDLSS: Boolean = false,      // NVIDIA DLSS
        val supportsFSR: Boolean = false,       // AMD FSR (works on all GPUs)
        val supportsFrameGen: Boolean = false,   // DLSS 3 Frame Gen (RTX 40+)
        val supportsReflex: Boolean = false,     // NVIDIA Reflex
        val supportsAntiLag: Boolean = false,    // AMD Anti-Lag
        val rtTier: String = "none",            // none, basic, full, ultra
        val dlssVersion: String = "none",       // none, 2.0, 3.0, 3.5
        val gpuGeneration: Int = 0              // 10, 20, 30, 40, 50 for NVIDIA
    )

    data class RTXRecommendation(
        val rayTracing: Boolean,
        val rtQuality: String,          // Off, Low, Medium, High, Ultra
        val upscaler: String,           // None, DLSS, FSR, XeSS
        val upscalerQuality: String,    // Quality, Balanced, Performance, Ultra Performance
        val frameGeneration: Boolean,
        val reflexOrAntiLag: Boolean,
        val estimatedFpsImpact: String, // e.g., "+30% FPS with DLSS"
        val notes: List<String>
    )

    /**
     * Detect GPU capabilities from hardware info
     */
    fun detectCapabilities(gpuName: String, gpuVendor: String, gpuVramGB: Int): GpuCapabilities {
        val name = gpuName.uppercase()
        
        return when (gpuVendor.uppercase()) {
            "NVIDIA" -> detectNvidiaCapabilities(name, gpuVramGB)
            "AMD" -> detectAmdCapabilities(name, gpuVramGB)
            "INTEL" -> detectIntelCapabilities(name)
            else -> GpuCapabilities(vendor = gpuVendor, supportsFSR = true) // FSR works on anything
        }
    }

    /**
     * Get optimal RT/DLSS recommendation for a game
     */
    fun getRecommendation(
        capabilities: GpuCapabilities,
        gpuTier: String,
        isCompetitive: Boolean = false
    ): RTXRecommendation {
        
        // Competitive mode: disable all eye candy, max FPS
        if (isCompetitive) {
            return RTXRecommendation(
                rayTracing = false,
                rtQuality = "Off",
                upscaler = if (capabilities.supportsDLSS) "DLSS" else if (capabilities.supportsFSR) "FSR" else "None",
                upscalerQuality = "Performance",
                frameGeneration = false, // Adds latency
                reflexOrAntiLag = true,  // Always on for competitive
                estimatedFpsImpact = "Max FPS mode - all RT disabled",
                notes = listOf(
                    "✅ NVIDIA Reflex/AMD Anti-Lag: ON (lower input lag)",
                    "❌ Ray Tracing: OFF (competitive priority)",
                    "❌ Frame Generation: OFF (adds latency)"
                )
            )
        }
        
        // Quality mode based on GPU tier
        return when (gpuTier) {
            "ultra" -> RTXRecommendation(
                rayTracing = capabilities.supportsRayTracing,
                rtQuality = if (capabilities.rtTier == "ultra") "Ultra" else "High",
                upscaler = when {
                    capabilities.supportsDLSS -> "DLSS"
                    capabilities.supportsFSR -> "FSR"
                    else -> "None"
                },
                upscalerQuality = "Quality",
                frameGeneration = capabilities.supportsFrameGen,
                reflexOrAntiLag = true,
                estimatedFpsImpact = when {
                    capabilities.supportsFrameGen -> "+80-120% FPS with DLSS 3 Frame Gen"
                    capabilities.supportsDLSS -> "+40-60% FPS with DLSS Quality"
                    else -> "Native resolution"
                },
                notes = buildList {
                    add("✅ Ray Tracing: ${if (capabilities.supportsRayTracing) "ON (Ultra tier GPU)" else "OFF"}")
                    if (capabilities.supportsFrameGen) add("✅ DLSS 3 Frame Gen: ON (+80% FPS)")
                    if (capabilities.supportsDLSS) add("✅ DLSS: Quality mode")
                    add("✅ Your GPU can handle max settings with RT")
                }
            )
            "high" -> RTXRecommendation(
                rayTracing = capabilities.supportsRayTracing && capabilities.rtTier != "basic",
                rtQuality = if (capabilities.supportsRayTracing) "Medium" else "Off",
                upscaler = when {
                    capabilities.supportsDLSS -> "DLSS"
                    capabilities.supportsFSR -> "FSR"
                    else -> "None"
                },
                upscalerQuality = "Balanced",
                frameGeneration = capabilities.supportsFrameGen,
                reflexOrAntiLag = true,
                estimatedFpsImpact = when {
                    capabilities.supportsDLSS -> "+40-50% FPS with DLSS Balanced"
                    capabilities.supportsFSR -> "+30-40% FPS with FSR Balanced"
                    else -> "Native resolution"
                },
                notes = buildList {
                    add("✅ RT Shadows only (RT Reflections too heavy)")
                    if (capabilities.supportsDLSS) add("✅ DLSS: Balanced mode recommended")
                    else if (capabilities.supportsFSR) add("✅ FSR: Balanced mode recommended")
                }
            )
            "medium" -> RTXRecommendation(
                rayTracing = false,
                rtQuality = "Off",
                upscaler = when {
                    capabilities.supportsDLSS -> "DLSS"
                    capabilities.supportsFSR -> "FSR"
                    else -> "None"
                },
                upscalerQuality = "Performance",
                frameGeneration = capabilities.supportsFrameGen,
                reflexOrAntiLag = true,
                estimatedFpsImpact = when {
                    capabilities.supportsDLSS -> "+50-70% FPS with DLSS Performance"
                    capabilities.supportsFSR -> "+40-60% FPS with FSR Performance"
                    else -> "Consider lowering resolution"
                },
                notes = listOf(
                    "❌ Ray Tracing: OFF (too demanding for this tier)",
                    "✅ Upscaler: Performance mode for smooth gameplay",
                    "✅ Focus on stable 60+ FPS over visual quality"
                )
            )
            else -> RTXRecommendation(
                rayTracing = false,
                rtQuality = "Off",
                upscaler = if (capabilities.supportsFSR) "FSR" else "None",
                upscalerQuality = "Ultra Performance",
                frameGeneration = false,
                reflexOrAntiLag = capabilities.supportsReflex || capabilities.supportsAntiLag,
                estimatedFpsImpact = if (capabilities.supportsFSR) "+60-80% FPS with FSR Ultra Perf" else "Lower resolution recommended",
                notes = listOf(
                    "❌ Ray Tracing: OFF",
                    "✅ FSR Ultra Performance if available",
                    "✅ Lower all settings to Medium/Low",
                    "✅ Consider 720p or 900p for playable FPS"
                )
            )
        }
    }

    /**
     * Log recommendation to optimizer log
     */
    fun logRecommendation(recommendation: RTXRecommendation, onLog: (String) -> Unit) {
        onLog("[RTX] Upscaler: ${recommendation.upscaler} (${recommendation.upscalerQuality})")
        onLog("[RTX] Ray Tracing: ${recommendation.rtQuality}")
        if (recommendation.frameGeneration) onLog("[RTX] Frame Generation: ON")
        if (recommendation.reflexOrAntiLag) onLog("[RTX] Low Latency Mode: ON")
        onLog("[RTX] ${recommendation.estimatedFpsImpact}")
        recommendation.notes.forEach { onLog("  $it") }
    }

    // ═══════════════════════════════════════════════════════════
    // VENDOR-SPECIFIC DETECTION
    // ═══════════════════════════════════════════════════════════

    private fun detectNvidiaCapabilities(name: String, vramGB: Int): GpuCapabilities {
        val gen = when {
            name.contains("50") && (name.contains("RTX") || name.contains("5090") || name.contains("5080") || name.contains("5070") || name.contains("5060")) -> 50
            name.contains("40") && (name.contains("RTX") || name.contains("4090") || name.contains("4080") || name.contains("4070") || name.contains("4060")) -> 40
            name.contains("30") && (name.contains("RTX") || name.contains("3090") || name.contains("3080") || name.contains("3070") || name.contains("3060") || name.contains("3050")) -> 30
            name.contains("20") && name.contains("RTX") -> 20
            name.contains("16") -> 16
            name.contains("10") -> 10
            else -> 0
        }

        return GpuCapabilities(
            vendor = "NVIDIA",
            supportsRayTracing = gen >= 20,
            supportsDLSS = gen >= 20,
            supportsFSR = true, // FSR works on all GPUs
            supportsFrameGen = gen >= 40, // DLSS 3 Frame Gen = RTX 40+
            supportsReflex = gen >= 20,
            supportsAntiLag = false,
            rtTier = when {
                gen >= 40 && vramGB >= 12 -> "ultra"
                gen >= 30 && vramGB >= 8 -> "full"
                gen >= 20 -> "basic"
                else -> "none"
            },
            dlssVersion = when {
                gen >= 40 -> "3.5"
                gen >= 30 -> "2.0"
                gen >= 20 -> "2.0"
                else -> "none"
            },
            gpuGeneration = gen
        )
    }

    private fun detectAmdCapabilities(name: String, vramGB: Int): GpuCapabilities {
        val hasRT = name.contains("6") || name.contains("7") // RX 6000+ has RT
        val isRDNA3 = name.contains("7") // RX 7000 series

        return GpuCapabilities(
            vendor = "AMD",
            supportsRayTracing = hasRT,
            supportsDLSS = false,
            supportsFSR = true,
            supportsFrameGen = isRDNA3, // FSR 3 Frame Gen on RDNA3
            supportsReflex = false,
            supportsAntiLag = true,
            rtTier = when {
                isRDNA3 && vramGB >= 16 -> "full"
                hasRT -> "basic"
                else -> "none"
            },
            dlssVersion = "none",
            gpuGeneration = if (isRDNA3) 3 else if (hasRT) 2 else 1
        )
    }

    private fun detectIntelCapabilities(name: String): GpuCapabilities {
        val isArc = name.contains("ARC") || name.contains("A7") || name.contains("A5") || name.contains("A3")

        return GpuCapabilities(
            vendor = "Intel",
            supportsRayTracing = isArc,
            supportsDLSS = false,
            supportsFSR = true,
            supportsFrameGen = false,
            supportsReflex = false,
            supportsAntiLag = false,
            rtTier = if (isArc) "basic" else "none",
            dlssVersion = "none",
            gpuGeneration = if (isArc) 1 else 0
        )
    }
}
