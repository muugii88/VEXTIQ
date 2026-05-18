package com.vextiq.core

import com.sun.jna.*
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.LongByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * ETW-based FPS monitor.
 *
 * Consumes DXGI Present events directly from the Windows ETW stream — the same
 * technique PresentMon uses internally, but without spawning an external .exe.
 *
 * Provider: Microsoft-Windows-DXGI  ({CA11C036-0102-4A2D-A6AD-F03CFED5D3C9})
 * Key event: PresentStart (ID 42) — fires once per frame that DXGI submits.
 *
 * Requirements:
 *  - Admin privileges (ETW real-time sessions require SeSystemProfilePrivilege)
 *  - Session name collision is handled: existing VEXTIQ sessions are stopped first.
 *
 * If ETW session creation fails for any reason (no admin, blocked by AV, etc.),
 * [isRunning] will remain false and callers should fall back to [FpsMonitor]'s
 * PresentMon path.
 *
 * Status: JNA bindings + session lifecycle implemented. Real-time event consumption
 * is in place with a best-effort frame counter. Event payload parsing for per-process
 * attribution uses the ProcessId field of the EVENT_HEADER which is always present.
 */
class EtwFpsMonitor {

    data class EtwFrameStats(
        val fps: Int = 0,
        val frametime: Double = 0.0,
        val processName: String = "",
        val available: Boolean = false,
        val isRunning: Boolean = false,
        val errorMessage: String = ""
    )

    private val _stats = MutableStateFlow(EtwFrameStats())
    val stats: StateFlow<EtwFrameStats> = _stats

    @Volatile private var sessionHandle: Long = 0
    @Volatile private var consumerHandle: Long = 0
    @Volatile private var consumerJob: Job? = null

    private val frameCountByPid = ConcurrentHashMap<Int, Int>()
    @Volatile private var lastSampleNs = 0L

    // Skip our own process so Compose Desktop's DXGI Present events don't
    // get picked as a "game" by GameDetection (they would otherwise drive
    // NetworkMonitor.setActiveGame and trigger region probing for the host app).
    private val selfPid: Int = try {
        ProcessHandle.current().pid().toInt()
    } catch (_: Throwable) { -1 }

    companion object {
        private const val SESSION_NAME = "VEXTIQ-FPS-Session"
        private const val EVENT_TRACE_REAL_TIME_MODE = 0x00000100
        private const val PROCESS_TRACE_MODE_REAL_TIME = 0x00000100
        private const val PROCESS_TRACE_MODE_EVENT_RECORD = 0x10000000
        private const val WNODE_FLAG_TRACED_GUID = 0x00020000
        private const val INVALID_PROCESSTRACE_HANDLE = -1L

        private const val ERROR_ACCESS_DENIED = 5
        private const val ERROR_ALREADY_EXISTS = 183

        // Microsoft-Windows-DXGI  {CA11C036-0102-4A2D-A6AD-F03CFED5D3C9}
        private val DXGI_PROVIDER_GUID: Guid.GUID = Guid.GUID("{CA11C036-0102-4A2D-A6AD-F03CFED5D3C9}")
    }

    /**
     * Best-effort check whether ETW is likely usable.
     * Actual availability is only known after [start] is called.
     */
    fun isAvailable(): Boolean {
        return try {
            Advapi32Ext.INSTANCE != null
        } catch (_: Throwable) {
            false
        }
    }

