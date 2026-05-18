package com.vextiq.core

import com.sun.jna.*
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Enumerate the IPv4 TCP connection table directly via Win32 GetExtendedTcpTable.
 *
 * Replaces the PowerShell `Get-NetTCPConnection` approach in NetworkMonitor:
 *  - PowerShell call: ~150-300ms spawn overhead, runs every 2s = noticeable CPU
 *  - This: single syscall, ~1-3ms, no process spawn
 *
 * References: Win32 GetExtendedTcpTable (iphlpapi.h), TCP_TABLE_OWNER_PID_ALL,
 * MIB_TCPROW_OWNER_PID layout.
 */
object TcpTableEnumerator {

    data class TcpConnection(
        val localAddress: String,
        val localPort: Int,
        val remoteAddress: String,
        val remotePort: Int,
        val state: Int,
        val pid: Int
    ) {
        /** TCP state 5 = ESTABLISHED per MIB_TCP_STATE. */
        val isEstablished: Boolean get() = state == 5
    }

    private const val AF_INET = 2
    private const val TCP_TABLE_OWNER_PID_ALL = 5

    private interface Iphlpapi : StdCallLibrary {
        fun GetExtendedTcpTable(
            pTcpTable: Pointer?,
            pdwSize: IntByReference,
            bOrder: Boolean,
            ulAf: Int,
            TableClass: Int,
            Reserved: Int
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
     * Snapshot all IPv4 TCP connections owned by [pid], including the connection state.
     * Returns empty list if the lib isn't available or PID has no connections.
     */
    fun enumerateForPid(pid: Int): List<TcpConnection> {
        val lib = Iphlpapi.INSTANCE ?: return emptyList()

        // First call with null buffer to get required size.
        val size = IntByReference(0)
        lib.GetExtendedTcpTable(null, size, false, AF_INET, TCP_TABLE_OWNER_PID_ALL, 0)
        if (size.value <= 0) return emptyList()

        // Allocate buffer + retry. Keep a tiny growth loop in case the table
        // expanded between calls.
        var bufSize = size.value
        var attempts = 0
        while (attempts < 3) {
            val buf = Memory(bufSize.toLong())
            val sz = IntByReference(bufSize)
            val rc = lib.GetExtendedTcpTable(buf, sz, false, AF_INET, TCP_TABLE_OWNER_PID_ALL, 0)
            if (rc == 0) return parseTable(buf, sz.value, pid)
            if (rc == 122 /* ERROR_INSUFFICIENT_BUFFER */) {
                bufSize = sz.value
                attempts++
                continue
            }
            // Any other failure: give up — caller falls back to legacy probe.
            return emptyList()
        }
        return emptyList()
    }

    /**
     * MIB_TCPTABLE_OWNER_PID layout (little-endian):
     *   DWORD dwNumEntries
     *   MIB_TCPROW_OWNER_PID table[1+] each 24 bytes:
     *     dwState (4) | dwLocalAddr (4) | dwLocalPort (4) | dwRemoteAddr (4) |
     *     dwRemotePort (4) | dwOwningPid (4)
     *
     * Ports are stored in NETWORK byte order in the LOW 16 bits of a DWORD.
     */
    private fun parseTable(buf: Memory, bytes: Int, pidFilter: Int): List<TcpConnection> {
        if (bytes < 4) return emptyList()
        val data = ByteBuffer.wrap(buf.getByteArray(0, bytes)).order(ByteOrder.LITTLE_ENDIAN)
        val n = data.int
        val result = ArrayList<TcpConnection>(n.coerceAtMost(64))
        val rowSize = 24
        for (i in 0 until n) {
            val offset = 4 + i * rowSize
            if (offset + rowSize > bytes) break
            val state = data.getInt(offset)
            val localAddr = data.getInt(offset + 4)
            val localPort = readNetPort(data.getInt(offset + 8))
            val remoteAddr = data.getInt(offset + 12)
            val remotePort = readNetPort(data.getInt(offset + 16))
            val pid = data.getInt(offset + 20)
            if (pid != pidFilter) continue
            result.add(
                TcpConnection(
                    localAddress = ipv4ToString(localAddr),
                    localPort = localPort,
                    remoteAddress = ipv4ToString(remoteAddr),
                    remotePort = remotePort,
                    state = state,
                    pid = pid
                )
            )
        }
        return result
    }

    private fun readNetPort(dword: Int): Int {
        // High byte then low byte of the lower 16 bits (network byte order).
        return ((dword and 0xFF) shl 8) or ((dword ushr 8) and 0xFF)
    }

    private fun ipv4ToString(addr: Int): String {
        return "${addr and 0xFF}.${(addr ushr 8) and 0xFF}.${(addr ushr 16) and 0xFF}.${(addr ushr 24) and 0xFF}"
    }
}
