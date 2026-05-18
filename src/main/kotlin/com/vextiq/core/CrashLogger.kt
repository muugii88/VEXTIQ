package com.vextiq.core

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Bootstrap logger that captures stdout, stderr, and uncaught exceptions to a
 * file at `%USERPROFILE%\.vextiq\app.log`. Needed because packaged release
 * builds run with console=false, so System.out / System.err go to a detached
 * stream that nobody can see — crashes silently disappear.
 *
 * Call [install] exactly once, as early in main() as possible. It rotates the
 * previous log to app.log.old and starts a fresh app.log.
 */
object CrashLogger {

    private val logDir = File(System.getProperty("user.home"), ".vextiq")
    val logFile: File = File(logDir, "app.log")
    val previousLogFile: File = File(logDir, "app.log.old")

    @Volatile private var installed = false

    fun install() {
        if (installed) return
        installed = true
        try {
            logDir.mkdirs()
            // Rotate previous log so each launch starts fresh but keeps last one
            if (logFile.exists()) {
                try {
                    if (previousLogFile.exists()) previousLogFile.delete()
                    logFile.renameTo(previousLogFile)
                } catch (_: Throwable) { /* ignore rotation errors */ }
            }
            val fileOut = PrintStream(FileOutputStream(logFile, true), true, Charsets.UTF_8)
            System.setOut(TeePrintStream(System.out, fileOut))
            System.setErr(TeePrintStream(System.err, fileOut))

            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                try {
                    val msg = "[FATAL] uncaught in thread '${t.name}': ${e.javaClass.name}: ${e.message}"
                    System.err.println(msg)
                    e.printStackTrace(System.err)
                } catch (_: Throwable) { /* swallow */ }
            }

            logBanner()
        } catch (t: Throwable) {
            // If we can't install, fall back to original streams — better silent
            // than crashing the launcher.
            try { System.err.println("[CrashLogger] install failed: ${t.message}") } catch (_: Throwable) {}
        }
    }

    fun log(msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss.SSS").format(Date())
        System.out.println("[$ts] $msg")
    }

    private fun logBanner() {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        System.out.println("============================================================")
        System.out.println("VEXTIQ PRO launched at $ts")
        System.out.println("  user.home = ${System.getProperty("user.home")}")
        System.out.println("  jpackage.app-path = ${System.getProperty("jpackage.app-path") ?: "(gradle run, not packaged)"}")
        System.out.println("  java.version = ${System.getProperty("java.version")} (${System.getProperty("java.vm.name")})")
        System.out.println("  os = ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})")
        System.out.println("  pid = ${ProcessHandle.current().pid()}")
        System.out.println("  log file = ${logFile.absolutePath}")
        System.out.println("============================================================")
    }

    private class TeePrintStream(
        private val a: PrintStream,
        private val b: PrintStream
    ) : PrintStream(object : OutputStream() {
        override fun write(byte: Int) {
            try { a.write(byte) } catch (_: Throwable) {}
            try { b.write(byte) } catch (_: Throwable) {}
        }
        override fun write(buf: ByteArray, off: Int, len: Int) {
            try { a.write(buf, off, len) } catch (_: Throwable) {}
            try { b.write(buf, off, len) } catch (_: Throwable) {}
        }
        override fun flush() {
            try { a.flush() } catch (_: Throwable) {}
            try { b.flush() } catch (_: Throwable) {}
        }
    }, true, Charsets.UTF_8)
}