    fun start(scope: CoroutineScope): Boolean {
        if (sessionHandle != 0L) return true
        val lib = Advapi32Ext.INSTANCE ?: run {
            _stats.value = _stats.value.copy(errorMessage = "JNA: advapi32 not loadable")
            return false
        }

        // Stop any lingering session from a previous crashed run
        try { stopExistingSession() } catch (_: Throwable) {}

        val props = makeSessionProperties()
        val handleRef = LongByReference(0)

        val result = lib.StartTraceW(handleRef, WString(SESSION_NAME), props)
        if (result != 0) {
            val msg = when (result) {
                ERROR_ACCESS_DENIED -> "ETW requires admin privileges"
                ERROR_ALREADY_EXISTS -> "ETW session already exists"
                else -> "StartTraceW failed ($result)"
            }
            _stats.value = _stats.value.copy(available = false, errorMessage = msg)
            return false
        }
        sessionHandle = handleRef.value

        val enableResult = lib.EnableTraceEx2(
            sessionHandle,
            DXGI_PROVIDER_GUID,
            1, // EVENT_CONTROL_CODE_ENABLE_PROVIDER
            0x05, // TRACE_LEVEL_VERBOSE
            0,
            0,
            0,
            null
        )
        if (enableResult != 0) {
            _stats.value = _stats.value.copy(errorMessage = "EnableTraceEx2 failed ($enableResult)")
            stop()
            return false
        }

        _stats.value = _stats.value.copy(available = true, isRunning = true, errorMessage = "")

        consumerJob = scope.launch(Dispatchers.IO) {
            lastSampleNs = System.nanoTime()
            try {
                runConsumerLoop()
            } catch (e: Throwable) {
                _stats.value = _stats.value.copy(errorMessage = "Consumer: ${e.message}")
            }
        }

        // Sampler coroutine - every second, turn accumulated counts into FPS
        scope.launch(Dispatchers.Default) {
            while (isActive && sessionHandle != 0L) {
                delay(1000)
                emitSample()
            }
        }
        return true
    }

    fun stop() {
        consumerJob?.cancel()
        consumerJob = null

        val lib = Advapi32Ext.INSTANCE
        if (lib != null && sessionHandle != 0L) {
            val props = makeSessionProperties()
            try { lib.ControlTraceW(sessionHandle, WString(SESSION_NAME), props, 1 /* EVENT_TRACE_CONTROL_STOP */) } catch (_: Throwable) {}
        }
        if (lib != null && consumerHandle != 0L && consumerHandle != INVALID_PROCESSTRACE_HANDLE) {
            try { lib.CloseTrace(consumerHandle) } catch (_: Throwable) {}
        }
        sessionHandle = 0
        consumerHandle = 0
        _stats.value = _stats.value.copy(isRunning = false)
    }

    private fun stopExistingSession() {
        val lib = Advapi32Ext.INSTANCE ?: return
        val props = makeSessionProperties()
        lib.ControlTraceW(0, WString(SESSION_NAME), props, 1)
    }

    private fun makeSessionProperties(): EventTraceProperties {
        val p = EventTraceProperties()
        p.Wnode.BufferSize = p.size()
        p.Wnode.Flags = WNODE_FLAG_TRACED_GUID
        p.Wnode.ClientContext = 1 // QPC clock
        p.BufferSize = 64
        p.MinimumBuffers = 4
        p.MaximumBuffers = 16
        p.LogFileMode = EVENT_TRACE_REAL_TIME_MODE
        p.LoggerNameOffset = EventTraceProperties.HEADER_SIZE
        p.LogFileNameOffset = 0
        p.write()
        return p
    }

    private fun runConsumerLoop() {
        val lib = Advapi32Ext.INSTANCE ?: return
        val logfile = EventTraceLogfile()
        logfile.LoggerName = WString(SESSION_NAME)
        logfile.LogFileName = null
        logfile.ProcessTraceMode = PROCESS_TRACE_MODE_REAL_TIME or PROCESS_TRACE_MODE_EVENT_RECORD
        logfile.EventRecordCallback = EventRecordCallback { record ->
            try {
                val pid = record?.EventHeader?.ProcessId ?: return@EventRecordCallback
                if (pid == selfPid) return@EventRecordCallback
                // Event ID filter — DXGI Present is ID 42, but collecting anything
                // from the DXGI provider still yields per-process frame counts
                // (PresentStart + PresentStop are the common frame events).
                val id = record.EventHeader.EventDescriptor.Id
                if (id.toInt() == 42 || id.toInt() == 43 /* PresentStop */) {
                    frameCountByPid.merge(pid, 1) { a, b -> a + b }
                }
            } catch (_: Throwable) {
                // Never throw from callback
            }
        }
        logfile.write()

        val handle = lib.OpenTraceW(logfile)
        if (handle == INVALID_PROCESSTRACE_HANDLE) {
            _stats.value = _stats.value.copy(errorMessage = "OpenTraceW failed")
            return
        }
        consumerHandle = handle

        val handles = Memory(8).apply { setLong(0, handle) }
        // ProcessTrace blocks until session is stopped
        val rc = lib.ProcessTrace(handles, 1, null, null)
        if (rc != 0) {
            _stats.value = _stats.value.copy(errorMessage = "ProcessTrace rc=$rc")
        }
    }

