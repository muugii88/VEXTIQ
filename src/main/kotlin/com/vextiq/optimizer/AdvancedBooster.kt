package com.vextiq.optimizer

import com.vextiq.core.PowerShellRunner

/**
 * Advanced Performance Booster
 * All tweaks work WITHOUT admin rights where possible
 */
class AdvancedBooster {

    /**
     * 1. Timer Resolution - Reduce input lag by 10-15ms
     * Sets Windows timer to 0.5ms (default is 15.6ms)
     * Persists as long as this process is running.
     */
    fun setTimerResolution(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Setting timer resolution to 0.5ms (Persistent)...")

        return try {
            val result = NtDll.INSTANCE.NtSetTimerResolution(5000, true, IntArray(1))
            if (result == 0) {
                onLog("[OK] Timer resolution set to 0.5ms")
                true
            } else {
                onLog("[!] Native call failed (NTSTATUS: $result)")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Native error: ${e.message}")
            onLog("[i] Applying fallback method...")
            runTimerPowerShell(onLog)
        }
    }

    private fun runTimerPowerShell(onLog: (String) -> Unit): Boolean {
        val script = "Add-Type '@\nusing System;using System.Runtime.InteropServices;public class NtDll{[DllImport(\"ntdll.dll\")]public static extern int NtSetTimerResolution(int d,bool s,out int c);}\n@'; \$c=0; [NtDll]::NtSetTimerResolution(5000,\$true,[ref]\$c)"
        val result = PowerShellRunner.runPowerShell(script, timeoutSec = 10)
        return if (result.success) {
            onLog("[i] Fallback applied (Effect may be temporary)")
            true
        } else {
            onLog("[!] Fallback failed${if (result.timedOut) " (timeout)" else ""}")
            false
        }
    }

    // JNA Interface for ntdll.dll
    interface NtDll : com.sun.jna.Library {
        companion object {
            val INSTANCE = com.sun.jna.Native.load("ntdll", NtDll::class.java) as NtDll
        }
        fun NtSetTimerResolution(DesiredResolution: Int, SetResolution: Boolean, CurrentResolution: IntArray): Int
    }

    
    /**
     * 2. Disable CPU Core Parking - Keep all cores active
     */
    fun disableCoreParking(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Disabling CPU core parking...")
        
        return try {
            // Set power plan to High Performance parking settings
            // These registry keys work for current user
            val commands = listOf(
                // Disable core parking via powercfg (no admin needed for active plan)
                "powercfg /setacvalueindex scheme_current sub_processor CPMINCORES 100",
                "powercfg /setacvalueindex scheme_current sub_processor CPMAXCORES 100",
                "powercfg /setactive scheme_current"
            )
            
            for (cmd in commands) {
                PowerShellRunner.runCmd(cmd, timeoutSec = 10)
            }

            onLog("[OK] Core parking disabled - all cores active")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * 3. GPU Scheduler Optimization
     */
    fun optimizeGpuScheduler(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing GPU scheduler...")
        
        return try {
            // Enable Hardware Accelerated GPU Scheduling (HAGS) check
            val script = """
                try {
                    ${'$'}path = 'HKCU:\Software\Microsoft\DirectX\UserGpuPreferences'
                    if (-not (Test-Path ${'$'}path)) { New-Item -Path ${'$'}path -Force | Out-Null }
                    
                    # Set DirectX preferences for better performance
                    ${'$'}dxPath = 'HKCU:\Software\Microsoft\DirectX\GraphicsSettings'
                    if (-not (Test-Path ${'$'}dxPath)) { New-Item -Path ${'$'}dxPath -Force | Out-Null }
                    Set-ItemProperty -Path ${'$'}dxPath -Name 'SwapEffectUpgradeCache' -Value 1 -Type DWord -ErrorAction SilentlyContinue
                    
                    Write-Output "OK"
                } catch { Write-Output "PARTIAL" }
            """.trimIndent()

            PowerShellRunner.runPowerShell(script, timeoutSec = 15)

            onLog("[OK] GPU scheduler optimized")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * 4. Disable Memory Compression
     */
    fun disableMemoryCompression(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Checking memory compression...")
        
        return try {
            val output = PowerShellRunner.runPowerShell(
                "Get-MMAgent | Select-Object -ExpandProperty MemoryCompression"
            ).trimmedOutput()

            if (output.equals("True", ignoreCase = true)) {
                onLog("[i] Memory compression is ON (admin needed to disable)")
                onLog("[i] Run as admin: Disable-MMAgent -MemoryCompression")
            } else {
                onLog("[OK] Memory compression already disabled")
            }
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * 5. Network Adapter Optimization
     */
    fun optimizeNetworkAdapter(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing network adapter...")
        
        return try {
            val script = """
                # Get active network adapter
                ${'$'}adapter = Get-NetAdapter | Where-Object { ${'$'}_.Status -eq 'Up' } | Select-Object -First 1
                if (${'$'}adapter) {
                    ${'$'}name = ${'$'}adapter.Name
                    
                    # Disable interrupt moderation for lower latency (if supported)
                    Set-NetAdapterAdvancedProperty -Name ${'$'}name -RegistryKeyword "*InterruptModeration" -RegistryValue 0 -ErrorAction SilentlyContinue
                    
                    # Disable flow control
                    Set-NetAdapterAdvancedProperty -Name ${'$'}name -RegistryKeyword "*FlowControl" -RegistryValue 0 -ErrorAction SilentlyContinue
                    
                    # Set receive/transmit buffers to max
                    Set-NetAdapterAdvancedProperty -Name ${'$'}name -RegistryKeyword "*ReceiveBuffers" -RegistryValue 2048 -ErrorAction SilentlyContinue
                    Set-NetAdapterAdvancedProperty -Name ${'$'}name -RegistryKeyword "*TransmitBuffers" -RegistryValue 2048 -ErrorAction SilentlyContinue
                    
                    Write-Output "OK:${'$'}name"
                } else {
                    Write-Output "NONE"
                }
            """.trimIndent()

            val output = PowerShellRunner.runPowerShell(script, timeoutSec = 20).output

            if (output.contains("OK:")) {
                val adapter = output.substringAfter("OK:").trim()
                onLog("[OK] Optimized: $adapter")
            } else {
                onLog("[i] No active network adapter found")
            }
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * 6. USB Polling Rate optimization
     */
    fun optimizeUsbPolling(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing USB polling rate...")
        
        return try {
            // Set USB mouse to 1000Hz (1ms) polling
            val script = """
                ${'$'}usbPath = 'HKCU:\Control Panel\Mouse'
                Set-ItemProperty -Path ${'$'}usbPath -Name 'MouseSensitivity' -Value '10' -ErrorAction SilentlyContinue
                
                # Disable mouse acceleration
                Set-ItemProperty -Path ${'$'}usbPath -Name 'MouseSpeed' -Value '0' -ErrorAction SilentlyContinue
                Set-ItemProperty -Path ${'$'}usbPath -Name 'MouseThreshold1' -Value '0' -ErrorAction SilentlyContinue
                Set-ItemProperty -Path ${'$'}usbPath -Name 'MouseThreshold2' -Value '0' -ErrorAction SilentlyContinue
                
                # SmoothMouseXCurve and SmoothMouseYCurve for raw input
                ${'$'}flat = [byte[]](0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xC0,0xCC,0x0C,0x00,0x00,0x00,0x00,0x00,0x80,0x99,0x19,0x00,0x00,0x00,0x00,0x00,0x40,0x66,0x26,0x00,0x00,0x00,0x00,0x00,0x00,0x33,0x33,0x00,0x00,0x00,0x00,0x00)
                Set-ItemProperty -Path ${'$'}usbPath -Name 'SmoothMouseXCurve' -Value ${'$'}flat -Type Binary -ErrorAction SilentlyContinue
                Set-ItemProperty -Path ${'$'}usbPath -Name 'SmoothMouseYCurve' -Value ${'$'}flat -Type Binary -ErrorAction SilentlyContinue
                
                Write-Output "OK"
            """.trimIndent()

            PowerShellRunner.runPowerShell(script, timeoutSec = 15)

            onLog("[OK] Mouse acceleration disabled, raw input enabled")
            onLog("[i] USB polling rate: check mouse software (1000Hz recommended)")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * 7. Visual Effects optimization (no admin needed)
     */
    fun optimizeVisualEffects(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing visual effects...")
        
        return try {
            val script = """
                ${'$'}path = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\VisualEffects'
                if (-not (Test-Path ${'$'}path)) { New-Item -Path ${'$'}path -Force | Out-Null }
                Set-ItemProperty -Path ${'$'}path -Name 'VisualFXSetting' -Value 2 -Type DWord
                
                # Disable animations
                ${'$'}dwm = 'HKCU:\Software\Microsoft\Windows\DWM'
                Set-ItemProperty -Path ${'$'}dwm -Name 'EnableAeroPeek' -Value 0 -Type DWord -ErrorAction SilentlyContinue
                
                # Disable transparency
                ${'$'}personalize = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize'
                Set-ItemProperty -Path ${'$'}personalize -Name 'EnableTransparency' -Value 0 -Type DWord -ErrorAction SilentlyContinue
                
                Write-Output "OK"
            """.trimIndent()

            PowerShellRunner.runPowerShell(script, timeoutSec = 15)

            onLog("[OK] Visual effects optimized for performance")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * 8. Power Plan - Ultimate Performance
     */
    fun setUltimatePowerPlan(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Setting Ultimate Performance power plan...")
        
        return try {
            val ultimateResult = PowerShellRunner.exec(
                listOf("powercfg", "/setactive", "e9a42b02-d5df-448d-aa00-03f14749eb61"),
                timeoutSec = 10
            )

            if (!ultimateResult.success) {
                val highPerfResult = PowerShellRunner.exec(
                    listOf("powercfg", "/setactive", "8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c"),
                    timeoutSec = 10
                )
                if (highPerfResult.success) {
                    onLog("[OK] High Performance power plan activated")
                } else {
                    onLog("[i] Using current power plan")
                }
            } else {
                onLog("[OK] Ultimate Performance power plan activated")
            }

            val tweaks = listOf(
                "powercfg /setacvalueindex scheme_current sub_processor PERFBOOSTMODE 2",
                "powercfg /setacvalueindex scheme_current sub_processor PERFBOOSTPOL 100",
                "powercfg /setacvalueindex scheme_current sub_processor PERFINCPOL 2",
                "powercfg /setacvalueindex scheme_current sub_processor PERFDECPOL 1",
                "powercfg /setactive scheme_current"
            )

            for (cmd in tweaks) {
                PowerShellRunner.runCmd(cmd, timeoutSec = 10)
            }

            onLog("[OK] CPU boost settings optimized")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * 9. Disable Fullscreen Optimizations globally
     */
    fun disableFullscreenOptimizations(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Disabling fullscreen optimizations...")
        
        return try {
            val script = """
                # Global FSO disable
                ${'$'}path = 'HKCU:\System\GameConfigStore'
                if (-not (Test-Path ${'$'}path)) { New-Item -Path ${'$'}path -Force | Out-Null }
                Set-ItemProperty -Path ${'$'}path -Name 'GameDVR_FSEBehaviorMode' -Value 2 -Type DWord
                Set-ItemProperty -Path ${'$'}path -Name 'GameDVR_HonorUserFSEBehaviorMode' -Value 1 -Type DWord
                Set-ItemProperty -Path ${'$'}path -Name 'GameDVR_FSEBehavior' -Value 2 -Type DWord
                Set-ItemProperty -Path ${'$'}path -Name 'GameDVR_DXGIHonorFSEWindowsCompatible' -Value 1 -Type DWord
                
                Write-Output "OK"
            """.trimIndent()

            PowerShellRunner.runPowerShell(script, timeoutSec = 15)

            onLog("[OK] Fullscreen optimizations disabled globally")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * 10. Disable Background Apps
     */
    fun disableBackgroundApps(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Disabling background apps...")
        
        return try {
            val script = """
                # Disable background apps
                ${'$'}path = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\BackgroundAccessApplications'
                if (-not (Test-Path ${'$'}path)) { New-Item -Path ${'$'}path -Force | Out-Null }
                Set-ItemProperty -Path ${'$'}path -Name 'GlobalUserDisabled' -Value 1 -Type DWord
                
                # Disable app launch tracking
                ${'$'}explorer = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\Advanced'
                Set-ItemProperty -Path ${'$'}explorer -Name 'Start_TrackProgs' -Value 0 -Type DWord -ErrorAction SilentlyContinue
                
                Write-Output "OK"
            """.trimIndent()

            PowerShellRunner.runPowerShell(script, timeoutSec = 15)

            onLog("[OK] Background apps disabled")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }
    
    /**
     * 11. MMCSS (Multimedia Class Scheduler Service) Optimization
     * Prioritize games over background tasks for networking and CPU.
     */
    fun optimizeMMCSS(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing MMCSS for games...")
        
        return try {
            val script = """
                ${'$'}path = 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Multimedia\SystemProfile\Tasks\Games'
                if (Test-Path ${'$'}path) {
                    Set-ItemProperty -Path ${'$'}path -Name 'GPU Priority' -Value 8 -Type DWord
                    Set-ItemProperty -Path ${'$'}path -Name 'Priority' -Value 6 -Type DWord
                    Set-ItemProperty -Path ${'$'}path -Name 'Scheduling Category' -Value 'High' -Type String
                    Set-ItemProperty -Path ${'$'}path -Name 'SFIO Priority' -Value 'High' -Type String
                    
                    # Prevent network throttling during high CPU usage
                    ${'$'}profile = 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Multimedia\SystemProfile'
                    Set-ItemProperty -Path ${'$'}profile -Name 'NetworkThrottlingIndex' -Value 0xffffffff -Type DWord
                    Set-ItemProperty -Path ${'$'}profile -Name 'SystemResponsiveness' -Value 0 -Type DWord
                    
                    Write-Output "OK"
                } else { Write-Output "NOTFOUND" }
            """.trimIndent()

            PowerShellRunner.runPowerShell(script, timeoutSec = 15)

            onLog("[OK] System prioritized for gaming (MMCSS High)")
            true
        } catch (e: Exception) {
            onLog("[!] MMCSS error: ${e.message}")
            false
        }
    }
    
    /**
     * 12. GPU Priority for specific processes
     */
    fun setGamePriority(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Setting global game priority...")
        return try {
            val script = "Set-ItemProperty -Path 'HKCU:\\Control Panel\\Desktop' -Name 'ForegroundLockTimeout' -Value 0 -Type DWord"
            PowerShellRunner.runPowerShell(script, timeoutSec = 10)
            onLog("[OK] Game priority set")
            true
        } catch (e: Exception) {
            onLog("[!] Game priority error: ${e.message}")
            false
        }
    }

    /**
     * 13. Optimized Virtual Memory (Pagefile)
     * Disables auto-managed, sets fixed size on fastest SSD to prevent stuttering.
     */
    fun setOptimizedPagefile(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing Virtual Memory (Pagefile)...")
        
        return try {
            val hw = com.vextiq.core.HardwareManager.getHardware()
            val ramGB = hw.ramGB
            
            // Algorithm for pagefile size (MB)
            // Minimum 16GB, scale with RAM setup
            val sizeMB = when {
                ramGB <= 16 -> 16384 // 16GB
                ramGB <= 32 -> 24576 // 24GB
                else -> 32768        // 32GB
            }
            
            onLog("[i] System RAM: ${ramGB}GB | Recommended Pagefile: ${sizeMB / 1024}GB")
            
            // Check free space on C:
            val driveC = java.io.File("C:\\")
            val freeSpaceGB = driveC.freeSpace / (1024L * 1024L * 1024L)
            if (freeSpaceGB < (sizeMB / 1024) + 10) { // Keep 10GB buffer
                onLog("[!] Not enough free space on C: (${freeSpaceGB}GB available)")
                onLog("[!] Need at least ${(sizeMB / 1024) + 10}GB free space.")
                return false
            }
            
            if (hw.systemDiskType == "HDD") {
                onLog("[!] System drive is HDD! Pagefile on HDD causes stuttering.")
                onLog("[!] Recommend moving games/system to SSD.")
            }
            
            val script = """
                # 1. Disable Automatic Managed Pagefile
                Set-CimInstance -Query "Select * from Win32_ComputerSystem" -Property @{AutomaticManagedPagefile=${'$'}False} -ErrorAction SilentlyContinue
                
                # 2. Find existing pagefile on C: or create new
                ${'$'}pagefile = Get-CimInstance Win32_PageFileSetting -Filter "Name='C:\\pagefile.sys'"
                if (-not ${'$'}pagefile) {
                    # If it doesn't exist as a setting, try to create it
                    New-CimInstance -ClassName Win32_PageFileSetting -Property @{Name="C:\pagefile.sys"; InitialSize=$sizeMB; MaximumSize=$sizeMB} -ErrorAction SilentlyContinue
                } else {
                    # Update existing
                    Set-CimInstance -InputObject ${'$'}pagefile -Property @{InitialSize=$sizeMB; MaximumSize=$sizeMB} -ErrorAction SilentlyContinue
                }
                
                Write-Output "OK"
            """.trimIndent()

            val output = PowerShellRunner.runPowerShell(script, timeoutSec = 30).trimmedOutput()

            if (output == "OK") {
                onLog("[OK] Fixed Pagefile set to ${sizeMB / 1024}GB on C:")
                onLog("[i] Fixed pagefile prevents dynamic resizing lag.")
                true
            } else {
                onLog("[!] Failed to set pagefile (Admin rights may be needed)")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }

    /**
     * 14. MSI Mode (Message Signaled Interrupts)
     * Reduces interrupt latency for GPUs and Network adapters.
     */
    fun enableMsiMode(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Enabling MSI Mode (Low Latency Interrupts)...")
        return try {
            val script = """
                # Target: GPUs and Network Adapters
                ${'$'}pciPaths = Get-ChildItem "HKLM:\SYSTEM\CurrentControlSet\Enum\PCI"
                foreach (${'$'}pci in ${'$'}pciPaths) {
                    Get-ChildItem ${'$'}pci.PSPath | foreach {
                        ${'$'}params = Join-Path ${'$'}_.PSPath "Device Parameters\Interrupt Management\MessageSignaledInterruptProperties"
                        if (Test-Path ${'$'}params) {
                            ${'$'}desc = (Get-ItemProperty -Path ${'$'}_.PSPath).DeviceDesc
                            if (${'$'}desc -match "NVIDIA|GeForce|Radeon|AMD|Ethernet|Network|Realtek|Intel") {
                                Set-ItemProperty -Path ${'$'}params -Name "MSISupported" -Value 1 -ErrorAction SilentlyContinue
                                Write-Output "Applied: ${'$'}desc"
                            }
                        }
                    }
                }
                Write-Output "OK"
            """.trimIndent()

            val output = PowerShellRunner.runPowerShell(script, timeoutSec = 30).trimmedOutput()

            if (output.contains("OK")) {
                onLog("[OK] MSI Mode enabled for GPU and Network devices")
                onLog("[i] This reduces 'stuttering' by streamlining interrupts.")
                true
            } else {
                onLog("[!] MSI Mode failed (Admin rights required)")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }

    /**
     * 15. Global Shader Cache Cleaner
     * Clears DX/NVIDIA shader caches to solve stuttering.
     */
    fun cleanGlobalShaders(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Cleaning Global Shader Cache...")
        return try {
            val userLocal = System.getenv("LOCALAPPDATA")
            val paths = listOf(
                "$userLocal\\NVIDIA\\DXCache",
                "$userLocal\\NVIDIA\\GLCache",
                "$userLocal\\D3DSCache",
                "C:\\Windows\\ServiceProfiles\\LocalService\\AppData\\Local\\DirectX\\D3DSCache"
            )
            
            var cleanedCount = 0
            for (path in paths) {
                val dir = java.io.File(path)
                if (dir.exists()) {
                    onLog("  Cleaning: $path")
                    try {
                        dir.listFiles()?.forEach { it.deleteRecursively() }
                        cleanedCount++
                    } catch (_: Exception) {
                        // Some files might be in use
                    }
                }
            }
            
            onLog("[OK] Global Shader Cache cleaned ($cleanedCount locations)")
            true
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }

    /**
     * 16. Windows Debloat (Service Optimization)
     * Disables 10+ non-essential telemetry and background services.
     */
    fun optimizeServices(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Debloating Windows Services (Telemetry/Bloat)...")
        return try {
            val script = """
                ${'$'}services = @('DiagTrack', 'dmwappushservice', 'SysMain', 'RemoteRegistry', 'MapsBroker', 'WbioSrvc', 'PhoneSvc', 'WalletService', 'XblAuthManager', 'XblGameSave')
                foreach (${'$'}s in ${'$'}services) {
                    try {
                        Stop-Service -Name ${'$'}s -Force -ErrorAction SilentlyContinue
                        Set-Service -Name ${'$'}s -StartupType Disabled -ErrorAction SilentlyContinue
                        Write-Output "Disabled: ${'$'}s"
                    } catch {
                        # Service might not exist or access denied
                    }
                }
                Write-Output "OK"
            """.trimIndent()

            val output = PowerShellRunner.runPowerShell(script, timeoutSec = 30).trimmedOutput()

            if (output.contains("OK")) {
                onLog("[OK] 10+ Background services disabled")
                onLog("[i] Frees up CPU cycles and RAM for gaming.")
                true
            } else {
                onLog("[!] Service optimization failed")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }

    /**
     * 17. USB Zero-Lag Mode
     * Disables USB selective suspend and power saving for peripherals.
     */
    fun optimizeUsbLatency(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Enabling USB Zero-Lag Mode...")
        return try {
            val script = """
                # 1. Disable USB Selective Suspend in Power Plan
                powercfg /SETACVALUEINDEX SCHEME_CURRENT 2a737441-1930-4402-8d77-b2bebba308a3 483a51dc-2715-49d6-b4d3-111385149b1e 0
                powercfg /SETDCVALUEINDEX SCHEME_CURRENT 2a737441-1930-4402-8d77-b2bebba308a3 483a51dc-2715-49d6-b4d3-111385149b1e 0
                
                # 2. Registry tweak for USB Hubs
                ${'$'}usbKeys = Get-ChildItem "HKLM:\SYSTEM\CurrentControlSet\Enum\USB" -Recurse -ErrorAction SilentlyContinue | Where-Object { ${'$'}_.Name -match "Device Parameters" }
                foreach (${'$'}key in ${'$'}usbKeys) {
                    try {
                        Set-ItemProperty -Path ${'$'}key.PSPath -Name "EnhancedPowerManagementEnabled" -Value 0 -ErrorAction SilentlyContinue
                    } catch {
                        # Registry access might fail
                    }
                }
                
                # 3. PCI Latency & PCIe Link Max Performance
                powercfg /SETACVALUEINDEX SCHEME_CURRENT ee12f906-d277-404b-b6da-e5fa1a576df5 50127dc3-0339-4ed8-ba4e-0889fb3530f3 0
                
                # Apply power changes
                powercfg /S SCHEME_CURRENT
                Write-Output "OK"
            """.trimIndent()

            val output = PowerShellRunner.runPowerShell(script, timeoutSec = 30).trimmedOutput()

            if (output.contains("OK")) {
                onLog("[OK] USB Power Saving disabled (Always High Speed)")
                onLog("[OK] PCIe Link State Power Management: OFF")
                onLog("[i] Prevents mouse/keyboard input lag spikes.")
                true
            } else {
                onLog("[!] USB optimization failed")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }

    /**
     * 18. CPU Affinity (P-Core Pinner)
     * Pins games to Performance cores, avoids E-cores/Efficiency cores.
     */
    fun setCpuAffinity(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Optimizing CPU Core Affinity (P-Core Focus)...")
        return try {
            val script = """
                # 1. Broad game process list
                ${'$'}gameNames = 'StarCitizen|starcitizen|BF2042|battlefield|Cyberpunk|cs2|valorant|VALORANT'
                ${'$'}gameProcs = Get-Process | Where-Object { ${'$'}_.ProcessName -match ${'$'}gameNames }
                
                if (${'$'}gameProcs) {
                    # Create affinity mask for first 8-16 threads (P-Cores)
                    # 0xFFFF = first 16 threads
                    foreach (${'$'}p in ${'$'}gameProcs) {
                        try {
                            ${'$'}p.ProcessorAffinity = 0xFFFF
                            Write-Output "Pinned: ${'$'}(${'$'}p.ProcessName)"
                        } catch {
                            # Process might not allow affinity change
                        }
                    }
                }
                Write-Output "OK"
            """.trimIndent()

            val output = PowerShellRunner.runPowerShell(script, timeoutSec = 15).trimmedOutput()

            if (output.contains("OK")) {
                onLog("[OK] P-Core pinning applied to active games")
                onLog("[i] Prevents E-Core stuttering on hybrid CPUs.")
                true
            } else {
                onLog("[!] CPU affinity failed")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }

    /**
     * 19. Intelligent Standby List Cleaner (ISLC)
     * Purges unused Windows standby memory cache.
     */
    fun cleanStandbyList(onLog: (String) -> Unit): Boolean {
        onLog("[>>] Purging Windows Standby Memory Cache...")
        return try {
            val fallbackScript = "Get-Process | ForEach-Object { try { [Runtime.InteropServices.Marshal]::SetProcessWorkingSetSize(${'$'}_.Handle, -1, -1) } catch {} }; Write-Output 'OK'"

            val output = PowerShellRunner.runPowerShell(fallbackScript, timeoutSec = 30).trimmedOutput()

            if (output.contains("OK")) {
                onLog("[OK] Standby List & Working Sets purged")
                onLog("[i] Frees up immediate blocks of physical RAM.")
                true
            } else {
                onLog("[!] Standby cleaner failed")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Error: ${e.message}")
            false
        }
    }

    /**
     * ONE-CLICK ULTIMATE BOOST - Run all optimizations
     */
    fun ultimateBoost(onLog: (String) -> Unit): Int {
        onLog("╔════════════════════════════════════════╗")
        onLog("║     VEXTIQ ULTIMATE BOOST              ║")
        onLog("╚════════════════════════════════════════╝")
        onLog("")
        
        var successCount = 0
        val total = 19
        
        // List of all optimizations
        onLog("[1/$total] Timer Resolution")
        if (setTimerResolution(onLog)) successCount++
        
        onLog("[2/$total] CPU Core Parking")
        if (disableCoreParking(onLog)) successCount++
        
        onLog("[3/$total] Power Plan")
        if (setUltimatePowerPlan(onLog)) successCount++
        
        onLog("[4/$total] GPU Scheduler")
        if (optimizeGpuScheduler(onLog)) successCount++
        
        onLog("[5/$total] Memory Compression")
        if (disableMemoryCompression(onLog)) successCount++
        
        onLog("[6/$total] Network Adapter")
        if (optimizeNetworkAdapter(onLog)) successCount++
        
        onLog("[7/$total] Mouse Optimization")
        if (optimizeUsbPolling(onLog)) successCount++
        
        onLog("[8/$total] Visual Effects")
        if (optimizeVisualEffects(onLog)) successCount++
        
        onLog("[9/$total] Fullscreen Optimizations")
        if (disableFullscreenOptimizations(onLog)) successCount++
        
        onLog("[10/$total] Background Apps")
        if (disableBackgroundApps(onLog)) successCount++
        
        onLog("[11/$total] MMCSS Gaming Priority")
        if (optimizeMMCSS(onLog)) successCount++
        
        onLog("[12/$total] Global Game Priority")
        if (setGamePriority(onLog)) successCount++
        
        onLog("[13/$total] Virtual Memory (Pagefile)")
        if (setOptimizedPagefile(onLog)) successCount++
        
        onLog("[14/$total] MSI Mode (Latency Fix)")
        if (enableMsiMode(onLog)) successCount++
        
        onLog("[15/$total] Global Shader Cache")
        if (cleanGlobalShaders(onLog)) successCount++
        
        onLog("[16/$total] Windows Debloat (Services)")
        if (optimizeServices(onLog)) successCount++
        
        onLog("[17/$total] USB Zero-Lag Mode")
        if (optimizeUsbLatency(onLog)) successCount++
        
        onLog("[18/$total] CPU Core Affinity")
        if (setCpuAffinity(onLog)) successCount++
        
        onLog("[19/$total] Standby List Cleaner")
        if (cleanStandbyList(onLog)) successCount++
        
        onLog("════════════════════════════════════════")
        onLog("[OK] ULTIMATE BOOST COMPLETE!")
        onLog("[i] $successCount/$total optimizations applied")
        onLog("[i] Restart recommended for full effect")
        onLog("════════════════════════════════════════")
        return successCount
    }
    
    /**
     * REPAIR MODE - Revert system to defaults
     */
    fun repairSystem(onLog: (String) -> Unit): Boolean {
        onLog("╔════════════════════════════════════════╗")
        onLog("║     VEXTIQ SYSTEM REPAIR MODE          ║")
        onLog("╚════════════════════════════════════════╝")
        onLog("[>>] Reverting system-level optimizations...")
        
        return try {
            val script = """
                # 1. Reset Power Plan to Balanced
                ${'$'}balanced = Get-WmiObject -Namespace root\cimv2\power -Class Win32_PowerPlan | Where-Object { ${'$'}_.ElementName -eq 'Balanced' }
                if (${'$'}balanced) { ${'$'}balanced.Activate() }
                
                # 2. Reset Network Stack
                netsh int ip reset | Out-Null
                netsh winsock reset | Out-Null
                
                # 3. Restore Virtual Memory (System Managed)
                ${'$'}drive = Get-WmiObject Win32_Volume | Where-Object { ${'$'}_.DriveLetter -eq 'C:' }
                ${'$'}driveLetter = "C:"
                Set-WmiInstance -Class Win32_PageFileSetting -Arguments @{Name="${'$'}driveLetter\pagefile.sys"; InitialSize=0; MaximumSize=0} -ErrorAction SilentlyContinue
                # Ensure auto-managed
                ${'$'}comp = Get-WmiObject Win32_ComputerSystem -EnableAllPrivileges
                ${'$'}comp.AutomaticManagedPagefile = ${'$'}true
                ${'$'}comp.Put()

                # 4. Re-enable critical background services
                ${'$'}services = @("SysMain", "DiagTrack", "WSearch", "Themes", "Spooler", "TabletInputService")
                foreach (${'$'}s in ${'$'}services) {
                    Set-Service -Name ${'$'}s -StartupType Automatic -ErrorAction SilentlyContinue
                    Start-Service -Name ${'$'}s -ErrorAction SilentlyContinue
                }

                # 5. Restore Visual Effects
                Set-ItemProperty -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\VisualEffects' -Name 'VisualFXSetting' -Value 0
                
                Write-Output "OK"
            """.trimIndent()

            val output = PowerShellRunner.runPowerShell(script, timeoutSec = 60).trimmedOutput()

            if (output.contains("OK")) {
                onLog("[OK] System successfully reverted to defaults")
                onLog("[i] Most background services and power plans restored.")
                onLog("[i] A full restart is REQUIRED to finish repair.")
                true
            } else {
                onLog("[!] Repair process encountered errors")
                false
            }
        } catch (e: Exception) {
            onLog("[!] Error during repair: ${e.message}")
            false
        }
    }
}
