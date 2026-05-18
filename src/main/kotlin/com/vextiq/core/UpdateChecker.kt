package com.vextiq.core

import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Update Checker - Check for new versions
 */
class UpdateChecker {
    
    companion object {
        const val CURRENT_VERSION = "2.0.0"
        const val VERSION_CHECK_URL = "https://api.github.com/repos/vextiq/vextiq-pro/releases/latest"
    }
    
    data class UpdateInfo(
        val available: Boolean,
        val currentVersion: String,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    )
    
    /**
     * Check for updates
     */
    fun checkForUpdates(onLog: (String) -> Unit): UpdateInfo {
        onLog("[>>] Checking for updates...")
        
        // For now, return mock data since we don't have a real update server
        // In production, this would query the actual update API
        
        return try {
            // Simulate checking
            Thread.sleep(500)
            
            val latestVersion = "2.0.0" // Would come from API
            val updateAvailable = compareVersions(latestVersion, CURRENT_VERSION) > 0
            
            if (updateAvailable) {
                onLog("[i] New version available: v$latestVersion")
                onLog("[i] Current version: v$CURRENT_VERSION")
            } else {
                onLog("[OK] You have the latest version (v$CURRENT_VERSION)")
            }
            
            UpdateInfo(
                available = updateAvailable,
                currentVersion = CURRENT_VERSION,
                latestVersion = latestVersion,
                downloadUrl = "https://github.com/vextiq/vextiq-pro/releases/latest",
                releaseNotes = if (updateAvailable) "Bug fixes and performance improvements" else ""
            )
        } catch (e: Exception) {
            onLog("[!] Could not check for updates: ${e.message}")
            UpdateInfo(
                available = false,
                currentVersion = CURRENT_VERSION,
                latestVersion = CURRENT_VERSION,
                downloadUrl = "",
                releaseNotes = ""
            )
        }
    }
    
    /**
     * Compare version strings
     * Returns: positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLen = maxOf(parts1.size, parts2.size)
        
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            
            if (p1 != p2) {
                return p1 - p2
            }
        }
        
        return 0
    }
    
    /**
     * Get current version
     */
    fun getCurrentVersion(): String = CURRENT_VERSION
    
    /**
     * Open download page in browser
     */
    fun openDownloadPage() {
        try {
            val url = "https://github.com/vextiq/vextiq-pro/releases/latest"
            ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
