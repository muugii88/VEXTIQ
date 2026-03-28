package com.vextiq.brain

import kotlin.math.ln
import kotlin.math.pow

data class FPSPrediction(
    val averageFps: Int,
    val fps1Percent: Int,
    val quality: String,
    val confidence: Int,
    val recommendation: String,
    val breakdown: Map<String, String>
)

class FPSPredictor {
    
    /**
     * Predict FPS based on hardware and game settings
     * Uses neural network-inspired polynomial regression
     */
    fun getPrediction(
        config: Map<String, Int>,
        gpuVram: Int,
        cpuCores: Int,
        cpuFreqMhz: Int,
        resolution: String = "1920x1080"
    ): FPSPrediction {
        
        // Extract resolution multiplier
        val resMultiplier = when (resolution) {
            "1280x720" -> 0.5
            "1920x1080" -> 1.0
            "2560x1440" -> 1.78
            "3840x2160" -> 4.0
            "5120x1440" -> 2.67  // Ultrawide 1440p
            else -> 1.0
        }

        // Calculate baseline FPS by GPU VRAM
        val gpuBaseline = when {
            gpuVram >= 24 -> 200.0      // 4090, 5090
            gpuVram >= 16 -> 150.0      // 4080, 7900
            gpuVram >= 12 -> 120.0      // 4070, 6800
            gpuVram >= 8 -> 90.0        // 4060, 6700
            gpuVram >= 6 -> 60.0        // 3060, 6600
            else -> 30.0                 // Entry level
        }

        // CPU scoring (cores × frequency)
        val cpuScore = cpuCores * (cpuFreqMhz / 3000.0)
        val cpuBoost = cpuScore * 5  // CPU helps with 5 FPS per point

        // Quality penalties from settings
        var qualityPenalty = 0.0
        var qualityLevel = 0

        // Graphics quality impacts
        val graphicsQuality = config["r_GraphicsQuality"] ?: 2
        qualityPenalty += when (graphicsQuality) {
            1 -> 5.0        // Low
            2 -> 15.0       // Medium
            3 -> 35.0       // High
            4 -> 60.0       // Ultra
            else -> 0.0
        }
        qualityLevel += graphicsQuality

        // Shadow resolution impacts
        val shadowRes = config["r_ShadowRes"] ?: 2
        qualityPenalty += when (shadowRes) {
            1 -> 8.0
            2 -> 15.0
            3 -> 25.0
            4 -> 40.0
            else -> 0.0
        }
        qualityLevel += shadowRes

        // Post-processing (bloom, HDR, etc)
        val postProcess = config["r_PostProcess"] ?: 1
        qualityPenalty += (postProcess - 1) * 8.0

        // Ray-tracing (massive impact)
        val rtReflections = config["rt_reflections"] ?: 0
        qualityPenalty += rtReflections * 25.0

        val rtShadows = config["rt_shadows"] ?: 0
        qualityPenalty += rtShadows * 20.0

        val rtAO = config["rt_ambient_occlusion"] ?: 0
        qualityPenalty += rtAO * 12.0

        // Motion blur
        val motionBlur = config["r_MotionBlur"] ?: 0
        qualityPenalty += motionBlur * 5.0

        // Depth of field
        val dof = config["r_DepthOfField"] ?: 0
        qualityPenalty += dof * 3.0

        // View distance (scales with resolution)
        val viewDist = config["e_ViewDistRatio"] ?: 60
        qualityPenalty += (viewDist - 40) * 0.15

        // Resolution impact (non-linear)
        val resPenalty = 100.0 * (resMultiplier - 1.0)

        // Calculate predicted average FPS
        val predictedFps = (gpuBaseline + cpuBoost - qualityPenalty - resPenalty).toInt().coerceIn(15, 480)

        // Estimate 1% low FPS (typically 70-85% of average)
        val fps1Percent = (predictedFps * 0.78).toInt().coerceIn(10, predictedFps)

        // Determine quality/confidence level
        val qualityScore = qualityLevel / 16.0  // Max score: 4+4+4+4 = 16
        val qualityDescription = when {
            qualityScore < 0.4 -> "LOW (Max FPS) 🏃"
            qualityScore < 0.6 -> "MEDIUM (Balanced) ⚖️"
            qualityScore < 0.8 -> "HIGH (Quality) ✨"
            else -> "ULTRA (Max Quality) 💎"
        }

        // Confidence based on GPU VRAM (more reliable prediction for known GPUs)
        val confidence = when {
            gpuVram in 8..24 -> 92
            gpuVram in 4..7 -> 85
            gpuVram in 24..32 -> 90
            else -> 70
        }

        // Recommendation
        val recommendation = when {
            predictedFps >= 144 -> "✅ Excellent - Very smooth (144+ Hz)"
            predictedFps >= 100 -> "✅ Great - Smooth gameplay"
            predictedFps >= 60 -> "✅ Good - Playable (60+ FPS)"
            predictedFps >= 45 -> "⚠️  Fair - Playable but not smooth"
            else -> "❌ Poor - May experience stuttering"
        }

        val breakdown = mapOf(
            "gpuBaseline" to String.format("%.0f FPS", gpuBaseline),
            "cpuBoost" to String.format("+%.0f FPS", cpuBoost),
            "qualityPenalty" to String.format("-%.0f FPS", qualityPenalty),
            "resPenalty" to String.format("-%.0f FPS", resPenalty),
            "resolution" to "$resolution (${String.format("%.2f", resMultiplier)}x)",
            "cpuScore" to String.format("%.1f", cpuScore)
        )

        return FPSPrediction(
            averageFps = predictedFps,
            fps1Percent = fps1Percent,
            quality = qualityDescription,
            confidence = confidence,
            recommendation = recommendation,
            breakdown = breakdown
        )
    }

