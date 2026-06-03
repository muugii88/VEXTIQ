package com.vextiq.optimizer

import java.io.File

/**
 * Startup Manager - Manage Windows startup programs
 */
class StartupManager {
    
    data class StartupItem(
        val name: String,
        val command: String,
        val location: String, // Registry or StartupFolder
        val enabled: Boolean
    )
    
    /**
     * Get all startup items
     */
    fun getStartupItems(onLog: (String) -> Unit): List<StartupItem> {
        onLog("[>>] Scanning startup items...")
        
        val items = mutableListOf<StartupItem>()
        
        // Registry: HKCU\SOFTWARE\Microsoft\Windows\CurrentVersion\Run
        items.addAll(getRegistryStartupItems("HKCU"))
        
        // Registry: HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Run
        items.addAll(getRegistryStartupItems("HKLM"))
        
        // Startup folder
        items.addAll(getStartupFolderItems())
        
        onLog("[OK] Found ${items.size} startup items")
        
        return items
    }
    
    /**
     * Get startup items from registry
     */
    private fun getRegistryStartupItems(hive: String): List<StartupItem> {
        val items = mutableListOf<StartupItem>()
        
        try {
            val regPath = "$hive\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run"
            
            val process = ProcessBuilder(listOf(
                "reg", "query", regPath
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            
            // Parse output
            output.lines().forEach { line ->
                if (line.contains("REG_SZ") || line.contains("REG_EXPAND_SZ")) {
                    val parts = line.trim().split("    ").filter { it.isNotEmpty() }
                    if (parts.size >= 3) {
                        items.add(StartupItem(
                            name = parts[0],
                            command = parts.last(),
                            location = regPath,
                            enabled = true
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        return items
    }
    
    /**
     * Get startup items from Startup folder
     */
    private fun getStartupFolderItems(): List<StartupItem> {
        val items = mutableListOf<StartupItem>()
        
        val startupFolder = File(
            System.getenv("APPDATA"),
            "Microsoft\\Windows\\Start Menu\\Programs\\Startup"
        )
        
        if (startupFolder.exists()) {
            startupFolder.listFiles()?.forEach { file ->
                if (file.extension.lowercase() in listOf("lnk", "exe", "bat")) {
                    items.add(StartupItem(
                        name = file.nameWithoutExtension,
                        command = file.absolutePath,
                        location = "StartupFolder",
                        enabled = true
                    ))
                }
            }
        }
        
        return items
    }
    
    /**
     * Disable a startup item
     */
    fun disableStartupItem(item: StartupItem, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Disabling ${item.name}...")
        
        return try {
            if (item.location == "StartupFolder") {
                // Just rename the file
                val file = File(item.command)
                if (file.exists()) {
                    file.renameTo(File(item.command + ".disabled"))
                }
            } else {
                // Delete from registry
                val process = ProcessBuilder(listOf(
                    "reg", "delete", item.location, "/v", item.name, "/f"
                )).redirectErrorStream(true).start()
                process.waitFor()
            }
            
            onLog("[OK] ${item.name} disabled")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * Enable a startup item (re-add to registry)
     */
    fun enableStartupItem(name: String, command: String, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Enabling $name...")
        
        return try {
            val regPath = "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run"
            
            val process = ProcessBuilder(listOf(
                "reg", "add", regPath, "/v", name, "/t", "REG_SZ", "/d", command, "/f"
            )).redirectErrorStream(true).start()
            process.waitFor()
            
            onLog("[OK] $name enabled")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * Add VEXTIQ to startup
     */
    fun addVextiqToStartup(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Adding VEXTIQ to startup...")
        
        // Get current executable path
        val jarPath = System.getProperty("java.class.path")
        val javaHome = System.getProperty("java.home")
        
        val command = "\"$javaHome\\bin\\javaw.exe\" -jar \"$jarPath\""
        
        return enableStartupItem("VEXTIQ PRO", command, onLog)
    }
    
    /**
     * Remove VEXTIQ from startup
     */
    fun removeVextiqFromStartup(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Removing VEXTIQ from startup...")
        
        return try {
            val regPath = "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run"
            
            val process = ProcessBuilder(listOf(
                "reg", "delete", regPath, "/v", "VEXTIQ PRO", "/f"
            )).redirectErrorStream(true).start()
            process.waitFor()
            
            onLog("[OK] VEXTIQ removed from startup")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * Optimize startup - disable known bloatware
     */
    fun optimizeStartup(onLog: (String) -> Unit): Int {
        onLog("[>>] Optimizing startup...")
        
        val bloatware = listOf(
            "OneDrive", "Spotify", "Discord", "Steam", "EpicGamesLauncher",
            "iTunesHelper", "AdobeGCInvoker", "CCXProcess", "Cortana",
            "Microsoft Teams", "Skype", "Zoom"
        )
        
        var disabled = 0
        val items = getStartupItems { }
        
        for (item in items) {
            if (bloatware.any { item.name.contains(it, ignoreCase = true) }) {
                if (disableStartupItem(item) { }) {
                    onLog("[OK] Disabled: ${item.name}")
                    disabled++
                }
            }
        }
        
        onLog("[OK] Startup optimized - disabled $disabled items")
        return disabled
    }
    
    /**
     * Alias for optimizeStartup
     */
    fun disableStartupApps(onLog: (String) -> Unit): Int {
        return optimizeStartup(onLog)
    }
}
