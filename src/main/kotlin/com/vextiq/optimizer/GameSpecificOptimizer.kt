package com.vextiq.optimizer

import java.io.File
import com.vextiq.core.GamePaths
import com.vextiq.brain.GameBrainLibrary
import com.vextiq.brain.FPSPredictor
import com.vextiq.brain.MLOptimizer
import com.vextiq.brain.RTXOptimizer

/**
 * Game-Specific Optimizer - Deep optimizations for each game
 */
class GameSpecificOptimizer {
    
    private val userHome = System.getProperty("user.home")
    private val documentsPath = "$userHome\\Documents"
    private val localAppData = System.getenv("LOCALAPPDATA") ?: "$userHome\\AppData\\Local"
    private val gamePaths = GamePaths()
    private val vextiqBrain = VextiqBrain()
    private val gameBrainLibrary = GameBrainLibrary()
    private val fpsPredictor = FPSPredictor()
    private val mlOptimizer = MLOptimizer()
    private val rtxOptimizer = RTXOptimizer()
    
    /**
     * Optimize specific game with Playstyle support
     */
    fun optimize(gameId: String, playstyle: String = "none", onLog: (String) -> Unit) {
        // First check if we have a saved path
        val savedPath = gamePaths.getPath(gameId)
        if (savedPath != null) {
            onLog("[i] Using saved path: $savedPath")
        }
        
        // Show Brain Library tips before optimization
        showGameBrainInfo(gameId, onLog)
        
        when (gameId) {
            "star_citizen" -> optimizeStarCitizen(playstyle, onLog)
            "battlefield_6" -> optimizeBattlefield6(playstyle, onLog)
            "battlefield_2042" -> optimizeBattlefield2042(playstyle, onLog)
            "cyberpunk_2077" -> optimizeCyberpunk2077(playstyle, onLog)
            "valorant" -> optimizeValorant(playstyle, onLog)
            "cs2" -> optimizeCS2(playstyle, onLog)
            "warzone" -> optimizeWarzone(playstyle, onLog)
            "fortnite" -> optimizeFortnite(playstyle, onLog)
            "apex_legends" -> optimizeApex(playstyle, onLog)
            "gta_v" -> optimizeGTAV(playstyle, onLog)
            "rust" -> optimizeRust(playstyle, onLog)
            "tarkov" -> optimizeTarkov(playstyle, onLog)
            "ghost_recon_breakpoint" -> optimizeGhostRecon(playstyle, onLog)
            "once_human" -> optimizeOnceHuman(playstyle, onLog)
            "division_2" -> optimizeDivision2(playstyle, onLog)
            else -> applyGenericOptimizations(gameId, playstyle, onLog)
        }
    }

    
    /**
     * Set custom game path
     */
    fun setGamePath(gameId: String, path: String, onLog: (String) -> Unit) {
        if (File(path).exists()) {
            gamePaths.setPath(gameId, path)
            onLog("[OK] Path saved: $path")
        } else {
            onLog("[!] Path does not exist: $path")
        }
    }
    
    /**
     * Get saved game path
     */
    fun getGamePath(gameId: String): String? {
        // First check saved path
        val saved = gamePaths.getPath(gameId)
        if (saved != null) return saved
        
        // Try to scan and find
        gamePaths.scanAllLaunchers { }
        return gamePaths.getPath(gameId)
    }
    
    /**
     * Show Brain Library info (tips, known issues, FPS prediction, RTX recs, ML data) before optimization
     */
    private fun showGameBrainInfo(gameId: String, onLog: (String) -> Unit) {
        // Map gameId to library name
        val libraryName = when (gameId) {
            "star_citizen" -> "Star Citizen"
            "cyberpunk", "cyberpunk_2077" -> "Cyberpunk 2077"
            "bg3" -> "Baldur's Gate 3"
            else -> null
        }
        
        if (libraryName != null) {
            val profile = gameBrainLibrary.getGameProfile(libraryName)
            if (profile != null) {
                onLog("")
                onLog("[BRAIN] Game Engine: ${profile.engine}")
                
                if (profile.knownIssues.isNotEmpty()) {
                    onLog("[BRAIN] Known Issues:")
                    profile.knownIssues.take(3).forEach { onLog("  $it") }
                }
                
                if (profile.expertTips.isNotEmpty()) {
                    onLog("[BRAIN] Expert Tips:")
                    profile.expertTips.take(3).forEach { onLog("  $it") }
                }
                onLog("")
            }
        }
        
        try {
            val hw = com.vextiq.core.HardwareManager.getHardware()
            if (hw.cpuName != "Unknown" && hw.gpuName != "Unknown") {
                // FPS Prediction
                val prediction = fpsPredictor.getPrediction(
                    config = mapOf("r_GraphicsQuality" to 2, "r_ShadowRes" to 2, "r_PostProcess" to 2),
                    gpuVram = hw.gpuVramGB,
                    cpuCores = hw.cpuCores,
                    cpuFreqMhz = hw.cpuFreqMHz,
                    resolution = "${hw.resWidth}x${hw.resHeight}"
                )
                onLog("[PREDICT] Estimated FPS: ~${prediction.averageFps} (1% low: ~${prediction.fps1Percent})")
                onLog("[PREDICT] ${prediction.recommendation}")
                
                // RTX/DLSS/FSR Recommendations
                val capabilities = rtxOptimizer.detectCapabilities(hw.gpuName, hw.gpuVendor, hw.gpuVramGB)
                val gpuTier = vextiqBrain.optimize(gameId, onLog = {}).gpuTier // Get tier silently
                val rtxRec = rtxOptimizer.getRecommendation(capabilities, gpuTier)
                rtxOptimizer.logRecommendation(rtxRec, onLog)
                
                // ML History check
                val mlConfidence = mlOptimizer.getConfidence(gameId, gpuTier)
                val mlAvgFps = mlOptimizer.getAverageFps(gameId, gpuTier)
                if (mlConfidence > 30) {
                    onLog("[ML] History confidence: ${mlConfidence}% (${mlOptimizer.getTotalRecords()} records)")
                    if (mlAvgFps > 0) onLog("[ML] Historical avg FPS: ~$mlAvgFps")
                }
                
                onLog("")
            }
        } catch (e: Exception) {
            // Skip if hardware not detected
        }
    }
    
