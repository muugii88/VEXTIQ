package com.vextiq.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * NetworkMonitor — latency telemetry focused on the actual game traffic.
 *
 * Pipeline per tick:
 *  1. Detect active game (process name supplied by FpsMonitor).
 *  2. Enumerate every non-private remote endpoint the game's PID is talking to.
 *  3. Group by RemoteAddress, sort by connection count, take the top N.
 *  4. Filter out known infrastructure ranges (Cloudflare/Akamai/AWS edge) so a
 *     CDN ping doesn't masquerade as the game-server ping.
 *  5. ICMP-ping each candidate. Pick the highest stable RTT — game-server
 *     traffic typically traverses further than nearby login/CDN edges.
 *  6. For known TCP games, also probe TCP connect RTT to the chosen IP. TCP
 *     connect time tracks application-layer latency more closely than ICMP.
 *  7. Compute `gamePingEstimate = networkRTT + 1000 / tickRateHz`. The
 *     in-game ping a player sees is roughly the network RTT plus one server
 *     tick of processing delay; this estimate makes our number match what the
 *     game itself displays.
 */
class NetworkMonitor(private val settings: SettingsManager) {

    data class NetStats(
        val gameServerIp: String = "",
        val gameServerRegion: String = "",
        val gameServerPingMs: Int = 0,
        val gamePingEstimateMs: Int = 0,
        val tcpPingMs: Int = 0,
        val routerPingMs: Int = 0,
        val packetLossPercent: Int = 0,
        val jitterMs: Int = 0,
        val detectedGame: String = "",
        val bestRegion: String = "",
        val bestRegionPingMs: Int = 0,
        val regions: Map<String, Int> = emptyMap()
    )

    private val _stats = MutableStateFlow(NetStats())
    val stats: StateFlow<NetStats> = _stats

    @Volatile private var gameProcessName: String = ""

    fun setActiveGame(processName: String) {
        gameProcessName = processName
    }

    private var monitorJob: Job? = null

