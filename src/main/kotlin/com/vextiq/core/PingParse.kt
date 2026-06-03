package com.vextiq.core

/**
 * Parses latency out of `ping.exe` output across Windows display languages.
 *
 * Extracted from SystemMonitor so it can be unit-tested without spawning a real
 * ping process. Prefers the per-reply `time=NNms` / `time<1ms` value and falls
 * back to any "<number> ms" token, so the noisy summary block ("Minimum =,
 * Maximum =, Average =") never shadows the actual round-trip time.
 */
object PingParse {

    // English "time=14ms" / "time<1ms", Russian/Mongolian "время=14мс".
    private val timeField = Regex("""time[=<]\s*(\d+)\s*(?:ms|мс)""", RegexOption.IGNORE_CASE)

    // Any "<digits> ms" token, for locales/formats the field regex misses.
    private val anyMs = Regex("""(\d+)\s*(?:ms|мс|мил|мсек)""", RegexOption.IGNORE_CASE)

    /**
     * @return round-trip latency in ms, or null if the output has no timing
     *   (timeout / unreachable / unparseable).
     */
    fun parseLatencyMs(output: String): Int? {
        timeField.find(output)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        return anyMs.find(output)?.groupValues?.get(1)?.toIntOrNull()
    }
}
