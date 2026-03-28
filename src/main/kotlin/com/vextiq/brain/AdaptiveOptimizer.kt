package com.vextiq.brain

import kotlinx.coroutines.*

class AdaptiveOptimizer(
    private val fpsProvider: () -> Int
) {
    private var currentConfig = mutableMapOf<String, Int>()
    private var targetFps = 60
    private var lastMeasuredFps = 0
    private var frameDropCounter = 0
    private var frameGainCounter = 0
    private var monitoringJob: Job? = null
    
    private val listeners = mutableListOf<(Map<String, Int>) -> Unit>()

    /**
     * Set initial config
     */
    fun setInitialConfig(config: Map<String, Int>) {
        currentConfig = config.toMutableMap()
        println("[ADAPTIVE] Initial config set with ${config.size} settings")
    }

    /**
     * Register listener for config changes
     */
    fun onConfigChanged(listener: (Map<String, Int>) -> Unit) {
        listeners.add(listener)
    }

    /**
     * Start real-time monitoring and adaptation
     */
    fun startMonitoring(scope: CoroutineScope, targetFps: Int = 60) {
        this.targetFps = targetFps
        frameDropCounter = 0
        frameGainCounter = 0
        
        monitoringJob = scope.launch(Dispatchers.IO) {
            println("[ADAPTIVE] Monitoring started (target: $targetFps FPS)")
            
            while (isActive) {
                try {
                    val currentFps = fpsProvider()
                    
                    if (lastMeasuredFps > 0) {
                        val fpsDiff = currentFps - lastMeasuredFps
                        
                        when {
                            // Severe drop: -20+ FPS
                            fpsDiff <= -20 -> {
                                frameDropCounter += 2
                                if (frameDropCounter >= 3) {
                                    println("[ADAPTIVE] 🔴 SEVERE drop ($fpsDiff FPS) - Aggressive reduction")
                                    aggressivelyReduceQuality()
                                    frameDropCounter = 0
                                }
                            }
                            // Moderate drop: -10 to -19 FPS
                            fpsDiff in -19..-10 -> {
                                frameDropCounter++
                                if (frameDropCounter >= 4) {
                                    println("[ADAPTIVE] 🟡 Moderate drop ($fpsDiff FPS) - Gentle reduction")
                                    gentlyReduceQuality()
                                    frameDropCounter = 0
                                }
                            }
                            // Headroom: +20 FPS or more
                            fpsDiff >= 20 -> {
                                frameGainCounter++
                                if (frameGainCounter >= 5) {
                                    println("[ADAPTIVE] 🟢 Headroom ($fpsDiff FPS) - Increase quality")
                                    increaseQuality()
                                    frameGainCounter = 0
                                }
                            }
                            else -> {
                                frameDropCounter = 0
                                frameGainCounter = 0
                            }
                        }
                    }
                    
                    lastMeasuredFps = currentFps
                    delay(1000)  // Check every second
                    
                } catch (e: Exception) {
                    println("[ADAPTIVE] Monitoring error: ${e.message}")
                }
            }
        }
    }

    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        println("[ADAPTIVE] Monitoring stopped")
    }

    /**
     * Aggressive quality reduction (emergency)
     */
    private fun aggressivelyReduceQuality() {
        val config = currentConfig.toMutableMap()
        var changed = false

        // Priority 1: Disable ray-traced reflections (+40 FPS)
        if ((config["rt_reflections"] ?: 0) > 0) {
            config["rt_reflections"] = 0
            println("  ⚡ Disabled ray-traced reflections")
            changed = true
        }

        // Priority 2: Reduce post-processing (+15 FPS)
        if ((config["r_PostProcess"] ?: 1) > 1) {
            config["r_PostProcess"] = 1
            println("  ⚡ Reduced post-processing")
            changed = true
        }

        // Priority 3: Reduce shadow resolution (+20 FPS)
        if ((config["r_ShadowRes"] ?: 2) > 1) {
            config["r_ShadowRes"] = 1
            println("  ⚡ Reduced shadow resolution")
            changed = true
        }

        // Priority 4: Reduce graphics quality (+25 FPS)
        if ((config["r_GraphicsQuality"] ?: 2) > 1) {
            config["r_GraphicsQuality"] = 1
            println("  ⚡ Reduced graphics quality to LOW")
            changed = true
        }

        // Priority 5: Enable VSync 60 (frame cap, last resort)
        if ((config["r_VSync"] ?: 0) == 0) {
            config["r_VSync"] = 1
            println("  ⚡ Enabled VSync 60 (frame cap)")
            changed = true
        }

        if (changed) {
            applyConfig(config)
        }
    }

    /**
     * Gentle quality reduction
     */
    private fun gentlyReduceQuality() {
        val config = currentConfig.toMutableMap()
        var changed = false

        // Priority 1: Disable ray-traced shadows (+20 FPS)
        if ((config["rt_shadows"] ?: 0) > 0) {
            config["rt_shadows"] = 0
            println("  ⚡ Disabled ray-traced shadows")
            changed = true
        }

        // Priority 2: Reduce view distance (+10 FPS)
        if ((config["e_ViewDistRatio"] ?: 70) > 50) {
            config["e_ViewDistRatio"] = (config["e_ViewDistRatio"] ?: 70) - 10
            println("  ⚡ Reduced view distance")
            changed = true
        }

        // Priority 3: Reduce shadow resolution slightly (+10 FPS)
        if ((config["r_ShadowRes"] ?: 3) > 2) {
            config["r_ShadowRes"] = 2
            println("  ⚡ Reduced shadow resolution")
            changed = true
        }

        if (changed) {
            applyConfig(config)
        }
    }

    /**
     * Increase quality (headroom detected)
     */
    private fun increaseQuality() {
        val config = currentConfig.toMutableMap()
        var changed = false

        // Priority 1: Increase shadow resolution
        if ((config["r_ShadowRes"] ?: 2) < 4) {
            config["r_ShadowRes"] = (config["r_ShadowRes"] ?: 2) + 1
            println("  ⬆️  Increased shadow resolution")
            changed = true
        }

        // Priority 2: Enable ray-traced reflections
        if ((config["rt_reflections"] ?: 0) < 2) {
            config["rt_reflections"] = 1
            println("  ⬆️  Enabled ray-traced reflections")
            changed = true
        }

        // Priority 3: Increase post-processing
        if ((config["r_PostProcess"] ?: 1) < 3) {
            config["r_PostProcess"] = (config["r_PostProcess"] ?: 1) + 1
            println("  ⬆️  Increased post-processing")
            changed = true
        }

        // Priority 4: Increase graphics quality
        if ((config["r_GraphicsQuality"] ?: 2) < 4) {
            config["r_GraphicsQuality"] = (config["r_GraphicsQuality"] ?: 2) + 1
            println("  ⬆️  Increased graphics quality")
            changed = true
        }

        // Priority 5: Increase view distance
        if ((config["e_ViewDistRatio"] ?: 70) < 100) {
            config["e_ViewDistRatio"] = (config["e_ViewDistRatio"] ?: 70) + 5
            println("  ⬆️  Increased view distance")
            changed = true
        }

        if (changed) {
            applyConfig(config)
        }
    }

    /**
     * Apply config and notify listeners
     */
    private fun applyConfig(config: Map<String, Int>) {
        currentConfig = config.toMutableMap()
        listeners.forEach { listener -> listener(config) }
    }

    /**
     * Get current config
     */
    fun getCurrentConfig(): Map<String, Int> = currentConfig.toMap()

    /**
     * Print stats
     */
    fun printStats() {
        println("""
            ╔════════════════════════════════════════╗
            ║      ADAPTIVE OPTIMIZER STATS          ║
            ╚════════════════════════════════════════╝
            Last FPS:         $lastMeasuredFps
            Target FPS:       $targetFps
            Drop Counter:     $frameDropCounter
            Gain Counter:     $frameGainCounter
            Active Settings:  ${currentConfig.size}
        """.trimIndent())
    }
}