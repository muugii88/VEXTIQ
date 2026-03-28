package com.vextiq.core

data class GamePlaystyle(
    val id: String,
    val name: String
)

data class Game(
    val id: String,
    val name: String,
    val icon: String,
    val profile: String,
    val color: Long = 0xFF00D2FF,
    val playstyles: List<GamePlaystyle> = listOf(
        GamePlaystyle("none", "None (General)"),
        GamePlaystyle("competitive", "Competitive Performance"),
        GamePlaystyle("balanced", "Balanced Smoothness"),
        GamePlaystyle("quality", "Visual Quality")
    )
)

object Games {
    val list = listOf(
        Game("star_citizen", "Star Citizen", "🌌", "SC", 0xFF00B4D8, listOf(
            GamePlaystyle("none", "None (General)"),
            GamePlaystyle("pvp", "PvP Combat"),
            GamePlaystyle("cargo", "Cargo Hauling"),
            GamePlaystyle("fps_bunker", "FPS Bunker Combat"),
            GamePlaystyle("exploration", "Exploration / Quality")
        )),
        Game("battlefield_6", "Battlefield 6", "🎖️", "BF6", 0xFFFF6B00, listOf(
            GamePlaystyle("none", "None (General)"),
            GamePlaystyle("competitive", "Competitive (Max FPS)"),
            GamePlaystyle("balanced", "Balanced Smoothness"),
            GamePlaystyle("immersion", "VFX / Immersion")
        )),
        Game("cs2", "Counter-Strike 2", "💥", "CS", 0xFFDE9B35, listOf(
            GamePlaystyle("competitive", "Competitive (Performance)"),
            GamePlaystyle("balanced", "Balanced"),
            GamePlaystyle("visibility", "High Visibility (PvP)")
        )),
        Game("valorant", "Valorant", "🎯", "VAL", 0xFFFF4655, listOf(
            GamePlaystyle("competitive", "Competitive Performance"),
            GamePlaystyle("balanced", "Balanced Smoothness")
        )),
        Game("ghost_recon_breakpoint", "Ghost Recon Breakpoint", "👻", "GRB", 0xFF4A5568, listOf(
            GamePlaystyle("stealth", "Stealth (Max Visuals)"),
            GamePlaystyle("tactical", "Tactical (Balanced)"),
            GamePlaystyle("combat", "Combat (High FPS)")
        )),
        Game("once_human", "Once Human", "🧟", "OH", 0xFF9333EA, listOf(
            GamePlaystyle("none", "None (General)"),
            GamePlaystyle("exploration", "Exploration Mode"),
            GamePlaystyle("dungeon", "Dungeon (Performance)"),
            GamePlaystyle("quality", "Max Quality / Cinematic")
        )),
        Game("division_2", "The Division 2", "🔶", "DIV2", 0xFFEA580C, listOf(
            GamePlaystyle("raid", "Raid (High Performance)"),
            GamePlaystyle("exploration", "Open World (Quality)"),
            GamePlaystyle("balanced", "Balanced Smoothness")
        )),
        Game("cyberpunk_2077", "Cyberpunk 2077", "🌆", "CP", 0xFFFFD700, listOf(
            GamePlaystyle("none", "None (General)"),
            GamePlaystyle("quality", "Max Visuals / RayTracing"),
            GamePlaystyle("balanced", "Balanced / DLSS"),
            GamePlaystyle("high_fps", "Performance (Max FPS)")
        )),
        Game("tarkov", "Escape from Tarkov", "☠️", "EFT", 0xFF1A1A1A, listOf(
            GamePlaystyle("competitive", "Competitive EFT"),
            GamePlaystyle("visibility", "Visibility Boost (No Blur)"),
            GamePlaystyle("balanced", "Balanced")
        )),
        Game("warzone", "Call of Duty Warzone", "🪖", "WZ", 0xFF4CAF50, listOf(
            GamePlaystyle("competitive", "Competitive (Performance)"),
            GamePlaystyle("visibility", "Visibility Focus"),
            GamePlaystyle("balanced", "Balanced Smoothness")
        )),
        Game("fortnite", "Fortnite", "🏝️", "FN", 0xFF9D4DFF, listOf(
            GamePlaystyle("competitive", "Competitive Performance"),
            GamePlaystyle("balanced", "Balanced Smoothness")
        )),
        Game("apex_legends", "Apex Legends", "🔺", "APEX", 0xFFDA292A, listOf(
            GamePlaystyle("competitive", "Competitive Performance"),
            GamePlaystyle("balanced", "Balanced Smoothness")
        )),
        Game("gta_v", "GTA V", "🚔", "GTA", 0xFF00FF00, listOf(
            GamePlaystyle("quality", "Visual Quality"),
            GamePlaystyle("balanced", "Balanced"),
            GamePlaystyle("high_fps", "High FPS Performance")
        )),
        Game("rust", "Rust", "🔶", "RUST", 0xFFCD412B, listOf(
            GamePlaystyle("competitive", "PvP Performance"),
            GamePlaystyle("balanced", "Balanced"),
            GamePlaystyle("visibility", "Visibility Over Distance")
        ))
    )
}


