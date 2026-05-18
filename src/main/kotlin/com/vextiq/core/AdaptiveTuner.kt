package com.vextiq.core

import com.vextiq.optimizer.ProcessManager
import kotlinx.coroutines.*

/**
 * Adaptive system tuner — watches frametime in real time and throttles
 * background processes when a stutter spike is detected.
 *
 * Spike rule: current frametime > 2x rolling median AND > 33ms (below 30 FPS).
 * Cooldown prevents firing more than once per [cooldownMs] window.
 */
class AdaptiveTuner(
    private val frametimeProvider: () -> Double,
    private val gameProvider: () -> String,
    private val windowSize: Int = 30,
    private val cooldownMs: Long = 15_000L,
    private val sampleIntervalMs: Long = 500L
) {
    private var monitoringJob: Job? = null
    private val frametimeWindow = ArrayDeque<Double>()
    private var lastActionTime = 0L
    private val processManager = ProcessManager()

    @Volatile var totalSpikes: Int = 0
        private set
    @Volatile var totalThrottles: Int = 0
        private set

    fun start(scope: CoroutineScope, onLog: (String) -> Unit = {}) {
        if (monitoringJob?.isActive == true) return
        monitoringJob = scope.launch(Dispatchers.IO) {
            onLog("[ADAPTIVE_TUNER] Started — watching for stutter spikes")
            while (isActive) {
                try {
                    val frametime = frametimeProvider()
                    if (frametime > 0) {
                        frametimeWindow.addLast(frametime)
                        while (frametimeWindow.size > windowSize) frametimeWindow.removeFirst()

                        if (frametimeWindow.size >= windowSize) {
                            val sorted = frametimeWindow.toList().sorted()
                            val median = sorted[sorted.size / 2]
                            val isSpike = frametime > median * 2.0 && frametime > 33.3

                            if (isSpike) {
                                totalSpikes++
                                val now = System.currentTimeMillis()
                                if (now - lastActionTime > cooldownMs) {
                                    lastActionTime = now
                                    totalThrottles++
                                    val game = gameProvider().ifEmpty { "GameProcess" }
                                    onLog(
                                        "[ADAPTIVE_TUNER] Spike: ${frametime.toInt()}ms (median ${median.toInt()}ms) — throttling background apps"
                                    )
                                    processManager.optimizeForGaming(game) { msg ->
                                        onLog("[ADAPTIVE_TUNER]   $msg")
                                    }
                                }
                            }
                        }
                    } else {
                        // Game stopped reporting frames — clear window so old data
                        // doesn't trigger false spikes on next game launch.
                        if (frametimeWindow.isNotEmpty()) frametimeWindow.clear()
                    }
                } catch (e: Exception) {
                    onLog("[ADAPTIVE_TUNER] Error: ${e.message}")
                }
                delay(sampleIntervalMs)
            }
        }
    }

    fun stop() {
        monitoringJob?.cancel()
        monitoringJob = null
        frametimeWindow.clear()
    }
}
