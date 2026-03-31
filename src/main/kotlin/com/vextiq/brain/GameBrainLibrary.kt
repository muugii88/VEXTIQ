package com.vextiq.brain

data class GameProfile(
    val name: String,
    val publisher: String,
    val releaseYear: Int,
    val engine: String,
    val optimalSettings: Map<String, Int>,
    val gpuTierRecommendations: Map<String, List<Int>>, 
    val cpuRecommendations: Map<String, String>,
    val knownIssues: List<String>,
    val expertTips: List<String>,
    val supportedResolutions: List<String> = listOf("1080p", "1440p", "4K"),
    val dlssRecommended: Boolean = false,
    val rayTracingRecommended: Boolean = false
)

class GameBrainLibrary {
    private val gameProfiles = mapOf(
        "Star Citizen" to GameProfile(
            name = "Star Citizen",
            publisher = "Cloud Imperium Games",
            releaseYear = 2012,
            engine = "Lumberyard (modified CryEngine)",
            optimalSettings = mapOf(
                "r_GraphicsQuality" to 2,  // High, not Ultra (too demanding)
                "r_ShadowRes" to 2,
                "r_MotionBlur" to 0,  // Known to cause stuttering
                "r_PostProcess" to 2,
                "r_DepthOfField" to 0,
                "r_VolumetricFog" to 1,  // Reduces fog-induced FPS drops
                "e_ViewDistRatio" to 60,  // Balance between LOD and draw distance
                "e_ViewDistRatioVegetation" to 50
            ),
            gpuTierRecommendations = mapOf(
                "ultra" to listOf(60, 90, 120),
                "high" to listOf(45, 60, 90),
                "medium" to listOf(30, 45, 60),
                "low" to listOf(20, 30, 45)
            ),
            cpuRecommendations = mapOf(
                "min" to "Ryzen 5 3600",
                "recommended" to "i7-10700K or Ryzen 7 5800X",
                "optimal" to "i9-13900K or Ryzen 7 7700X (CPU-bound game)"
            ),
            knownIssues = listOf(
                "⚠️  Volumetric fog causes 20-30 FPS drops",
                "⚠️  Motion blur creates stutter - always disable",
                "⚠️  Streaming issues on HDDs - use SSD mandatory",
                "⚠️  32GB+ RAM recommended (uses 28GB+ in Stanton)",
                "⚠️  Multi-threaded rendering required for smooth gameplay"
            ),
            expertTips = listOf(
                "✅ Enable Object Streaming Cache (high impact)",
                "✅ Set Object Count to Medium or High",
                "✅ Use High-performance power plan",
                "✅ Close Discord/YouTube/Chrome (massive RAM hogs)",
                "✅ Update GPU drivers weekly (Star Citizen changes monthly)"
            ),
            dlssRecommended = false,  // Game doesn't support DLSS well
            rayTracingRecommended = false  // Causes severe FPS drop
        ),

        "Cyberpunk 2077" to GameProfile(
            name = "Cyberpunk 2077",
            publisher = "CD Projekt Red",
            releaseYear = 2020,
            engine = "REDengine 4",
            optimalSettings = mapOf(
                "r_GraphicsQuality" to 3,  // High (Ultra is diminishing returns)
                "r_ShadowRes" to 3,
                "r_MotionBlur" to 1,
                "r_PostProcess" to 3,
                "r_DepthOfField" to 1,
                "r_RayTracing" to 1,  // Medium ray-traced shadows only
                "r_AmbientOcclusion" to 2,
                "e_ViewDistRatio" to 80
            ),
            gpuTierRecommendations = mapOf(
                "ultra" to listOf(120, 144),  // 4K, ray-traced
                "high" to listOf(90, 120),    // 1440p high settings
                "medium" to listOf(60, 90),   // 1440p medium
                "low" to listOf(30, 60)       // 1080p low
            ),
            cpuRecommendations = mapOf(
                "min" to "i5-10400",
                "recommended" to "i7-12700K",
                "optimal" to "i9-13900K (GPU-limited now after patches)"
            ),
            knownIssues = listOf(
                "⚠️  DLSS 2.0 can cause ghosting - use balanced",
                "⚠️  Ray-tracing causes massive FPS drops",
                "⚠️  Night City causes CPU stutters",
                "⚠️  Memory leak in some drivers (update GPU drivers)"
            ),
            expertTips = listOf(
                "✅ Enable DLSS 3 Frame Generation (if RTX 40-series)",
                "✅ Ray tracing only on 4080+ with DLSS",
                "✅ Drive-by streaming is CPU-intensive (avoid fast traversal)",
                "✅ Use NVIDIA driver 535+ for stability",
                "✅ Crowd density down to 0.7 if CPU-bound"
            ),
            dlssRecommended = true,
            rayTracingRecommended = true
        ),

        "Baldur's Gate 3" to GameProfile(
            name = "Baldur's Gate 3",
            publisher = "Larian Studios",
            releaseYear = 2023,
            engine = "Vulkan",
            optimalSettings = mapOf(
                "r_GraphicsQuality" to 3,
                "r_ShadowRes" to 3,
                "r_MotionBlur" to 0,  // Optional, causes stutter
                "r_PostProcess" to 2,
                "r_RayTracing" to 0,  // Not recommended (very demanding)
                "e_ViewDistRatio" to 100,
                "shadowQuality" to 3
            ),
            gpuTierRecommendations = mapOf(
                "ultra" to listOf(90, 120),
                "high" to listOf(60, 90),
                "medium" to listOf(45, 60),
                "low" to listOf(30, 45)
            ),
            cpuRecommendations = mapOf(
                "min" to "i5-10400",
                "recommended" to "i7-12700K (CPU-bound in cities)",
                "optimal" to "i9-13900KS (16 cores needed)"
            ),
            knownIssues = listOf(
                "⚠️  MAJOR CPU bottleneck in Act 3 (Baldur's Gate city)",
                "⚠️  FPS drops to 40-50 FPS even on high-end systems",
                "⚠️  Vulkan driver issues on AMD - use latest drivers",
                "⚠️  Memory leak after 4+ hours of gameplay"
            ),
            expertTips = listOf(
                "✅ Enable Vulkan API (faster than DirectX 12)",
                "✅ Lower crowd density in Act 3",
                "✅ Disable ambient occlusion in cities",
                "✅ Use High Performance power plan",
                "✅ Restart game every 4 hours to clear memory"
            ),
            dlssRecommended = true,
            rayTracingRecommended = false
        ),

        "Starfield" to GameProfile(
            name = "Starfield",
            publisher = "Bethesda",
            releaseYear = 2023,
            engine = "Creation Engine 2",
            optimalSettings = mapOf(
                "r_GraphicsQuality" to 3,
                "r_ShadowRes" to 3,
                "r_MotionBlur" to 0,
                "r_PostProcess" to 2,
                "drawDistance" to 80,
                "shadowDistance" to 100000,
                "enableRayTracing" to 0  // Too demanding
            ),
            gpuTierRecommendations = mapOf(
                "ultra" to listOf(90, 120),  // 4K
                "high" to listOf(60, 90),    // 1440p
                "medium" to listOf(45, 60),  // 1080p
                "low" to listOf(30, 45)      // 720p
            ),
            cpuRecommendations = mapOf(
                "min" to "i5-10400",
                "recommended" to "i7-10700K",
                "optimal" to "i9-13900K"
            ),
            knownIssues = listOf(
                "⚠️  Locked to 60 FPS (engine limitation)",
                "⚠️  AMD GPU issues with texture quality",
                "⚠️  Memory usage can exceed 16GB in space stations"
            ),
            expertTips = listOf(
                "✅ Starfield is FPS-capped at 60 (by design)",
                "✅ Use 120Hz monitor for smoother input despite 60 FPS cap",
                "✅ Disable TAA if quality blur bothers you",
                "✅ SSE Upscaler mod for better visuals at same FPS"
            ),
            dlssRecommended = false,  // Game doesn't support DLSS
            rayTracingRecommended = false
        ),

        "Black Myth: Wukong" to GameProfile(
            name = "Black Myth: Wukong",
            publisher = "Game Science",
            releaseYear = 2024,
            engine = "Unreal Engine 5",
            optimalSettings = mapOf(
                "r_GraphicsQuality" to 3,
                "r_ShadowRes" to 3,
                "r_MotionBlur" to 1,
                "r_PostProcess" to 3,
                "r_RayTracing" to 2,  // Medium RT recommended
                "dlssQuality" to 1  // Performance DLSS 3
            ),
            gpuTierRecommendations = mapOf(
                "ultra" to listOf(120, 144),  // 4K RTX On
                "high" to listOf(90, 120),    // 1440p RT High
                "medium" to listOf(60, 90),   // 1080p RT Medium
                "low" to listOf(30, 60)       // 720p RT Off
            ),
            cpuRecommendations = mapOf(
                "min" to "i5-9400",
                "recommended" to "i7-10700K",
                "optimal" to "i9-13900K with RTX 4090"
            ),
            knownIssues = listOf(
                "⚠️  Extremely demanding (2024 AAA title)",
                "⚠️  Ray-traced shadows cause major FPS drops",
                "⚠️  VRAM hungry (needs 12GB+)",
                "⚠️  Some driver stutters on older NVIDIA drivers"
            ),
            expertTips = listOf(
                "✅ MUST use DLSS 3 or FSR for playable FPS",
                "✅ Frame Generation recommended (RTX 40-series)",
                "✅ RT Shadows off for 20+ FPS boost",
                "✅ Update to latest GPU drivers immediately",
                "✅ Use Performance preset as starting point, then tweak"
            ),
            dlssRecommended = true,
            rayTracingRecommended = true
        ),

        "Dragon Age: Inquisition" to GameProfile(
            name = "Dragon Age: Inquisition",
            publisher = "BioWare",
            releaseYear = 2014,
            engine = "Frostbite 3",
            optimalSettings = mapOf(
                "r_GraphicsQuality" to 4,  // Can max easily
                "r_ShadowRes" to 4,
                "r_MotionBlur" to 1,
                "r_PostProcess" to 4,
                "r_AmbientOcclusion" to 2,
                "hairWorks" to 1  // Optional, subtle visual impact
            ),
            gpuTierRecommendations = mapOf(
                "ultra" to listOf(120, 144),  // Max settings, 4K possible
                "high" to listOf(90, 120),
                "medium" to listOf(60, 90),
                "low" to listOf(30, 60)
            ),
            cpuRecommendations = mapOf(
                "min" to "i5-2620M",
                "recommended" to "i7-3770K",
                "optimal" to "Any modern CPU (not demanding)"
            ),
            knownIssues = listOf(
                "⚠️  Older game, extremely well-optimized",
                "⚠️  No major issues reported"
            ),
            expertTips = listOf(
                "✅ This game runs great on almost any hardware",
                "✅ Max all settings and enjoy 100+ FPS",
                "✅ Only game that needs NO tweaking!"
            ),
            dlssRecommended = false,
            rayTracingRecommended = false
        ),

        "Helldivers 2" to GameProfile(
            name = "Helldivers 2",
            publisher = "Arrowhead Game Studios",
            releaseYear = 2024,
            engine = "Autodesk Stingray",
            optimalSettings = mapOf(
                "r_GraphicsQuality" to 2,  // Medium (fps > graphics)
                "r_MotionBlur" to 0,  // Competitive - disable
                "r_PostProcess" to 1,
                "shadowQuality" to 1,  // Competitive setting
                "fov" to 110  // High FOV for competitive play
            ),
            gpuTierRecommendations = mapOf(
                "ultra" to listOf(240, 360),  // Competitive 240+ FPS
                "high" to listOf(144, 240),   // 144 Hz gaming
                "medium" to listOf(90, 144),  // Stable 100+ FPS
                "low" to listOf(60, 90)       // 60 FPS minimum
            ),
            cpuRecommendations = mapOf(
                "min" to "i5-9400",
                "recommended" to "i7-10700K",
                "optimal" to "Ryzen 5 5600X (smooth 240+ FPS)"
            ),
            knownIssues = listOf(
                "⚠️  Competitive shooter - low latency is priority",
                "⚠️  Motion blur causes input lag - always OFF"
            ),
            expertTips = listOf(
                "✅ Prioritize FPS over graphics (target 144+ Hz)",
                "✅ Disable motion blur for competitive play",
                "✅ Lower settings get higher FPS for better responsiveness",
                "✅ 240 FPS on 240 Hz monitor recommended for esports",
                "✅ Lock FPS to monitor refresh rate (no tearing)"
            ),
            dlssRecommended = false,  // Low-latency requirement
            rayTracingRecommended = false
        )
    )

