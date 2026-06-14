package org.multipaz.samples.wallet.cmp.benchmark

/** Last measured VP token payload size (Table 3.1: len(base64url.decode(vp_token))). */
data class VpTokenPayloadSnapshot(
    val payloadBytes: Long,
    val completedAtEpochMs: Long,
)
