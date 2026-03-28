package com.vextiq.optimizer

import java.io.File

/**
 * Shader Cache Cleaner - Cleans GPU shader caches
 */
class CacheCleaner {
    
    data class CleanResult(
        val filesDeleted: Int,
        val bytesFreed: Long,
        val locations: List<String>
    )
    
    private val userHome = System.getProperty("user.home")
    private val localAppData = System.getenv("LOCALAPPDATA") ?: "$userHome\\AppData\\Local"
    private val appData = System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming"
    
    /**
     * Clean NVIDIA shader cache
     */
    fun cleanNvidiaCache(onLog: (String) -> Unit): CleanResult {
        onLog("[>>] Cleaning NVIDIA shader cache...")
        
        val paths = listOf(
            "$localAppData\\NVIDIA\\DXCache",
            "$localAppData\\NVIDIA\\GLCache",
            "$localAppData\\NVIDIA Corporation\\NV_Cache",
            "$localAppData\\D3DSCache"
        )
        
        return cleanPaths(paths, onLog, "NVIDIA")
    }
    
    /**
     * Clean AMD shader cache
     */
    fun cleanAmdCache(onLog: (String) -> Unit): CleanResult {
        onLog("[>>] Cleaning AMD shader cache...")
        
        val paths = listOf(
            "$localAppData\\AMD\\DxCache",
            "$localAppData\\AMD\\GLCache",
            "$localAppData\\AMD\\VkCache",
            "$localAppData\\AMD\\DxcCache"
        )
        
        return cleanPaths(paths, onLog, "AMD")
    }
    
    /**
     * Clean DirectX shader cache
     */
    fun cleanDirectXCache(onLog: (String) -> Unit): CleanResult {
        onLog("[>>] Cleaning DirectX shader cache...")
        
        val paths = listOf(
            "$localAppData\\D3DSCache",
            "$localAppData\\Microsoft\\DirectX Shader Cache"
        )
        
        return cleanPaths(paths, onLog, "DirectX")
    }
    
    /**
     * Clean Star Citizen shaders
     */
    fun cleanStarCitizenCache(onLog: (String) -> Unit): CleanResult {
        onLog("[>>] Cleaning Star Citizen shaders...")
        
        val paths = listOf(
            "$localAppData\\Star Citizen",
            "$appData\\rsilern"  // RSI Launcher cache
        )
        
        // Also look for USER folder shaders
        val scPaths = mutableListOf<String>()
        scPaths.addAll(paths)
        
        // Common SC install locations
        listOf("C:", "D:", "E:").forEach { drive ->
            listOf(
                "$drive\\Program Files\\Roberts Space Industries\\StarCitizen\\LIVE\\USER\\Client\\0\\shaders",
                "$drive\\Games\\StarCitizen\\LIVE\\USER\\Client\\0\\shaders",
                "$drive\\StarCitizen\\LIVE\\USER\\Client\\0\\shaders"
            ).forEach { path ->
                if (File(path).exists()) {
                    scPaths.add(path)
                }
            }
        }
        
        return cleanPaths(scPaths, onLog, "Star Citizen")
    }
    
    /**
     * Clean Battlefield shaders
     */
    fun cleanBattlefieldCache(onLog: (String) -> Unit): CleanResult {
        onLog("[>>] Cleaning Battlefield shaders...")
        
        val paths = listOf(
            "$localAppData\\Battlefield 2042\\cache",
            "$localAppData\\Battlefield V\\cache",
            "$localAppData\\Battlefield 1\\cache",
            // Frostbite engine cache
            "$localAppData\\Electronic Arts\\EA Desktop\\cache"
        )
        
        return cleanPaths(paths, onLog, "Battlefield")
    }
    
    /**
     * Clean Windows temp files
     */
    fun cleanWindowsTemp(onLog: (String) -> Unit): CleanResult {
        onLog("[>>] Cleaning Windows temp files...")
        
        val tempDir = System.getenv("TEMP") ?: "$localAppData\\Temp"
        val paths = listOf(tempDir)
        
        return cleanPaths(paths, onLog, "Windows Temp")
    }
    
    /**
     * Clean ALL caches
     */
    fun cleanAll(onLog: (String) -> Unit): CleanResult {
        onLog("[>>] Cleaning all shader caches...")
        onLog("")
        
        var totalFiles = 0
        var totalBytes = 0L
        val allLocations = mutableListOf<String>()
        
        listOf(
            { cleanNvidiaCache(onLog) },
            { cleanAmdCache(onLog) },
            { cleanDirectXCache(onLog) },
            { cleanStarCitizenCache(onLog) },
            { cleanBattlefieldCache(onLog) }
        ).forEach { cleaner ->
            try {
                val result = cleaner()
                totalFiles += result.filesDeleted
                totalBytes += result.bytesFreed
                allLocations.addAll(result.locations)
            } catch (e: Exception) {
                onLog("[!] Error: ${e.message}")
            }
        }
        
        onLog("")
        onLog("[OK] Total: $totalFiles files, ${formatBytes(totalBytes)} freed")
        
        return CleanResult(totalFiles, totalBytes, allLocations)
    }
    
    private fun cleanPaths(paths: List<String>, onLog: (String) -> Unit, name: String): CleanResult {
        var files = 0
        var bytes = 0L
        val cleaned = mutableListOf<String>()
        
        for (path in paths) {
            val dir = File(path)
            if (dir.exists()) {
                try {
                    val result = deleteRecursively(dir)
                    files += result.first
                    bytes += result.second
                    cleaned.add(path)
                    onLog("[OK] Cleaned: ${dir.name} (${result.first} files)")
                } catch (e: Exception) {
                    onLog("[!] Cannot clean ${dir.name}: ${e.message}")
                }
            }
        }
        
        if (cleaned.isEmpty()) {
            onLog("[i] No $name cache found")
        }
        
        return CleanResult(files, bytes, cleaned)
    }
    
    private fun deleteRecursively(file: File): Pair<Int, Long> {
        var count = 0
        var size = 0L
        
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                val result = deleteRecursively(child)
                count += result.first
                size += result.second
            }
        }
        
        val fileSize = if (file.isFile) file.length() else 0L
        
        try {
            if (file.delete()) {
                count++
                size += fileSize
            }
        } catch (e: Exception) {
            // Skip locked files
        }
        
        return Pair(count, size)
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.2f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
            else -> "$bytes bytes"
        }
    }
}