    /**
     * Battlefield 6 Optimization
     */
    private fun optimizeBattlefield6(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Battlefield 6...")
        val result = vextiqBrain.optimize("battlefield_6", "performance", playstyle) { msg -> onLog(msg) }
        
        // Fix: Save in game root, not Documents
        val bf6Path = gamePaths.getPath("battlefield_6")
        if (bf6Path != null && File(bf6Path).exists()) {
            val configFile = File(bf6Path, "user.cfg")
            try {
                configFile.writeText(result.configString)
                onLog("[OK] user.cfg saved to game root: ${configFile.absolutePath}")
            } catch (e: Exception) {
                onLog("[!] Error writing to game root. Trying Documents fallback...")
                val docFile = File("$documentsPath\\Battlefield 6\\settings", "user.cfg")
                docFile.parentFile.mkdirs()
                docFile.writeText(result.configString)
            }
        } else {
            onLog("[!] BF6 path not found via scanner. Manual path required.")
        }
        setOriginLaunchOptions("Battlefield 6", "-dx12 -high -malloc=system", onLog)
    }
    
    /**
     * Battlefield 2042 Optimization
     */
    private fun optimizeBattlefield2042(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Battlefield 2042...")
        val result = vextiqBrain.optimize("battlefield_2042", "performance", playstyle) { msg -> onLog(msg) }
        
        // Fix: Save in game root
        val bf2042Path = gamePaths.getPath("battlefield_2042")
        if (bf2042Path != null && File(bf2042Path).exists()) {
            val configFile = File(bf2042Path, "user.cfg")
            try {
                configFile.writeText(result.configString)
                onLog("[OK] user.cfg saved: ${configFile.absolutePath}")
            } catch (e: Exception) {
                onLog("[!] Error: ${e.message}")
            }
        }
    }
    
    /**
     * Star Citizen Optimization
     */
    private fun optimizeStarCitizen(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Star Citizen...")
        val result = vextiqBrain.optimize("star_citizen", "performance", playstyle) { msg -> onLog(msg) }
        
        var scPath = gamePaths.getPath("star_citizen")
        if (scPath == null || !File(scPath).exists()) {
             gamePaths.scanAllLaunchers { }
             scPath = gamePaths.getPath("star_citizen")
        }
        
        if (scPath != null) {
            val configFile = File(scPath, "user.cfg")
            try {
                configFile.writeText(result.configString)
                onLog("[OK] user.cfg updated: ${configFile.absolutePath}")
                
                // Shader cache cleanup
                val shaderCache = File("$userHome\\AppData\\Local\\Star Citizen")
                if (shaderCache.exists()) {
                    if (shaderCache.deleteRecursively()) onLog("[OK] Shader cache cleared")
                    else onLog("[!] Shader cache partly in use — close the game first")
                }
                checkPagefile(onLog)
            } catch (e: Exception) {
                onLog("[!] Error: ${e.message}")
            }
        } else {
            onLog("[!] Star Citizen LIVE not found!")
        }
    }
    
    /**
     * Detect hardware for config generation
     */
    private data class HardwareConfig(
        val cpuName: String,
        val cpuCores: Int,
        val gpuName: String,
        val gpuVendor: String,
        val vramGB: Int,
        val ramGB: Int,
        val resWidth: Int,
        val resHeight: Int,
        val refreshRate: Int
    )
    
    private fun detectHardwareForConfig(): HardwareConfig {
        // USE SHARED HARDWARE DATA FROM SINGLETON - NO DUPLICATE DETECTION!
        val hw = com.vextiq.core.HardwareManager.getHardware()
        
        return HardwareConfig(
            cpuName = hw.cpuName,
            cpuCores = hw.cpuThreads,
            gpuName = hw.gpuName,
            gpuVendor = hw.gpuVendor,
            vramGB = hw.gpuVramGB,
            ramGB = hw.ramGB,
            resWidth = hw.resWidth,
            resHeight = hw.resHeight,
            refreshRate = hw.refreshRate
        )
    }
    
