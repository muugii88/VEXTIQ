package com.vextiq.optimizer

import java.io.File

/**
 * Game Profile Optimizer - Game-specific optimizations
 */
class GameProfileOptimizer {
    
    private val userHome = System.getProperty("user.home")
    private val localAppData = System.getenv("LOCALAPPDATA") ?: "$userHome\\AppData\\Local"
    private val appData = System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming"
    
    /**
     * Optimize Star Citizen
     */
    fun optimizeStarCitizen(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing Star Citizen...")
        
        // Find USER.cfg location
        val scPaths = listOf("C:", "D:", "E:").flatMap { drive ->
            listOf(
                "$drive\\Program Files\\Roberts Space Industries\\StarCitizen\\LIVE",
                "$drive\\Games\\StarCitizen\\LIVE",
                "$drive\\StarCitizen\\LIVE"
            )
        }
        
        val scDir = scPaths.map { File(it) }.firstOrNull { it.exists() }
        
        if (scDir == null) {
            onLog("[!] Star Citizen installation not found")
            return false
        }
        
        onLog("[OK] Found SC at: ${scDir.absolutePath}")
        
        // Create optimized USER.cfg
        val userCfg = File(scDir, "USER.cfg")
        val cfgContent = """
            ; VEXTIQ PRO - Star Citizen Performance Config
            ; Generated for maximum FPS
            
            ; Threading
            r_MultiThreaded = 1
            sys_MaxFPS = 0
            sys_budget_sysmem = 0
            sys_budget_videomem = 0
            
            ; Graphics
            r_VSync = 0
            r_MotionBlur = 0
            r_ChromaticAberration = 0
            r_FilmGrain = 0
            r_Sharpening = 1
            
            ; Performance
            gpu_MaxFrameLatency = 1
            e_ShadowsMaxTexRes = 1024
            r_ssdo = 0
            r_SSReflections = 0
            
            ; Network
            cl_packetrate = 128
            cl_cmdrate = 128
        """.trimIndent()
        
        try {
            userCfg.writeText(cfgContent)
            onLog("[OK] Created optimized USER.cfg")
        } catch (e: Exception) {
            onLog("[!] Cannot write USER.cfg: ${e.message}")
            return false
        }

        // Clean shaders
        val shadersDir = File(scDir, "USER\\Client\\0\\shaders")
        if (shadersDir.exists()) {
            if (shadersDir.deleteRecursively()) onLog("[OK] Cleaned shader cache")
            else onLog("[!] Shader cache partly in use — close the game first")
        }
        
        onLog("[OK] Star Citizen optimization complete")
        return true
    }
    
    /**
     * Optimize Battlefield 2042/6
     */
    fun optimizeBattlefield(gameVersion: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing $gameVersion...")
        
        // Find Documents folder
        val docsDir = File(userHome, "Documents")
        val bfDir = File(docsDir, gameVersion)
        
        if (!bfDir.exists()) {
            bfDir.mkdirs()
        }
        
        // Create PROFSAVE_profile with optimized settings
        val settingsDir = File(bfDir, "settings")
        settingsDir.mkdirs()
        
        val configContent = """
            ; VEXTIQ PRO - Battlefield Performance Config
            
            GstRender.ResolutionScale 1.000000
            GstRender.FrameRateLimiter 0
            GstRender.VSyncEnabled 0
            GstRender.MotionBlurEnabled 0
            GstRender.ChromaticAberrationEnabled 0
            GstRender.FilmGrainEnabled 0
            GstRender.LensDistortionEnabled 0
            GstRender.VignetteEnabled 0
            
            ; FPS priority
            GstRender.AntiAliasingPost 0
            GstRender.AmbientOcclusionMethod 0
            GstRender.DynamicReflectionMethod 0
            
            ; Network
            GstAudio.VoIPEnabled 1
            GstInput.RawMouseInput 1
        """.trimIndent()
        
        try {
            File(settingsDir, "PROFSAVE_profile").writeText(configContent)
            onLog("[OK] Created optimized profile")
        } catch (e: Exception) {
            onLog("[!] Cannot write config: ${e.message}")
            return false
        }

        onLog("[OK] $gameVersion optimization complete")
        return true
    }
    