    private fun emitSample() {
        val now = System.nanoTime()
        val elapsedNs = now - lastSampleNs
        lastSampleNs = now
        if (elapsedNs <= 0) return

        // Resolve process names for every PID with frames, then apply the
        // shared game-detection filter. Without this step, Discord or Chrome
        // overlay frames would happily win the "max frame count" race.
        val snapshot = frameCountByPid.toMap()
        frameCountByPid.clear()

        if (snapshot.isEmpty()) return

        val nameToCount = HashMap<String, Int>(snapshot.size)
        for ((pid, count) in snapshot) {
            val name = resolveProcessName(pid)
            if (name.isEmpty()) continue
            nameToCount.merge(name, count) { a, b -> a + b }
        }

        val winner = GameDetection.pickActiveGame(nameToCount, minFramesForUnknown = 30)
        if (winner.isEmpty()) return

        val frames = nameToCount[winner] ?: return
        val seconds = elapsedNs / 1_000_000_000.0
        val fps = if (seconds > 0) (frames / seconds).toInt() else 0
        val frametime = if (fps > 0) 1000.0 / fps else 0.0

        _stats.value = _stats.value.copy(
            fps = fps,
            frametime = frametime,
            processName = winner,
            isRunning = true,
            available = true
        )
    }

    // PID → process-name cache. Each lookup spawns a PowerShell process, so
    // re-resolving the same PID every second blows up CPU/IO. PIDs are recycled
    // when the OS reuses them, but a stale name for a now-dead PID is harmless
    // here — we only use it for display.
    private val pidNameCache = ConcurrentHashMap<Int, String>()

    private fun resolveProcessName(pid: Int): String {
        pidNameCache[pid]?.let { return it }
        val name = try {
            val out = PowerShellRunner.runPowerShell(
                "(Get-Process -Id $pid -ErrorAction SilentlyContinue).ProcessName",
                timeoutSec = 3
            ).trimmedOutput()
            out.ifEmpty { "PID-$pid" }
        } catch (_: Exception) {
            "PID-$pid"
        }
        pidNameCache[pid] = name
        // Cap cache size — without bounding this, a long-running session could
        // accumulate thousands of dead PIDs.
        if (pidNameCache.size > 256) {
            val iterator = pidNameCache.keys.iterator()
            repeat(64) { if (iterator.hasNext()) { iterator.next(); iterator.remove() } }
        }
        return name
    }

    // ─── JNA types ──────────────────────────────────────────────────────────

    fun interface EventRecordCallback : StdCallLibrary.StdCallCallback {
        fun invoke(eventRecord: EventRecord?)
    }

    @Structure.FieldOrder("EventHeader")
    class EventRecord : Structure() {
        @JvmField var EventHeader = EventHeader()
        // The full EVENT_RECORD has more fields but we only need EventHeader
        // for pid + event id. JNA pointer-to-struct handoff skips past the rest.
    }

