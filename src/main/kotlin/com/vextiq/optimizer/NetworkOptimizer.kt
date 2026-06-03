package com.vextiq.optimizer

/**
 * Network Optimizer - TCP/IP optimizations for gaming
 */
class NetworkOptimizer {
    
    /**
     * Disable Nagle's Algorithm (reduces latency)
     */
    fun disableNagle(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Disabling Nagle's Algorithm...")
        
        return try {
            // Find all network interfaces
            val interfaces = findNetworkInterfaces()
            if (interfaces.isEmpty()) {
                onLog("[!] No network interfaces found (registry read needs admin)")
                return false
            }

            var applied = 0
            for (iface in interfaces) {
                val path = "HKLM\\SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters\\Interfaces\\$iface"
                val r = runReg("add", path, "/v", "TcpAckFrequency", "/t", "REG_DWORD", "/d", "1", "/f")
                runReg("add", path, "/v", "TCPNoDelay", "/t", "REG_DWORD", "/d", "1", "/f")
                runReg("add", path, "/v", "TcpDelAckTicks", "/t", "REG_DWORD", "/d", "0", "/f")
                if (r.ok) applied++
            }

            if (applied > 0) {
                onLog("[OK] Nagle's Algorithm disabled on $applied/${interfaces.size} interfaces")
                true
            } else {
                onLog("[!] Nagle: registry writes were denied (run as admin)")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Nagle (needs admin): ${e.message}")
            false
        }
    }
    
    /**
     * Optimize TCP settings globally
     */
    fun optimizeTcp(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing TCP/IP stack...")
        
        return try {
            val tcpPath = "HKLM\\SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters"
            var failures = 0

            // Helper: run a step, log [OK]/[!] based on the real exit code.
            fun step(label: String, r: ProcessRunner.Result) {
                if (r.ok) onLog("[OK] $label")
                else { onLog("[!] $label — failed (run as admin)"); failures++ }
            }

            // Increase TCP window size
            step("TCP Window Size: 64KB",
                runReg("add", tcpPath, "/v", "TcpWindowSize", "/t", "REG_DWORD", "/d", "65535", "/f"))
            // Enable TCP timestamps
            step("TCP Timestamps: Enabled",
                runReg("add", tcpPath, "/v", "Tcp1323Opts", "/t", "REG_DWORD", "/d", "1", "/f"))
            // Disable TCP auto-tuning (for lower latency)
            step("TCP Auto-tuning: Disabled",
                runCmd("netsh", "int", "tcp", "set", "global", "autotuninglevel=disabled"))
            // Enable Direct Cache Access
            step("Direct Cache Access: Enabled",
                runCmd("netsh", "int", "tcp", "set", "global", "dca=enabled"))
            // Disable ECN
            step("ECN: Disabled",
                runCmd("netsh", "int", "tcp", "set", "global", "ecncapability=disabled"))

            failures == 0
        } catch (e: Exception) {
            onLog("[!] TCP optimization (needs admin): ${e.message}")
            false
        }
    }
    
    /**
     * Disable Network Throttling
     */
    fun disableThrottling(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Disabling Network Throttling...")
        
        return try {
            val mmPath = "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Multimedia\\SystemProfile"
            var failures = 0
            fun step(label: String, r: ProcessRunner.Result) {
                if (r.ok) onLog("[OK] $label")
                else { onLog("[!] $label — failed (run as admin)"); failures++ }
            }

            // Disable network throttling (0xffffffff = no throttling; reg.exe accepts 0x hex)
            step("Network Throttling: Disabled",
                runReg("add", mmPath, "/v", "NetworkThrottlingIndex", "/t", "REG_DWORD", "/d", "0xffffffff", "/f"))
            // System responsiveness
            step("System Responsiveness: 0% (Gaming priority)",
                runReg("add", mmPath, "/v", "SystemResponsiveness", "/t", "REG_DWORD", "/d", "0", "/f"))

            // Gaming priority task profile
            val gamesPath = "$mmPath\\Tasks\\Games"
            val g = runReg("add", gamesPath, "/v", "Priority", "/t", "REG_DWORD", "/d", "6", "/f")
            runReg("add", gamesPath, "/v", "Scheduling Category", "/t", "REG_SZ", "/d", "High", "/f")
            runReg("add", gamesPath, "/v", "SFIO Priority", "/t", "REG_SZ", "/d", "High", "/f")
            runReg("add", gamesPath, "/v", "GPU Priority", "/t", "REG_DWORD", "/d", "8", "/f")
            step("Gaming Priority: High", g)

            failures == 0
        } catch (e: Exception) {
            onLog("[!] Throttling (needs admin): ${e.message}")
            false
        }
    }
    
    /**
     * Optimize DNS settings
     */
    fun optimizeDns(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing DNS...")
        
        return try {
            var failures = 0
            fun step(label: String, r: ProcessRunner.Result) {
                if (r.ok) onLog("[OK] $label")
                else { onLog("[!] $label — failed (run as admin)"); failures++ }
            }

            // Flush DNS cache (no admin needed)
            step("DNS Cache flushed", runCmd("ipconfig", "/flushdns"))

            // Set DNS cache TTL
            val dnsPath = "HKLM\\SYSTEM\\CurrentControlSet\\Services\\Dnscache\\Parameters"
            val d = runReg("add", dnsPath, "/v", "MaxCacheTtl", "/t", "REG_DWORD", "/d", "86400", "/f")
            runReg("add", dnsPath, "/v", "MaxNegativeCacheTtl", "/t", "REG_DWORD", "/d", "0", "/f")
            step("DNS Cache TTL optimized", d)

            failures == 0
        } catch (e: Exception) {
            onLog("[!] DNS optimization: ${e.message}")
            false
        }
    }
    
    /**
     * Apply all network optimizations
     */
    fun applyAll(onLog: (String) -> Unit): Int {
        onLog("[>>] Applying all network optimizations...")
        onLog("")
        
        var success = 0
        
        if (disableNagle(onLog)) success++
        if (optimizeTcp(onLog)) success++
        if (disableThrottling(onLog)) success++
        if (optimizeDns(onLog)) success++
        
        onLog("")
        onLog("[OK] Network optimization: $success/4 applied")
        
        return success
    }
    
    private fun findNetworkInterfaces(): List<String> {
        return try {
            val result = ProcessRunner.reg("query", "HKLM\\SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters\\Interfaces")
            result.output.lines()
                .filter { it.contains("HKEY_LOCAL_MACHINE") }
                .map { it.substringAfterLast("\\") }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun runReg(vararg args: String) = ProcessRunner.reg(*args)

    private fun runCmd(vararg args: String) = ProcessRunner.run(*args)
    
    /**
     * General optimize - alias for applyAll
     */
    fun optimize(onLog: (String) -> Unit) {
        applyAll(onLog)
    }
}
