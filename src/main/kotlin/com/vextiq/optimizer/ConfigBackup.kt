package com.vextiq.optimizer

import java.io.File

/**
 * Shared helper for the one-time `.vextiq.bak` backups that the game-config
 * writers create before overwriting a game's settings file.
 *
 * Keeping the suffix and the restore logic in one place means "write a config"
 * and "Undo All" can never drift apart, and the restore step is unit-testable
 * without touching any real game directory.
 */
object ConfigBackup {

    const val SUFFIX = ".vextiq.bak"

    /** Backup file name for [originalName] (e.g. GameUserSettings.ini → GameUserSettings.ini.vextiq.bak). */
    fun backupNameFor(originalName: String): String = originalName + SUFFIX

    /**
     * Restore every `<name>$SUFFIX` in [dir] back to `<name>`, then delete the
     * backup. Returns the list of restored original file names.
     */
    fun restoreAll(dir: File): List<String> {
        if (!dir.isDirectory) return emptyList()
        val restored = mutableListOf<String>()
        dir.listFiles { f -> f.isFile && f.name.endsWith(SUFFIX) }?.forEach { bak ->
            val original = File(dir, bak.name.removeSuffix(SUFFIX))
            try {
                bak.copyTo(original, overwrite = true)
                bak.delete()
                restored += original.name
            } catch (_: Exception) {
                // Leave the backup in place if restore fails — better to keep it.
            }
        }
        return restored
    }
}