    @Structure.FieldOrder("Size", "HeaderType", "Flags", "EventProperty", "ThreadId", "ProcessId",
        "TimeStamp", "ProviderId", "EventDescriptor", "KernelTime", "UserTime", "ActivityId")
    class EventHeader : Structure() {
        @JvmField var Size: Short = 0
        @JvmField var HeaderType: Short = 0
        @JvmField var Flags: Short = 0
        @JvmField var EventProperty: Short = 0
        @JvmField var ThreadId: Int = 0
        @JvmField var ProcessId: Int = 0
        @JvmField var TimeStamp: Long = 0
        @JvmField var ProviderId = Guid.GUID()
        @JvmField var EventDescriptor = EventDescriptor()
        @JvmField var KernelTime: Int = 0
        @JvmField var UserTime: Int = 0
        @JvmField var ActivityId = Guid.GUID()
    }

    @Structure.FieldOrder("Id", "Version", "Channel", "Level", "Opcode", "Task", "Keyword")
    class EventDescriptor : Structure() {
        @JvmField var Id: Short = 0
        @JvmField var Version: Byte = 0
        @JvmField var Channel: Byte = 0
        @JvmField var Level: Byte = 0
        @JvmField var Opcode: Byte = 0
        @JvmField var Task: Short = 0
        @JvmField var Keyword: Long = 0
    }

    @Structure.FieldOrder("BufferSize", "ProviderId", "HistoricalContext", "TimeStamp", "Guid",
        "ClientContext", "Flags")
    class WnodeHeader : Structure() {
        @JvmField var BufferSize: Int = 0
        @JvmField var ProviderId: Int = 0
        @JvmField var HistoricalContext: Long = 0
        @JvmField var TimeStamp: Long = 0
        @JvmField var Guid: com.sun.jna.platform.win32.Guid.GUID = com.sun.jna.platform.win32.Guid.GUID()
        @JvmField var ClientContext: Int = 0
        @JvmField var Flags: Int = 0
    }

    @Structure.FieldOrder("Wnode", "BufferSize", "MinimumBuffers", "MaximumBuffers", "MaximumFileSize",
        "LogFileMode", "FlushTimer", "EnableFlags", "AgeLimit", "NumberOfBuffers", "FreeBuffers",
        "EventsLost", "BuffersWritten", "LogBuffersLost", "RealTimeBuffersLost", "LoggerThreadId",
        "LogFileNameOffset", "LoggerNameOffset", "LoggerName", "LogFileName")
    open class EventTraceProperties : Structure() {
        companion object {
            // Offset of LoggerName field — after all scalar fields
            const val HEADER_SIZE = 120
            const val NAME_BUFFER = 1024
        }
        @JvmField var Wnode = WnodeHeader()
        @JvmField var BufferSize: Int = 0
        @JvmField var MinimumBuffers: Int = 0
        @JvmField var MaximumBuffers: Int = 0
        @JvmField var MaximumFileSize: Int = 0
        @JvmField var LogFileMode: Int = 0
        @JvmField var FlushTimer: Int = 0
        @JvmField var EnableFlags: Int = 0
        @JvmField var AgeLimit: Int = 0
        @JvmField var NumberOfBuffers: Int = 0
        @JvmField var FreeBuffers: Int = 0
        @JvmField var EventsLost: Int = 0
        @JvmField var BuffersWritten: Int = 0
        @JvmField var LogBuffersLost: Int = 0
        @JvmField var RealTimeBuffersLost: Int = 0
        @JvmField var LoggerThreadId: Pointer? = null
        @JvmField var LogFileNameOffset: Int = 0
        @JvmField var LoggerNameOffset: Int = 0
        @JvmField var LoggerName = CharArray(NAME_BUFFER)
        @JvmField var LogFileName = CharArray(NAME_BUFFER)
    }

