package com.vextiq.core

import java.io.File

/**
 * Game Scanner - Auto-detect installed games
 * Searches Steam, Epic Games, EA, Ubisoft, and common paths
 */
class GameScanner {
    
    data class InstalledGame(
        val name: String,
        val path: String,
        val launcher: String, // Steam, Epic, EA, etc.
        val exeName: String
    )
    
    private val userHome = System.getProperty("user.home")
    
    /**
     * Scan for all installed games
     */
    fun scanAll(onLog: (String) -> Unit): List<InstalledGame> {
        val games = mutableListOf<InstalledGame>()
        
        onLog("[>>] Scanning for installed games...")
        
        // Steam
        val steamGames = scanSteam(onLog)
        games.addAll(steamGames)
        if (steamGames.isNotEmpty()) {
            onLog("[OK] Steam: ${steamGames.size} games found")
        }
        
        // Epic Games
        val epicGames = scanEpic()
        games.addAll(epicGames)
        if (epicGames.isNotEmpty()) {
            onLog("[OK] Epic Games: ${epicGames.size} games found")
        }
        
        // EA App
        val eaGames = scanEA()
        games.addAll(eaGames)
        if (eaGames.isNotEmpty()) {
            onLog("[OK] EA App: ${eaGames.size} games found")
        }
        
        // RSI Launcher (Star Citizen)
        val rsiGames = scanRSI()
        games.addAll(rsiGames)
        if (rsiGames.isNotEmpty()) {
            onLog("[OK] RSI: ${rsiGames.size} games found")
        }
        
        // Common paths
        val commonGames = scanCommonPaths()
        games.addAll(commonGames)
        
        onLog("[OK] Total: ${games.size} games detected")
        
        return games.distinctBy { it.name.lowercase() }
    }
    
    /**
     * Scan Steam library
     */
    private fun scanSteam(onLog: (String) -> Unit): List<InstalledGame> {
        val games = mutableListOf<InstalledGame>()
        
        // Find Steam installation
        val steamPaths = listOf(
            "C:\\Program Files (x86)\\Steam",
            "C:\\Program Files\\Steam",
            "D:\\Steam",
            "D:\\SteamLibrary",
            "E:\\Steam",
            "E:\\SteamLibrary"
        )
        
        for (steamPath in steamPaths) {
            val steamApps = File(steamPath, "steamapps\\common")
            if (steamApps.exists() && steamApps.isDirectory) {
                steamApps.listFiles()?.filter { it.isDirectory }?.forEach { gameDir ->
                    // Find main exe
                    val exeFiles = gameDir.walkTopDown()
                        .filter { it.extension.equals("exe", ignoreCase = true) }
                        .filter { !it.name.contains("unins", ignoreCase = true) }
                        .filter { !it.name.contains("redist", ignoreCase = true) }
                        .filter { !it.name.contains("crash", ignoreCase = true) }
                        .toList()
                    
                    val mainExe = exeFiles.maxByOrNull { it.length() } ?: run {
                        onLog("[!] No executable found in ${gameDir.name}")
                        return@forEach
                    }
                    
                    games.add(InstalledGame(
                        name = gameDir.name,
                        path = gameDir.absolutePath,
                        launcher = "Steam",
                        exeName = mainExe.name
                    ))
                }
            }
        }
        
        return games
    }
    
    /**
     * Scan Epic Games library
     */
    private fun scanEpic(): List<InstalledGame> {
        val games = mutableListOf<InstalledGame>()
        
        val epicPaths = listOf(
            "C:\\Program Files\\Epic Games",
            "D:\\Epic Games",
            "E:\\Epic Games"
        )
        
        for (epicPath in epicPaths) {
            val epicDir = File(epicPath)
            if (epicDir.exists() && epicDir.isDirectory) {
                epicDir.listFiles()?.filter { it.isDirectory }?.forEach { gameDir ->
                    if (gameDir.name != "Launcher") {
                        games.add(InstalledGame(
                            name = gameDir.name,
                            path = gameDir.absolutePath,
                            launcher = "Epic Games",
                            exeName = ""
                        ))
                    }
                }
            }
        }
        
        return games
    }
    
    /**
     * Scan EA App / Origin
     */
    private fun scanEA(): List<InstalledGame> {
        val games = mutableListOf<InstalledGame>()
        
        val eaPaths = listOf(
            "C:\\Program Files\\EA Games",
            "C:\\Program Files (x86)\\Origin Games",
            "D:\\EA Games",
            "D:\\Origin Games"
        )
        
        for (eaPath in eaPaths) {
            val eaDir = File(eaPath)
            if (eaDir.exists() && eaDir.isDirectory) {
                eaDir.listFiles()?.filter { it.isDirectory }?.forEach { gameDir ->
                    games.add(InstalledGame(
                        name = gameDir.name,
                        path = gameDir.absolutePath,
                        launcher = "EA App",
                        exeName = ""
                    ))
                }
            }
        }
        
        return games
    }
    
    /**
     * Scan RSI Launcher (Star Citizen)
     */
    private fun scanRSI(): List<InstalledGame> {
        val games = mutableListOf<InstalledGame>()
        
        val rsiPaths = listOf(
            "C:\\Program Files\\Roberts Space Industries",
            "D:\\Roberts Space Industries",
            "E:\\Roberts Space Industries",
            "D:\\Games\\StarCitizen",
            "E:\\Games\\StarCitizen"
        )
        
        for (rsiPath in rsiPaths) {
            val scLive = File(rsiPath, "StarCitizen\\LIVE")
            if (scLive.exists()) {
                games.add(InstalledGame(
                    name = "Star Citizen",
                    path = scLive.absolutePath,
                    launcher = "RSI Launcher",
                    exeName = "StarCitizen.exe"
                ))
            }
            
            val scPtu = File(rsiPath, "StarCitizen\\PTU")
            if (scPtu.exists()) {
                games.add(InstalledGame(
                    name = "Star Citizen PTU",
                    path = scPtu.absolutePath,
                    launcher = "RSI Launcher",
                    exeName = "StarCitizen.exe"
                ))
            }
        }
        
        return games
    }
    
    /**
     * Scan common game installation paths
     */
    private fun scanCommonPaths(): List<InstalledGame> {
        val games = mutableListOf<InstalledGame>()
        
        val knownGames = mapOf(
            "Cyberpunk 2077" to listOf(
                "C:\\Program Files (x86)\\GOG Galaxy\\Games\\Cyberpunk 2077",
                "C:\\Program Files\\CD Projekt Red\\Cyberpunk 2077"
            ),
            "Valorant" to listOf(
                "C:\\Riot Games\\VALORANT"
            ),
            "League of Legends" to listOf(
                "C:\\Riot Games\\League of Legends"
            )
        )
        
        for ((gameName, paths) in knownGames) {
            for (path in paths) {
                if (File(path).exists()) {
                    games.add(InstalledGame(
                        name = gameName,
                        path = path,
                        launcher = "Standalone",
                        exeName = ""
                    ))
                    break
                }
            }
        }
        
        return games
    }
}
