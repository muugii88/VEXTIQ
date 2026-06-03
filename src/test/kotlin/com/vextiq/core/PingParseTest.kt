package com.vextiq.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PingParseTest {

    @Test
    fun `parses English ping reply`() {
        val out = """
            Pinging 1.1.1.1 with 32 bytes of data:
            Reply from 1.1.1.1: bytes=32 time=14ms TTL=57

            Ping statistics for 1.1.1.1:
                Packets: Sent = 1, Received = 1, Lost = 0 (0% loss),
            Approximate round trip times in milli-seconds:
                Minimum = 14ms, Maximum = 14ms, Average = 14ms
        """.trimIndent()
        assertEquals(14, PingParse.parseLatencyMs(out))
    }

    @Test
    fun `sub-millisecond reply parses as 1`() {
        val out = "Reply from 192.168.1.1: bytes=32 time<1ms TTL=64"
        assertEquals(1, PingParse.parseLatencyMs(out))
    }

    @Test
    fun `prefers the reply time over the summary numbers`() {
        // The reply line (23ms) must win over the summary block, which lists
        // Minimum=20 first. A naive "first number before ms" could grab 20.
        val out = """
            Reply from 8.8.8.8: bytes=32 time=23ms TTL=117
            Minimum = 20ms, Maximum = 25ms, Average = 22ms
        """.trimIndent()
        assertEquals(23, PingParse.parseLatencyMs(out))
    }

    @Test
    fun `parses three-digit latency`() {
        assertEquals(184, PingParse.parseLatencyMs("Reply from 1.2.3.4: bytes=32 time=184ms TTL=45"))
    }

    @Test
    fun `parses Russian locale output`() {
        val out = "Ответ от 8.8.8.8: число байт=32 время=42мс TTL=117"
        assertEquals(42, PingParse.parseLatencyMs(out))
    }

    @Test
    fun `timeout output yields null`() {
        assertNull(PingParse.parseLatencyMs("Request timed out."))
        assertNull(PingParse.parseLatencyMs("Destination host unreachable."))
    }

    @Test
    fun `empty output yields null`() {
        assertNull(PingParse.parseLatencyMs(""))
    }
}
