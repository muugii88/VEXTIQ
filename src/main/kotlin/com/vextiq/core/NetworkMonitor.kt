package com.vextiq.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * NetworkMonitor — latency telemetry focused on actual game traffic.
 *
 * Key behaviors:
 *  - Detects active game process and its established network flows (TCP + UDP)
 *  - Pings the real remote IP the game is talking to (not generic 8.8.8.8)
 *  - Pings user's default gateway (router baseline) to estimate LAN/WiFi overhead
 *  - Probes all known regional servers for the detected game → shows best region
 *  - Measures jitter and packet loss via multi-ping
 *
 * All ICMP goes through [IcmpPing] (JNA, no process spawn) when `rawIcmp` is enabled.
 */
class NetworkMonitor(private val settings: SettingsManager) {

    data class NetStats(
        val gameServerIp: String = "",
        val gameServerRegion: String = "",
        val gameServerPingMs: Int = 0,
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

        // Find actual remote server the game is connected to
        val remoteIp = if (settings.gameServerPingEnabled && gameProcessName.isNotEmpty()) {
            findGameRemoteIp(gameProcessName, gameInfo?.protocol ?: GameServerDatabase.Protocol.UDP)
        } else ""

        val gameMulti = if (remoteIp.isNotEmpty()) {
            IcmpPing.pingMulti(remoteIp, count = 4, timeoutMs = 800)
        } else null

        val regions: Map<String, Int> = if (
            gameInfo != null &&
            settings.regionalProbingEnabled &&
            regionalCheckDue
        ) {
            probeRegions(gameInfo)
        } else {
            _stats.value.regions // Keep previous until next probe cycle
        }

        val (bestRegion, bestPing) = regions
            .filter { it.value > 0 }
            .minByOrNull { it.value }
            ?.let { it.key to it.value }
            ?: ("" to 0)

        val serverRegion = regions.entries
            .firstOrNull { kotlin.math.abs(it.value - (gameMulti?.avgMs ?: -1)) <= 10 }
            ?.key ?: ""

        return NetStats(
            gameServerIp = remoteIp,
            gameServerRegion = serverRegion,
            gameServerPingMs = gameMulti?.avgMs?.coerceAtLeast(0) ?: 0,
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

    private fun findGameRemoteIp(processName: String, protocol: GameServerDatabase.Protocol): String {
        val cleaned = processName.removeSuffix(".exe")
        val script = when (protocol) {
            GameServerDatabase.Protocol.UDP -> """
                ${'$'}proc = Get-Process -Name '$cleaned' -ErrorAction SilentlyContinue | Select-Object -First 1
                if (-not ${'$'}proc) { return }
                # PowerShell doesn't expose UDP remote endpoints directly,
                # but netstat -an -p UDP -o does. Parse it:
                ${'$'}pid = ${'$'}proc.Id
                ${'$'}lines = netstat -ano -p UDP | Select-String " ${'$'}pid$"
                ${'$'}ips = @{}
                foreach (${'$'}l in ${'$'}lines) {
                    if (${'$'}l -match '\s+(\d+\.\d+\.\d+\.\d+):\d+\s+\*:\*') {
                        # UDP doesn't show remote in netstat-ano reliably; skip
                    }
                }
                # Fallback: use TCP connections if any
                ${'$'}tcp = Get-NetTCPConnection -OwningProcess ${'$'}pid -State Established -ErrorAction SilentlyContinue |
                    Where-Object {
                        ${'$'}_.RemoteAddress -notmatch '^(127\.|0\.|10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[01])\.|::1|fe80|255\.)' -and
                        ${'$'}_.RemoteAddress -match '^\d+\.\d+\.\d+\.\d+$'
                    } | Select-Object -First 1
                if (${'$'}tcp) { ${'$'}tcp.RemoteAddress }
            """.trimIndent()
            GameServerDatabase.Protocol.TCP -> """
                ${'$'}proc = Get-Process -Name '$cleaned' -ErrorAction SilentlyContinue | Select-Object -First 1
                if (-not ${'$'}proc) { return }
                ${'$'}conn = Get-NetTCPConnection -OwningProcess ${'$'}proc.Id -State Established -ErrorAction SilentlyContinue |
                    Where-Object {
                        ${'$'}_.RemoteAddress -notmatch '^(127\.|0\.|10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[01])\.|::1|fe80|255\.)' -and
                        ${'$'}_.RemoteAddress -match '^\d+\.\d+\.\d+\.\d+$'
                    } |
                    Group-Object RemoteAddress |
                    Sort-Object Count -Descending |
                    Select-Object -First 1
                if (${'$'}conn) { ${'$'}conn.Name }
            """.trimIndent()
        }
        val out = PowerShellRunner.runPowerShell(script, timeoutSec = 5).trimmedOutput()
        return out.takeIf { it.matches(Regex("""^\d+\.\d+\.\d+\.\d+$""")) } ?: ""
    }

    private fun probeRegions(game: GameServerDatabase.GameInfo): Map<String, Int> {
        // Parallel probe: 11 regions sequentially would take ~7s; parallel ≤ 1s.
        return runBlocking {
            game.regions.map { (region, host) ->
                async(Dispatchers.IO) {
                    val r = IcmpPing.ping(host, timeoutMs = 800)
                    region to if (r.success) r.rttMs else -1
                }
            }.awaitAll().toMap()
        }
    }
}
