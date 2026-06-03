package com.vextiq.core

import java.io.File
import java.util.Properties

/**
 * VEXTIQ PRO Game Scanner
 * 3 үндсэн арга:
 * 1. Folder Scan - Common install folders
 * 2. Registry Scan - Windows registry
 * 3. Launcher Scan - Steam, Epic, Riot, EA, Ubisoft, Rockstar
 */
class GamePaths {
    
    private val userHome = System.getProperty("user.home")
    private val configDir = File(userHome, ".vextiq")
    private val pathsFile = File(configDir, "game_paths.properties")
    
    private val gamePaths = mutableMapOf<String, String>()
    
    // Supported games mapping - name patterns -> game_id
    private val supportedGames = mapOf(
        "star citizen" to "star_citizen",
        "starcitizen" to "star_citizen",
        "roberts space industries" to "star_citizen",
        
        "battlefield 6" to "battlefield_6",
        "bf6" to "battlefield_6",
        
        "ghost recon breakpoint" to "ghost_recon_breakpoint",
        "ghostreconbreakpoint" to "ghost_recon_breakpoint",
        "breakpoint" to "ghost_recon_breakpoint",
        
        "once human" to "once_human",
        "oncehuman" to "once_human",
        
        "division 2" to "division_2",
        "the division 2" to "division_2",
        "thedivision2" to "division_2",
        
        "cyberpunk 2077" to "cyberpunk_2077",
        "cyberpunk2077" to "cyberpunk_2077",
        
        "valorant" to "valorant",
        
        "counter-strike 2" to "cs2",
        "counter strike 2" to "cs2",
        "cs2" to "cs2",
        "csgo" to "cs2",
        "counter-strike global offensive" to "cs2",
        
        "rust" to "rust",
        
        "escape from tarkov" to "tarkov",
        "eft" to "tarkov",
        "tarkov" to "tarkov",
        "battlestate" to "tarkov",
        
        "call of duty" to "warzone",
        "warzone" to "warzone",
        "modern warfare" to "warzone",
        
        "fortnite" to "fortnite",
        
        "apex legends" to "apex_legends",
        "apex" to "apex_legends",
        
        "gta v" to "gta_v",
        "gta5" to "gta_v",
        "grand theft auto" to "gta_v"
    )
    
    init {
        configDir.mkdirs()
        load()
    }
    
    private fun load() {
        if (pathsFile.exists()) {
            try {
                val props = Properties()
                pathsFile.inputStream().use { props.load(it) }
                props.forEach { key, value ->
                    gamePaths[key.toString()] = value.toString()
                }
            } catch (e: Exception) {
                println("Error loading game paths: ${e.message}")
            }
        }
    }
    
    private fun save() {
        try {
            val props = Properties()
            gamePaths.forEach { (key, value) ->
                props.setProperty(key, value)
            }
            pathsFile.outputStream().use { props.store(it, "VEXTIQ Game Paths") }
        } catch (e: Exception) {
            println("Error saving game paths: ${e.message}")
        }
    }
    
    fun getPath(gameId: String): String? = gamePaths[gameId]
    
    fun setPath(gameId: String, path: String) {
        gamePaths[gameId] = path
        save()
    }
    
    fun getAllPaths(): Map<String, String> = gamePaths.toMap()
    
    /**
     * Match folder name to game ID
     */
    private fun matchGame(folderName: String): String? {
        val lower = folderName.lowercase()
        for ((pattern, gameId) in supportedGames) {
            if (lower.contains(pattern)) {
                return gameId
            }
        }
        return null
    }
    
    /**
     * MASTER SCAN - All methods combined
     */
    fun scanAllLaunchers(onLog: (String) -> Unit): Map<String, String> {
        val found = mutableMapOf<String, String>()
        val drives = File.listRoots().map { it.absolutePath.trimEnd('\\') }
        
        onLog("[>>] Starting PRO Game Scan...")
        onLog("[i] Drives: ${drives.joinToString(", ")}")
        onLog("")
        
        // 1. STEAM SCAN
        onLog("[1/6] Scanning Steam...")
        found.putAll(scanSteam(drives, onLog))
        
        // 2. EPIC GAMES SCAN
        onLog("[2/6] Scanning Epic Games...")
        found.putAll(scanEpic(onLog))
        
        // 3. RIOT GAMES SCAN
        onLog("[3/6] Scanning Riot Games...")
        found.putAll(scanRiot(drives, onLog))
        
        // 4. EA / ORIGIN SCAN
        onLog("[4/6] Scanning EA / Origin...")
        found.putAll(scanEA(drives, onLog))
        
        // 5. UBISOFT SCAN
        onLog("[5/6] Scanning Ubisoft...")
        found.putAll(scanUbisoft(drives, onLog))
        
        // 6. COMMON FOLDERS SCAN
        onLog("[6/6] Scanning common folders...")
        found.putAll(scanCommonFolders(drives, onLog))
        
        onLog("")
        onLog("════════════════════════════════════════")
        onLog("[OK] Scan complete! Found ${found.size} games")
        onLog("════════════════════════════════════════")
        
        return found
    }
    
