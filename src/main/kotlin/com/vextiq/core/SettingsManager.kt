package com.vextiq.core

import java.io.File
import java.util.Properties

/**
 * Settings Manager - Persist user preferences
 */
class SettingsManager {
    
    private val settingsDir = File(System.getProperty("user.home"), ".vextiq")
    private val settingsFile = File(settingsDir, "settings.properties")
    private val properties = Properties()
    
    init {
        settingsDir.mkdirs()
        load()
    }
    
    /**
     * Load settings from file
     */
    fun load() {
        try {
            if (settingsFile.exists()) {
                settingsFile.inputStream().use { properties.load(it) }
            }
        } catch (e: Exception) {
            // Ignore, use defaults
        }
    }
    
    /**
     * Save settings to file
     */
    fun save() {
        try {
            settingsFile.outputStream().use { 
                properties.store(it, "VEXTIQ PRO Settings") 
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    // Language
    var language: String
        get() = properties.getProperty("language", "en")
        set(value) {
            properties.setProperty("language", value)
            save()
        }
    
    // Theme
    var darkMode: Boolean
        get() = properties.getProperty("darkMode", "true").toBoolean()
        set(value) {
            properties.setProperty("darkMode", value.toString())
            save()
        }
    
    // FPS Overlay
    var fpsOverlayEnabled: Boolean
        get() = properties.getProperty("fpsOverlay", "false").toBoolean()
        set(value) {
            properties.setProperty("fpsOverlay", value.toString())
            save()
        }
    
    var fpsOverlayX: Int
        get() = properties.getProperty("fpsOverlayX", "16").toIntOrNull() ?: 16
        set(value) {
            properties.setProperty("fpsOverlayX", value.toString())
            save()
        }
    
    var fpsOverlayY: Int
        get() = properties.getProperty("fpsOverlayY", "16").toIntOrNull() ?: 16
        set(value) {
            properties.setProperty("fpsOverlayY", value.toString())
            save()
        }
    
    // Last selected game
    var lastSelectedGame: String
        get() = properties.getProperty("lastGame", "star_citizen")
        set(value) {
            properties.setProperty("lastGame", value)
            save()
        }
    
    // Minimize to tray
    var minimizeToTray: Boolean
        get() = properties.getProperty("minimizeToTray", "true").toBoolean()
        set(value) {
            properties.setProperty("minimizeToTray", value.toString())
            save()
        }
    
    // Start with Windows
    var startWithWindows: Boolean
        get() = properties.getProperty("startWithWindows", "false").toBoolean()
        set(value) {
            properties.setProperty("startWithWindows", value.toString())
            save()
        }
    
    // Auto-optimize on game launch
    var autoOptimize: Boolean
        get() = properties.getProperty("autoOptimize", "false").toBoolean()
        set(value) {
            properties.setProperty("autoOptimize", value.toString())
            save()
        }
    
    // Check for updates
    var checkUpdates: Boolean
        get() = properties.getProperty("checkUpdates", "true").toBoolean()
        set(value) {
            properties.setProperty("checkUpdates", value.toString())
            save()
        }
    
    // Overlay Toggles
    var overlayShowGpu: Boolean
        get() = properties.getProperty("overlayShowGpu", "true").toBoolean()
        set(value) { properties.setProperty("overlayShowGpu", value.toString()); save() }
        
    var overlayShowCpu: Boolean
        get() = properties.getProperty("overlayShowCpu", "true").toBoolean()
        set(value) { properties.setProperty("overlayShowCpu", value.toString()); save() }
        
    var overlayShowRam: Boolean
        get() = properties.getProperty("overlayShowRam", "true").toBoolean()
        set(value) { properties.setProperty("overlayShowRam", value.toString()); save() }
        
    var overlayShowNet: Boolean
        get() = properties.getProperty("overlayShowNet", "true").toBoolean()
        set(value) { properties.setProperty("overlayShowNet", value.toString()); save() }
        
    var overlayShowFps: Boolean
        get() = properties.getProperty("overlayShowFps", "true").toBoolean()
        set(value) { properties.setProperty("overlayShowFps", value.toString()); save() }
        
    var overlayShowFrametime: Boolean
        get() = properties.getProperty("overlayShowFrametime", "true").toBoolean()
        set(value) { properties.setProperty("overlayShowFrametime", value.toString()); save() }
    
    // Optimization behavior
    var restoreOnExit: Boolean
        get() = properties.getProperty("restoreOnExit", "false").toBoolean()
        set(value) { properties.setProperty("restoreOnExit", value.toString()); save() }
    
    var backupBeforeChanges: Boolean
        get() = properties.getProperty("backupBeforeChanges", "true").toBoolean()
        set(value) { properties.setProperty("backupBeforeChanges", value.toString()); save() }
    
    /**
     * Set/remove Windows autostart registry entry
     */
    fun applyStartWithWindows(enabled: Boolean) {
        try {
            val appPath = java.io.File(
                System.getProperty("java.class.path").split(";").firstOrNull() ?: return
            ).absolutePath
            
            if (enabled) {
                ProcessBuilder(listOf(
                    "reg", "add", 
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v", "VEXTIQ", "/t", "REG_SZ", "/d", "\"$appPath\"", "/f"
                )).redirectErrorStream(true).start().waitFor()
                println("[Settings] Added VEXTIQ to Windows startup")
            } else {
                ProcessBuilder(listOf(
                    "reg", "delete",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v", "VEXTIQ", "/f"
                )).redirectErrorStream(true).start().waitFor()
                println("[Settings] Removed VEXTIQ from Windows startup")
            }
        } catch (e: Exception) {
            println("[Settings] Autostart error: ${e.message}")
        }
    }
    
    /**
     * Reset all settings to defaults
     */
    fun resetAll() {
        properties.clear()
        save()
        load()
        // Remove autostart entry
        applyStartWithWindows(false)
        println("[Settings] All settings reset to defaults")
    }
    
    /**
     * Clear ML/optimization cache
     */
    fun clearOptimizationCache(): Int {
        var cleared = 0
        try {
            val mlFile = java.io.File(settingsDir, "ml_history.properties")
            if (mlFile.exists()) { mlFile.delete(); cleared++ }
            
            val cacheDir = java.io.File(settingsDir, "cache")
            if (cacheDir.exists()) { 
                cacheDir.listFiles()?.forEach { it.delete(); cleared++ }
            }
        } catch (e: Exception) {
            println("[Settings] Cache clear error: ${e.message}")
        }
        return cleared
    }
    
    /**
     * Export settings + logs to file on Desktop
     */
    fun exportLogs(): String? {
        return try {
            val desktop = java.io.File(System.getProperty("user.home"), "Desktop")
            val exportFile = java.io.File(desktop, "vextiq_export_${System.currentTimeMillis()}.txt")
            
            val sb = StringBuilder()
            sb.appendLine("═══ VEXTIQ PRO v2.0 Settings Export ═══")
            sb.appendLine("Date: ${java.time.LocalDateTime.now()}")
            sb.appendLine()
            sb.appendLine("═══ Settings ═══")
            properties.forEach { key, value -> sb.appendLine("$key = $value") }
            sb.appendLine()
            sb.appendLine("═══ System Info ═══")
            sb.appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
            sb.appendLine("Java: ${System.getProperty("java.version")}")
            sb.appendLine("Cores: ${Runtime.getRuntime().availableProcessors()}")
            sb.appendLine("RAM: ${Runtime.getRuntime().maxMemory() / (1024*1024)}MB")
            
            exportFile.writeText(sb.toString())
            exportFile.absolutePath
        } catch (e: Exception) {
            println("[Settings] Export error: ${e.message}")
            null
        }
    }
}
