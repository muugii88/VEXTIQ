package com.vextiq.optimizer

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigBackupTest {

    @Test
    fun `backupNameFor appends the suffix`() {
        assertEquals("GameUserSettings.ini.vextiq.bak", ConfigBackup.backupNameFor("GameUserSettings.ini"))
    }

    @Test
    fun `restoreAll restores original from backup and deletes the backup`() {
        val dir = Files.createTempDirectory("vextiq-cfg").toFile()
        try {
            val original = File(dir, "GameUserSettings.ini").apply { writeText("MODIFIED BY VEXTIQ") }
            val backup = File(dir, ConfigBackup.backupNameFor("GameUserSettings.ini")).apply { writeText("PRISTINE ORIGINAL") }

            val restored = ConfigBackup.restoreAll(dir)

            assertEquals(listOf("GameUserSettings.ini"), restored)
            assertEquals("PRISTINE ORIGINAL", original.readText())
            assertFalse(backup.exists(), "backup should be removed after a successful restore")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `restoreAll restores multiple backups in a directory`() {
        val dir = Files.createTempDirectory("vextiq-cfg-multi").toFile()
        try {
            File(dir, "a.ini").writeText("new-a")
            File(dir, ConfigBackup.backupNameFor("a.ini")).writeText("old-a")
            File(dir, "b.xml").writeText("new-b")
            File(dir, ConfigBackup.backupNameFor("b.xml")).writeText("old-b")

            val restored = ConfigBackup.restoreAll(dir).sorted()

            assertEquals(listOf("a.ini", "b.xml"), restored)
            assertEquals("old-a", File(dir, "a.ini").readText())
            assertEquals("old-b", File(dir, "b.xml").readText())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `restoreAll is a no-op when there are no backups`() {
        val dir = Files.createTempDirectory("vextiq-cfg-none").toFile()
        try {
            File(dir, "settings.xml").writeText("UNCHANGED")
            assertTrue(ConfigBackup.restoreAll(dir).isEmpty())
            assertEquals("UNCHANGED", File(dir, "settings.xml").readText())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `restoreAll on a missing directory returns empty`() {
        assertTrue(ConfigBackup.restoreAll(File("/no/such/vextiq/dir/zzz")).isEmpty())
    }
}
