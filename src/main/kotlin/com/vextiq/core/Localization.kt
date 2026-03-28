package com.vextiq.core

/**
 * Localization - Multi-language support
 */
object Strings {
    
    private var currentLang = "en"
    
    fun setLanguage(lang: String) {
        currentLang = lang
    }
    
    fun get(key: String): String {
        return when (currentLang) {
            "mn" -> mongolian[key] ?: english[key] ?: key
            else -> english[key] ?: key
        }
    }
    
    fun getCurrentLanguage(): String = currentLang
    
    private val english = mapOf(
        // App
        "app_name" to "VEXTIQ PRO",
        "app_version" to "v1.8.1",
        "system_online" to "System Online",
        
        // Navigation
        "nav_dashboard" to "Dashboard",
        "nav_boost" to "System Boost",
        "nav_tools" to "Tools",
        "nav_settings" to "Settings",
        
        // Dashboard
        "target_game" to "TARGET GAME",
        "smart_boost" to "Smart Boost",
        "smart_boost_desc" to "Optimize %s for maximum performance",
        "optimize_now" to "OPTIMIZE NOW",
        "optimizing" to "OPTIMIZING...",
        "system_log" to "System Log",
        
        // Stats
        "cpu" to "CPU",
        "gpu" to "GPU",
        "ram" to "RAM",
        "ping" to "PING",
        "fps" to "FPS",
        "temp" to "TEMP",
        
        // Boost
        "select_game" to "SELECT GAME",
        "boost_log" to "Boost Log",
        "windows" to "Windows",
        "windows_desc" to "Game Mode, HAGS",
        "gpu_opt" to "GPU",
        "gpu_desc" to "Driver profile",
        "network" to "Network",
        "network_desc" to "TCP/IP optimize",
        "cache" to "Cache",
        "cache_desc" to "Clear shaders",
        
        // Tools
        "detect_hardware" to "Detect Hardware",
        "backup_settings" to "Backup Settings",
        "restore_backup" to "Restore Backup",
        "clean_cache" to "Clean All Cache",
        "network_boost" to "Network Boost",
        "ram_cleaner" to "RAM Cleaner",
        "startup_manager" to "Startup Manager",
        "scan_games" to "Scan Games",
        "system_info" to "SYSTEM INFORMATION",
        "tools_log" to "Tools Log",
        
        // Settings
        "language" to "Language",
        "theme" to "Theme",
        "dark_mode" to "Dark Mode",
        "light_mode" to "Light Mode",
        "fps_overlay" to "FPS Overlay",
        "minimize_tray" to "Minimize to Tray",
        "start_windows" to "Start with Windows",
        "auto_optimize" to "Auto-optimize on game launch",
        "check_updates" to "Check for updates",
        
        // Messages
        "msg_initialized" to "[OK] VEXTIQ PRO v11 initialized",
        "msg_monitoring" to "[OK] Real-time monitoring active",
        "msg_select_game" to "[i] Select a game and click OPTIMIZE NOW",
        "msg_boost_complete" to "[OK] BOOST COMPLETE!",
        "msg_restart_game" to "[i] Restart your game for best results",
        "msg_backup_created" to "[OK] Backup created",
        "msg_restored" to "[OK] Settings restored",
        "msg_cache_cleaned" to "[OK] Cache cleaned",
        "msg_ram_freed" to "[OK] RAM freed: %s MB",
        
        // New
        "sidebar_target" to "TARGET GAME",
        "sidebar_overlay" to "FPS Overlay",
        "sidebar_status" to "System Online",
        "tools_expert" to "EXPERT OPTIMIZATIONS",
        "tools_cleanup" to "CLEANUP",
        "tools_repair" to "SYSTEM REPAIR",
        "tools_repair_desc" to "Restore all Windows defaults",
        "settings_behavior" to "BEHAVIOR",
        "settings_overlay_content" to "FPS OVERLAY CONTENT",
        "tray_show" to "Show VEXTIQ PRO",
        "tray_toggle_fps" to "Toggle FPS Overlay",
        "tray_exit" to "Exit VEXTIQ PRO"
    )
    
