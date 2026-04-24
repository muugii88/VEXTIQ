package com.vextiq.core

/**
 * Game server registry — maps game process names to their regional server endpoints.
 * Used for accurate latency probing to the actual game servers the user connects to,
 * especially relevant for Mongolia where closest regions (SG/JP/KR/HK) vary per game.
 */
object GameServerDatabase {

    enum class Protocol { UDP, TCP }

    data class GameInfo(
        val id: String,
        val displayName: String,
        val processNames: List<String>,
        val protocol: Protocol,
        val regions: Map<String, String>
    )

    private val games: List<GameInfo> = listOf(
        GameInfo(
            id = "valorant",
            displayName = "Valorant",
            processNames = listOf("VALORANT-Win64-Shipping", "VALORANT", "valorant"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Singapore" to "gp-sg-sin-1.a.pvp.net",
                "Tokyo"     to "gp-jp-tk-1.a.pvp.net",
                "HongKong"  to "gp-hk-hk-1.a.pvp.net",
                "Seoul"     to "gp-kr-kr-1.a.pvp.net",
                "Sydney"    to "gp-au-syd-1.a.pvp.net",
                "US"        to "na.leagueoflegends.com",
                "Europe"    to "euw.leagueoflegends.com"
            )
        ),
        GameInfo(
            id = "cs2",
            displayName = "Counter-Strike 2",
            processNames = listOf("cs2"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Singapore" to "scl.valve.net.sg",
                "Tokyo"     to "tyo.valve.net.sg",
                "HongKong"  to "hkg.valve.net",
                "Seoul"     to "sel.valve.net",
                "US"        to "iad.valve.net",
                "Europe"    to "fra.valve.net"
            )
        ),
        GameInfo(
            id = "dota2",
            displayName = "Dota 2",
            processNames = listOf("dota2"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Singapore" to "155.133.248.50",
                "Tokyo"     to "155.133.246.50",
                "Europe"    to "155.133.227.74",  // Stockholm
                "US"        to "155.133.239.50"   // NA East
            )
        ),
        GameInfo(
            id = "lol",
            displayName = "League of Legends",
            processNames = listOf("League of Legends", "LeagueClient"),
            protocol = Protocol.TCP,
            regions = linkedMapOf(
                "Singapore" to "sg.leagueoflegends.com",
                "Tokyo"     to "jp.leagueoflegends.com",
                "Seoul"     to "kr.leagueoflegends.com"
            )
        ),
        GameInfo(
            id = "overwatch",
            displayName = "Overwatch 2",
            processNames = listOf("Overwatch"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Tokyo"     to "jp.actual.battle.net",
                "Seoul"     to "kr.actual.battle.net",
                "Singapore" to "sg.actual.battle.net",
                "US"        to "us.actual.battle.net",
                "Europe"    to "eu.actual.battle.net"
            )
        ),
        GameInfo(
            id = "pubg",
            displayName = "PUBG",
            processNames = listOf("TslGame"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Singapore" to "pubg-as-sg.krafton.com",
                "Tokyo"     to "pubg-as-jp.krafton.com",
                "Seoul"     to "pubg-as-kr.krafton.com"
            )
        ),
        GameInfo(
            id = "apex",
            displayName = "Apex Legends",
            processNames = listOf("r5apex"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Tokyo"     to "ping-tok.ea.com",
                "Singapore" to "ping-sin.ea.com",
                "Seoul"     to "ping-kor.ea.com",
                "Sydney"    to "ping-syd.ea.com",
                "US"        to "ping-iad.ea.com",
                "Europe"    to "ping-fra.ea.com"
            )
        ),
        GameInfo(
            id = "fortnite",
            displayName = "Fortnite",
            processNames = listOf("FortniteClient-Win64-Shipping"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Singapore" to "ping-sg.epicgames.com",
                "Tokyo"     to "ping-tyo.epicgames.com",
                "US"        to "ping-na.epicgames.com",
                "Europe"    to "ping-eu.epicgames.com"
            )
        ),
        GameInfo(
            id = "genshin",
            displayName = "Genshin Impact",
            processNames = listOf("GenshinImpact", "YuanShen"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Asia"    to "os.hoyoverse.com",
                "America" to "na.os.hoyoverse.com"
            )
        ),
        GameInfo(
            id = "wuwa",
            displayName = "Wuthering Waves",
            processNames = listOf("Client-Win64-Shipping", "Wuthering Waves"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Asia" to "api-os.wutheringwaves.kurogame.com"
            )
        ),
        GameInfo(
            id = "wow",
            displayName = "World of Warcraft",
            processNames = listOf("WowClassic", "Wow"),
            protocol = Protocol.TCP,
            regions = linkedMapOf(
                "Seoul" to "kr.actual.battle.net",
                "Tokyo" to "jp.actual.battle.net"
            )
        ),
        GameInfo(
            id = "warthunder",
            displayName = "War Thunder",
            processNames = listOf("aces"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Europe" to "login.warthunder.com"
            )
        ),
        GameInfo(
            id = "starcitizen",
            displayName = "Star Citizen",
            processNames = listOf("StarCitizen", "starcitizen"),
            protocol = Protocol.TCP,
            regions = linkedMapOf(
                "US" to "public.cloudimperiumgames.com"
            )
        ),
        GameInfo(
            id = "battlefield",
            displayName = "Battlefield 2042",
            processNames = listOf("BF2042", "bf2042", "battlefield"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Tokyo"     to "ping-tok.ea.com",
                "Singapore" to "ping-sin.ea.com",
                "Sydney"    to "ping-syd.ea.com"
            )
        ),
        GameInfo(
            id = "cod",
            displayName = "Call of Duty",
            processNames = listOf("ModernWarfare", "BlackOpsColdWar", "cod"),
            protocol = Protocol.UDP,
            regions = linkedMapOf(
                "Asia" to "cod-as.publisher.activision.com"
            )
        )
    )

    private val byProcess: Map<String, GameInfo> = buildMap {
        games.forEach { g ->
            g.processNames.forEach { name -> put(name.lowercase(), g) }
        }
    }

    fun findByProcessName(processName: String): GameInfo? {
        val cleaned = processName.removeSuffix(".exe").lowercase()
        byProcess[cleaned]?.let { return it }
        // Partial match fallback
        return games.firstOrNull { g ->
            g.processNames.any { it.lowercase().contains(cleaned) || cleaned.contains(it.lowercase()) }
        }
    }

    fun findById(id: String): GameInfo? = games.firstOrNull { it.id == id }

    fun all(): List<GameInfo> = games
}