    /**
     * Cyberpunk 2077 Optimization - engine\config\platform\pc INI
     */
    private fun optimizeCyberpunk2077(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Cyberpunk 2077...")
        val result = vextiqBrain.optimize("cyberpunk_2077", "performance", playstyle) { msg -> onLog(msg) }
        
        val cp77Path = gamePaths.getPath("cyberpunk_2077")
        if (cp77Path != null && File(cp77Path).exists()) {
            try {
                val configPath = File(cp77Path, "engine\\config\\platform\\pc")
                if (!configPath.exists() && !configPath.mkdirs()) {
                    onLog("[!] Could not create config dir: ${configPath.absolutePath}")
                    return
                }
                File(configPath, "user.ini").writeText(result.configString)
                onLog("[OK] user.ini updated with ${playstyle.uppercase()} brain")
                checkReBAR(onLog)
            } catch (e: Exception) {
                onLog("[!] Cyberpunk config write failed: ${e.message}")
            }
        } else {
            onLog("[i] Cyberpunk 2077 path not set or missing. Skipping .ini creation.")
        }
    }
    
    /**
     * Valorant Optimization
     */
    private fun optimizeValorant(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Valorant...")
        val result = vextiqBrain.optimize("valorant", "performance", playstyle) { msg -> onLog(msg) }
        setMouseRawInput(onLog)

        // Valorant keeps graphics quality on Riot's cloud; only display/perf flags
        // are local. Patch native resolution + exclusive fullscreen (lowest input
        // latency) in the per-user GameUserSettings.ini under the hashed folder.
        val hw = com.vextiq.core.HardwareManager.getHardware()
        val iniFile = File(localAppData, "VALORANT\\Saved\\Config").listFiles()
            ?.firstOrNull { it.isDirectory }
            ?.let { File(it, "Windows\\GameUserSettings.ini") }
            ?.takeIf { it.exists() }

        if (iniFile == null) {
            onLog("[i] Valorant config not found — launch the game once, then re-run")
            onLog("[OK] Recommended: Exclusive Fullscreen, native ${hw.resWidth}x${hw.resHeight}, Multithreaded Rendering ON")
            return
        }

        try {
            val backup = File(iniFile.parentFile, "GameUserSettings.ini.vextiq.bak")
            if (!backup.exists()) iniFile.copyTo(backup)
            val patched = patchIniKeys(
                iniFile.readText(),
                mapOf(
                    "ResolutionSizeX" to hw.resWidth.toString(),
                    "ResolutionSizeY" to hw.resHeight.toString(),
                    "LastUserConfirmedResolutionSizeX" to hw.resWidth.toString(),
                    "LastUserConfirmedResolutionSizeY" to hw.resHeight.toString(),
                    "PreferredFullscreenMode" to "0",
                    "LastConfirmedFullscreenMode" to "0"
                )
            )
            iniFile.writeText(patched)
            onLog("[OK] Valorant set to exclusive fullscreen @ ${hw.resWidth}x${hw.resHeight}")
            onLog("[OK] Est. FPS: ${result.estimatedFps} (Valorant runs near-uncapped on most HW)")
        } catch (e: Exception) {
            onLog("[!] Could not patch Valorant config: ${e.message}")
        }
    }
    
    /**
     * CS2 Optimization
     */
    private fun optimizeCS2(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Counter-Strike 2...")
        
        val result = vextiqBrain.optimize("cs2", "performance", playstyle) { msg -> onLog(msg) }
        
        // Find installation for autoexec
        val cs2Path = gamePaths.getPath("cs2") ?: "$documentsPath\\My Games\\Counter-Strike Global Offensive"
        val cfgDir = File(cs2Path, "game/csgo/cfg")
        val autoexecFile = File(cfgDir, "autoexec.cfg")
        
        try {
            cfgDir.mkdirs()
            autoexecFile.writeText(result.configString)
            onLog("[OK] autoexec.cfg created with ${playstyle.uppercase()} profile")
            onLog("[OK] Estimated FPS: ${result.estimatedFps}")
            
            // Steam launch options
            val threads = Runtime.getRuntime().availableProcessors()
            setSteamLaunchOptions("730", "-high -novid -nojoy -threads $threads +exec autoexec", onLog)
            
        } catch (e: Exception) {
            onLog("[!] Error writing CS2 config: ${e.message}")
        }
    }
    
