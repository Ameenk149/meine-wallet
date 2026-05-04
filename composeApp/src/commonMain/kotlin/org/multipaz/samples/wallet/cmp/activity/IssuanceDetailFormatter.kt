package org.multipaz.samples.wallet.cmp.activity

fun IssuanceActivityRecord.listTitle(): String = "Issuance · OpenID4VCI"

fun IssuanceActivityRecord.listSubtitle(): String {
    val status = when (outcome) {
        IssuanceOutcome.IN_PROGRESS -> "In progress"
        IssuanceOutcome.SUCCESS -> "Success"
        IssuanceOutcome.FAILED -> "Failed"
        IssuanceOutcome.CANCELLED -> "Cancelled"
    }
    val doc = documentId?.let { " · doc $it" } ?: ""
    val calls = countHttpRequestsInTrace(httpTrace)
    val callsHint = if (calls > 0) " · $calls HTTP call(s)" else ""
    return "$status · OpenID4VCI$callsHint$doc"
}

/**
 * Ktor [io.ktor.client.plugins.logging.Logging] at [LogLevel.ALL] emits blocks containing
 * `REQUEST:` with the URL, then request body/headers, then `RESPONSE`/`STATUS` for the response.
 */
fun countHttpRequestsInTrace(trace: String): Int {
    if (trace.isBlank()) return 0
    val byColon = trace.split("REQUEST:", ignoreCase = true).size - 1
    if (byColon > 0) return byColon
    return trace.lineSequence().count { line ->
        val t = line.trimStart()
        t.startsWith("REQUEST ", ignoreCase = true) ||
            (t.contains("REQUEST", ignoreCase = true) && t.contains("http", ignoreCase = true))
    }
}

/** Summary block for the collapsible “Summary” section on the issuance detail screen. */
fun IssuanceActivityRecord.issuanceSummarySectionText(): String = buildString {
    val calls = countHttpRequestsInTrace(httpTrace)
    appendLine("=== Issuance (OpenID4VCI) ===")
    appendLine("Record ID: $id")
    appendLine("Started: ${startedAtEpochMs} (epoch ms)")
    appendLine("Ended: ${endedAtEpochMs ?: "—"}")
    appendLine("Outcome: $outcome")
    appendLine("HTTP calls (from Ktor log): ${if (calls > 0) calls else "—"}")
    offerUri?.let { appendLine("Offer URI: $it") }
    documentId?.let { appendLine("Document ID: $it") }
    errorMessage?.let { appendLine("Error: $it") }
    appendLine()
    appendLine("Open the HTTP sections below for request/response logs; JSON is pretty-printed there.")
}

/** State progression lines for the collapsible “Provisioning state” section. */
fun IssuanceActivityRecord.issuanceStateSectionText(): String = buildString {
    if (stateTrace.isEmpty()) {
        appendLine("(no transitions recorded)")
    } else {
        stateTrace.forEach { appendLine(it) }
    }
}

/** Body for a single collapsible when no HTTP trace was captured. */
fun IssuanceActivityRecord.issuanceHttpEmptySectionText(): String =
    "(no HTTP captured for this session — try a new issuance after updating the app)"

/** Full issuance detail for “copy entire activity” (matches on-screen sections). */
fun IssuanceActivityRecord.fullDetailCopyText(): String = buildString {
    appendLine(issuanceSummarySectionText().trimEnd())
    appendLine()
    appendLine(issuanceStateSectionText().trimEnd())
    appendLine()
    if (httpTrace.isBlank()) {
        appendLine(issuanceHttpEmptySectionText().trimEnd())
    } else {
        append(httpTrace.trimEnd())
    }
}
