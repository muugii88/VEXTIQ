package com.vextiq.core

/**
 * Single source of truth for "is this process a game".
 *
 * Used by [FpsMonitor], [EtwFpsMonitor], and network latency code so all three
 * agree on which running process counts as the active game. Without this, each
 * detector grew its own filter list and they drifted apart — that's how the
 * app started "grabbing" Discord or Chrome as the active game.
 */
object GameDetection {

    /**
     * Process names (or substrings, case-insensitive) that must NEVER be
     * treated as a game even if they render many frames.
     */
    val ignoreList: List<String> = listOf(
        // Browsers — Chromium variants render lots of frames
        "chrome", "msedge", "firefox", "brave", "opera", "vivaldi", "iexplore",
        // Communication
        "Discord", "Skype", "Teams", "Slack", "Zoom", "WhatsApp", "Telegram",
        "Signal", "Viber", "WebexHost",
        // Streaming / capture
        "Spotify", "obs64", "obs32", "streamlabs", "XSplit", "Twitch", "Overwolf",
        "NVIDIA Share", "NvBackend", "ShadowPlay", "RadeonSoftware", "RTSS",
        "Afterburner", "PrecisionX",
        // Media
        "vlc", "mpc-hc64", "mpv", "PotPlayer", "Netflix", "PrimeVideo",
        // Dev tools / editors
        "Code", "Cursor", "idea64", "pycharm64", "webstorm64", "rubymine64",
        "sublime_text", "Notepad", "notepad++", "atom", "cmd", "powershell",
        "WindowsTerminal", "wt", "ConEmu", "Hyper",
        // Office
        "WINWORD", "EXCEL", "POWERPNT", "OUTLOOK", "ONENOTE", "Acrobat", "AcroRd32",
        // Game launchers (UI-only, not gameplay)
        "Steam", "steamwebhelper", "EpicGamesLauncher", "GalaxyClient",
        "Battle.net", "BlizzardError", "Origin", "EADesktop", "EALauncher",
        "Uplay", "UplayWebCore", "RockstarGamesLauncher", "RiotClientServices",
        "BethesdaNetLauncher", "itchio",
        // System UI processes that render frames
        "Taskmgr", "explorer", "SearchHost", "StartMenuExperienceHost",
        "ApplicationFrameHost", "TextInputHost", "ShellExperienceHost",
        "dwm", "LockApp", "Widgets", "ctfmon", "RuntimeBroker",
        // Our own app and runtimes
        "VEXTIQ", "java", "javaw", "node", "electron", "python",
        "NVIDIA Web Helper", "GameBar", "GameBarPresenceWriter"
    )

    /**
     * Process names (substrings, case-insensitive) we explicitly know are games.
     * Matching the whitelist is the strongest signal — overrides the
     * unknown-candidate heuristic.
     */
    val knownGames: List<String> = listOf(
        "StarCitizen", "starcitizen",
        "BF2042", "bf2042", "Battlefield", "BF6", "Battlefield6",
        "Cyberpunk2077", "Cyberpunk",
        "cs2", "csgo", "Counter-Strike",
        "VALORANT", "valorant",
        "r5apex", "ApexLegends", "Apex",
        "FortniteClient", "Fortnite",
        "EscapeFromTarkov", "Tarkov",
        "RustClient", "rust",
        "GTA5", "GTAV", "PlayGTAV",
        "cod", "ModernWarfare", "BlackOpsColdWar", "Warzone", "BlackOps",
        "PUBG", "TslGame",
        "Overwatch",
        "LeagueOfLegends", "League of Legends",
        "Dota2", "dota2",
        "WorldOfWarcraft", "Wow", "WowClassic",
        "Minecraft", "MinecraftLauncher",
        "EldenRing", "DarkSouls",
        "Hogwarts", "HogwartsLegacy",
        "Diablo", "DiabloIV",
        "PathOfExile", "PathOfExile_x64",
        "DestinyTwo", "destiny2",
        "RocketLeague",
        "RainbowSix", "RainbowSixSiege",
        "DeadByDaylight",
        "NARAKA", "NarakaBladepoint",
        "TheFinals",
        "HellLetLoose",
        "SquadGame", "Squad",
        "arma3", "Arma3",
        "DayZ",
        "TheWitcher3",
        "Baldur", "bg3",
        "Lethal Company", "Phasmophobia",
        "SeaOfThieves",
        "NoMansSky",
        "MicrosoftFlightSimulator", "FlightSimulator",
        "DeltaForce", "deltaforce", "DFBarracks",
        "XDefiant", "xdefiant",
        "GhostRecon", "GRB", "GhostReconBreakpoint",
        "TheDivision", "TheDivision2", "thedivision",
        "OnceHuman", "oncehuman",
        "Warthunder", "aces",
        "MarvelRivals", "marvel",
        "Palworld",
        "Enshrouded",
        "GrayZone", "GrayZoneWarfare",
        "Deadlock",
        "FragPunk", "fragpunk",
        "InZoi", "inzoi",
        "MonsterHunter", "MonsterHunterWilds",
        "SpaceMarine2", "SpaceMarine",
        "StalkerGame", "Stalker2",
        "CallistoProtocol",
        "ReadyOrNot",
        "Insurgency", "InsurgencySandstorm",
        "HuntShowdown", "Hunt",
        "Helldivers", "helldivers2",
        "GroundBranch",
        "Satisfactory",
        "Factorio"
    )

    /** True if the process name matches the ignore list (substring, case-insensitive). */
    fun isIgnored(processName: String): Boolean =
        ignoreList.any { processName.contains(it, ignoreCase = true) }

    /** True if the process name matches the known-games whitelist. */
    fun isKnownGame(processName: String): Boolean =
        knownGames.any { processName.contains(it, ignoreCase = true) }

    /**
     * Pick the best candidate process name from a frame-count map.
     *
     * Rules:
     *  1. Prefer the known-game with the highest frame count.
     *  2. Fallback: any non-ignored process with frame count > [minFramesForUnknown].
     *  3. Otherwise empty string ("no game").
     *
     * @param minFramesForUnknown stricter than the old default — must be sustained
     *   rendering (a game at 60 fps over a 1s window). Prevents Discord's
     *   sporadic UI frames from being mistaken for a game.
     */
    fun pickActiveGame(
        frameCounts: Map<String, Int>,
        minFramesForUnknown: Int = 50
    ): String {
        val knownWinner = frameCounts.entries
            .filter { isKnownGame(it.key) }
            .maxByOrNull { it.value }
        if (knownWinner != null) return knownWinner.key

        val unknownWinner = frameCounts.entries
            .filter { !isIgnored(it.key) && !isKnownGame(it.key) }
            .filter { it.value >= minFramesForUnknown }
            .maxByOrNull { it.value }
        return unknownWinner?.key ?: ""
    }
}
