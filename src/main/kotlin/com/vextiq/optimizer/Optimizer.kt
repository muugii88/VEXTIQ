package com.vextiq.optimizer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Main Optimizer - Coordinates all optimization modules
 * HIGH BRANT EDITION
 */
class Optimizer {
    
    private val windowsOptimizer = WindowsOptimizer()
    private val gpuOptimizer = GpuOptimizer()
    private val networkOptimizer = NetworkOptimizer()
    private val cacheCleaner = CacheCleaner()
    private val gameProfileOptimizer = GameProfileOptimizer()
    private val gameSpecificOptimizer = GameSpecificOptimizer()
    private val advancedBooster = AdvancedBooster()
    private val backupManager = BackupManager()
    private val vextiqBrain = VextiqBrain()
    
    /**
     * Run Smart Boost - Full optimization for a specific game
     */
    suspend fun runSmartBoost(gameId: String, gameName: String, playstyle: String, onLog: (String) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            onLog("[>>] Starting Smart Boost for $gameName")
            onLog("[i] Profile: HIGH BRANT")
            onLog("[i] Mode: ${playstyle.uppercase()}")
            onLog("")
            
            var success = 0
            val total = 8
            
            // Step 1: Backup
            onLog("[1/$total] Creating backup...")
            try {
                backupManager.createBackup { }
                onLog("[OK] Backup created")
                success++
            } catch (e: Exception) {
                onLog("[!] Backup skipped")
            }
            delay(200)
            
            // Step 2: Game-specific optimization
            onLog("[2/$total] Loading $gameName profile...")
            try {
                gameSpecificOptimizer.optimize(gameId, playstyle, onLog)
                success++
            } catch (e: Exception) {
                onLog("[!] Profile error: ${e.message}")
            }
            delay(200)
            
            // Step 3: Timer Resolution (Input Lag fix)
            onLog("[3/$total] Timer Resolution...")
            try {
                advancedBooster.setTimerResolution(onLog)
                success++
            } catch (e: Exception) {
                onLog("[!] Timer error")
            }
            delay(200)
            
            // Step 4: Windows optimizations
            onLog("[4/$total] Windows optimization...")
            try {
                windowsOptimizer.enableGameMode { }
                windowsOptimizer.disableGameDVR { }
                windowsOptimizer.disableFullscreenOptimizations { }
                onLog("[OK] Windows optimized")
                success++
            } catch (e: Exception) {
                onLog("[!] Some Windows optimizations need admin")
            }
            delay(200)
            
            // Step 5: GPU optimization
            onLog("[5/$total] GPU optimization...")
            try {
                gpuOptimizer.autoOptimize(onLog)
                success++
            } catch (e: Exception) {
                onLog("[!] GPU error: ${e.message}")
            }
            delay(200)
            
            // Step 6: Shader cache
            onLog("[6/$total] Clearing shader cache...")
            try {
                cacheCleaner.cleanAll { }
                onLog("[OK] Cache cleaned")
                success++
            } catch (e: Exception) {
                onLog("[!] Cache error: ${e.message}")
            }
            delay(200)
            
            // Step 7: Network optimization
            onLog("[7/$total] Network optimization...")
            try {
                networkOptimizer.disableThrottling { }
                onLog("[OK] Network optimized")
                success++
            } catch (e: Exception) {
                onLog("[!] Network needs admin")
            }
            delay(200)
            
            // Step 8: Power plan + Core parking
            onLog("[8/$total] Power & CPU optimization...")
            try {
                advancedBooster.setUltimatePowerPlan { }
                advancedBooster.disableCoreParking { }
                onLog("[OK] Ultimate Performance active")
                onLog("[OK] All CPU cores active")
                success++
            } catch (e: Exception) {
                onLog("[!] Power plan error")
            }
            
            onLog("")
            onLog("═══════════════════════════════════")
            onLog("[OK] SMART BOOST COMPLETE!")
            onLog("[i] Success: $success/$total optimizations")
            onLog("[i] Restart $gameName for best results")
            
            success >= total / 2
        }
    }
    
    /**
     * Windows optimization only
     */
    suspend fun optimizeWindows(onLog: (String) -> Unit) = withContext(Dispatchers.IO) {
        windowsOptimizer.applyAll(onLog)
    }
    
    /**
     * GPU optimization only
     */
    suspend fun optimizeGpu(onLog: (String) -> Unit) = withContext(Dispatchers.IO) {
        gpuOptimizer.autoOptimize(onLog)
    }
    
    /**
     * Network optimization only
     */
    suspend fun optimizeNetwork(onLog: (String) -> Unit) = withContext(Dispatchers.IO) {
        networkOptimizer.applyAll(onLog)
    }
    
    /**
     * Clean all caches
     */
    suspend fun cleanCache(onLog: (String) -> Unit) = withContext(Dispatchers.IO) {
        cacheCleaner.cleanAll(onLog)
    }
    
    /**
     * Create backup
     */
    suspend fun createBackup(onLog: (String) -> Unit) = withContext(Dispatchers.IO) {
        backupManager.createBackup(onLog)
    }
    
    /**
     * Restore backup
     */
    suspend fun restoreBackup(onLog: (String) -> Unit) = withContext(Dispatchers.IO) {
        backupManager.restoreLatest(onLog)
    }
    
    /**
     * Detect GPU info
     */
    fun detectGpu(): GpuOptimizer.GpuInfo {
        return gpuOptimizer.detectGpu()
    }
}
