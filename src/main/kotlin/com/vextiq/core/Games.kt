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
    val genre: String = "Action",
    val color: Long = 0xFF00D2FF,
    val publisher: String = "",
    val imagePath: String = "",  // Resource path for game cover image
    val playstyles: List<GamePlaystyle> = listOf(
        GamePlaystyle("none", "None (General)"),
        GamePlaystyle("competitive", "Competitive Performance"),
        GamePlaystyle("balanced", "Balanced Smoothness"),
        GamePlaystyle("quality", "Visual Quality")
    )
)

object Games {
    val list = listOf(
        Game(
            id = "star_citizen", name = "Star Citizen", icon = "🌌", profile = "SC",
            genre = "Space Sim", color = 0xFF00B4D8, publisher = "Cloud Imperium Games",
            imagePath = "games/star_citizen.png",
            playstyles = listOf(
                GamePlaystyle("none", "None (General)"), GamePlaystyle("pvp", "PvP Combat"),
                GamePlaystyle("cargo", "Cargo Hauling"), GamePlaystyle("fps_bunker", "FPS Bunker Combat"),
                GamePlaystyle("exploration", "Exploration / Quality")
            )
        ),
        Game(
            id = "battlefield_6", name = "Battlefield 6", icon = "🎖️", profile = "BF6",
            genre = "FPS", color = 0xFFFF6B00, publisher = "Electronic Arts",
            imagePath = "games/battlefield_6.png",
            playstyles = listOf(
                GamePlaystyle("none", "None (General)"), GamePlaystyle("competitive", "Competitive (Max FPS)"),
                GamePlaystyle("balanced", "Balanced Smoothness"), GamePlaystyle("immersion", "VFX / Immersion")
            )
        ),
        Game(
            id = "cs2", name = "Counter-Strike 2", icon = "💥", profile = "CS",
            genre = "FPS", color = 0xFFDE9B35, publisher = "Valve",
            imagePath = "games/cs2.png",
            playstyles = listOf(
                GamePlaystyle("competitive", "Competitive (Performance)"),
                GamePlaystyle("balanced", "Balanced"), GamePlaystyle("visibility", "High Visibility (PvP)")
            )
        ),
        Game(
            id = "valorant", name = "Valorant", icon = "🎯", profile = "VAL",
            genre = "Tactical Shooter", color = 0xFFFF4655, publisher = "Riot Games",
            imagePath = "games/valorant.png",
            playstyles = listOf(
                GamePlaystyle("competitive", "Competitive Performance"), GamePlaystyle("balanced", "Balanced Smoothness")
            )
        ),
        Game(
            id = "ghost_recon_breakpoint", name = "Ghost Recon Breakpoint", icon = "👻", profile = "GRB",
            genre = "Action", color = 0xFF4A5568, publisher = "Ubisoft",
            imagePath = "games/ghost_recon_breakpoint.png",
            playstyles = listOf(
                GamePlaystyle("stealth", "Stealth (Max Visuals)"), GamePlaystyle("tactical", "Tactical (Balanced)"),
                GamePlaystyle("combat", "Combat (High FPS)")
            )
        ),
        Game(
            id = "once_human", name = "Once Human", icon = "🧟", profile = "OH",
            genre = "Survival", color = 0xFF9333EA, publisher = "Starry Studio",
            imagePath = "games/once_human.png",
            playstyles = listOf(
                GamePlaystyle("none", "None (General)"), GamePlaystyle("exploration", "Exploration Mode"),
                GamePlaystyle("dungeon", "Dungeon (Performance)"), GamePlaystyle("quality", "Max Quality / Cinematic")
            )
        ),
        Game(
            id = "division_2", name = "The Division 2", icon = "🔶", profile = "DIV2",
            genre = "Action RPG", color = 0xFFEA580C, publisher = "Ubisoft",
            imagePath = "games/division_2.png",
            playstyles = listOf(
                GamePlaystyle("raid", "Raid (High Performance)"), GamePlaystyle("exploration", "Open World (Quality)"),
                GamePlaystyle("pvp", "Dark Zone PvP")
            )
        ),
        Game(id = "cyberpunk", name = "Cyberpunk 2077", icon = "🌃", profile = "CP", genre = "RPG", color = 0xFFCCFF00, publisher = "CD Projekt Red", imagePath = "games/cyberpunk.png"),
        Game(id = "warzone", name = "Call of Duty: Warzone", icon = "🎖️", profile = "WZ", genre = "Battle Royale", color = 0xFF8B6914, publisher = "Activision", imagePath = "games/warzone.png"),
        Game(id = "fortnite", name = "Fortnite", icon = "🏗️", profile = "FN", genre = "Battle Royale", color = 0xFF4169E1, publisher = "Epic Games", imagePath = "games/fortnite.png"),
        Game(id = "apex", name = "Apex Legends", icon = "🏆", profile = "APEX", genre = "Battle Royale", color = 0xFFCC2936, publisher = "Respawn Entertainment", imagePath = "games/apex.png"),
        Game(id = "gtav", name = "GTA V", icon = "🚗", profile = "GTA", genre = "Action", color = 0xFF3B8132, publisher = "Rockstar Games", imagePath = "games/gtav.png"),
        Game(id = "rust", name = "Rust", icon = "🔨", profile = "RUST", genre = "Survival", color = 0xFFCD4A22, publisher = "Facepunch Studios", imagePath = "games/rust.png"),
        Game(id = "tarkov", name = "Escape from Tarkov", icon = "🎒", profile = "EFT", genre = "FPS", color = 0xFF5C5C5C, publisher = "Battlestate Games", imagePath = "games/tarkov.png")
    )
}