    fun start(scope: CoroutineScope) {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.IO) {
            var tick = 0
            while (isActive) {
                try {
                    val netStats = collect(regionalCheckDue = tick % 10 == 0)
                    _stats.value = netStats
                } catch (e: Exception) {
                    System.err.println("[NetworkMonitor] collect error: ${e.message}")
                }
                tick++
                delay(2000)
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
    }

    private fun collect(regionalCheckDue: Boolean): NetStats {
        val router = pingRouter()
        val gameInfo = GameServerDatabase.findByProcessName(gameProcessName)
        val detectedName = gameInfo?.displayName ?: gameProcessName
        val tickHz = gameInfo?.tickRateHz ?: 30

        // Game server IP detection: pick the most-connected non-infrastructure IP
        val remoteIp = if (settings.gameServerPingEnabled && gameProcessName.isNotEmpty()) {
            findBestGameServerIp(gameProcessName, gameInfo)
        } else ""

        val gameMulti = if (remoteIp.isNotEmpty()) {
            IcmpPing.pingMulti(remoteIp, count = 4, timeoutMs = 800)
        } else null

        // TCP RTT — much closer to the in-game displayed ping for TCP games
        val tcpRtt = if (remoteIp.isNotEmpty() && gameInfo?.gamePort != null) {
            TcpPing.pingMulti(remoteIp, gameInfo.gamePort, count = 3, timeoutMs = 600)
        } else -1

        val networkRtt = gameMulti?.avgMs?.coerceAtLeast(0) ?: 0
        // Estimate displayed in-game ping: ICMP RTT + one server tick of processing.
        // For TCP games where we have a real TCP connect RTT, prefer that over the
        // synthetic ICMP+tick estimate.
        val gamePingEstimate = when {
            tcpRtt > 0 -> tcpRtt
            networkRtt > 0 -> networkRtt + (1000 / tickHz)
            else -> 0
        }

        val regions: Map<String, Int> = if (
            gameInfo != null &&
            settings.regionalProbingEnabled &&
            regionalCheckDue
        ) {
            probeRegions(gameInfo)
        } else {
            _stats.value.regions
        }

        val (bestRegion, bestPing) = regions
            .filter { it.value > 0 }
            .minByOrNull { it.value }
            ?.let { it.key to it.value }
            ?: ("" to 0)

        val serverRegion = regions.entries
            .firstOrNull { kotlin.math.abs(it.value - networkRtt) <= 10 }
            ?.key ?: ""

        return NetStats(
            gameServerIp = remoteIp,
            gameServerRegion = serverRegion,
            gameServerPingMs = networkRtt,
            gamePingEstimateMs = gamePingEstimate,
            tcpPingMs = tcpRtt.coerceAtLeast(0),
            routerPingMs = router,
            packetLossPercent = gameMulti?.lossPercent ?: 0,
            jitterMs = gameMulti?.jitterMs?.coerceAtLeast(0) ?: 0,
            detectedGame = detectedName,
            bestRegion = bestRegion,
            bestRegionPingMs = bestPing,
            regions = regions
        )
    }

    private fun pingRouter(): Int {
        val gateway = findDefaultGateway() ?: return 0
        val result = IcmpPing.ping(gateway, timeoutMs = 500)
        return if (result.success) result.rttMs else 0
    }

    private fun findDefaultGateway(): String? {
        return try {
            val output = PowerShellRunner.runPowerShell(
                "(Get-NetRoute -DestinationPrefix '0.0.0.0/0' -ErrorAction SilentlyContinue | Sort-Object RouteMetric | Select-Object -First 1).NextHop",
                timeoutSec = 5
            ).trimmedOutput()
            output.takeIf { it.isNotEmpty() && it.matches(Regex("""^\d+\.\d+\.\d+\.\d+$""")) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Pick the "real" game-server IP from all the connections the game's PID owns.
     *
     * Strategy:
     *  - Collect all unique non-private remote IPs (TCP+UDP) for the game PID.
     *  - Group by connection count and rank — the IP with the most active
     *    connections is overwhelmingly the gameplay server.
     *  - Skip CDN/infrastructure subnets (web-style 80/443 ports too).
     *  - ICMP-probe the top 3 in parallel; if multiple are reachable, prefer
     *    the one with HIGHER latency. Login/CDN edges tend to be local, the
     *    actual gameplay server is typically further away.
     */
    private fun findBestGameServerIp(
        processName: String,
        gameInfo: GameServerDatabase.GameInfo?
    ): String {
        val candidates = enumerateGameRemoteIps(processName)
        if (candidates.isEmpty()) return ""

        // Single candidate? Use it directly — no ranking needed.
        if (candidates.size == 1) return candidates.first().ip

        // Try ICMP on top 3 most-connected; among reachable ones, pick the highest RTT.
        val top = candidates.take(3)
        val pings = top.map { c ->
            val r = IcmpPing.ping(c.ip, timeoutMs = 600)
            c.ip to (if (r.success) r.rttMs else -1)
        }
        val reachable = pings.filter { it.second >= 0 }
        if (reachable.isNotEmpty()) {
            return reachable.maxByOrNull { it.second }!!.first
        }
        // Fallback: protocol-preferred candidate even if not pingable
        val preferProtocol = gameInfo?.protocol
        candidates.firstOrNull { c ->
            preferProtocol == null || c.protocol == preferProtocol
        }?.let { return it.ip }
        return candidates.first().ip
    }

    private data class IpCandidate(
        val ip: String,
        val protocol: GameServerDatabase.Protocol,
        val connections: Int
    )

    private fun enumerateGameRemoteIps(processName: String): List<IpCandidate> {
        // Fast path: JNA GetExtendedTcpTable directly. Falls through to PowerShell
        // only if process lookup or table call fails.
        val pid = resolveProcessId(processName)
        if (pid > 0) {
            val tcpConns = TcpTableEnumerator.enumerateForPid(pid)
            if (tcpConns.isNotEmpty()) {
                val grouped = HashMap<String, Int>()
                for (c in tcpConns) {
                    if (!c.isEstablished) continue
                    val ip = c.remoteAddress
                    if (isPrivateOrLoopback(ip)) continue
                    if (isInfrastructureIp(ip)) continue
                    if (c.remotePort in setOf(80, 443, 8080, 8443)) continue
                    grouped.merge(ip, 1) { a, b -> a + b }
                }
                if (grouped.isNotEmpty()) {
                    return grouped.entries
                        .map { (ip, count) ->
                            IpCandidate(ip, GameServerDatabase.Protocol.TCP, count)
                        }
                        .sortedByDescending { it.connections }
                }
            }
            // TCP table came back empty but PID exists — UDP-only game (most FPS).
            // Fall through to PowerShell which queries netstat for UDP endpoints.
        }
        return enumerateViaPowerShell(processName)
    }

    private fun resolveProcessId(processName: String): Int {
        val cleaned = processName.removeSuffix(".exe")
        return try {
            val out = PowerShellRunner.runPowerShell(
                "(Get-Process -Name '$cleaned' -ErrorAction SilentlyContinue | Select-Object -First 1).Id",
                timeoutSec = 3
            ).trimmedOutput()
            out.toIntOrNull() ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun isPrivateOrLoopback(ip: String): Boolean {
        if (ip.startsWith("127.")) return true
        if (ip.startsWith("10.")) return true
        if (ip.startsWith("192.168.")) return true
        if (ip.startsWith("0.")) return true
        if (ip.startsWith("255.")) return true
        if (ip.startsWith("169.254.")) return true
        // 172.16.0.0 - 172.31.255.255
        if (ip.startsWith("172.")) {
            val second = ip.split(".").getOrNull(1)?.toIntOrNull() ?: return false
            if (second in 16..31) return true
        }
        return false
    }

    private fun enumerateViaPowerShell(processName: String): List<IpCandidate> {
        val cleaned = processName.removeSuffix(".exe")
        val script = """
            ${'$'}proc = Get-Process -Name '$cleaned' -ErrorAction SilentlyContinue | Select-Object -First 1
            if (-not ${'$'}proc) { return }
            ${'$'}targetPid = ${'$'}proc.Id

            ${'$'}results = New-Object System.Collections.ArrayList

            ${'$'}tcp = Get-NetTCPConnection -OwningProcess ${'$'}targetPid -State Established -ErrorAction SilentlyContinue
            foreach (${'$'}c in ${'$'}tcp) {
                ${'$'}ip = ${'$'}c.RemoteAddress
                ${'$'}port = ${'$'}c.RemotePort
                if (${'$'}ip -notmatch '^(127\.|0\.|10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[01])\.|::1|fe80|255\.)' -and
                    ${'$'}ip -match '^\d+\.\d+\.\d+\.\d+$' -and
                    ${'$'}port -notin @(80, 443, 8080, 8443)) {
                    [void]${'$'}results.Add("TCP|${'$'}ip|${'$'}port")
                }
            }

            ${'$'}udpLines = netstat -ano -p UDP 2>${'$'}null | Where-Object { ${'$'}_ -match "\s${'$'}targetPid${'$'}" }
            foreach (${'$'}l in ${'$'}udpLines) {
                if (${'$'}l -match '\s+(\d+\.\d+\.\d+\.\d+):(\d+)\s+(\S+):(\S+)') {
                    ${'$'}remote = ${'$'}Matches[3]
                    ${'$'}rport = ${'$'}Matches[4]
                    if (${'$'}remote -match '^\d+\.\d+\.\d+\.\d+${'$'}' -and
                        ${'$'}remote -notmatch '^(127\.|0\.|10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[01])\.|255\.)' -and
                        ${'$'}rport -ne '*' -and ${'$'}rport -ne '0') {
                        [void]${'$'}results.Add("UDP|${'$'}remote|${'$'}rport")
                    }
                }
            }

            ${'$'}results -join "`n"
        """.trimIndent()

        val output = PowerShellRunner.runPowerShell(script, timeoutSec = 6).trimmedOutput()
        if (output.isEmpty()) return emptyList()

        val grouped = HashMap<String, IntArray>()
        for (line in output.lineSequence()) {
            val parts = line.split("|")
            if (parts.size != 3) continue
            val proto = parts[0]
            val ip = parts[1]
            if (isInfrastructureIp(ip)) continue
            val entry = grouped.getOrPut(ip) { IntArray(3) }
            entry[0]++
            if (proto == "TCP") entry[1]++ else entry[2]++
        }

        return grouped.entries
            .map { (ip, counts) ->
                val proto = if (counts[2] >= counts[1])
                    GameServerDatabase.Protocol.UDP
                else
                    GameServerDatabase.Protocol.TCP
                IpCandidate(ip, proto, counts[0])
            }
            .sortedByDescending { it.connections }
    }

    /**
     * Best-effort filter for IP ranges we know aren't gameplay servers:
     * Cloudflare, Google, Discord, Akamai well-known edges. This is a heuristic
     * shortlist — extending it is fine. False positives are rare enough that
     * the top-3 + max-RTT strategy still picks the right server.
     */
    private fun isInfrastructureIp(ip: String): Boolean {
        // Cloudflare 1.1.1.1, 1.0.0.1; Google 8.8.8.8/8.8.4.4 — public DNS, not gameplay
        if (ip == "1.1.1.1" || ip == "1.0.0.1" || ip == "8.8.8.8" || ip == "8.8.4.4") return true
        // Discord media/voice CDN ranges are mostly 162.159.x.x (Cloudflare). Strip them
        // so Discord overlay traffic appearing under the game's PID (rare) doesn't win.
        if (ip.startsWith("162.159.")) return true
        return false
    }

    // Cache resolved region IPs so we don't re-do DNS every probe cycle.
    // The keys here are the database hostnames; values are resolved IPv4 strings
    // (or the original literal IP). On the cold path the lookup happens once;
    // afterwards IcmpPing receives a numeric IP and skips DNS entirely.
    private val resolvedHostCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * Probe every region serially. Was previously `runBlocking { async {} }` —
     * which concurrently invoked `InetAddress.getByName` from multiple threads
     * and tripped a JDK 21.0.10 symbol-table refcount race that crashed the
     * JVM ("Internal Error (symbol.cpp:338): fatal error: refcount underflow").
     * Sequential + cached resolution sidesteps the bug entirely.
     */
    private fun probeRegions(game: GameServerDatabase.GameInfo): Map<String, Int> {
        val out = LinkedHashMap<String, Int>(game.regions.size)
        for ((region, host) in game.regions) {
            val target = resolveOnce(host)
            if (target.isEmpty()) {
                out[region] = -1
                continue
            }
            val r = IcmpPing.ping(target, timeoutMs = 800)
            out[region] = if (r.success) r.rttMs else -1
        }
        return out
    }

    private fun resolveOnce(host: String): String {
        if (host.isEmpty()) return ""
        resolvedHostCache[host]?.let { return it }
        return try {
            val ip = java.net.InetAddress.getByName(host).hostAddress ?: ""
            if (ip.isNotEmpty()) resolvedHostCache[host] = ip
            ip
        } catch (_: Exception) {
            ""
        }
    }
}