    /**
     * 1. STEAM SCAN
     */
    private fun scanSteam(drives: List<String>, onLog: (String) -> Unit): Map<String, String> {
        val found = mutableMapOf<String, String>()
        val steamPaths = mutableListOf<String>()
        
        for (drive in drives) {
            steamPaths.add("$drive\\Program Files (x86)\\Steam\\steamapps\\common")
            steamPaths.add("$drive\\Program Files\\Steam\\steamapps\\common")
            steamPaths.add("$drive\\Steam\\steamapps\\common")
            steamPaths.add("$drive\\SteamLibrary\\steamapps\\common")
            steamPaths.add("$drive\\Games\\Steam\\steamapps\\common")
        }
        
        // Parse libraryfolders.vdf for additional libraries on all drives
        for (drive in drives) {
            val vdfPaths = listOf(
                "$drive\\Program Files (x86)\\Steam\\steamapps\\libraryfolders.vdf",
                "$drive\\Program Files\\Steam\\steamapps\\libraryfolders.vdf",
                "$drive\\Steam\\steamapps\\libraryfolders.vdf",
                "$drive\\SteamLibrary\\steamapps\\libraryfolders.vdf"
            )
            
            for (vdfPath in vdfPaths) {
                val vdfFile = File(vdfPath)
                if (vdfFile.exists()) {
                    try {
                        val content = vdfFile.readText()
                        val pathRegex = """"path"\s*"([^"]+)"""".toRegex()
                        pathRegex.findAll(content).forEach { match ->
                            val libPath = match.groupValues[1].replace("\\\\", "\\")
                            steamPaths.add("$libPath\\steamapps\\common")
                        }
                    } catch (e: Exception) {
                        println("Error parsing Steam VDF: ${e.message}")
                    }
                }
            }
        }
        
        for (steamPath in steamPaths) {
            val dir = File(steamPath)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.filter { it.isDirectory }?.forEach { gameDir ->
                    val gameId = matchGame(gameDir.name)
                    if (gameId != null && !found.containsKey(gameId)) {
                        found[gameId] = gameDir.absolutePath
                        gamePaths[gameId] = gameDir.absolutePath
                        onLog("[OK] ${gameDir.name}")
                    }
                }
            }
        }
        
