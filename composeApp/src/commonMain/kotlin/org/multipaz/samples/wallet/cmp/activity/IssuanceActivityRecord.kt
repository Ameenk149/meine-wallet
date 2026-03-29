package org.multipaz.samples.wallet.cmp.activity

import kotlinx.serialization.Serializable

@Serializable
data class IssuanceActivityRecord(
    val id: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val offerUri: String? = null,
    val outcome: IssuanceOutcome = IssuanceOutcome.IN_PROGRESS,
    val documentId: String? = null,
    val errorMessage: String? = null,
    val stateTrace: List<String> = emptyList(),
    /** Full Ktor client log (requests, headers, bodies, responses) for this issuance session. */
    val httpTrace: String = "",
)

@Serializable
enum class IssuanceOutcome {
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    CANCELLED,
}
