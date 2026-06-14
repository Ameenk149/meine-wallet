package org.multipaz.samples.wallet.cmp.benchmark

import kotlin.system.getTimeNanos

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual object BenchmarkPlatform {
    actual fun elapsedRealtimeNanos(): Long = getTimeNanos()

    actual fun startResourceSampler(): ResourceSamplerHandle = ResourceSamplerHandle()
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual class ResourceSamplerHandle actual constructor() {
    actual fun stop() {
        // ZKP proof generation is not available on iOS in this prototype.
    }

    actual val peakMemoryKb: Int = 0

    actual val peakCpuPercent: Double = 0.0
}
