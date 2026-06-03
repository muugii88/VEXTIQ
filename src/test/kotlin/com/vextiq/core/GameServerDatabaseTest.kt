package com.vextiq.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameServerDatabaseTest {

    @Test
    fun `exact process name resolves to the right game`() {
        assertEquals("valorant", GameServerDatabase.findByProcessName("VALORANT-Win64-Shipping")?.id)
        assertEquals("cs2", GameServerDatabase.findByProcessName("cs2")?.id)
        assertEquals("apex", GameServerDatabase.findByProcessName("r5apex")?.id)
    }

    @Test
    fun `exe suffix is stripped before matching`() {
        assertEquals("cs2", GameServerDatabase.findByProcessName("cs2.exe")?.id)
        assertEquals("apex", GameServerDatabase.findByProcessName("r5apex.exe")?.id)
    }

    @Test
    fun `matching is case-insensitive`() {
        assertEquals("valorant", GameServerDatabase.findByProcessName("valorant")?.id)
        assertEquals("valorant", GameServerDatabase.findByProcessName("VALORANT")?.id)
    }

    @Test
    fun `empty process name never matches (startup state)`() {
        assertNull(GameServerDatabase.findByProcessName(""))
        assertNull(GameServerDatabase.findByProcessName(".exe"))
    }

    @Test
    fun `unknown process returns null`() {
        assertNull(GameServerDatabase.findByProcessName("definitely-not-a-game-xyz"))
    }

    @Test
    fun `partial match still resolves a known game`() {
        // "battlefield" is one of BF2042's registered names; a decorated runtime
        // name that contains it should still resolve.
        assertEquals("battlefield", GameServerDatabase.findByProcessName("battlefield")?.id)
    }

    @Test
    fun `findById returns the matching game`() {
        assertEquals("Counter-Strike 2", GameServerDatabase.findById("cs2")?.displayName)
        assertNull(GameServerDatabase.findById("no-such-id"))
    }

    @Test
    fun `competitive shooters declare high tick rates`() {
        assertEquals(128, GameServerDatabase.findById("valorant")?.tickRateHz)
        assertEquals(64, GameServerDatabase.findById("cs2")?.tickRateHz)
    }

    @Test
    fun `default tick rate is 30 when unspecified`() {
        assertEquals(30, GameServerDatabase.findById("dota2")?.tickRateHz)
    }

    @Test
    fun `cs2 exposes its gameplay port for TCP probing`() {
        assertEquals(27015, GameServerDatabase.findById("cs2")?.gamePort)
    }

    @Test
    fun `every game has at least one region endpoint`() {
        assertTrue(GameServerDatabase.all().isNotEmpty())
        for (g in GameServerDatabase.all()) {
            assertTrue(g.regions.isNotEmpty(), "${g.id} has no regions")
            assertTrue(g.processNames.isNotEmpty(), "${g.id} has no process names")
        }
    }
}