    private val mongolian = mapOf(
        // App
        "app_name" to "VEXTIQ PRO",
        "app_version" to "v1.8.1",
        "system_online" to "Систем идэвхтэй",
        
        // Navigation
        "nav_dashboard" to "Хянах самбар",
        "nav_boost" to "Систем Boost",
        "nav_tools" to "Хэрэгслүүд",
        "nav_settings" to "Тохиргоо",
        
        // Dashboard
        "target_game" to "ТОГЛООМ",
        "smart_boost" to "Ухаалаг Boost",
        "smart_boost_desc" to "%s тоглоомыг оновчлох",
        "optimize_now" to "OPTIMIZE",
        "optimizing" to "АЖИЛЛАЖ БАЙНА...",
        "system_log" to "Системийн лог",
        
        // Stats
        "cpu" to "CPU",
        "gpu" to "GPU",
        "ram" to "RAM",
        "ping" to "PING",
        "fps" to "FPS",
        "temp" to "ТЕМП",
        
        // Boost
        "select_game" to "ТОГЛООМ СОНГОХ",
        "boost_log" to "Boost лог",
        "windows" to "Windows",
        "windows_desc" to "Game Mode, HAGS",
        "gpu_opt" to "GPU",
        "gpu_desc" to "Драйвер профайл",
        "network" to "Сүлжээ",
        "network_desc" to "TCP/IP оновчлох",
        "cache" to "Кэш",
        "cache_desc" to "Shader цэвэрлэх",
        
        // Tools
        "detect_hardware" to "Техник хэрэгсэл илрүүлэх",
        "backup_settings" to "Нөөц хадгалах",
        "restore_backup" to "Нөөц сэргээх",
        "clean_cache" to "Бүх кэш цэвэрлэх",
        "network_boost" to "Сүлжээ Boost",
        "ram_cleaner" to "RAM цэвэрлэгч",
        "startup_manager" to "Startup менежер",
        "scan_games" to "Тоглоом хайх",
        "system_info" to "СИСТЕМИЙН МЭДЭЭЛЭЛ",
        "tools_log" to "Хэрэгслийн лог",
        
        // Settings
        "language" to "Хэл",
        "theme" to "Загвар",
        "dark_mode" to "Бараан горим",
        "light_mode" to "Цайвар горим",
        "fps_overlay" to "FPS Overlay",
        "minimize_tray" to "Tray руу багасгах",
        "start_windows" to "Windows-тэй эхлэх",
        "auto_optimize" to "Автомат оновчлох",
        "check_updates" to "Шинэчлэл шалгах",
        
        // Messages
        "msg_initialized" to "[OK] VEXTIQ PRO v11 эхэлсэн",
        "msg_monitoring" to "[OK] Бодит цагийн хяналт идэвхтэй",
        "msg_select_game" to "[i] Тоглоом сонгоод OPTIMIZE дарна уу",
        "msg_boost_complete" to "[OK] BOOST ДУУССАН!",
        "msg_restart_game" to "[i] Тоглоомоо дахин эхлүүлнэ үү",
        "msg_backup_created" to "[OK] Нөөц үүсгэсэн",
        "msg_restored" to "[OK] Тохиргоо сэргээгдсэн",
        "msg_cache_cleaned" to "[OK] Кэш цэвэрлэгдсэн",
        "msg_ram_freed" to "[OK] RAM чөлөөлсөн: %s MB",
        
        // New
        "sidebar_target" to "ЗОРИЛТОТ ТОГЛООМ",
        "sidebar_overlay" to "FPS Overlay",
        "sidebar_status" to "Систем идэвхтэй",
        "tools_expert" to "ЭКСПЕРТ ОНОВЧЛОЛ",
        "tools_cleanup" to "ЦЭВЭРЛЭГЭЭ",
        "tools_repair" to "СИСТЕМ ЗАСВАРЛАХ",
        "tools_repair_desc" to "Windows-ийн анхны байдалд оруулах",
        "settings_behavior" to "ҮЙЛДЭЛ",
        "settings_overlay_content" to "OVERLAY-Д ХАРАГДАХ ЗҮЙЛС",
        "tray_show" to "VEXTIQ-г харуулах",
        "tray_toggle_fps" to "Overlay асаах/унтраах",
        "tray_exit" to "Гарах"
    )
}

// Extension function for easy access
fun String.localized(): String = Strings.get(this)