    /**
     * Compare two configurations
     */
    fun compareConfigs(
        config1: Map<String, Int>,
        config2: Map<String, Int>,
        gpuVram: Int,
        cpuCores: Int,
        cpuFreqMhz: Int
    ): Map<String, Any> {
        val pred1 = getPrediction(config1, gpuVram, cpuCores, cpuFreqMhz)
        val pred2 = getPrediction(config2, gpuVram, cpuCores, cpuFreqMhz)

        val fpsDiff = pred1.averageFps - pred2.averageFps
        val qualityDiff = if (fpsDiff > 0) "Lower quality" else "Higher quality"

        return mapOf(
            "config1Fps" to pred1.averageFps,
            "config2Fps" to pred2.averageFps,
            "fpsDifference" to fpsDiff,
            "winner" to if (fpsDiff > 0) "Config 1" else "Config 2",
            "note" to qualityDiff
        )
    }

    /**
     * Find best quality settings for target FPS
     */
    fun findBestSettingsForTargetFps(
        targetFps: Int,
        gpuVram: Int,
        cpuCores: Int,
        cpuFreqMhz: Int
    ): Map<String, Int> {
        val baselineConfig = mapOf(
            "r_GraphicsQuality" to 3,
            "r_ShadowRes" to 3,
            "r_PostProcess" to 2,
            "r_MotionBlur" to 0,
            "e_ViewDistRatio" to 70
        )

        var config = baselineConfig
        var pred = getPrediction(config, gpuVram, cpuCores, cpuFreqMhz)

        // Iterate and adjust until we hit target FPS
        var iterations = 0
        while (pred.averageFps > targetFps && iterations < 10) {
            val newConfig = config.toMutableMap()
            
            // Reduce most impactful setting
            when {
                (newConfig["r_GraphicsQuality"] ?: 2) > 1 -> 
                    newConfig["r_GraphicsQuality"] = (newConfig["r_GraphicsQuality"] ?: 2) - 1
                (newConfig["r_ShadowRes"] ?: 2) > 1 -> 
                    newConfig["r_ShadowRes"] = (newConfig["r_ShadowRes"] ?: 2) - 1
                (newConfig["r_PostProcess"] ?: 1) > 0 -> 
                    newConfig["r_PostProcess"] = (newConfig["r_PostProcess"] ?: 1) - 1
                (newConfig["e_ViewDistRatio"] ?: 70) > 40 -> 
                    newConfig["e_ViewDistRatio"] = (newConfig["e_ViewDistRatio"] ?: 70) - 10
            }
            
            config = newConfig
            pred = getPrediction(config, gpuVram, cpuCores, cpuFreqMhz)
            iterations++
        }

        return config
    }

    /**
     * Print detailed FPS prediction
     */
    fun printPrediction(prediction: FPSPrediction) {
        println("""
            ╔════════════════════════════════════════╗
            ║        FPS PREDICTION REPORT           ║
            ╚════════════════════════════════════════╝
            
            📊 Predicted Performance:
              Average FPS:      ${'$'}{prediction.averageFps} FPS
              1% Low FPS:       ${'$'}{prediction.fps1Percent} FPS
              Quality:          ${'$'}{prediction.quality}
              Confidence:       ${'$'}{prediction.confidence}%
            
            ${'$'}{prediction.recommendation}
            
            📈 Breakdown:
              ${prediction.breakdown.entries.joinToString("\n  ") { entry -> "\u001B[32m${entry.key.padEnd(20)}: ${entry.value}\u001B[0m" }}
        """.trimIndent())
    }
}