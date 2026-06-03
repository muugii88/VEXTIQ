package com.vextiq.core

/**
 * Temperature Monitor - Get CPU and GPU temperatures
 */
class TemperatureMonitor {
    
    data class TempInfo(
        val cpuTemp: Int,
        val gpuTemp: Int,
        val gpuHotspot: Int = 0
    )
    
    private var gpuVendor: String = "Unknown"
    
    fun setGpuVendor(vendor: String) {
        gpuVendor = vendor
    }
    
    /**
     * Get all temperatures
     */
    fun getTemperatures(): TempInfo {
        return TempInfo(
            cpuTemp = getCpuTemperature(),
            gpuTemp = getGpuTemperature(),
            gpuHotspot = 0
        )
    }
    
    /**
     * Get CPU temperature
     */
    fun getCpuTemperature(): Int {
        return try {
            // Try PowerShell WMI
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                """
                try {
                    ${'$'}temp = (Get-WmiObject MSAcpi_ThermalZoneTemperature -Namespace "root/wmi" -ErrorAction Stop).CurrentTemperature
                    [math]::Round((${'$'}temp[0] - 2732) / 10)
                } catch {
                    # Try alternative method
                    ${'$'}temp = (Get-CimInstance -ClassName Win32_PerfFormattedData_Counters_ThermalZoneInformation -ErrorAction SilentlyContinue).Temperature
                    if (${'$'}temp) { ${'$'}temp[0] - 273 } else { 0 }
                }
                """.trimIndent()
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            
            output.toIntOrNull()?.coerceIn(0, 120) ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get GPU temperature
     */
    fun getGpuTemperature(): Int {
        return when (gpuVendor) {
            "NVIDIA" -> getNvidiaTemperature()
            "AMD" -> getAmdTemperature()
            else -> getGenericGpuTemperature()
        }
    }
    
    /**
     * Get NVIDIA GPU temperature
     */
    private fun getNvidiaTemperature(): Int {
        return try {
            val process = ProcessBuilder(listOf(
                "nvidia-smi", "--query-gpu=temperature.gpu", "--format=csv,noheader,nounits"
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                output.toIntOrNull() ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get AMD GPU temperature
     */
    private fun getAmdTemperature(): Int {
        return try {
            val process = ProcessBuilder(listOf(
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                """
                try {
                    ${'$'}temp = (Get-Counter '\GPU Adapter Memory(*)\Temperature' -ErrorAction Stop).CounterSamples | 
                        Select-Object -First 1 -ExpandProperty CookedValue
                    [math]::Round(${'$'}temp)
                } catch { 0 }
                """.trimIndent()
            )).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            
            output.toIntOrNull()?.coerceIn(0, 120) ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Generic GPU temperature query
     */
    private fun getGenericGpuTemperature(): Int {
        // Try AMD method first (works for most GPUs via Windows counters)
        val temp = getAmdTemperature()
        if (temp > 0) return temp
        
        // Try NVIDIA
        return getNvidiaTemperature()
    }
    
    /**
     * Get temperature status color
     */
    fun getTemperatureStatus(temp: Int): String {
        return when {
            temp <= 0 -> "unknown"
            temp < 60 -> "cool"
            temp < 75 -> "warm"
            temp < 85 -> "hot"
            else -> "critical"
        }
    }
    
    /**
     * Get temperature color (for UI)
     */
    fun getTemperatureColor(temp: Int): Long {
        return when {
            temp <= 0 -> 0xFF808080 // Gray
            temp < 60 -> 0xFF00E5A0 // Green
            temp < 75 -> 0xFFFFB800 // Yellow
            temp < 85 -> 0xFFFF8C00 // Orange
            else -> 0xFFFF3B3B // Red
        }
    }
}