    /**
     * Optimize Cyberpunk 2077
     */
    fun optimizeCyberpunk(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing Cyberpunk 2077...")
        
        val cpDir = File(localAppData, "CD Projekt Red\\Cyberpunk 2077")
        if (!cpDir.exists()) {
            cpDir.mkdirs()
        }
        
        val userSettingsFile = File(cpDir, "UserSettings.json")
        
        // Create optimized settings
        val settings = """
            {
                "VEXTIQ_Optimized": true,
                "graphics/basic/MotionBlur": "Off",
                "graphics/basic/ChromaticAberration": "Off",
                "graphics/basic/FilmGrain": "Off",
                "graphics/basic/DepthOfField": "Off",
                "graphics/basic/LensFlare": "Off",
                "graphics/advanced/MaxFPS": "0",
                "graphics/advanced/VSync": "Off"
            }
        """.trimIndent()
        
        try {
            userSettingsFile.writeText(settings)
            onLog("[OK] Created optimized settings")
        } catch (e: Exception) {
            onLog("[!] Cannot write settings: ${e.message}")
            return false
        }

        onLog("[OK] Cyberpunk 2077 optimization complete")
        return true
    }
    
    /**
     * Optimize CS2 / Valorant (competitive FPS)
     */
    fun optimizeCompetitiveFps(gameName: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing $gameName...")
        
        // Generic competitive FPS optimizations
        // Set process priority to High via registry (IFEO — needs admin)
        return try {
            val exeName = when (gameName.lowercase()) {
                "cs2" -> "cs2.exe"
                "valorant" -> "VALORANT-Win64-Shipping.exe"
                else -> "$gameName.exe"
            }
            val perfPath =
                "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Image File Execution Options\\$exeName\\PerfOptions"

            val cpu = runCommand(listOf("reg", "add", perfPath, "/v", "CpuPriorityClass", "/t", "REG_DWORD", "/d", "3", "/f"))
            if (cpu.ok) onLog("[OK] Process priority: High") else onLog("[!] Process priority failed (needs admin)")

            val io = runCommand(listOf("reg", "add", perfPath, "/v", "IoPriority", "/t", "REG_DWORD", "/d", "3", "/f"))
            if (io.ok) onLog("[OK] I/O priority: High") else onLog("[!] I/O priority failed (needs admin)")

            if (cpu.ok && io.ok) {
                onLog("[OK] $gameName optimization complete")
                true
            } else {
                onLog("[!] $gameName optimization incomplete — run as admin")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Priority settings (needs admin): ${e.message}")
            false
        }
    }
    
    /**
     * Optimize by game ID
     */
    fun optimizeGame(gameId: String, onLog: (String) -> Unit): Boolean {
        return when (gameId.lowercase()) {
            "star_citizen", "sc" -> optimizeStarCitizen(onLog)
            "bf6", "battlefield_6" -> optimizeBattlefield("Battlefield 6", onLog)
            "bf2042", "battlefield_2042" -> optimizeBattlefield("Battlefield 2042", onLog)
            "cyberpunk", "cp2077" -> optimizeCyberpunk(onLog)
            "cs2" -> optimizeCompetitiveFps("CS2", onLog)
            "valorant" -> optimizeCompetitiveFps("Valorant", onLog)
            else -> {
                onLog("[i] Using generic optimization for $gameId")
                optimizeCompetitiveFps(gameId, onLog)
            }
        }
    }
    
    private data class CmdResult(val exitCode: Int, val output: String) {
        val ok: Boolean get() = exitCode == 0
    }

    private fun runCommand(cmd: List<String>): CmdResult {
        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        return CmdResult(exit, output)
    }
}
