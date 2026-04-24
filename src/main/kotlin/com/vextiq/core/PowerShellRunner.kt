package com.vextiq.core

import java.util.concurrent.TimeUnit

object PowerShellRunner {

    data class Result(val exitCode: Int, val output: String, val timedOut: Boolean) {
        val success: Boolean get() = !timedOut && exitCode == 0
        fun trimmedOutput(): String = output.trim()
    }

    fun runPowerShell(command: String, timeoutSec: Long = 15): Result =
        exec(listOf("powershell", "-NoProfile", "-NonInteractive", "-Command", command), timeoutSec)

    fun runCmd(command: String, timeoutSec: Long = 15): Result =
        exec(listOf("cmd", "/c", command), timeoutSec)

    fun exec(command: List<String>, timeoutSec: Long = 15): Result {
        var process: Process? = null
        return try {
            process = ProcessBuilder(command).redirectErrorStream(true).start()
            val p = process

            val output = StringBuilder()
            val reader = Thread {
                try {
                    p.inputStream.bufferedReader().use { r ->
                        r.forEachLine { line ->
                            synchronized(output) { output.append(line).append('\n') }
                        }
                    }
                } catch (_: Exception) {
                }
            }.apply {
                isDaemon = true
                start()
            }

            val finished = p.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                p.destroyForcibly()
                reader.join(1000)
                return Result(-1, synchronized(output) { output.toString() }, timedOut = true)
            }

            reader.join(1000)
            Result(p.exitValue(), synchronized(output) { output.toString() }, timedOut = false)
        } catch (e: Exception) {
            Result(-1, e.message ?: "", timedOut = false)
        } finally {
            process?.let { if (it.isAlive) it.destroyForcibly() }
        }
    }
}
