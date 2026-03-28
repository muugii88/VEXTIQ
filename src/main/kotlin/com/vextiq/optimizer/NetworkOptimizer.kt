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
            
            for (iface in interfaces) {
                val path = "HKLM\\SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters\\Interfaces\\$iface"
                runReg("add", path, "/v", "TcpAckFrequency", "/t", "REG_DWORD", "/d", "1", "/f")
                runReg("add", path, "/v", "TCPNoDelay", "/t", "REG_DWORD", "/d", "1", "/f")
                runReg("add", path, "/v", "TcpDelAckTicks", "/t", "REG_DWORD", "/d", "0", "/f")
            }
            
            onLog("[OK] Nagle's Algorithm disabled on ${interfaces.size} interfaces")
            true
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
            
            // Increase TCP window size
            runReg("add", tcpPath, "/v", "TcpWindowSize", "/t", "REG_DWORD", "/d", "65535", "/f")
            onLog("[OK] TCP Window Size: 64KB")
            
            // Enable TCP timestamps
            runReg("add", tcpPath, "/v", "Tcp1323Opts", "/t", "REG_DWORD", "/d", "1", "/f")
            onLog("[OK] TCP Timestamps: Enabled")
            
            // Disable TCP auto-tuning (for lower latency)
            runCmd("netsh", "int", "tcp", "set", "global", "autotuninglevel=disabled")
            onLog("[OK] TCP Auto-tuning: Disabled")
            
            // Enable Direct Cache Access
            runCmd("netsh", "int", "tcp", "set", "global", "dca=enabled")
            onLog("[OK] Direct Cache Access: Enabled")
            
            // Disable ECN
            runCmd("netsh", "int", "tcp", "set", "global", "ecncapability=disabled")
            onLog("[OK] ECN: Disabled")
            
            true
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
            
            // Disable network throttling
            runReg("add", mmPath, "/v", "NetworkThrottlingIndex", "/t", "REG_DWORD", "/d", "0xffffffff", "/f")
            onLog("[OK] Network Throttling: Disabled")
            
            // System responsiveness
            runReg("add", mmPath, "/v", "SystemResponsiveness", "/t", "REG_DWORD", "/d", "0", "/f")
            onLog("[OK] System Responsiveness: 0% (Gaming priority)")
            
            // Gaming priority
            val gamesPath = "$mmPath\\Tasks\\Games"
            runReg("add", gamesPath, "/v", "Priority", "/t", "REG_DWORD", "/d", "6", "/f")
            runReg("add", gamesPath, "/v", "Scheduling Category", "/t", "REG_SZ", "/d", "High", "/f")
            runReg("add", gamesPath, "/v", "SFIO Priority", "/t", "REG_SZ", "/d", "High", "/f")
            runReg("add", gamesPath, "/v", "GPU Priority", "/t", "REG_DWORD", "/d", "8", "/f")
            onLog("[OK] Gaming Priority: High")
            
            true
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
            // Flush DNS cache
            runCmd("ipconfig", "/flushdns")
            onLog("[OK] DNS Cache flushed")
            
            // Set DNS priority
            runReg("add", "HKLM\\SYSTEM\\CurrentControlSet\\Services\\Dnscache\\Parameters", "/v", "MaxCacheTtl", "/t", "REG_DWORD", "/d", "86400", "/f")
            runReg("add", "HKLM\\SYSTEM\\CurrentControlSet\\Services\\Dnscache\\Parameters", "/v", "MaxNegativeCacheTtl", "/t", "REG_DWORD", "/d", "0", "/f")
            onLog("[OK] DNS Cache TTL optimized")
            
            true
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
            val output = runCommand(listOf("reg", "query", "HKLM\\SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters\\Interfaces"))
            output.lines()
                .filter { it.contains("HKEY_LOCAL_MACHINE") }
                .map { it.substringAfterLast("\\") }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun runReg(vararg args: String): String {
        val cmd = listOf("reg") + args.toList()
        return runCommand(cmd)
    }
    
    private fun runCmd(vararg args: String): String {
        return runCommand(args.toList())
    }
    
    private fun runCommand(cmd: List<String>): String {
        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
        
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output
    }
}
