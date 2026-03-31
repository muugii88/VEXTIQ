package com.vextiq.optimizer

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Backup and Restore Manager
 */
class BackupManager {
    
    private val backupDir = File(System.getProperty("user.home"), ".vextiq\\backups")
    
    init {
        backupDir.mkdirs()
    }
    
    data class BackupInfo(
        val name: String,
        val timestamp: LocalDateTime,
        val file: File,
        val size: Long
    )
    
    /**
     * Create backup of current settings
     */
    fun createBackup(onLog: (String) -> Unit): BackupInfo? {
        onLog("[>>] Creating settings backup...")
        
        val timestamp = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val backupName = "vextiq_backup_${timestamp.format(formatter)}.reg"
        val backupFile = File(backupDir, backupName)
        
        try {
            // Export relevant registry keys
            val keysToBackup = listOf(
                "HKCU\\Software\\Microsoft\\GameBar",
                "HKCU\\System\\GameConfigStore",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\GameDVR",
                "HKCU\\Control Panel\\Desktop",
                "HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences"
            )
            
            val allExports = StringBuilder()
            allExports.appendLine("Windows Registry Editor Version 5.00")
            allExports.appendLine("; VEXTIQ PRO Backup - ${timestamp.format(formatter)}")
            allExports.appendLine()
            
            for (key in keysToBackup) {
                try {
                    val tempFile = File.createTempFile("vextiq_export", ".reg")
                    val result = ProcessBuilder(listOf("reg", "export", key, tempFile.absolutePath, "/y"))
                        .redirectErrorStream(true)
                        .start()
                    result.waitFor()
                    
                    if (tempFile.exists()) {
                        // reg export outputs UTF-16 LE with BOM
                        val content = tempFile.readBytes().toString(Charsets.UTF_16)
                        // Skip the header, remove BOM if present, and append the rest
                        val cleanContent = content.replace("\uFEFF", "")
                        val lines = cleanContent.lines().filter { it.isNotBlank() && !it.startsWith("Windows Registry Editor Version 5.00") }
                        allExports.appendLine(lines.joinToString("\n"))
                        tempFile.delete()
                    }
                    onLog("[OK] Backed up: ${key.substringAfterLast("\\")}")
                } catch (e: Exception) {
                    onLog("[!] Cannot backup $key")
                }
            }
            
            backupFile.writeText(allExports.toString())
            onLog("[OK] Backup saved: $backupName")
            onLog("[i] Location: ${backupDir.absolutePath}")
            
            return BackupInfo(
                name = backupName,
                timestamp = timestamp,
                file = backupFile,
                size = backupFile.length()
            )
        } catch (e: Exception) {
            onLog("[!] Backup failed: ${e.message}")
            return null
        }
    }
    
    /**
     * List available backups
     */
    fun listBackups(): List<BackupInfo> {
        return backupDir.listFiles()
            ?.filter { it.name.endsWith(".reg") }
            ?.mapNotNull { file ->
                try {
                    val name = file.name
                    // Parse timestamp from filename
                    val dateStr = name.removePrefix("vextiq_backup_").removeSuffix(".reg")
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                    val timestamp = LocalDateTime.parse(dateStr, formatter)
                    
                    BackupInfo(
                        name = name,
                        timestamp = timestamp,
                        file = file,
                        size = file.length()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }
    
    /**
     * Restore from backup
     */
    fun restoreBackup(backup: BackupInfo, onLog: (String) -> Unit): Boolean {
        onLog("[>>] Restoring from ${backup.name}...")
        
        return try {
            val result = ProcessBuilder(listOf("reg", "import", backup.file.absolutePath))
                .redirectErrorStream(true)
                .start()
            
            val output = result.inputStream.bufferedReader().readText()
            val exitCode = result.waitFor()
            
            if (exitCode == 0) {
                onLog("[OK] Settings restored successfully")
                onLog("[i] Some changes may require restart")
                true
            } else {
                onLog("[!] Restore failed: $output")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Restore error: ${e.message}")
            false
        }
    }
    
    /**
     * Restore latest backup
     */
    fun restoreLatest(onLog: (String) -> Unit): Boolean {
        val backups = listBackups()
        
        if (backups.isEmpty()) {
            onLog("[!] No backups found")
            return false
        }
        
        val latest = backups.first()
        onLog("[i] Found latest backup: ${latest.name}")
        
        return restoreBackup(latest, onLog)
    }
    
    /**
     * Delete old backups (keep last N)
     */
    fun cleanOldBackups(keepCount: Int = 5, onLog: (String) -> Unit) {
        val backups = listBackups()
        
        if (backups.size <= keepCount) {
            onLog("[i] No old backups to clean")
            return
        }
        
        val toDelete = backups.drop(keepCount)
        var deleted = 0
        
        for (backup in toDelete) {
            try {
                backup.file.delete()
                deleted++
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        onLog("[OK] Deleted $deleted old backups")
    }
    
    /**
     * Restore backup (no parameter version - uses latest)
     */
    fun restoreBackup(onLog: (String) -> Unit): Boolean {
        return restoreLatest(onLog)
    }
}
