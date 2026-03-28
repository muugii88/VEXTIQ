package com.vextiq.optimizer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * VEXTIQ BRAIN v13.0 - Advanced Multi-Game Optimizer
 * 
 * GPU Tier-based optimization:
 * - ULTRA: 4090, 5090, 7900 XTX (RTX On, Max Quality)
 * - HIGH: 4080, 4070, 6800, 7800 (High/Very High)
 * - MEDIUM: 4060, 3060, 6600, 7600 (Medium-High)
 * - LOW: 1060, 1650, 5500 (Low-Medium, Max FPS)
 * 
 * Supported Games:
 * - Star Citizen (USER.cfg)
 * - Cyberpunk 2077 (UserSettings.json hints)
 * - Battlefield 2042/6 (PROFSAVE_profile)
 * - And more...
 */
class VextiqBrain {
    
    // ═══════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════
    
    data class HardwareProfile(
        val cpuName: String = "Unknown",
        val cpuCores: Int = 8,
        val cpuThreads: Int = 16,
        val cpuFreqMHz: Int = 3600,
        val gpuName: String = "Unknown",
        val gpuVendor: String = "Unknown",
        val gpuVramMB: Int = 8192,
        val gpuVramGB: Int = 8,
        val ramGB: Int = 16,
        val ramSpeedMHz: Int = 3200,
        val resWidth: Int = 1920,
        val resHeight: Int = 1080,
        val refreshRate: Int = 60
    )
    
    data class OptimizationResult(
        val config: Map<String, Any>,
        val configString: String,
        val estimatedFps: Int,
        val score: Double,
        val iterations: Int,
        val profile: String,
        val playstyle: String,
        val gpuTier: String
    )
    
    // ═══════════════════════════════════════════════════════════
    // GPU TIER DETECTION
    // ═══════════════════════════════════════════════════════════
    
    private val gpuTiers = mapOf(
        "ultra" to listOf(
            "5090", "4090", "4080 SUPER", "4080",
            "7900 XTX", "7900 XT", "6950 XT", "6900 XT",
            "3090 TI", "3090", "TITAN"
        ),
        "high" to listOf(
            "5080", "5070 TI", "4070 TI SUPER", "4070 TI", "4070 SUPER", "4070",
            "7900 GRE", "7800 XT", "6800 XT", "6800",
            "3080 TI", "3080", "3070 TI", "3070", "2080 TI", "2080 SUPER", "2080"
        ),
        "medium" to listOf(
            "5070", "5060", "4060 TI", "4060",
            "7700 XT", "7600 XT", "7600", "6750 XT", "6700 XT", "6700", "6650 XT", "6600 XT", "6600",
            "3060 TI", "3060", "2070 SUPER", "2070", "2060 SUPER", "2060",
            "1660 TI", "1660 SUPER", "1660"
        ),
        "low" to listOf(
            "6500 XT", "6400", "5500 XT",
            "3050", "2050", "1650 SUPER", "1650", "1050 TI", "1050",
            "1080", "1070", "1060"
        )
    )
    
    // ═══════════════════════════════════════════════════════════
    // BRANT SEARCH SPACE
    // ═══════════════════════════════════════════════════════════
    
    private val searchSpace = mapOf(
        "r_TextureMipBias" to Triple(-3, 0, 1),
        "r_ShadowRes" to Triple(1, 4, 1),
        "r_GraphicsQuality" to Triple(1, 4, 1),
        "r_VSync" to Triple(0, 1, 1),
        "r_MotionBlur" to Triple(0, 1, 1),
        "r_DepthOfField" to Triple(0, 1, 1),
        "r_PostProcess" to Triple(1, 4, 1),
        "r_TerrainAO" to Triple(0, 1, 1),
        "e_ViewDistRatio" to Triple(40, 100, 10),
        "e_ViewDistRatioVegetation" to Triple(30, 90, 10)
    )
    
    private val costWeights = mapOf(
        "r_TextureMipBias" to 2.0,
        "r_ShadowRes" to 1.5,
        "r_GraphicsQuality" to 2.5,
        "r_VSync" to 0.5,
        "r_MotionBlur" to 0.2,
        "r_DepthOfField" to 0.2,
        "r_PostProcess" to 1.0,
        "r_TerrainAO" to 0.8,
        "e_ViewDistRatio" to 1.2,
        "e_ViewDistRatioVegetation" to 1.2
    )
    
