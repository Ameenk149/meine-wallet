package org.multipaz.samples.wallet.cmp.benchmark

/**
 * Captured RQ1 wallet-side metrics for a single ZKP proof generation (Table 3.1).
 *
 * Proof verification time is measured on the verifier server and is intentionally omitted here.
 */
data class ZkpBenchmarkSnapshot(
    val operation: String,
    val zkSystemName: String,
    val zkSystemSpecId: String,
    /** Wall-clock proof generation duration (nanoseconds). */
    val proofGenerationNanos: Long,
    /** Peak PSS while generating the proof (kilobytes). */
    val peakMemoryKb: Int,
    /** Peak CPU utilisation while generating the proof (percentage). */
    val peakCpuPercent: Double,
    /** Epoch milliseconds when proof generation finished (for correlating with activity events). */
    val completedAtEpochMs: Long,
)