    /**
     * EVENT_TRACE_LOGFILEW layout for 64-bit Windows.
     *
     * The first version of this struct declared CurrentEvent and LogfileHeader
     * as `Pointer?` (8 bytes each). They are actually INLINE structs of size
     * ~96 (EVENT_TRACE) and ~280 (TRACE_LOGFILE_HEADER) bytes. That mismatch
     * placed EventRecordCallback at offset 68 instead of ~432 — when admin
     * unblocked StartTraceW, OpenTraceW read a garbage function pointer at
     * offset 432 and ProcessTrace dispatched into it: EXCEPTION_ACCESS_VIOLATION.
     *
     * We don't read those inline structs from Java, so they're modeled here as
     * opaque ByteArray placeholders sized to match the real struct. That keeps
     * the offsets of the fields we DO read/write (BufferCallback,
     * EventRecordCallback, Context) at the byte positions Windows expects.
     */
    @Structure.FieldOrder("LogFileName", "LoggerName", "CurrentTime", "BuffersRead",
        "ProcessTraceMode", "CurrentEventPad", "LogfileHeaderPad", "BufferCallback",
        "BufferSize", "Filled", "EventsLost", "EventRecordCallback", "IsKernelTrace", "Context")
    open class EventTraceLogfile : Structure() {
        @JvmField var LogFileName: WString? = null
        @JvmField var LoggerName: WString? = null
        @JvmField var CurrentTime: Long = 0
        @JvmField var BuffersRead: Int = 0
        @JvmField var ProcessTraceMode: Int = 0
        @JvmField var CurrentEventPad: ByteArray = ByteArray(EVENT_TRACE_SIZE)
        @JvmField var LogfileHeaderPad: ByteArray = ByteArray(TRACE_LOGFILE_HEADER_SIZE)
        @JvmField var BufferCallback: Pointer? = null
        @JvmField var BufferSize: Int = 0
        @JvmField var Filled: Int = 0
        @JvmField var EventsLost: Int = 0
        @JvmField var EventRecordCallback: EventRecordCallback? = null
        @JvmField var IsKernelTrace: Int = 0
        @JvmField var Context: Pointer? = null

        companion object {
            // sizeof(EVENT_TRACE) on x64: EVENT_TRACE_HEADER (56) + InstanceId (4)
            //   + ParentInstanceId (4) + ParentGuid (16) + MofData ptr (8) +
            //   MofLength (4) + ClientContext/BufferContext union (4) = 96
            const val EVENT_TRACE_SIZE = 96

            // sizeof(TRACE_LOGFILE_HEADER) on x64: BufferSize (4) + Version (4)
            //   + ProviderVersion (4) + NumberOfProcessors (4) + EndTime (8) +
            //   TimerResolution (4) + MaximumFileSize (4) + LogFileMode (4) +
            //   BuffersWritten (4) + LogInstanceGuid union (16) + LoggerName ptr (8)
            //   + LogFileName ptr (8) + TIME_ZONE_INFORMATION (172) + BootTime (8)
            //   + PerfFreq (8) + StartTime (8) + ReservedFlags (4) + BuffersLost (4)
            //   = 276, rounded up to 8-byte alignment = 280
            const val TRACE_LOGFILE_HEADER_SIZE = 280
        }
    }

    interface Advapi32Ext : StdCallLibrary {
        fun StartTraceW(
            TraceHandle: LongByReference,
            InstanceName: WString,
            Properties: EventTraceProperties
        ): Int

        fun ControlTraceW(
            TraceHandle: Long,
            InstanceName: WString,
            Properties: EventTraceProperties,
            ControlCode: Int
        ): Int

        fun EnableTraceEx2(
            TraceHandle: Long,
            ProviderId: Guid.GUID,
            ControlCode: Int,
            Level: Int,
            MatchAnyKeyword: Long,
            MatchAllKeyword: Long,
            Timeout: Int,
            EnableParameters: Pointer?
        ): Int

        fun OpenTraceW(Logfile: EventTraceLogfile): Long

        fun ProcessTrace(
            HandleArray: Pointer,
            HandleCount: Int,
            StartTime: Pointer?,
            EndTime: Pointer?
        ): Int

        fun CloseTrace(TraceHandle: Long): Int

        companion object {
            val INSTANCE: Advapi32Ext? = try {
                Native.load("Advapi32", Advapi32Ext::class.java, W32APIOptions.UNICODE_OPTIONS) as Advapi32Ext
            } catch (_: Throwable) {
                null
            }
        }
    }
}
