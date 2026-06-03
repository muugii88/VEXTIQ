package com.vextiq.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [GameDetection] — the single source of truth deciding which
 * running process counts as "the active game". These guard against regressions
 * where overlays/browsers/launchers get mistaken for a game.
 */
class GameDetectionTest {

    @Test
    fun `known games are detected case-insensitively`() {
        assertTrue(GameDetection.isKnownGame("StarCitizen"))
        assertTrue(GameDetection.isKnownGame("starcitizen"))
        assertTrue(GameDetection.isKnownGame("cs2"))
        assertTrue(GameDetection.isKnownGame("VALORANT-Win64-Shipping"))
        assertTrue(GameDetection.isKnownGame("r5apex"))
    }

    @Test
    fun `non-games are not known games`() {
        assertFalse(GameDetection.isKnownGame("Discord"))
        assertFalse(GameDetection.isKnownGame("chrome"))
        assertFalse(GameDetection.isKnownGame("explorer"))
    }

    @Test
    fun `ignored processes are flagged`() {
        assertTrue(GameDetection.isIgnored("Discord"))
        assertTrue(GameDetection.isIgnored("chrome"))
        assertTrue(GameDetection.isIgnored("Steam"))
        assertTrue(GameDetection.isIgnored("VEXTIQ"))
        // Substring + case-insensitive: a decorated process name still matches.
        assertTrue(GameDetection.isIgnored("Discord (Canary)"))
    }

    @Test
    fun `a real game is not ignored`() {
        assertFalse(GameDetection.isIgnored("StarCitizen"))
        assertFalse(GameDetection.isIgnored("Cyberpunk2077"))
    }

    @Test
    fun `pickActiveGame prefers a known game over a high-frame unknown`() {
        val counts = mapOf(
            "Discord" to 500,        // ignored
            "SomeApp" to 400,        // unknown
            "StarCitizen" to 120     // known game, fewer frames
        )
        assertEquals("StarCitizen", GameDetection.pickActiveGame(counts))
    }

    @Test
    fun `pickActiveGame picks the known game with the most frames`() {
        val counts = mapOf("cs2" to 80, "valorant" to 240)
        assertEquals("valorant", GameDetection.pickActiveGame(counts))
    }

    @Test
    fun `pickActiveGame never returns an ignored process`() {
        val counts = mapOf("Discord" to 1000, "chrome" to 900)
        assertEquals("", GameDetection.pickActiveGame(counts))
    }

    @Test
    fun `unknown process needs sustained frames to count`() {
        val low = mapOf("MysteryGame" to 10)
        assertEquals("", GameDetection.pickActiveGame(low, minFramesForUnknown = 50))

        val high = mapOf("MysteryGame" to 75)
        assertEquals("MysteryGame", GameDetection.pickActiveGame(high, minFramesForUnknown = 50))
    }

    @Test
    fun `empty frame map yields no game`() {
        assertEquals("", GameDetection.pickActiveGame(emptyMap()))
    }
}