    /**
     * Get game profile by name
     */
    fun getGameProfile(gameName: String): GameProfile? {
        return gameProfiles[gameName]
    }

    /**
     * Get all supported games
     */
    fun getSupportedGames(): List<String> {
        return gameProfiles.keys.toList().sorted()
    }

    /**
     * Get known issues for a game
     */
    fun getKnownIssues(gameName: String): List<String> {
        return gameProfiles[gameName]?.knownIssues ?: emptyList()
    }

    /**
     * Get expert tips for a game
     */
    fun getExpertTips(gameName: String): List<String> {
        return gameProfiles[gameName]?.expertTips ?: emptyList()
    }

    /**
     * Get recommended FPS for GPU tier
     */
    fun getRecommendedFps(gameName: String, gpuTier: String): List<Int>? {
        return gameProfiles[gameName]?.gpuTierRecommendations?.get(gpuTier)
    }

    /**
     * Print full game analysis
     */
    fun printGameAnalysis(gameName: String) {
        val profile = gameProfiles[gameName] ?: run {
            println("❌ Game not found: $gameName")
            return
        }

        println("""
            ╔════════════════════════════════════════╗
            ║     ${"${profile.name.uppercase()}".padEnd(36)} ║
            ╚════════════════════════════════════════╝
            
            📊 Game Info:
              Publisher:        ${profile.publisher}
              Release Year:     ${profile.releaseYear}
              Engine:           ${profile.engine}
            
            ⚙️  Optimal Settings:
              ${profile.optimalSettings.entries.joinToString("\n  ") { "${it.key.padEnd(20)}: ${it.value}" }}
            
            🎮 FPS Targets by GPU Tier:
              Ultra:            ${profile.gpuTierRecommendations["ultra"]?.joinToString(", ")} FPS
              High:             ${profile.gpuTierRecommendations["high"]?.joinToString(", ")} FPS
              Medium:           ${profile.gpuTierRecommendations["medium"]?.joinToString(", ")} FPS
              Low:              ${profile.gpuTierRecommendations["low"]?.joinToString(", ")} FPS
            
            💻 CPU Recommendations:
              Min:              ${profile.cpuRecommendations["min"]}
              Recommended:      ${profile.cpuRecommendations["recommended"]}
              Optimal:          ${profile.cpuRecommendations["optimal"]}
            
            ⚠️  Known Issues:
              ${profile.knownIssues.joinToString("\n  ")}
            
            ✅ Expert Tips:
              ${profile.expertTips.joinToString("\n  ")}
            
            📱 Features:
              DLSS Support:     ${if (profile.dlssRecommended) "✅ Recommended" else "❌ Not Recommended"}
              Ray-Tracing:      ${if (profile.rayTracingRecommended) "✅ Recommended" else "❌ Not Recommended"}
        """.trimIndent())
    }

    /**
     * Print all games
     */
    fun printAllGames() {
        println("\n📚 VEXTIQ GAME BRAIN LIBRARY - Supported Games:\n")
        gameProfiles.forEach { (name, profile) ->
            val dlss = if (profile.dlssRecommended) "✅" else "❌"
            val rt = if (profile.rayTracingRecommended) "✅" else "❌"
            println("  ${name.padEnd(25)} | DLSS: $dlss | RT: $rt | ${profile.releaseYear}")
        }
        println("\nUse: library.printGameAnalysis(\"Game Name\") for details\n")
    }
}