    // ═══════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════
    
    private var hw: HardwareProfile = HardwareProfile()
    private var bestScore = -1.0
    private var bestConfig = mutableMapOf<String, Int>()
    private var iterations = 0
    
    // ═══════════════════════════════════════════════════════════
    // GPU TIER & SCORE
    // ═══════════════════════════════════════════════════════════
    
    fun getGpuTier(): String {
        val gpuUpper = hw.gpuName.uppercase()
        for ((tier, gpus) in gpuTiers) {
            for (gpu in gpus) {
                if (gpuUpper.contains(gpu.uppercase())) return tier
            }
        }
        // Fallback by VRAM
        return when {
            hw.gpuVramGB >= 16 -> "ultra"
            hw.gpuVramGB >= 10 -> "high"
            hw.gpuVramGB >= 6 -> "medium"
            else -> "low"
        }
    }
    
    fun calculateHardwareScore(): Int {
        var score = 0
        // CPU (max 35)
        score += min(hw.cpuCores * 2, 16)
        score += min(hw.cpuThreads, 12)
        score += min((hw.cpuFreqMHz - 2000) / 100, 7)
        // GPU (max 45)
        score += min(hw.gpuVramGB * 3, 36)
        score += when (getGpuTier()) { "ultra" -> 9; "high" -> 6; "medium" -> 3; else -> 0 }
        // RAM (max 20)
        score += min(hw.ramGB, 16)
        score += min((hw.ramSpeedMHz - 2400) / 200, 4)
        return score.coerceIn(0, 100)
    }
    
    // ═══════════════════════════════════════════════════════════
    // BRANT ALGORITHM
    // ═══════════════════════════════════════════════════════════
    
    private fun calculateCost(config: Map<String, Int>): Double {
        var cost = 0.0
        for ((key, value) in config) {
            val w = costWeights[key] ?: 1.0
            cost += if (key == "r_TextureMipBias") abs(value) * w else value * w
        }
        if (hw.gpuVramMB < 8192) cost *= 1.3
        return cost
    }
    
    private fun estimateFps(config: Map<String, Int>): Double {
        val cost = calculateCost(config)
        val baseFps = (hw.gpuVramMB / 1024.0) * 7 + (hw.cpuCores * 2)
        return max(15.0, baseFps - cost * 1.1)
    }
    