        save()
        return found
    }
    
    /**
     * 2. EPIC GAMES SCAN
     */
    private fun scanEpic(onLog: (String) -> Unit): Map<String, String> {
        val found = mutableMapOf<String, String>()
        
        // Parse Epic manifests
        val manifestPath = File("C:\\ProgramData\\Epic\\EpicGamesLauncher\\Data\\Manifests")
        if (manifestPath.exists()) {
            manifestPath.listFiles()?.filter { it.extension == "item" }?.forEach { manifest ->
                try {
                    val content = manifest.readText()
                    val displayNameRegex = """"DisplayName"\s*:\s*"([^"]+)"""".toRegex()
                    val installLocRegex = """"InstallLocation"\s*:\s*"([^"]+)"""".toRegex()
                    
                    val nameMatch = displayNameRegex.find(content)
                    val locMatch = installLocRegex.find(content)
                    
                    if (nameMatch != null && locMatch != null) {
                        val displayName = nameMatch.groupValues[1]
                        val installLoc = locMatch.groupValues[1].replace("\\\\", "\\").replace("\\/", "/")
                        
                        val gameId = matchGame(displayName)
                        if (gameId != null && File(installLoc).exists() && !found.containsKey(gameId)) {
                            found[gameId] = installLoc
                            gamePaths[gameId] = installLoc
                            onLog("[OK] $displayName")
                        }
                    }
                } catch (e: Exception) {
                    println("Error parsing Epic manifest ${manifest.name}: ${e.message}")
                }
            }
        }
        
        // Common Epic paths
        val drives = File.listRoots().map { it.absolutePath.trimEnd('\\') }
        for (drive in drives) {
            listOf(
                "$drive\\Program Files\\Epic Games",
                "$drive\\Epic Games"
            ).forEach { epicPath ->
                val dir = File(epicPath)
                if (dir.exists()) {
                    dir.listFiles()?.filter { it.isDirectory }?.forEach { gameDir ->
                        val gameId = matchGame(gameDir.name)
                        if (gameId != null && !found.containsKey(gameId)) {
                            found[gameId] = gameDir.absolutePath
                            gamePaths[gameId] = gameDir.absolutePath
                            onLog("[OK] ${gameDir.name}")
                        }
                    }
                }
            }
        }
        
        save()
        return found
    }
    
    /**
     * 3. RIOT GAMES SCAN
     */
    private fun scanRiot(drives: List<String>, onLog: (String) -> Unit): Map<String, String> {
        val found = mutableMapOf<String, String>()
        
        for (drive in drives) {
            val riotPath = File("$drive\\Riot Games")
            if (riotPath.exists()) {
                riotPath.listFiles()?.filter { it.isDirectory }?.forEach { gameDir ->
                    val gameId = matchGame(gameDir.name)
                    if (gameId != null && !found.containsKey(gameId)) {
                        found[gameId] = gameDir.absolutePath
                        gamePaths[gameId] = gameDir.absolutePath
                        onLog("[OK] ${gameDir.name}")
                    }
                }
            }
        }
        
        save()
        return found
    }
    
    /**
     * 4. EA / ORIGIN SCAN
     */
    private fun scanEA(drives: List<String>, onLog: (String) -> Unit): Map<String, String> {
        val found = mutableMapOf<String, String>()
        
        // Method 1: Registry via direct reg query (no powershell loop)
        try {
            val keys = listOf(
                "HKLM\\SOFTWARE\\Electronic Arts\\EA Desktop\\InstallLocations",
                "HKLM\\SOFTWARE\\WOW6432Node\\Electronic Arts\\EA Desktop\\InstallLocations"
            )
            for (key in keys) {
                val output = runRegQuery(key)
                output.lines().forEach { line ->
                    if (line.contains("REG_SZ")) {
                        val path = line.substringAfter("REG_SZ").trim()
                        if (path.isNotEmpty() && File(path).exists()) {
                            val gameDir = File(path)
                            val gameId = matchGame(gameDir.name)
                            if (gameId != null && !found.containsKey(gameId)) {
                                found[gameId] = gameDir.absolutePath
                                gamePaths[gameId] = gameDir.absolutePath
                                onLog("[OK] ${gameDir.name} (EA Desktop)")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error scanning EA registry: ${e.message}")
        }

        
        // Method 2: EA Desktop config
        try {
            val eaConfigPath = File("C:\\ProgramData\\Electronic Arts\\EA Desktop")
            if (eaConfigPath.exists()) {
                eaConfigPath.walkTopDown().maxDepth(3).forEach { file ->
                    if (file.extension == "json" || file.name.contains("install", ignoreCase = true)) {
                        try {
                            val content = file.readText()
                            val pathRegex = """"installLocation"\s*:\s*"([^"]+)"""".toRegex()
                            pathRegex.findAll(content).forEach { match ->
                                val installPath = match.groupValues[1].replace("\\\\", "\\")
                                if (File(installPath).exists()) {
                                    val gameDir = File(installPath)
                                    val gameId = matchGame(gameDir.name)
                                    if (gameId != null && !found.containsKey(gameId)) {
                                        found[gameId] = gameDir.absolutePath
                                        gamePaths[gameId] = gameDir.absolutePath
                                        onLog("[OK] ${gameDir.name} (EA config)")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("Error parsing EA config file ${file.name}: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error scanning EA config: ${e.message}")
        }
        
        // Method 3: Folder scan (all drives)
        for (drive in drives) {
            listOf(
                "$drive\\Program Files\\EA Games",
                "$drive\\Program Files (x86)\\EA Games",
                "$drive\\Program Files\\Origin Games",
                "$drive\\Program Files (x86)\\Origin Games",
                "$drive\\EA Games",
                "$drive\\Origin Games"
            ).forEach { eaPath ->
                val dir = File(eaPath)
                if (dir.exists()) {
                    dir.listFiles()?.filter { it.isDirectory }?.forEach { gameDir ->
                        val gameId = matchGame(gameDir.name)
                        if (gameId != null && !found.containsKey(gameId)) {
                            found[gameId] = gameDir.absolutePath
                            gamePaths[gameId] = gameDir.absolutePath
                            onLog("[OK] ${gameDir.name}")
                        }
                    }
                }
            }
        }
        
        save()
        return found
    }
    
    /**
     * 5. UBISOFT SCAN
     */
    private fun scanUbisoft(drives: List<String>, onLog: (String) -> Unit): Map<String, String> {
        val found = mutableMapOf<String, String>()
        
        for (drive in drives) {
            listOf(
                "$drive\\Program Files\\Ubisoft\\Ubisoft Game Launcher\\games",
                "$drive\\Program Files (x86)\\Ubisoft\\Ubisoft Game Launcher\\games",
                "$drive\\Ubisoft Games"
            ).forEach { ubiPath ->
                val dir = File(ubiPath)
                if (dir.exists()) {
                    dir.listFiles()?.filter { it.isDirectory }?.forEach { gameDir ->
                        val gameId = matchGame(gameDir.name)
                        if (gameId != null && !found.containsKey(gameId)) {
                            found[gameId] = gameDir.absolutePath
                            gamePaths[gameId] = gameDir.absolutePath
                            onLog("[OK] ${gameDir.name}")
                        }
                    }
                }
            }
        }
        
        save()
        return found
    }
    
    /**
     * 6. COMMON FOLDERS SCAN - X:\Games, etc.
     */
    private fun scanCommonFolders(drives: List<String>, onLog: (String) -> Unit): Map<String, String> {
        val found = mutableMapOf<String, String>()
        
        for (drive in drives) {
            // Common game folders
            listOf(
                "$drive\\Games",
                "$drive\\Program Files\\Games",
                "$drive\\Program Files (x86)\\Games"
            ).forEach { gamesPath ->
                val dir = File(gamesPath)
                if (dir.exists()) {
                    dir.listFiles()?.filter { it.isDirectory }?.forEach { gameDir ->
                        val gameId = matchGame(gameDir.name)
                        if (gameId != null && !found.containsKey(gameId)) {
                            // Special handling for Star Citizen - need LIVE folder
                            if (gameId == "star_citizen") {
                                val livePath = File(gameDir, "LIVE")
                                if (livePath.exists()) {
                                    found[gameId] = livePath.absolutePath
                                    gamePaths[gameId] = livePath.absolutePath
                                    onLog("[OK] ${gameDir.name}\\LIVE")
                                } else {
                                    found[gameId] = gameDir.absolutePath
                                    gamePaths[gameId] = gameDir.absolutePath
                                    onLog("[OK] ${gameDir.name}")
                                }
                            } else {
                                found[gameId] = gameDir.absolutePath
                                gamePaths[gameId] = gameDir.absolutePath
                                onLog("[OK] ${gameDir.name}")
                            }
                        }
                    }
                }
            }
            
            // RSI / Star Citizen specific paths
            listOf(
                "$drive\\Roberts Space Industries\\StarCitizen\\LIVE",
                "$drive\\Program Files\\Roberts Space Industries\\StarCitizen\\LIVE"
            ).forEach { rsiPath ->
                val dir = File(rsiPath)
                if (dir.exists() && (File(dir, "Bin64").exists() || File(dir, "Data").exists())) {
                    if (!found.containsKey("star_citizen")) {
                        found["star_citizen"] = dir.absolutePath
                        gamePaths["star_citizen"] = dir.absolutePath
                        onLog("[OK] Star Citizen LIVE")
                    }
                }
            }
            
            // Rockstar
            listOf(
                "$drive\\Program Files\\Rockstar Games",
                "$drive\\Rockstar Games"
            ).forEach { rockstarPath ->
                val dir = File(rockstarPath)
                if (dir.exists()) {
                    dir.listFiles()?.filter { it.isDirectory }?.forEach { gameDir ->
                        val gameId = matchGame(gameDir.name)
                        if (gameId != null && !found.containsKey(gameId)) {
                            found[gameId] = gameDir.absolutePath
                            gamePaths[gameId] = gameDir.absolutePath
                            onLog("[OK] ${gameDir.name}")
                        }
                    }
                }
            }
            
            // Battlestate / Tarkov
            listOf(
                "$drive\\Battlestate Games\\EFT",
                "$drive\\Battlestate Games\\Escape from Tarkov"
            ).forEach { bsgPath ->
                val dir = File(bsgPath)
                if (dir.exists()) {
                    if (!found.containsKey("tarkov")) {
                        found["tarkov"] = dir.absolutePath
                        gamePaths["tarkov"] = dir.absolutePath
                        onLog("[OK] Escape from Tarkov")
                    }
                }
            }
        }
        
        save()
        return found
    }
    
    /**
     * Get config file path for a game
     */
    fun getConfigPath(gameId: String): String? {
        val gamePath = gamePaths[gameId] ?: return null
        val docs = System.getProperty("user.home") + "\\Documents"
        
        return when (gameId) {
            "star_citizen" -> "$gamePath\\user.cfg"
            "battlefield_6" -> "$docs\\Battlefield 6\\settings\\user.cfg"
            "cyberpunk_2077" -> "$gamePath\\engine\\config\\platform\\pc\\user.ini"
            "cs2" -> "$gamePath\\game\\csgo\\cfg\\autoexec.cfg"
            "apex_legends" -> "$gamePath\\cfg\\autoexec.cfg"
            else -> null
        }
    }    private fun runRegQuery(key: String): String {
        return try {
            val process = ProcessBuilder(listOf("reg", "query", key, "/s"))
                .redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            output
        } catch (e: Exception) {
            ""
        }
    }
}
