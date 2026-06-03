package com.vextiq.core

/**
 * Detect whether the current process has Administrator privileges.
 *
 * Many of VEXTIQ's optimizations require admin: registry writes (HKLM, power
 * plans), core-parking changes, network adapter tweaks, scheduled tasks.
 * Without admin those PowerShell/reg commands silently fail and the user just
 * sees logs that LOOK successful but produce no real effect.
 *
 * Check is run once and cached — admin status doesn't change during a process
 * lifetime.
 */
object AdminCheck {

    val isAdmin: Boolean by lazy { detect() }

    private fun detect(): Boolean {
        // Cheapest reliable test: try to read a write-protected HKLM key path
        // that requires admin to enumerate. `net session` is a common idiom
        // but spawns a process; PowerShell WindowsIdentity is one syscall.
        return try {
            val proc = ProcessBuilder(
                listOf(
                    "powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "([Security.Principal.WindowsPrincipal]" +
                        "[Security.Principal.WindowsIdentity]::GetCurrent())" +
                        ".IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)"
                )
            ).redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().use { it.readText() }.trim()
            proc.waitFor()
            output.equals("True", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