    private fun branchRecursive(current: MutableMap<String, Int>, depth: Int, keys: List<String>, targetFps: Int) {
        iterations++
        if (depth == keys.size) {
            val fps = estimateFps(current)
            val penalty = if (fps < targetFps) (targetFps - fps) * 2 else 0.0
            val score = fps - penalty
            if (score > bestScore) {
                bestScore = score
                bestConfig = current.toMutableMap()
            }
            return
        }
        
        val key = keys[depth]
        val (minV, maxV, step) = searchSpace.getValue(key)
        for (v in minV..maxV step step) {
            current[key] = v
            // Pruning
            val temp = current.toMutableMap()
            for (k in keys.drop(depth + 1)) {
                val (kMin, _, _) = searchSpace.getValue(k)
                temp[k] = if (k == "r_TextureMipBias") 0 else kMin
            }
            if (estimateFps(temp) >= targetFps * 0.2) {
                branchRecursive(current, depth + 1, keys, targetFps)
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // TIER-BASED PRESETS
    // ═══════════════════════════════════════════════════════════
    
    private fun getTierPreset(tier: String, playstyle: String): Map<String, Any> {
        val vg = hw.gpuVramGB
        val isCompetitive = playstyle in listOf("competitive", "pvp", "high_fps", "combat", "raid", "visibility")
        val isQuality = playstyle in listOf("quality", "exploration", "immersion", "stealth")
        
        return when (tier) {
            "ultra" -> mapOf(
                "textureQuality" to if (isCompetitive) "high" else "ultra",
                "shadowQuality" to if (isCompetitive) "high" else "ultra",
                "effectsQuality" to if (isCompetitive) "high" else "ultra",
                "viewDistance" to if (isCompetitive) "high" else "ultra",
                "antiAliasing" to if (isCompetitive) "TAA" else "DLAA",
                "rayTracing" to if (isQuality) "ultra" else "off",
                "ambientOcclusion" to if (isCompetitive) "SSAO" else "RTAO",
                "motionBlur" to if (isCompetitive) 0 else 1,
                "depthOfField" to if (isCompetitive) 0 else 1,
                "filmGrain" to 0,
                "vsync" to 0,
                "targetFps" to if (isCompetitive) min(hw.refreshRate, 240) else min(hw.refreshRate, 120),
                "shadowPoolSize" to 4096,
                "texturePoolSize" to min(vg * 512, 8192),
                "volumetricFog" to if (isCompetitive) 0 else 1
            )
            "high" -> mapOf(
                "textureQuality" to if (isCompetitive) "medium" else "high",
                "shadowQuality" to if (isCompetitive) "medium" else "high",
                "effectsQuality" to if (isCompetitive) "medium" else "high",
                "viewDistance" to if (isCompetitive) "medium" else "high",
                "antiAliasing" to "TAA",
                "rayTracing" to "off",
                "ambientOcclusion" to if (isCompetitive) "off" else "SSAO",
                "motionBlur" to 0,
                "depthOfField" to if (isQuality) 1 else 0,
                "filmGrain" to 0,
                "vsync" to 0,
                "targetFps" to if (isCompetitive) min(hw.refreshRate, 200) else min(hw.refreshRate, 120),
                "shadowPoolSize" to 3072,
                "texturePoolSize" to min(vg * 512, 6144),
                "volumetricFog" to if (isQuality) 1 else 0
            )
            "medium" -> mapOf(
                "textureQuality" to if (isCompetitive) "low" else "medium",
                "shadowQuality" to if (isCompetitive) "low" else "medium",
                "effectsQuality" to if (isCompetitive) "low" else "medium",
                "viewDistance" to if (isCompetitive) "low" else "medium",
                "antiAliasing" to if (isCompetitive) "FXAA" else "TAA",
                "rayTracing" to "off",
                "ambientOcclusion" to "off",
                "motionBlur" to 0,
                "depthOfField" to 0,
                "filmGrain" to 0,
                "vsync" to 0,
                "targetFps" to if (isCompetitive) min(hw.refreshRate, 165) else min(hw.refreshRate, 90),
                "shadowPoolSize" to 2048,
                "texturePoolSize" to min(vg * 512, 4096),
                "volumetricFog" to 0
            )
            else -> mapOf( // low
                "textureQuality" to "low",
                "shadowQuality" to "low",
                "effectsQuality" to "low",
                "viewDistance" to "low",
                "antiAliasing" to "off",
                "rayTracing" to "off",
                "ambientOcclusion" to "off",
                "motionBlur" to 0,
                "depthOfField" to 0,
                "filmGrain" to 0,
                "vsync" to 0,
                "targetFps" to min(hw.refreshRate, 120),
                "shadowPoolSize" to 1024,
                "texturePoolSize" to min(vg * 512, 2048),
                "volumetricFog" to 0
            )
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // GAME-SPECIFIC CFG GENERATORS
    // ═══════════════════════════════════════════════════════════
    
    private fun generateStarCitizenCfg(tier: String, playstyle: String): String {
        val preset = getTierPreset(tier, playstyle)
        val vg = hw.gpuVramGB
        val isCompetitive = playstyle in listOf("pvp", "fps_bunker", "competitive")
        
        // Star Citizen specific values
        val renderer = if (hw.gpuVendor == "AMD") "vulkan" else "dx11"
        val maxFps = preset["targetFps"] as Int
        val texPool = (vg * 512).coerceIn(2048, 8192)
        val shadowPool = when (tier) {
            "ultra" -> 4096; "high" -> 3072; "medium" -> 2048; else -> 1536
        }
        val viewDist = when {
            isCompetitive -> 0.5
            playstyle == "exploration" -> 1.0
            tier == "ultra" -> 0.9
            tier == "high" -> 0.7
            else -> 0.55
        }
        val vegDist = when {
            isCompetitive -> 0.3
            tier == "ultra" -> 0.8
            tier == "high" -> 0.6
            else -> 0.4
        }
        val volFog = if (isCompetitive || tier == "low") 0 else 1
        val particles = when {
            isCompetitive -> 1
            tier in listOf("ultra", "high") -> 2
            else -> 1
        }
        
        // CPU threading
        val workers = min(hw.cpuThreads - 2, 16)
        val mainCpu = 0
        val physicsCpu = min(hw.cpuCores - 1, 7)
        val streamCpu = min(hw.cpuCores, 8)
        
        return """
; ═══════════════════════════════════════════════════════════
; VEXTIQ BRAIN v13.0 — Star Citizen USER.cfg
; GPU Tier: ${tier.uppercase()} | Playstyle: ${playstyle.uppercase()}
; Generated: ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}
;
; Hardware:
; CPU: ${hw.cpuName} (${hw.cpuCores}C/${hw.cpuThreads}T)
; GPU: ${hw.gpuName} (${hw.gpuVramGB}GB VRAM)
; RAM: ${hw.ramGB}GB @ ${hw.ramSpeedMHz}MHz
; Display: ${hw.resWidth}x${hw.resHeight} @ ${hw.refreshRate}Hz
; ═══════════════════════════════════════════════════════════

; ── Engine ──
Con_Restricted = 0
r.graphicsRenderer = $renderer

; ── FPS Control ──
sys_MaxFPS = $maxFps
sys_maxIdleFPS = 30
r_VSync = 0

; ── Post-Processing (Disabled for clarity) ──
r_MotionBlur = 0
r_DepthOfField = ${if (playstyle == "exploration") 1 else 0}
r_filmgrain = 0
r_vignetteBlur = 0
r_ChromaticAberration = 0
r_Glow = ${if (tier in listOf("ultra", "high") && !isCompetitive) 1 else 0}

; ── VRAM Management (${vg}GB GPU) ──
r_TexturesStreamPoolSize = $texPool
r_TexturesStreamingUpdatePoolSize = ${texPool / 4}

; ── Stability ──
r_ShadersAsyncCompile = 1
r_ssdo = ${if (tier == "ultra" && !isCompetitive) 1 else 0}
r_WindowMode = 2

; ── Quality (Tier: ${tier.uppercase()}) ──
r_ShadowsPoolSize = $shadowPool
e_VolumetricFog = $volFog
e_ViewDistRatio = $viewDist
e_ViewDistRatioVegetation = $vegDist
e_ParticlesQuality = $particles

; ── CPU Threading (${hw.cpuThreads}T) ──
sys_job_system_max_worker = $workers
sys_main_CPU = $mainCpu
sys_physics_CPU = $physicsCpu
sys_streaming_CPU = $streamCpu

; ── Resolution ──
r_Width = ${hw.resWidth}
r_Height = ${hw.resHeight}

; ═══════════════════════════════════════════════════════════
; VEXTIQ Score: ${calculateHardwareScore()}/100 | Est. FPS: ~${estimateFps(bestConfig).toInt()}
; ═══════════════════════════════════════════════════════════
""".trimIndent()
    }
    
    private fun generateCyberpunkCfg(tier: String, playstyle: String): String {
        val vg = hw.gpuVramGB
        val isQuality = playstyle in listOf("quality", "exploration")
        val isPerf = playstyle in listOf("high_fps", "competitive")
        
        // Cyberpunk specific
        val dlss = when {
            hw.gpuVendor == "NVIDIA" && tier == "ultra" && isQuality -> "DLAA"
            hw.gpuVendor == "NVIDIA" && tier == "ultra" -> "Quality"
            hw.gpuVendor == "NVIDIA" && tier == "high" -> "Balanced"
            hw.gpuVendor == "NVIDIA" -> "Performance"
            hw.gpuVendor == "AMD" && tier in listOf("ultra", "high") -> "FSR2_Quality"
            hw.gpuVendor == "AMD" -> "FSR2_Balanced"
            else -> "off"
        }
        val rayTracing = when {
            tier == "ultra" && isQuality && hw.gpuVendor == "NVIDIA" -> "Psycho"
            tier == "ultra" && hw.gpuVendor == "NVIDIA" -> "Ultra"
            tier == "high" && isQuality && hw.gpuVendor == "NVIDIA" -> "Medium"
            else -> "Off"
        }
        val crowdDensity = when {
            isPerf -> "Low"
            tier == "low" -> "Low"
            tier == "medium" -> "Medium"
            else -> "High"
        }
        val preset = getTierPreset(tier, playstyle)
        
        return """
; ═══════════════════════════════════════════════════════════
; VEXTIQ BRAIN v13.0 — Cyberpunk 2077 Settings Guide
; GPU Tier: ${tier.uppercase()} | Playstyle: ${playstyle.uppercase()}
;
; Hardware: ${hw.gpuName} (${hw.gpuVramGB}GB) | ${hw.cpuName}
; ═══════════════════════════════════════════════════════════
;
; RECOMMENDED IN-GAME SETTINGS:
;
; [Graphics]
; Quick Preset: ${if (tier == "ultra") "Ray Tracing: Ultra" else if (tier == "high") "High" else if (tier == "medium") "Medium" else "Low"}
;
; [Advanced]
; Texture Quality: ${preset["textureQuality"]}
; Shadow Quality: ${if (isPerf) "Medium" else preset["shadowQuality"]}
; Ambient Occlusion: ${if (isPerf) "Low" else if (tier in listOf("ultra", "high")) "High" else "Medium"}
; Volumetric Fog: ${if (isPerf) "Medium" else if (tier == "ultra") "Ultra" else "High"}
; Screen Space Reflections: ${if (tier == "ultra" && !isPerf) "Psycho" else if (tier == "high") "High" else "Medium"}
; Level of Detail: ${if (isPerf) "Medium" else "High"}
; Crowd Density: $crowdDensity
;
; [Ray Tracing] (NVIDIA Only)
; Ray Tracing: $rayTracing
; Path Tracing: ${if (tier == "ultra" && isQuality && vg >= 12) "On" else "Off"}
;
; [Upscaling]
; DLSS/FSR: $dlss
; Frame Generation: ${if (hw.gpuName.contains("40") && tier in listOf("ultra", "high")) "On" else "Off"}
;
; [Display]
; Max FPS: ${preset["targetFps"]}
; VSync: Off
;
; Motion Blur: ${if (isPerf) "Off" else "Low"}
; Film Grain: Off
; Chromatic Aberration: Off
; Depth of Field: ${if (isQuality) "On" else "Off"}
;
; ═══════════════════════════════════════════════════════════
; Expected FPS: ${if (tier == "ultra") "60-90" else if (tier == "high") "70-100" else if (tier == "medium") "60-80" else "45-60"}
; ═══════════════════════════════════════════════════════════
""".trimIndent()
    }
    
    private fun generateBattlefieldCfg(tier: String, playstyle: String): String {
        val preset = getTierPreset(tier, playstyle)
        val isCompetitive = playstyle in listOf("competitive", "high_fps")
        
        // Battlefield specific
        val meshQuality = when {
            isCompetitive -> "Low"
            tier == "ultra" -> "Ultra"
            tier == "high" -> "High"
            tier == "medium" -> "Medium"
            else -> "Low"
        }
        val textureFilter = when {
            isCompetitive -> "Medium"
            tier in listOf("ultra", "high") -> "Ultra"
            else -> "High"
        }
        val ffr = if (isCompetitive || tier in listOf("low", "medium")) 100 else 75
        
        return """
; ═══════════════════════════════════════════════════════════
; VEXTIQ BRAIN v13.0 — Battlefield 2042/6 Settings Guide
; GPU Tier: ${tier.uppercase()} | Playstyle: ${playstyle.uppercase()}
;
; Hardware: ${hw.gpuName} (${hw.gpuVramGB}GB) | ${hw.cpuName}
; ═══════════════════════════════════════════════════════════
;
; RECOMMENDED IN-GAME SETTINGS:
;
; [Video]
; Texture Quality: ${if (isCompetitive) "Medium" else preset["textureQuality"]}
; Texture Filtering: $textureFilter
; Lighting Quality: ${if (isCompetitive) "Low" else if (tier in listOf("ultra", "high")) "Ultra" else "Medium"}
; Effects Quality: ${if (isCompetitive) "Low" else preset["effectsQuality"]}
; Post Process Quality: ${if (isCompetitive) "Low" else if (tier == "ultra") "Ultra" else "High"}
; Mesh Quality: $meshQuality
;
; [Advanced]
; Ambient Occlusion: ${if (isCompetitive) "Off" else if (tier == "ultra") "HBAO" else "SSAO"}
; Dynamic Reflection: ${if (isCompetitive) "Off" else "On"}
; Undergrowth Quality: ${if (isCompetitive) "Low" else if (tier in listOf("ultra", "high")) "High" else "Medium"}
;
; [Performance]
; Future Frame Rendering: ${if (isCompetitive) "On" else "Off"}
; GPU Memory Restriction: Off
; Vertical Sync: Off
; Framerate Limiter: ${preset["targetFps"]}
; Render Ahead Limit: ${if (isCompetitive) 1 else 2}
; Future Frame Rendering Scale: $ffr%
;
; [Display]
; Resolution Scale: ${if (isCompetitive && tier != "ultra") "75-85%" else "100%"}
; Field of View: ${if (isCompetitive) "90-100" else "80-90"}
;
; ═══════════════════════════════════════════════════════════
; Expected FPS: ${if (tier == "ultra") "120-165" else if (tier == "high") "100-144" else if (tier == "medium") "80-120" else "60-90"}
; ═══════════════════════════════════════════════════════════
""".trimIndent()
    }
    
    private fun generateGenericCfg(gameId: String, tier: String, playstyle: String): String {
        val preset = getTierPreset(tier, playstyle)
        val isCompetitive = playstyle in listOf("competitive", "pvp", "high_fps", "combat", "visibility")
        
        val gameName = when (gameId) {
            "cs2" -> "Counter-Strike 2"
            "valorant" -> "Valorant"
            "tarkov" -> "Escape from Tarkov"
            "warzone" -> "Call of Duty Warzone"
            "fortnite" -> "Fortnite"
            "apex_legends" -> "Apex Legends"
            "gta_v" -> "GTA V"
            "rust" -> "Rust"
            "ghost_recon_breakpoint" -> "Ghost Recon Breakpoint"
            "once_human" -> "Once Human"
            "division_2" -> "The Division 2"
            else -> gameId.replace("_", " ").uppercase()
        }
        
        return """
; ═══════════════════════════════════════════════════════════
; VEXTIQ BRAIN v13.0 — $gameName Settings Guide
; GPU Tier: ${tier.uppercase()} | Playstyle: ${playstyle.uppercase()}
;
; Hardware: ${hw.gpuName} (${hw.gpuVramGB}GB) | ${hw.cpuName}
; Display: ${hw.resWidth}x${hw.resHeight} @ ${hw.refreshRate}Hz
; ═══════════════════════════════════════════════════════════
;
; RECOMMENDED SETTINGS:
;
; [Quality Preset]
; Overall Quality: ${if (isCompetitive) "Low-Medium (Max FPS)" else preset["textureQuality"]?.toString()?.uppercase()}
;
; [Graphics]
; Texture Quality: ${preset["textureQuality"]}
; Shadow Quality: ${if (isCompetitive) "Low" else preset["shadowQuality"]}
; Effects Quality: ${if (isCompetitive) "Low" else preset["effectsQuality"]}
; View Distance: ${if (isCompetitive) "Medium" else preset["viewDistance"]}
; Anti-Aliasing: ${preset["antiAliasing"]}
;
; [Post-Processing]
; Motion Blur: Off
; Depth of Field: ${if (isCompetitive) "Off" else if (playstyle in listOf("quality", "exploration")) "On" else "Off"}
; Film Grain: Off
; Chromatic Aberration: Off
; Ambient Occlusion: ${if (isCompetitive) "Off" else preset["ambientOcclusion"]}
;
; [Performance]
; VSync: Off
; Frame Rate Limit: ${preset["targetFps"]}
; ${if (hw.gpuVendor == "NVIDIA") "NVIDIA Reflex: On + Boost" else "AMD Anti-Lag: On"}
;
; [Resolution]
; Render Scale: ${if (isCompetitive && tier !in listOf("ultra", "high")) "80-90%" else "100%"}
;
; ═══════════════════════════════════════════════════════════
; Hardware Score: ${calculateHardwareScore()}/100
; Expected FPS: ${if (tier == "ultra") "144-240" else if (tier == "high") "120-165" else if (tier == "medium") "90-144" else "60-100"}
; ═══════════════════════════════════════════════════════════
""".trimIndent()
    }
    
    // ═══════════════════════════════════════════════════════════
    // MAIN OPTIMIZE FUNCTION
    // ═══════════════════════════════════════════════════════════
    
    fun optimize(
        gameId: String = "generic",
        profile: String = "performance",
        playstyle: String = "none",
        targetFps: Int = 60,
        onLog: (String) -> Unit = {}
    ): OptimizationResult {
        onLog("")
        onLog("╔════════════════════════════════════════╗")
        onLog("║       VEXTIQ BRAIN v13.0               ║")
        onLog("╚════════════════════════════════════════╝")
        onLog("")
        
        // Get hardware from singleton
        val hwm = com.vextiq.core.HardwareManager.getHardware()
        hw = HardwareProfile(
            cpuName = hwm.cpuName,
            cpuCores = hwm.cpuCores,
            cpuThreads = hwm.cpuThreads,
            cpuFreqMHz = hwm.cpuFreqMHz,
            gpuName = hwm.gpuName,
            gpuVendor = hwm.gpuVendor,
            gpuVramMB = hwm.gpuVramMB,
            gpuVramGB = hwm.gpuVramGB,
            ramGB = hwm.ramGB,
            ramSpeedMHz = hwm.ramSpeedMHz,
            resWidth = hwm.resWidth,
            resHeight = hwm.resHeight,
            refreshRate = hwm.refreshRate
        )
        
        val tier = getGpuTier()
        val score = calculateHardwareScore()
        
        onLog("[OK] CPU: ${hw.cpuName}")
        onLog("[OK] GPU: ${hw.gpuName} (${hw.gpuVramGB}GB)")
        onLog("[OK] RAM: ${hw.ramGB}GB @ ${hw.ramSpeedMHz}MHz")
        onLog("[OK] Display: ${hw.resWidth}x${hw.resHeight} @ ${hw.refreshRate}Hz")
        onLog("")
        onLog("[i] Hardware Score: $score/100")
        onLog("[i] GPU Tier: ${tier.uppercase()}")
        onLog("[i] Game: ${gameId.uppercase()}")
        if (playstyle != "none") onLog("[i] Playstyle: ${playstyle.uppercase()}")
        onLog("")
        
        // Run BRANT
        bestScore = -1.0
        bestConfig = mutableMapOf()
        iterations = 0
        
        onLog("[>>] Running BRANT optimization...")
        val startNode = searchSpace.mapValues { it.value.first }.toMutableMap()
        branchRecursive(startNode, 0, searchSpace.keys.toList(), targetFps)
        onLog("[OK] BRANT: $iterations iterations")
        
        // Generate game-specific config
        onLog("[>>] Generating ${gameId.uppercase()} config...")
        val configString = when (gameId) {
            "star_citizen" -> generateStarCitizenCfg(tier, playstyle)
            "cyberpunk_2077" -> generateCyberpunkCfg(tier, playstyle)
            "battlefield_6", "battlefield_2042" -> generateBattlefieldCfg(tier, playstyle)
            else -> generateGenericCfg(gameId, tier, playstyle)
        }
        
        val estimatedFps = estimateFps(bestConfig).toInt()
        onLog("")
        onLog("[OK] Config generated!")
        onLog("[OK] Estimated FPS: ~$estimatedFps")
        
        return OptimizationResult(
            config = getTierPreset(tier, playstyle),
            configString = configString,
            estimatedFps = estimatedFps,
            score = bestScore,
            iterations = iterations,
            profile = profile,
            playstyle = playstyle,
            gpuTier = tier
        )
    }
    
    fun getHardware(): HardwareProfile = hw
}
