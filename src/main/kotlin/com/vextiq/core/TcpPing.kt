package com.vextiq.core

import java.net.InetSocketAddress
import java.net.Socket

/**
 * TCP connect-time ping.
 *
 * Why this exists: ICMP shows raw network RTT but the in-game "ping" value
 * usually reflects application-layer round-trip (network + server-side
 * processing). For TCP-based games (MMOs, MOBAs) a TCP connect to the
 * game's port is a much closer proxy to the displayed ping than ICMP.
 *
 * For UDP games this isn't perfect either, but if the game's host also
 * accepts a TCP port (login/auth, often on the same address), measuring
 * that gives a more honest latency floor than ICMP.
 */
object TcpPing {

    data class Result(
        val success: Boolean,
        val rttMs: Int
    ) {
        companion object {
            val FAIL = Result(false, -1)
        }
    }

    /**
     * Open a TCP socket to [host]:[port] and time the connect. Closes immediately.
     */
    fun ping(host: String, port: Int, timeoutMs: Int = 1000): Result {
        return try {
            val start = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            val rtt = ((System.nanoTime() - start) / 1_000_000).toInt()
            Result(success = true, rttMs = rtt)
        } catch (e: Exception) {
            Result.FAIL
        }
    }

    /**
     * Try several common game/server ports and return the fastest successful RTT.
     * Returns FAIL if no port is reachable.
     */
    fun pingFirstReachable(host: String, ports: List<Int>, timeoutMs: Int = 800): Result {
        var best: Result = Result.FAIL
        for (p in ports) {
            val r = ping(host, p, timeoutMs)
            if (r.success && (!best.success || r.rttMs < best.rttMs)) {
                best = r
            }
        }
        return best
    }

    fun pingMulti(host: String, port: Int, count: Int = 4, timeoutMs: Int = 800): Int {
        val rtts = mutableListOf<Int>()
        repeat(count) {
            val r = ping(host, port, timeoutMs)
            if (r.success) rtts.add(r.rttMs)
        }
        return if (rtts.isEmpty()) -1 else rtts.average().toInt()
    }
}