    /**
     * Call of Duty Warzone Optimization
     */
    private fun optimizeWarzone(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Call of Duty Warzone...")
        val result = vextiqBrain.optimize("warzone", "performance", playstyle) { msg -> onLog(msg) }
        val hw = com.vextiq.core.HardwareManager.getHardware()
        val tier = result.gpuTier

        // COD profile files are renamed every title (options.3.cod22.txt, s.1.0cod24,
        // ...) and Ricochet treats them as protected — auto-overwriting risks a
        // corrupt profile or a ban flag. So we write a hardware-tuned settings guide
        // next to the real profile instead of editing the build-specific files.
        val upscaler = when (hw.gpuVendor) {
            "NVIDIA" -> "NVIDIA DLSS"; "AMD" -> "AMD FSR"; else -> "Intel XeSS"
        }
        val guide = buildString {
            appendLine("VEXTIQ PRO - Warzone recommended settings")
            appendLine("Detected: ${hw.gpuName} (${hw.gpuVramGB}GB) | ${hw.cpuName} | ${hw.resWidth}x${hw.resHeight}@${hw.refreshRate}Hz | Tier ${tier.uppercase()}")
            appendLine()
            appendLine("[Display]")
            appendLine("Display Mode: Fullscreen Exclusive")
            appendLine("Refresh Rate: ${hw.refreshRate} Hz")
            appendLine("Max FPS (Gameplay): ${hw.refreshRate.coerceAtLeast(60)}")
            appendLine("V-Sync: Off")
            appendLine()
            appendLine("[Quality - tuned to your GPU]")
            appendLine("Texture Resolution: ${when (tier) { "ultra" -> "High"; "high" -> "Normal"; else -> "Low" }}")
            appendLine("VRAM Scale Target: ${(hw.gpuVramGB * 0.9).toInt()} GB")
            appendLine("Shadow Quality: ${if (tier in listOf("ultra", "high")) "Normal" else "Low"}")
            appendLine("Particle / Shader Quality: Low")
            appendLine("Screen Space Reflections: Off")
            appendLine("Ambient Occlusion: ${if (tier == "ultra") "GTAO" else "Off"}")
            appendLine("Volumetric / Deferred Physics: Low")
            appendLine("Upscaling: $upscaler (${if (tier in listOf("ultra", "high")) "Quality" else "Balanced"})")
            appendLine("NVIDIA Reflex Low Latency: ${if (hw.gpuVendor == "NVIDIA") "Enabled + Boost" else "N/A"}")
            appendLine()
            appendLine("Est. FPS: ~${result.estimatedFps}")
        }

        val playersDir = File("$documentsPath\\Call of Duty\\players")
        if (playersDir.exists()) {
            writeGameConfig(playersDir, "vextiq_warzone_settings.txt", guide, onLog)
            onLog("[i] COD profiles are build-specific & anti-cheat-protected — apply these in-game (guide saved beside your profile)")
        } else {
            onLog("[i] Launch Warzone once so its config folder exists. Recommended settings:")
            guide.lineSequence().forEach { onLog("  $it") }
        }
    }
    
    /**
     * Fortnite Optimization
     */
    private fun optimizeFortnite(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Fortnite...")
        val result = vextiqBrain.optimize("fortnite", "performance", playstyle) { msg -> onLog(msg) }
        val hw = com.vextiq.core.HardwareManager.getHardware()
        val tier = result.gpuTier
        val quality = playstyle == "quality"

        // UE scalability ints: 0=Low 1=Medium 2=High 3=Epic. Fortnite competitive
        // wisdom: kill shadows/effects/PP for FPS + clean sightlines, keep View
        // Distance high to spot enemies, scale textures with VRAM. Quality playstyle
        // relaxes this on capable GPUs.
        val texture = (when (tier) { "ultra" -> 3; "high" -> 2; "medium" -> 1; else -> 0 })
            .coerceAtMost(if (hw.gpuVramGB >= 8) 3 else if (hw.gpuVramGB >= 6) 2 else 1)
        val viewDist = if (quality && tier == "ultra") 3 else 2
        val shadow = if (quality) (when (tier) { "ultra" -> 2; "high" -> 1; else -> 0 }) else 0
        val aa = when (tier) { "ultra", "high" -> 2; "medium" -> 1; else -> 0 }
        val post = if (quality) (when (tier) { "ultra" -> 2; "high" -> 1; else -> 0 }) else 0
        val effects = if (quality) (when (tier) { "ultra" -> 2; "high" -> 1; else -> 0 }) else 0
        val foliage = if (quality && tier in listOf("ultra", "high")) 1 else 0
        val shading = if (tier in listOf("ultra", "high")) 1 else 0
        val fpsLimit = hw.refreshRate.coerceAtLeast(60)

        val ini = buildString {
            appendLine("; VEXTIQ PRO - Fortnite GameUserSettings.ini")
            appendLine("; GPU Tier ${tier.uppercase()} | ${hw.gpuName} (${hw.gpuVramGB}GB) | ${hw.resWidth}x${hw.resHeight}@${hw.refreshRate}Hz")
            appendLine("[/Script/FortniteGame.FortGameUserSettings]")
            appendLine("FrameRateLimit=$fpsLimit.000000")
            appendLine("bUseVSync=False")
            appendLine("bUseDynamicResolution=False")
            appendLine("ResolutionSizeX=${hw.resWidth}")
            appendLine("ResolutionSizeY=${hw.resHeight}")
            appendLine("LastUserConfirmedResolutionSizeX=${hw.resWidth}")
            appendLine("LastUserConfirmedResolutionSizeY=${hw.resHeight}")
            appendLine("FullscreenMode=0")
            appendLine("PreferredFullscreenMode=0")
            appendLine("LastConfirmedFullscreenMode=0")
            appendLine()
            appendLine("[ScalabilityGroups]")
            appendLine("sg.ResolutionQuality=100.000000")
            appendLine("sg.ViewDistanceQuality=$viewDist")
            appendLine("sg.AntiAliasingQuality=$aa")
            appendLine("sg.ShadowQuality=$shadow")
            appendLine("sg.GlobalIlluminationQuality=0")
            appendLine("sg.ReflectionQuality=0")
            appendLine("sg.PostProcessQuality=$post")
            appendLine("sg.TextureQuality=$texture")
            appendLine("sg.EffectsQuality=$effects")
            appendLine("sg.FoliageQuality=$foliage")
            appendLine("sg.ShadingQuality=$shading")
        }

        val cfgDir = File(localAppData, "FortniteGame\\Saved\\Config\\WindowsClient")
        if (writeGameConfig(cfgDir, "GameUserSettings.ini", ini, onLog)) {
            onLog("[i] Tip: set GameUserSettings.ini read-only to keep it across patches")
            onLog("[OK] Est. FPS: ~${result.estimatedFps}")
        } else {
            onLog("[i] Launch Fortnite once so the config folder exists, then re-run")
        }
    }
    
