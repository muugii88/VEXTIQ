package com.vextiq.core

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.win32.StdCallLibrary
import java.net.InetAddress

/**
 * Raw ICMP ping via Windows IP Helper API (Iphlpapi.dll).
 *
 * Much faster than spawning `ping.exe` or PowerShell `Test-Connection`:
 * - No process spawn
 * - Single syscall, ~5-10ms on LAN
 * - Returns RTT + status per packet
 *
 * References: Win32 IcmpSendEcho2 (Iphlpapi.h)
 */
object IcmpPing {

    data class Result(
        val success: Boolean,
        val rttMs: Int,
        val status: Int
    ) {
        companion object {
            val FAIL = Result(false, -1, -1)
        }
    }

    private interface Iphlpapi : StdCallLibrary {
        fun IcmpCreateFile(): Pointer?
        fun IcmpCloseHandle(handle: Pointer): Boolean
        fun IcmpSendEcho(
            IcmpHandle: Pointer,
            DestinationAddress: Int,
            RequestData: Pointer,
            RequestSize: Short,
            RequestOptions: Pointer?,
            ReplyBuffer: Pointer,
            ReplySize: Int,
            Timeout: Int
        ): Int

        companion object {
            val INSTANCE: Iphlpapi? = try {
                Native.load("Iphlpapi", Iphlpapi::class.java) as Iphlpapi
            } catch (_: Throwable) {
                null
            }
        }
    }

    /**
     * Ping a host by name or IP. Returns RTT in milliseconds, or FAIL result.
     *
     * @param host hostname or IPv4 literal
     * @param timeoutMs per-attempt timeout in ms (default 1000)
     */
    fun ping(host: String, timeoutMs: Int = 1000): Result {
        val lib = Iphlpapi.INSTANCE ?: return Result.FAIL

        val ipv4 = try {
            val addr = InetAddress.getByName(host).address
            if (addr.size != 4) return Result.FAIL
            // Little-endian DWORD as Windows expects
            (addr[0].toInt() and 0xFF) or
                ((addr[1].toInt() and 0xFF) shl 8) or
                ((addr[2].toInt() and 0xFF) shl 16) or
                ((addr[3].toInt() and 0xFF) shl 24)
        } catch (_: Exception) {
            return Result.FAIL
        }

        val handle = lib.IcmpCreateFile() ?: return Result.FAIL
        return try {
            val payload = "vextiq".toByteArray(Charsets.US_ASCII)
            val request = Memory(payload.size.toLong())
            request.write(0, payload, 0, payload.size)

            // ICMP_ECHO_REPLY = 28 bytes + payload
            val replySize = 28 + payload.size + 8
            val reply = Memory(replySize.toLong())

            val count = lib.IcmpSendEcho(
                handle,
                ipv4,
                request,
                payload.size.toShort(),
                null,
                reply,
                replySize,
                timeoutMs
            )

            if (count <= 0) {
                Result.FAIL
            } else {
                // ICMP_ECHO_REPLY layout: Address(4), Status(4), RoundTripTime(4), ...
                val status = reply.getInt(4)
                val rtt = reply.getInt(8)
                if (status == 0) {
                    Result(success = true, rttMs = rtt, status = status)
                } else {
                    Result(success = false, rttMs = -1, status = status)
                }
            }
        } catch (e: Exception) {
            Result.FAIL
        } finally {
            lib.IcmpCloseHandle(handle)
        }
    }

    /**
     * Ping multiple times and return (avg, min, max, lossPercent).
     */
    data class MultiResult(
        val attempts: Int,
        val received: Int,
        val avgMs: Int,
        val minMs: Int,
        val maxMs: Int,
        val jitterMs: Int,
        val lossPercent: Int
    )

    fun pingMulti(host: String, count: Int = 4, timeoutMs: Int = 800): MultiResult {
        val rtts = mutableListOf<Int>()
        repeat(count) {
            val r = ping(host, timeoutMs)
            if (r.success) rtts.add(r.rttMs)
        }
        if (rtts.isEmpty()) {
            return MultiResult(count, 0, -1, -1, -1, -1, 100)
        }
        val avg = rtts.average().toInt()
        val min = rtts.min()
        val max = rtts.max()
        val jitter = if (rtts.size > 1) {
            var sum = 0
            for (i in 1 until rtts.size) sum += kotlin.math.abs(rtts[i] - rtts[i - 1])
            sum / (rtts.size - 1)
        } else 0
        val loss = ((count - rtts.size) * 100) / count
        return MultiResult(count, rtts.size, avg, min, max, jitter, loss)
    }
}
