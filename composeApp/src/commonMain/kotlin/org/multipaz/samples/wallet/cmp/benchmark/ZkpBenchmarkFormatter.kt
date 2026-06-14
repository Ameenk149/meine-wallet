package org.multipaz.samples.wallet.cmp.benchmark

import org.multipaz.eventlogger.Event
import org.multipaz.eventlogger.EventPresentment

fun formatProofGenerationDuration(nanos: Long): String {
    if (nanos < 1_000) {
        return "$nanos ns"
    }
    val micros = nanos / 1_000.0
    if (micros < 1_000) {
        return String.format("%.2f µs", micros)
    }
    val millis = micros / 1_000.0
    if (millis < 1_000) {
        return String.format("%.2f ms", millis)
    }
    return String.format("%.2f s", millis / 1_000.0)
}

fun formatVpTokenPayloadSize(bytes: Long): String {
    if (bytes < 1024) {
        return "$bytes B"
    }
    val kb = bytes / 1024.0
    if (kb < 1024) {
        return String.format("%.1f KB", kb)
    }
    return String.format("%.2f MB", kb / 1024.0)
}

fun formatPeakMemoryKb(kilobytes: Int): String = "$kilobytes KB"

fun formatPeakCpuPercent(percent: Double): String = String.format("%.1f%%", percent)

fun ZkpBenchmarkSnapshot.toLogLine(): String = buildString {
    append("operation=$operation")
    append(" zk_system=$zkSystemName")
    append(" spec=$zkSystemSpecId")
    append(" proof_generation=").append(proofGenerationNanos).append(" ns")
    append(" (").append(formatProofGenerationDuration(proofGenerationNanos)).append(')')
    append(" peak_memory_kb=$peakMemoryKb")
    append(" peak_cpu_percent=").append(String.format("%.1f", peakCpuPercent))
}

fun formatZkpBenchmarkSection(
    event: Event,
    generationSnapshots: List<ZkpBenchmarkSnapshot>,
    vpTokenBytes: Long?,
): String? {
    if (event !is EventPresentment) {
        return null
    }
    if (generationSnapshots.isEmpty() && vpTokenBytes == null) {
        return null
    }
    return buildString {
        appendLine("=== RQ1 ZKP benchmarking (Table 3.1, wallet-side) ===")
        appendLine("Note: Proof verification time is measured on the verifier server, not in the wallet.")
        appendLine()
        if (generationSnapshots.isEmpty()) {
            appendLine("Proof generation: (no ZKP proof was generated for this presentation)")
        } else {
            generationSnapshots.forEachIndexed { index, snap ->
                if (generationSnapshots.size > 1) {
                    appendLine("--- Proof generation ${index + 1} of ${generationSnapshots.size} ---")
                } else {
                    appendLine("--- Proof generation ---")
                }
                appendLine("Operation: ${snap.operation}")
                appendLine("ZK system: ${snap.zkSystemName}")
                appendLine("ZK system spec: ${snap.zkSystemSpecId}")
                appendLine("Instrument: SystemClock.elapsedRealtimeNanos() (Android)")
                appendLine(
                    "Proof generation time: ${snap.proofGenerationNanos} ns " +
                        "(${formatProofGenerationDuration(snap.proofGenerationNanos)})",
                )
                appendLine("Peak memory usage: ${snap.peakMemoryKb} KB (Debug.MemoryInfo, sampled every 50 ms)")
                appendLine(
                    "Peak CPU utilisation: ${
                        String.format(
                            "%.1f",
                            snap.peakCpuPercent,
                        )
                    } % (/proc/self/stat, sampled every 50 ms)",
                )
                appendLine()
            }
        }
        if (vpTokenBytes != null) {
            appendLine("--- VP token payload ---")
            appendLine("Instrument: len(base64url.decode(vp_token))")
            appendLine("VP token payload size: $vpTokenBytes bytes (${formatVpTokenPayloadSize(vpTokenBytes)})")
        } else {
            appendLine("--- VP token payload ---")
            appendLine("VP token payload size: (not applicable — no vp_token on this presentation type)")
        }
    }.trimEnd()
}

fun formatZkpBenchmarkListHint(
    generationSnapshots: List<ZkpBenchmarkSnapshot>,
    vpTokenBytes: Long?,
): String? {
    if (generationSnapshots.isEmpty() && vpTokenBytes == null) {
        return null
    }
    return buildString {
        append("RQ1 ZKP · ")
        if (generationSnapshots.isNotEmpty()) {
            val totalNanos = generationSnapshots.sumOf { it.proofGenerationNanos }
            val latest = generationSnapshots.last()
            append("proof gen ${formatProofGenerationDuration(totalNanos)}")
            append(" · mem ${formatPeakMemoryKb(latest.peakMemoryKb)}")
            append(" · CPU ${formatPeakCpuPercent(latest.peakCpuPercent)}")
        }
        if (vpTokenBytes != null) {
            if (generationSnapshots.isNotEmpty()) {
                append(" · ")
            }
            append("VP ${formatVpTokenPayloadSize(vpTokenBytes)}")
        }
    }
}