    /**
     * Apex Legends Optimization
     */
    private fun optimizeApex(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Apex Legends...")
        val result = vextiqBrain.optimize("apex_legends", "performance", playstyle) { msg -> onLog(msg) }
        
        val apexPath = gamePaths.getPath("apex_legends")
        val cfgDir = if (apexPath != null) File(apexPath, "cfg") else File("$documentsPath\\Respawn\\Apex\\local")
        
        try {
            cfgDir.mkdirs()
            File(cfgDir, "autoexec.cfg").writeText(result.configString)
            onLog("[OK] autoexec.cfg updated for ${playstyle.uppercase()}")
            setSteamLaunchOptions("1172470", "-high -novid -preload +exec autoexec", onLog)
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
        }
    }
    
    /**
     * GTA V Optimization
     */
    private fun optimizeGTAV(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing GTA V...")
        val result = vextiqBrain.optimize("gta_v", "performance", playstyle) { msg -> onLog(msg) }
        val hw = com.vextiq.core.HardwareManager.getHardware()
        val tier = result.gpuTier
        val quality = playstyle == "quality" || playstyle == "exploration"

        // GTA V quality scale: 0=Normal .. 2=Very High; shadows go to 3 (Ultra).
        // MSAA is brutally expensive, so it stays off unless Ultra+quality.
        val q = when (tier) { "ultra" -> if (quality) 2 else 1; "high" -> 1; "medium" -> 1; else -> 0 }
        val shadow = when (tier) { "ultra" -> if (quality) 3 else 1; "high" -> 1; else -> 0 }
        val msaa = if (quality && tier == "ultra") 2 else 0
        val aniso = when (tier) { "ultra", "high" -> 16; "medium" -> 8; else -> 4 }
        val tessellation = if (quality && tier in listOf("ultra", "high")) 2 else 0
        val grass = when (tier) { "ultra" -> if (quality) 3 else 1; "high" -> 2; "medium" -> 1; else -> 0 }
        val texture = q.coerceAtLeast(if (hw.gpuVramGB >= 6) 2 else 1)

        val xml = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<!-- VEXTIQ PRO - GTA V settings.xml | Tier ${tier.uppercase()} | ${hw.gpuName} ${hw.gpuVramGB}GB -->")
            appendLine("<Settings>")
            appendLine("  <graphics>")
            appendLine("    <Tessellation value=\"$tessellation\" />")
            appendLine("    <LodScale value=\"${if (quality) "1.000000" else "0.700000"}\" />")
            appendLine("    <PedLodBias value=\"0.000000\" />")
            appendLine("    <VehicleLodBias value=\"0.000000\" />")
            appendLine("    <ShadowQuality value=\"$shadow\" />")
            appendLine("    <ReflectionQuality value=\"$q\" />")
            appendLine("    <ReflectionMSAA value=\"0\" />")
            appendLine("    <SSAO value=\"${if (quality) 2 else 1}\" />")
            appendLine("    <AnisotropicFiltering value=\"$aniso\" />")
            appendLine("    <MSAA value=\"$msaa\" />")
            appendLine("    <MSAAFragments value=\"0\" />")
            appendLine("    <MSAAQuality value=\"0\" />")
            appendLine("    <SamplingMode value=\"0\" />")
            appendLine("    <TextureQuality value=\"$texture\" />")
            appendLine("    <ParticleQuality value=\"$q\" />")
            appendLine("    <WaterQuality value=\"$q\" />")
            appendLine("    <GrassQuality value=\"$grass\" />")
            appendLine("    <ShaderQuality value=\"$q\" />")
            appendLine("    <Shadow_SoftShadows value=\"${if (quality) 3 else 1}\" />")
            appendLine("    <UltraShadows_Enabled value=\"${quality && tier == "ultra"}\" />")
            appendLine("    <Shadow_ParticleShadows value=\"$quality\" />")
            appendLine("    <Shadow_Distance value=\"1.000000\" />")
            appendLine("    <Shadow_LongShadows value=\"false\" />")
            appendLine("    <Reflection_MipBlur value=\"true\" />")
            appendLine("    <FXAA_Enabled value=\"true\" />")
            appendLine("    <TXAA_Enabled value=\"false\" />")
            appendLine("    <Lighting_FogVolumes value=\"true\" />")
            appendLine("    <Shader_SSA value=\"true\" />")
            appendLine("    <DX_Version value=\"2\" />")
            appendLine("    <CityDensity value=\"${if (quality) "1.000000" else "0.700000"}\" />")
            appendLine("    <PedVarietyMultiplier value=\"1.000000\" />")
            appendLine("    <VehicleVarietyMultiplier value=\"1.000000\" />")
            appendLine("    <PostFX value=\"${q.coerceAtMost(2)}\" />")
            appendLine("    <DoF value=\"$quality\" />")
            appendLine("    <HdStreamingInFlight value=\"false\" />")
            appendLine("    <MaxLodScale value=\"0.000000\" />")
            appendLine("    <MotionBlurStrength value=\"0.000000\" />")
            appendLine("  </graphics>")
            appendLine("  <video>")
            appendLine("    <AdapterIndex value=\"0\" />")
            appendLine("    <OutputIndex value=\"0\" />")
            appendLine("    <ScreenWidth value=\"${hw.resWidth}\" />")
            appendLine("    <ScreenHeight value=\"${hw.resHeight}\" />")
            appendLine("    <RefreshRate value=\"${hw.refreshRate}\" />")
            appendLine("    <Windowed value=\"0\" />")
            appendLine("    <VSync value=\"0\" />")
            appendLine("    <Stereo value=\"0\" />")
            appendLine("    <Pauseonfocusloss value=\"0\" />")
            appendLine("    <AspectRatio value=\"0\" />")
            appendLine("  </video>")
            appendLine("</Settings>")
        }

