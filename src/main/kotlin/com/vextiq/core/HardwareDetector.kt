import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.MemoryUsage

object HardwareDetector {
    fun getCpuInfo(): String {
        return "${Runtime.getRuntime().availableProcessors()} CPU cores"
    }

    fun getGpuInfo(): String {
        // Using a library or command specific to the environment is needed for GPU details
        return "GPU information retrieval needs specific library support"
    }

    fun getRamInfo(): String {
        val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
        val heapMemoryUsage: MemoryUsage = memoryBean.heapMemoryUsage

        return "Total RAM: ${heapMemoryUsage.max / (1024 * 1024)} MB, Used RAM: ${heapMemoryUsage.used / (1024 * 1024)} MB"
    }

    fun printHardwareSpecs() {
        println(getCpuInfo())
        println(getGpuInfo())
        println(getRamInfo())
    }
}