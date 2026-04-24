package com.vextiq.optimizer

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Game-fix backup manager — stores zipped backups of directories before
 * destructive operations so the Fix is fully reversible.
 *
 * Layout:
 *   %APPDATA%\VEXTIQ\Backups\<fix-id>\<timestamp>\
 *     manifest.txt
 *     <name>.zip
 *
 * The manifest lists every archive + its original source path, so Restore
 * can unpack each archive back to its origin.
 */
object SmartFixBackup {

    data class Entry(val sourcePath: String, val archiveName: String)

    data class BackupHandle(
        val fixId: String,
        val timestamp: LocalDateTime,
        val folder: File,
        val entries: MutableList<Entry> = mutableListOf()
    )

    private val root: File by lazy {
        File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "VEXTIQ\\Backups")
            .apply { mkdirs() }
    }

    fun begin(fixId: String): BackupHandle {
        val ts = LocalDateTime.now()
        val folderName = ts.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val folder = File(root, "$fixId\\$folderName").apply { mkdirs() }
        return BackupHandle(fixId, ts, folder)
    }

    /**
     * Zip [source] (file or directory) into the backup folder under [archiveName].
     * Missing sources are skipped silently (returns false).
     */
    fun addSource(handle: BackupHandle, source: File, archiveName: String): Boolean {
        if (!source.exists()) return false
        val zipFile = File(handle.folder, "$archiveName.zip")
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                if (source.isFile) {
                    writeFileEntry(zos, source, source.name)
                } else {
                    val basePath = source.absolutePath
                    zipDirectory(zos, source, basePath)
                }
            }
        } catch (e: Exception) {
            System.err.println("[SmartFixBackup] Failed zip ${source.absolutePath}: ${e.message}")
            return false
        }
        handle.entries.add(Entry(source.absolutePath, archiveName))
        return true
    }

    fun commit(handle: BackupHandle) {
        val manifest = File(handle.folder, "manifest.txt")
        manifest.writeText(buildString {
            appendLine("VEXTIQ SmartFix Backup")
            appendLine("fixId=${handle.fixId}")
            appendLine("timestamp=${handle.timestamp}")
            appendLine("---")
            for (e in handle.entries) {
                appendLine("${e.archiveName}\t${e.sourcePath}")
            }
        })
    }

    fun listBackups(fixId: String): List<BackupHandle> {
        val dir = File(root, fixId)
        if (!dir.isDirectory) return emptyList()
        return (dir.listFiles() ?: emptyArray())
            .filter { it.isDirectory }
            .mapNotNull { folder ->
                val manifest = File(folder, "manifest.txt")
                if (!manifest.isFile) return@mapNotNull null
                val ts = try {
                    LocalDateTime.parse(
                        folder.name,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                    )
                } catch (_: Exception) { return@mapNotNull null }
                val entries = mutableListOf<Entry>()
                var past = false
                manifest.forEachLine { line ->
                    if (line == "---") { past = true; return@forEachLine }
                    if (!past) return@forEachLine
                    val parts = line.split("\t")
                    if (parts.size == 2) entries.add(Entry(parts[1], parts[0]))
                }
                BackupHandle(fixId, ts, folder, entries)
            }
            .sortedByDescending { it.timestamp }
    }

    fun restore(handle: BackupHandle, onLog: (String) -> Unit): Boolean {
        var allOk = true
        for (entry in handle.entries) {
            val zip = File(handle.folder, "${entry.archiveName}.zip")
            val target = File(entry.sourcePath)
            if (!zip.isFile) {
                onLog("[!] Missing archive ${entry.archiveName}")
                allOk = false
                continue
            }
            try {
                // Ensure parent exists
                target.parentFile?.mkdirs()
                unzipTo(zip, target)
                onLog("[OK] Restored ${entry.archiveName} → ${target.absolutePath}")
            } catch (e: Exception) {
                onLog("[!] Restore ${entry.archiveName} failed: ${e.message}")
                allOk = false
            }
        }
        return allOk
    }

    private fun zipDirectory(zos: ZipOutputStream, dir: File, basePath: String) {
        val stack = ArrayDeque<File>()
        stack.add(dir)
        while (stack.isNotEmpty()) {
            val cur = stack.removeFirst()
            val children = cur.listFiles() ?: continue
            for (c in children) {
                if (c.isDirectory) {
                    stack.add(c)
                } else {
                    val rel = c.absolutePath.removePrefix(basePath).trimStart('\\', '/').replace('\\', '/')
                    writeFileEntry(zos, c, rel)
                }
            }
        }
    }

    private fun writeFileEntry(zos: ZipOutputStream, file: File, entryName: String) {
        try {
            zos.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        } catch (_: Exception) {
            // skip unreadable files; still best-effort backup
            try { zos.closeEntry() } catch (_: Exception) {}
        }
    }

    private fun unzipTo(zip: File, target: File) {
        // If zip contained a single file with matching name, restore as file;
        // otherwise treat target as a directory.
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            var singleFileMode: Boolean? = null
            while (entry != null) {
                if (singleFileMode == null) {
                    // Look ahead: if only one entry and name matches target.name, treat as file
                    singleFileMode = (entry.name == target.name)
                }
                val dest = if (singleFileMode == true) target else File(target, entry.name)
                if (entry.isDirectory) {
                    dest.mkdirs()
                } else {
                    dest.parentFile?.mkdirs()
                    dest.outputStream().use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
                if (entry != null && singleFileMode == true) {
                    // More than one entry — not single-file mode after all
                    singleFileMode = false
                    // Re-handle the entry as directory entry
                    val dest2 = File(target, entry.name)
                    if (entry.isDirectory) dest2.mkdirs() else {
                        dest2.parentFile?.mkdirs()
                        dest2.outputStream().use { out -> zis.copyTo(out) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }
}
