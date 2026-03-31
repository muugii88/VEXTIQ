package com.vextiq.brain

import java.io.File
import java.util.Properties

/**
 * MLOptimizer - History-based Learning Optimizer
 * 
 * Tracks past optimizations and their FPS results.
 * Uses weighted scoring to learn which settings work best
 * for different hardware + game combinations.
 */
class MLOptimizer {

    data class OptimizationRecord(
        val gameId: String,
        val gpuTier: String,
        val playstyle: String,
        val settings: Map<String, Int>,
        val resultFps: Int,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val history = mutableListOf<OptimizationRecord>()
    private val settingsWeights = mutableMapOf<String, Double>()
    private val storageFile = File(System.getProperty("user.home"), ".vextiq/ml_history.properties")

    init {
        loadHistory()
    }

    /**
     * Record an optimization result for learning
     */
    fun recordResult(gameId: String, gpuTier: String, playstyle: String, settings: Map<String, Int>, resultFps: Int) {
        val record = OptimizationRecord(gameId, gpuTier, playstyle, settings, resultFps)
        history.add(record)
        updateWeights(record)
        saveHistory()
        println("[ML] Recorded: $gameId | $gpuTier | ${resultFps}fps | ${history.size} total records")
    }

    /**
     * Predict best settings for a game based on history
     */
    fun predictBestSettings(gameId: String, gpuTier: String, playstyle: String): Map<String, Int>? {
        val matches = history.filter { record ->
            record.gameId == gameId && record.gpuTier == gpuTier
        }

        if (matches.size < 2) {
            println("[ML] Not enough data for $gameId ($gpuTier) - ${matches.size} records")
            return null
        }

        // Score by FPS + recency + playstyle match
        val now = System.currentTimeMillis()
        val scored = matches.map { record ->
            val recency = 1.0 - ((now - record.timestamp).toDouble() / (30L * 24 * 60 * 60 * 1000)).coerceIn(0.0, 0.8)
            val fpsScore = record.resultFps.toDouble() / 200.0
            val playstyleBonus = if (record.playstyle == playstyle) 0.2 else 0.0
            Pair(record, fpsScore + recency + playstyleBonus)
        }.sortedByDescending { it.second }

        // Weighted average of top 3 results
        val topResults = scored.take(3).map { it.first }
        val allKeys = topResults.flatMap { it.settings.keys }.toSet()

        val bestSettings = mutableMapOf<String, Int>()
        for (key in allKeys) {
            val values = topResults.mapNotNull { it.settings[key] }
            if (values.isNotEmpty()) {
                val weightedSum = values.mapIndexed { i, v -> v * (3.0 - i) }.sum()
                val totalWeight = values.indices.sumOf { (3.0 - it) }
                bestSettings[key] = (weightedSum / totalWeight).toInt()
            }
        }

        println("[ML] Predicted settings from ${topResults.size} best records for $gameId")
        return bestSettings
    }

    /**
     * Get optimization confidence (0-100) based on history depth
     */
    fun getConfidence(gameId: String, gpuTier: String): Int {
        val matches = history.count { it.gameId == gameId && it.gpuTier == gpuTier }
        return when {
            matches >= 10 -> 95
            matches >= 5 -> 80
            matches >= 3 -> 65
            matches >= 1 -> 40
            else -> 10
        }
    }

    /**
     * Get average FPS for a game from history
     */
    fun getAverageFps(gameId: String, gpuTier: String): Int {
        val matches = history.filter { it.gameId == gameId && it.gpuTier == gpuTier }
        return if (matches.isNotEmpty()) matches.map { it.resultFps }.average().toInt() else 0
    }

    fun getTotalRecords(): Int = history.size

    private fun updateWeights(record: OptimizationRecord) {
        if (record.resultFps <= 0) return
        for ((key, _) in record.settings) {
            val currentWeight = settingsWeights.getOrDefault(key, 0.5)
            val fpsNorm = (record.resultFps.toDouble() / 120.0).coerceIn(0.0, 1.5)
            settingsWeights[key] = currentWeight * 0.9 + fpsNorm * 0.1
        }
    }

    private fun saveHistory() {
        try {
            storageFile.parentFile?.mkdirs()
            val props = Properties()
            // Only keep last 50 records
            val recent = history.takeLast(50)
            props.setProperty("count", recent.size.toString())
            recent.forEachIndexed { i, record ->
                props.setProperty("r${i}_game", record.gameId)
                props.setProperty("r${i}_tier", record.gpuTier)
                props.setProperty("r${i}_style", record.playstyle)
                props.setProperty("r${i}_fps", record.resultFps.toString())
                props.setProperty("r${i}_time", record.timestamp.toString())
                props.setProperty("r${i}_settings", record.settings.entries.joinToString(";") { "${it.key}=${it.value}" })
            }
            storageFile.outputStream().use { props.store(it, "VEXTIQ ML History") }
        } catch (e: Exception) {
            println("[ML] Save error: ${e.message}")
        }
    }

    private fun loadHistory() {
        try {
            if (!storageFile.exists()) return
            val props = Properties()
            storageFile.inputStream().use { props.load(it) }

            val count = props.getProperty("count")?.toIntOrNull() ?: 0
            for (i in 0 until count) {
                val gameId = props.getProperty("r${i}_game") ?: continue
                val tier = props.getProperty("r${i}_tier") ?: "medium"
                val style = props.getProperty("r${i}_style") ?: "none"
                val fps = props.getProperty("r${i}_fps")?.toIntOrNull() ?: 0
                val time = props.getProperty("r${i}_time")?.toLongOrNull() ?: 0L
                val settingsStr = props.getProperty("r${i}_settings") ?: ""
                val settings = settingsStr.split(";").mapNotNull { entry ->
                    val parts = entry.split("=")
                    if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
                }.toMap()

                history.add(OptimizationRecord(gameId, tier, style, settings, fps, time))
            }
            if (history.isNotEmpty()) println("[ML] Loaded ${history.size} optimization records")
        } catch (e: Exception) {
            println("[ML] Load error: ${e.message}")
        }
    }

    fun clearHistory() {
        history.clear()
        settingsWeights.clear()
        storageFile.delete()
        println("[ML] History cleared")
    }
}
