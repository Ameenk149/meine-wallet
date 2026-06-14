package org.multipaz.samples.wallet.cmp.benchmark

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object BenchmarkPlatform {
    fun elapsedRealtimeNanos(): Long

    fun startResourceSampler(): ResourceSamplerHandle
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect class ResourceSamplerHandle() {
    fun stop()
    val peakMemoryKb: Int
    val peakCpuPercent: Double
}
