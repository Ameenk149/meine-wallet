package org.multipaz.samples.wallet.cmp.benchmark

import android.os.Debug
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

private const val SAMPLE_INTERVAL_MS = 50L
private const val USER_HZ = 100L

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual object BenchmarkPlatform {
    actual fun elapsedRealtimeNanos(): Long = SystemClock.elapsedRealtimeNanos()

    actual fun startResourceSampler(): ResourceSamplerHandle = ResourceSamplerHandle()
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual class ResourceSamplerHandle actual constructor() {
    private val memoryInfo = Debug.MemoryInfo()
    private var peakMemoryKbInternal = 0
    private var peakCpuPercentInternal = 0.0

    private var lastCpuJiffies: Long? = null
    private var lastSampleRealtimeMs: Long? = null

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = scope.launch {
        while (isActive) {
            sample()
            delay(SAMPLE_INTERVAL_MS)
        }
    }

    actual val peakMemoryKb: Int get() = peakMemoryKbInternal

    actual val peakCpuPercent: Double get() = peakCpuPercentInternal

    actual fun stop() {
        sample()
        job?.cancel()
        scope.cancel()
        job = null
    }

    private fun sample() {
        Debug.getMemoryInfo(memoryInfo)
        peakMemoryKbInternal = max(peakMemoryKbInternal, memoryInfo.totalPss)

        val cpuTimes = readProcSelfCpuJiffies() ?: return
        val nowMs = SystemClock.elapsedRealtime()
        val previousJiffies = lastCpuJiffies
        val previousMs = lastSampleRealtimeMs
        lastCpuJiffies = cpuTimes
        lastSampleRealtimeMs = nowMs
        if (previousJiffies != null && previousMs != null) {
            val deltaJiffies = cpuTimes - previousJiffies
            val deltaMs = (nowMs - previousMs).coerceAtLeast(1L)
            val cpuPercent =
                (deltaJiffies.toDouble() * 1_000.0 / USER_HZ) / deltaMs * 100.0 /
                    Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            peakCpuPercentInternal = max(peakCpuPercentInternal, cpuPercent)
        }
    }
}

private fun readProcSelfCpuJiffies(): Long? {
    return runCatching {
        val stat = File("/proc/self/stat").readText()
        val afterComm = stat.substringAfter(") ")
        val fields = afterComm.trim().split(Regex("\\s+"))
        val utime = fields[11].toLong()
        val stime = fields[12].toLong()
        utime + stime
    }.getOrNull()
}
