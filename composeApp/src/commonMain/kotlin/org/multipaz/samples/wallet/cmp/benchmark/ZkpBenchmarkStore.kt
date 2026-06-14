package org.multipaz.samples.wallet.cmp.benchmark

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.multipaz.eventlogger.Event
import org.multipaz.eventlogger.EventPresentment
import org.multipaz.eventlogger.EventPresentmentDigitalCredentialsOpenID4VP
import org.multipaz.eventlogger.EventPresentmentUriSchemeOpenID4VP
import org.multipaz.util.Logger

/** Tag used for structured benchmark lines in Activity → View app logs. */
const val ZKP_BENCHMARK_LOG_TAG = "ZKP-Benchmark"

private const val MAX_SNAPSHOTS = 64
private const val CORRELATION_WINDOW_MS = 120_000L

object ZkpBenchmarkStore {
    private val lock = Any()
    private val snapshots = ArrayDeque<ZkpBenchmarkSnapshot>()
    private val _lastProofGeneration = MutableStateFlow<ZkpBenchmarkSnapshot?>(null)
    private val _lastVpTokenPayload = MutableStateFlow<VpTokenPayloadSnapshot?>(null)

    /** Most recently completed ZKP proof generation (updated after each presentation). */
    val lastProofGeneration: StateFlow<ZkpBenchmarkSnapshot?> = _lastProofGeneration.asStateFlow()

    /** Most recently measured VP token payload size (OpenID4VP presentations only). */
    val lastVpTokenPayload: StateFlow<VpTokenPayloadSnapshot?> = _lastVpTokenPayload.asStateFlow()

    fun record(snapshot: ZkpBenchmarkSnapshot) {
        synchronized(lock) {
            snapshots.addLast(snapshot)
            while (snapshots.size > MAX_SNAPSHOTS) {
                snapshots.removeFirst()
            }
        }
        _lastProofGeneration.value = snapshot
        Logger.i(ZKP_BENCHMARK_LOG_TAG, snapshot.toLogLine())
    }

    fun recordVpTokenPayload(payloadBytes: Long, completedAtEpochMs: Long) {
        val snapshot = VpTokenPayloadSnapshot(
            payloadBytes = payloadBytes,
            completedAtEpochMs = completedAtEpochMs,
        )
        _lastVpTokenPayload.value = snapshot
        Logger.i(
            ZKP_BENCHMARK_LOG_TAG,
            "operation=VP token payload (presentation) instrument=len(base64url.decode(vp_token)) " +
                "vp_token_payload_bytes=$payloadBytes (${formatVpTokenPayloadSize(payloadBytes)})",
        )
    }

    fun snapshotsForPresentation(event: Event): List<ZkpBenchmarkSnapshot> {
        if (event !is EventPresentment) {
            return emptyList()
        }
        val eventMs = event.timestamp.toEpochMilliseconds()
        return synchronized(lock) {
            snapshots.filter { snap ->
                kotlin.math.abs(snap.completedAtEpochMs - eventMs) <= CORRELATION_WINDOW_MS
            }
        }
    }
}

fun logVpTokenPayloadForEvent(event: Event) {
    val bytes = vpTokenPayloadBytesForEvent(event) ?: return
    val completedAtMs = if (event is EventPresentment) {
        event.timestamp.toEpochMilliseconds()
    } else {
        kotlin.time.Clock.System.now().toEpochMilliseconds()
    }
    ZkpBenchmarkStore.recordVpTokenPayload(bytes, completedAtMs)
}

fun vpTokenPayloadBytesForEvent(event: Event): Long? = when (event) {
    is EventPresentmentUriSchemeOpenID4VP -> measureVpTokenPayloadBytes(event.vpToken)
    is EventPresentmentDigitalCredentialsOpenID4VP -> measureVpTokenPayloadBytes(event.vpToken)
    else -> null
}
