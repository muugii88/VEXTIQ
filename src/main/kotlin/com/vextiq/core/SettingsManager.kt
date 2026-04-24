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
            System.err.println("[SettingsManager] Failed to load ${settingsFile.absolutePath}: ${e.message} — using defaults")
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
            System.err.println("[SettingsManager] Failed to save ${settingsFile.absolutePath}: ${e.message}")
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

    // --- Network overlay extras ---
    var overlayShowWifiBaseline: Boolean
        get() = properties.getProperty("overlayShowWifiBaseline", "false").toBoolean()
        set(value) { properties.setProperty("overlayShowWifiBaseline", value.toString()); save() }

    var overlayShowPacketLoss: Boolean
        get() = properties.getProperty("overlayShowPacketLoss", "false").toBoolean()
        set(value) { properties.setProperty("overlayShowPacketLoss", value.toString()); save() }

    var overlayShowJitter: Boolean
        get() = properties.getProperty("overlayShowJitter", "false").toBoolean()
        set(value) { properties.setProperty("overlayShowJitter", value.toString()); save() }

    var overlayShowRegion: Boolean
        get() = properties.getProperty("overlayShowRegion", "true").toBoolean()
        set(value) { properties.setProperty("overlayShowRegion", value.toString()); save() }

    // "fps" (default) or "network" — overlay mode
    var overlayMode: String
        get() = properties.getProperty("overlayMode", "fps")
        set(value) { properties.setProperty("overlayMode", value); save() }

    // --- Core monitoring toggles ---
    var gameServerPingEnabled: Boolean
        get() = properties.getProperty("gameServerPingEnabled", "true").toBoolean()
        set(value) { properties.setProperty("gameServerPingEnabled", value.toString()); save() }

    var regionalProbingEnabled: Boolean
        get() = properties.getProperty("regionalProbingEnabled", "true").toBoolean()
        set(value) { properties.setProperty("regionalProbingEnabled", value.toString()); save() }

    var etwFpsEnabled: Boolean
        get() = properties.getProperty("etwFpsEnabled", "true").toBoolean()
        set(value) { properties.setProperty("etwFpsEnabled", value.toString()); save() }

    var rawIcmpEnabled: Boolean
        get() = properties.getProperty("rawIcmpEnabled", "true").toBoolean()
        set(value) { properties.setProperty("rawIcmpEnabled", value.toString()); save() }
}
