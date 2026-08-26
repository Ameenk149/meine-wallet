package org.multipaz.samples.wallet.cmp.benchmark

import kotlin.time.Clock
import kotlin.time.Instant
import org.multipaz.cbor.DataItem
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.zkp.ZkDocument
import org.multipaz.mdoc.zkp.ZkSystem
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.request.RequestedClaim
import org.multipaz.util.Logger

/**
 * Wraps a [ZkSystem] to capture RQ1 wallet-side metrics around [generateProof].
 */
class InstrumentedZkSystem(
    private val delegate: ZkSystem,
) : ZkSystem {
    override val name: String get() = delegate.name

    override val systemSpecs: List<ZkSystemSpec> get() = delegate.systemSpecs

    override fun generateProof(
        zkSystemSpec: ZkSystemSpec,
        document: MdocDocument,
        sessionTranscript: DataItem,
        timestamp: Instant,
    ): ZkDocument {
        val sampler = BenchmarkPlatform.startResourceSampler()
        val startNanos = BenchmarkPlatform.elapsedRealtimeNanos()
        try {
            return delegate.generateProof(
                zkSystemSpec = zkSystemSpec,
                document = document,
                sessionTranscript = sessionTranscript,
                timestamp = timestamp,
            )
        } finally {
            val endNanos = BenchmarkPlatform.elapsedRealtimeNanos()
            sampler.stop()
            val completedAtMs = Clock.System.now().toEpochMilliseconds()
            val snapshot = ZkpBenchmarkSnapshot(
                operation = "ZKP proof generation (presentation)",
                zkSystemName = name,
                zkSystemSpecId = zkSystemSpec.id,
                proofGenerationNanos = (endNanos - startNanos).coerceAtLeast(0L),
                peakMemoryKb = sampler.peakMemoryKb,
                peakCpuPercent = sampler.peakCpuPercent,
                completedAtEpochMs = completedAtMs,
            )
            ZkpBenchmarkStore.record(snapshot)
        }
    }

    override fun verifyProof(
        zkDocument: ZkDocument,
        zkSystemSpec: ZkSystemSpec,
        sessionTranscript: DataItem,
    ) {
        delegate.verifyProof(zkDocument, zkSystemSpec, sessionTranscript)
    }

    override fun getMatchingSystemSpec(
        zkSystemSpecs: List<ZkSystemSpec>,
        requestedClaims: List<RequestedClaim>,
    ): ZkSystemSpec? {
        // Multipaz 0.99.0+ already picks the highest mutually supported circuit
        // version; just log which spec was chosen for the benchmark records.
        val match = delegate.getMatchingSystemSpec(zkSystemSpecs, requestedClaims)
        if (match != null) {
            Logger.i(
                "InstrumentedZkSystem",
                "Matched ZK spec ${match.id} (version=${match.getParam<Long>("version")}, " +
                    "num_attributes=${requestedClaims.size})"
            )
        }
        return match
    }
}