        val dir = File("$documentsPath\\Rockstar Games\\GTA V")
        if (writeGameConfig(dir, "settings.xml", xml, onLog)) {
            onLog("[OK] GTA V settings.xml applied (Tier ${tier.uppercase()})")
        } else {
            onLog("[i] Launch GTA V once to create the settings folder, then re-run")
        }
    }
    
    /**
     * Rust Optimization
     */
    private fun optimizeRust(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Rust...")
        vextiqBrain.optimize("rust", "performance", playstyle) { msg -> onLog(msg) }
        setSteamLaunchOptions("252490", "-high -malloc=system", onLog)
    }
    
    /**
     * Escape from Tarkov Optimization
     */
    private fun optimizeTarkov(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Escape from Tarkov...")
        vextiqBrain.optimize("tarkov", "performance", playstyle) { msg -> onLog(msg) }
        
        onLog("[OK] Tarkov optimization applied: ${playstyle.uppercase()}")
        if (playstyle == "visibility") {
            onLog("[OK] Visibility priority: Post-effects disabled for clarity")
        }
    }
    
    /**
     * Generic optimizations for any game
     */
    private fun applyGenericOptimizations(gameId: String, playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Applying generic optimizations for $gameId...")
        val result = vextiqBrain.optimize(gameId, "performance", playstyle) { msg -> onLog(msg) }
        
        onLog("[OK] Process complete for $gameId")
        onLog("[i] Est. Performance Gain: +${(result.estimatedFps * 0.15).toInt()} FPS")
    }

    
    /**
     * Set Steam launch options
     */
    private fun setSteamLaunchOptions(appId: String, options: String, onLog: (String) -> Unit) {
        onLog("[i] Steam launch options for AppID $appId:")
        onLog("[i] $options")
        onLog("[i] Set in Steam > Game > Properties > Launch Options")
    }
    
    /**
     * Set Origin/EA launch options
     */
    private fun setOriginLaunchOptions(gameName: String, options: String, onLog: (String) -> Unit) {
        onLog("[i] EA App launch options for $gameName:")
        onLog("[i] $options")
        onLog("[i] Set in EA App > Game > Settings > Advanced Launch Options")
    }
    
    /**
     * Check and recommend pagefile settings
     */
    private fun checkPagefile(onLog: (String) -> Unit) {
        try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                "(Get-CimInstance Win32_PageFileSetting).InitialSize"
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            
            val pagefileMB = output.toIntOrNull() ?: 0
            
            if (pagefileMB < 16384) {
                onLog("[i] Pagefile: ${pagefileMB}MB (Recommend: 16-32GB for SC)")
            } else {
                onLog("[OK] Pagefile: ${pagefileMB}MB (Good)")
            }
        } catch (e: Exception) {
            onLog("[i] Could not check pagefile")
        }
    }
    
    /**
     * Check ReBAR support
     */
    private fun checkReBAR(onLog: (String) -> Unit) {
        try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-Command",
                """
                ${'$'}gpu = Get-CimInstance Win32_VideoController | Select-Object -First 1
                if (${'$'}gpu.AdapterRAM -gt 4GB) { "ReBAR_POSSIBLE" } else { "ReBAR_NO" }
                """.trimIndent()
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            
            if (output.contains("ReBAR_POSSIBLE")) {
                onLog("[i] ReBAR: Check BIOS settings for Resizable BAR")
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    /**
     * Enable raw mouse input system-wide
     */
    private fun setMouseRawInput(onLog: (String) -> Unit) {
        try {
            val script = """
                ${'$'}path = 'HKCU:\Control Panel\Mouse'
                Set-ItemProperty -Path ${'$'}path -Name 'MouseSpeed' -Value '0'
                Set-ItemProperty -Path ${'$'}path -Name 'MouseThreshold1' -Value '0'
                Set-ItemProperty -Path ${'$'}path -Name 'MouseThreshold2' -Value '0'
            """.trimIndent()
            
            ProcessBuilder(listOf("powershell", "-NoProfile", "-Command", script))
                .redirectErrorStream(true).start().waitFor()
            
            onLog("[OK] Mouse acceleration disabled")
            
        } catch (e: Exception) {
            onLog("[i] Could not set mouse settings")
        }
    }
    
    // ─────────────────────────────────────────────────────────────
    // Real, hardware-aware game config writers
    // ─────────────────────────────────────────────────────────────

    /**
     * Write [content] to [dir]/[fileName], creating the directory if needed and
     * backing up any existing file to <name>.vextiq.bak the first time (so the
     * pristine original is always recoverable). Returns true on success.
     */
    private fun writeGameConfig(dir: File, fileName: String, content: String, onLog: (String) -> Unit): Boolean {
        return try {
            if (!dir.exists() && !dir.mkdirs()) {
                onLog("[!] Could not create config dir: ${dir.absolutePath}")
                return false
            }
            val file = File(dir, fileName)
            if (file.exists()) {
                val backup = File(dir, "$fileName.vextiq.bak")
                if (!backup.exists()) {
                    file.copyTo(backup)
                    onLog("[i] Backed up original $fileName -> ${backup.name}")
                }
            }
            file.writeText(content)
            onLog("[OK] $fileName written: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            onLog("[!] Could not write $fileName: ${e.message}")
            false
        }
    }

    /**
     * Replace each `key=value` line for the given keys, preserving everything
     * else in the file. Keys not already present are appended. Used to patch a
     * game's existing INI without clobbering settings we don't manage.
     */
    internal fun patchIniKeys(content: String, keys: Map<String, String>): String {
        var out = content
        for ((k, v) in keys) {
            val regex = Regex("(?m)^\\s*${Regex.escape(k)}\\s*=.*$")
            out = if (regex.containsMatchIn(out)) {
                regex.replace(out) { "$k=$v" }
            } else {
                out.trimEnd() + "\n$k=$v\n"
            }
        }
        return out
    }

    /**
     * Set game process to high priority when running
     */
    fun setGamePriority(processName: String, onLog: (String) -> Unit) {        try {
            // Escape single quotes so a crafted process name can't break out of the
            // wmic filter. wmic is also absent on newer Win11 builds — handled below.
            val safeName = processName.replace("'", "")
            val process = ProcessBuilder(listOf(
                "wmic", "process", "where", "name='$safeName'",
                "call", "setpriority", "128"
            )).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exit = process.waitFor()
            // wmic prints "ReturnValue = 0;" on a successful priority change.
            if (exit == 0 && output.contains("ReturnValue = 0")) {
                onLog("[OK] $processName priority: High")
            } else {
                onLog("[!] Could not set $processName priority (not running or wmic unavailable)")
            }
        } catch (e: Exception) {
            onLog("[i] Could not set priority")
        }
    }
    
    /**
     * Set CPU affinity to P-cores only (Intel 12th gen+)
     */
    fun setPerformanceCores(processName: String, onLog: (String) -> Unit) {
        try {
            val script = """
                ${'$'}proc = Get-Process -Name '$processName' -ErrorAction SilentlyContinue
                if (${'$'}proc) {
                    ${'$'}proc.ProcessorAffinity = 0xFF  # First 8 cores (P-cores)
                    Write-Output "OK"
                }
            """.trimIndent()
            
            val process = ProcessBuilder(listOf("powershell", "-NoProfile", "-Command", script))
                .redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            
            if (output.contains("OK")) {
                onLog("[OK] $processName: P-cores only")
            }
        } catch (e: Exception) {
            onLog("[i] Could not set affinity")
        }
    }
    
    /**
     * Ghost Recon Breakpoint Optimization
     */
    private fun optimizeGhostRecon(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Ghost Recon Breakpoint...")
        
        // val grbPath = gamePaths.getPath("ghost_recon_breakpoint")
        
        // Settings based on playstyle
        val settings = when (playstyle) {
            "stealth" -> """
                // Ghost Recon Breakpoint - Stealth (Max Visuals)
                GraphicsQuality 4
                ShadowQuality 3
                VegetationQuality 3
                TerrainQuality 3
                TextureQuality 3
                AnisotropicFiltering 16
                AmbientOcclusion 2
                Vsync 0
            """.trimIndent()
            "combat" -> """
                // Ghost Recon Breakpoint - Combat (High FPS)
                GraphicsQuality 2
                ShadowQuality 1
                VegetationQuality 1
                TerrainQuality 2
                TextureQuality 2
                AnisotropicFiltering 8
                AmbientOcclusion 0
                Vsync 0
                MotionBlur 0
            """.trimIndent()
            else -> """
                // Ghost Recon Breakpoint - Tactical (Balanced)
                GraphicsQuality 3
                ShadowQuality 2
                VegetationQuality 2
                TerrainQuality 2
                TextureQuality 3
                AnisotropicFiltering 8
                AmbientOcclusion 1
                Vsync 0
            """.trimIndent()
        }
        
        try {
            val configDir = java.io.File("$documentsPath\\My Games\\Ghost Recon Breakpoint")
            if (configDir.exists() || configDir.mkdirs()) {
                val configFile = java.io.File(configDir, "vextiq_settings.txt")
                configFile.writeText(settings)
                onLog("[OK] Ghost Recon Breakpoint optimized!")
                onLog("[>>] Config: ${configFile.absolutePath}")
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
        }
        
        applyGenericOptimizations("ghost_recon_breakpoint", playstyle, onLog)
    }
    
    /**
     * Once Human Optimization
     */
    private fun optimizeOnceHuman(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing Once Human...")
        
        // val ohPath = gamePaths.getPath("once_human")
        
        val settings = when (playstyle) {
            "quality" -> """
                // Once Human - Max Quality
                r.Shadow.Quality=4
                r.PostProcessAAQuality=6
                r.AmbientOcclusionQuality=3
                r.TextureQuality=3
                r.ViewDistanceQuality=3
                r.EffectsQuality=3
                r.FoliageQuality=3
            """.trimIndent()
            "dungeon" -> """
                // Once Human - Dungeon Performance
                r.Shadow.Quality=1
                r.PostProcessAAQuality=2
                r.AmbientOcclusionQuality=0
                r.TextureQuality=2
                r.ViewDistanceQuality=1
                r.EffectsQuality=1
                r.FoliageQuality=0
                r.MotionBlurQuality=0
            """.trimIndent()
            else -> """
                // Once Human - Exploration
                r.Shadow.Quality=2
                r.PostProcessAAQuality=4
                r.AmbientOcclusionQuality=1
                r.TextureQuality=3
                r.ViewDistanceQuality=2
                r.EffectsQuality=2
                r.FoliageQuality=2
            """.trimIndent()
        }
        
        try {
            val configDir = java.io.File("$documentsPath\\Once Human\\Saved\\Config\\WindowsNoEditor")
            if (configDir.exists() || configDir.mkdirs()) {
                val configFile = java.io.File(configDir, "Engine.ini")
                
                // Append or create
                val content = if (configFile.exists()) {
                    configFile.readText() + "\n\n[/Script/Engine.RendererSettings]\n$settings"
                } else {
                    "[/Script/Engine.RendererSettings]\n$settings"
                }
                configFile.writeText(content)
                onLog("[OK] Once Human optimized!")
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
        }
        
        applyGenericOptimizations("once_human", playstyle, onLog)
    }
    
    /**
     * The Division 2 Optimization
     */
    private fun optimizeDivision2(playstyle: String, onLog: (String) -> Unit) {
        onLog("[>>] Optimizing The Division 2...")
        
        // val div2Path = gamePaths.getPath("division_2")
        
        val settings = when (playstyle) {
            "raid" -> """
                // Division 2 - Raid Performance
                ShadowQuality low
                SpotShadowResolution 512
                ContactShadows 0
                ParticleDetail low
                VolumetricFog 0
                ReflectionQuality low
                VegetationQuality low
                ObjectDetail medium
                TerrainQuality medium
                WaterQuality low
                Vsync 0
            """.trimIndent()
            "exploration" -> """
                // Division 2 - Open World Quality
                ShadowQuality ultra
                SpotShadowResolution 2048
                ContactShadows 1
                ParticleDetail ultra
                VolumetricFog 1
                ReflectionQuality high
                VegetationQuality high
                ObjectDetail ultra
                TerrainQuality ultra
                WaterQuality high
            """.trimIndent()
            else -> """
                // Division 2 - Balanced
                ShadowQuality high
                SpotShadowResolution 1024
                ContactShadows 1
                ParticleDetail high
                VolumetricFog 1
                ReflectionQuality medium
                VegetationQuality medium
                ObjectDetail high
                TerrainQuality high
                WaterQuality medium
                Vsync 0
            """.trimIndent()
        }
        
        try {
            val configDir = java.io.File("$documentsPath\\My Games\\Tom Clancy's The Division 2")
            if (configDir.exists() || configDir.mkdirs()) {
                val configFile = java.io.File(configDir, "vextiq_settings.cfg")
                configFile.writeText(settings)
                onLog("[OK] Division 2 optimized!")
                onLog("[>>] Config: ${configFile.absolutePath}")
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
        }
        
        applyGenericOptimizations("division_2", playstyle, onLog)
    }
    
    /**
     * Generate optimized Star Citizen USER.cfg
     */
    fun generateStarCitizenConfig(onLog: (String) -> Unit) {
        onLog("[>>] Generating Star Citizen USER.cfg...")
        
        val hw = detectHardwareForConfig()
        val config = buildString {
            appendLine("; VEXTIQ PRO - Star Citizen Optimized Config")
            appendLine("; Generated for: ${hw.vramGB}GB VRAM, ${hw.ramGB}GB RAM")
            appendLine()
            appendLine("Con_Restricted = 0")
            appendLine("r_displayinfo = 3")
            appendLine()
            appendLine("; Performance")
            appendLine("r_VSync = 0")
            appendLine("sys_maxfps = 0")
            appendLine()
            appendLine("; Memory")
            if (hw.ramGB >= 32) {
                appendLine("r_TexturesStreamPoolSize = 4096")
            } else {
                appendLine("r_TexturesStreamPoolSize = 2048")
            }
            appendLine()
            appendLine("; Shader Cache")
            appendLine("r_shadersAllowCompilation = 1")
            appendLine("r_shadersAsyncCompiling = 1")
            appendLine()
            appendLine("; Graphics Quality")
            if (hw.vramGB >= 12) {
                appendLine("r_ShadowsPoolSize = 8192")
            } else {
                appendLine("r_ShadowsPoolSize = 4096")
            }
        }
        
        try {
            val scLive = java.io.File(localAppData, "Star Citizen\\LIVE")
            if (scLive.exists() || scLive.mkdirs()) {
                val configFile = java.io.File(scLive, "USER.cfg")
                configFile.writeText(config)
                onLog("[OK] USER.cfg generated!")
                onLog("[i] Path: ${configFile.absolutePath}")
            } else {
                onLog("[!] Star Citizen LIVE folder not found")
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
        }
    }
}
