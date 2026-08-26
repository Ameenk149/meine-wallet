package org.multipaz.samples.wallet.cmp.activity

import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DiagnosticOption
import org.multipaz.cbor.DataItem
import org.multipaz.eventlogger.Event
import org.multipaz.eventlogger.EventPresentment
import org.multipaz.eventlogger.EventPresentmentData
import org.multipaz.eventlogger.EventPresentmentDigitalCredentialsMdocApi
import org.multipaz.eventlogger.EventPresentmentDigitalCredentialsOpenID4VP
import org.multipaz.eventlogger.EventPresentmentIso18013AnnexA
import org.multipaz.eventlogger.EventPresentmentIso18013Proximity
import org.multipaz.eventlogger.EventPresentmentUriSchemeOpenID4VP
import org.multipaz.samples.wallet.cmp.benchmark.ZkpBenchmarkStore
import org.multipaz.samples.wallet.cmp.benchmark.formatZkpBenchmarkListHint
import org.multipaz.samples.wallet.cmp.benchmark.formatZkpBenchmarkSection
import org.multipaz.samples.wallet.cmp.benchmark.vpTokenPayloadBytesForEvent

/** Keeps each heavy section bounded so building the full detail string does not OOM the app. */
private const val MAX_DETAIL_SECTION_CHARS = 56_000

private fun String.elideDetailSection(): String {
    if (length <= MAX_DETAIL_SECTION_CHARS) return this
    val omitted = length - MAX_DETAIL_SECTION_CHARS
    return take(MAX_DETAIL_SECTION_CHARS) + "\n… ($omitted characters omitted) …\n"
}

private fun DataItem.toDiagnosticsString(): String = try {
    val encoded = Cbor.encode(this)
    Cbor.toDiagnostics(
        encoded,
        setOf(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR),
    ).elideDetailSection()
} catch (e: Throwable) {
    "(CBOR diagnostic failed: ${e.message ?: e::class.simpleName})"
}

private fun EventPresentmentData.tryFormatOverview(): String = try {
    formatOverview().elideDetailSection()
} catch (e: Throwable) {
    "(Request summary failed: ${e.message ?: e::class.simpleName})"
}

private fun EventPresentmentData.formatOverview(): String = buildString {
    appendLine("Requester name: ${requesterName ?: "—"}")
    appendLine("Trust display: ${trustMetadata?.displayName ?: "—"}")
    requesterCertChain?.let { chain ->
        appendLine("Requester certificate chain: present (${chain.certificates.size} cert(s))")
    }
    appendLine("Requested documents:")
    if (requestedDocuments.isEmpty()) {
        appendLine("  (none)")
    } else {
        requestedDocuments.forEach { doc ->
            val label = doc.documentName ?: doc.documentId
            appendLine("  • $label (${doc.documentId})")
            doc.claims.forEach { (requested, claim) ->
                appendLine("      - $requested → $claim")
            }
        }
    }
}

fun Event.summaryTitle(): String = when (this) {
    is EventPresentmentIso18013Proximity -> "Presentation · ISO 18013-5 (NFC)"
    is EventPresentmentIso18013AnnexA -> "Presentation · ISO 18013-7 Annex A"
    is EventPresentmentUriSchemeOpenID4VP -> "Presentation · OpenID4VP (URI)"
    is EventPresentmentDigitalCredentialsOpenID4VP -> "Presentation · OpenID4VP (Digital Credentials API)"
    is EventPresentmentDigitalCredentialsMdocApi -> "Presentation · mdoc (Digital Credentials API)"
    else -> this::class.simpleName ?: "Event"
}

fun Event.summarySubtitle(): String {
    val p = this as? EventPresentment ?: return timestamp.toString()
    val name = p.presentmentData.requesterName ?: "Unknown requester"
    val benchmarkHint = formatZkpBenchmarkListHint(
        generationSnapshots = ZkpBenchmarkStore.snapshotsForPresentation(this),
        vpTokenBytes = vpTokenPayloadBytesForEvent(this),
    )
    return if (benchmarkHint != null) {
        "$name · $benchmarkHint · $timestamp"
    } else {
        "$name · $timestamp"
    }
}

fun Event.formatDetailText(): String = when (this) {
    is EventPresentmentIso18013Proximity -> buildString {
        appendLine("=== Overview ===")
        appendLine("Type: ISO/IEC 18013-5 proximity (NFC)")
        appendLine("Time: $timestamp")
        appendLine()
        appendLine("=== Requester / request summary ===")
        appendLine(presentmentData.tryFormatOverview())
        appendLine()
        appendLine("=== Request (CBOR diagnostic) ===")
        appendLine(request.toDiagnosticsString())
        appendLine()
        appendLine("=== Response (CBOR diagnostic) ===")
        appendLine(response.toDiagnosticsString())
        appendLine()
        appendLine("=== Session transcript (CBOR diagnostic) ===")
        appendLine(sessionTranscript.toDiagnosticsString())
        appendZkpBenchmarkSection(this@formatDetailText)
    }
    is EventPresentmentIso18013AnnexA -> buildString {
        appendLine("=== Overview ===")
        appendLine("Type: ISO/IEC 18013-7 Annex A")
        appendLine("Time: $timestamp")
        appendLine("URI: ${uri.elideDetailSection()}")
        appendLine("App ID: ${appId ?: "—"}")
        appendLine("Origin: ${origin ?: "—"}")
        appendLine()
        appendLine("=== Requester / request summary ===")
        appendLine(presentmentData.tryFormatOverview())
        appendLine()
        appendLine("=== Request (CBOR diagnostic) ===")
        appendLine(request.toDiagnosticsString())
        appendLine()
        appendLine("=== Response (CBOR diagnostic) ===")
        appendLine(response.toDiagnosticsString())
        appendLine()
        appendLine("=== Session transcript (CBOR diagnostic) ===")
        appendLine(sessionTranscript.toDiagnosticsString())
        appendLine()
        appendLine("=== Reader engagement (CBOR diagnostic) ===")
        appendLine(readerEngagement.toDiagnosticsString())
        appendZkpBenchmarkSection(this@formatDetailText)
    }
    is EventPresentmentUriSchemeOpenID4VP -> buildString {
        appendLine("=== Overview ===")
        appendLine("Type: OpenID4VP via URI scheme")
        appendLine("Time: $timestamp")
        appendLine("URI: ${uri.elideDetailSection()}")
        appendLine("App ID: ${appId ?: "—"}")
        appendLine("Origin: ${origin ?: "—"}")
        appendLine("Redirect URI: ${redirectUri?.elideDetailSection() ?: "—"}")
        appendLine()
        appendLine("=== Requester / request summary ===")
        appendLine(presentmentData.tryFormatOverview())
        appendLine()
        appendLine("=== Authorization request JWT ===")
        appendLine(requestJwt.elideDetailSection())
        appendLine()
        appendLine("=== VP token ===")
        appendLine(vpToken.elideDetailSection())
        appendZkpBenchmarkSection(this@formatDetailText)
    }
    is EventPresentmentDigitalCredentialsOpenID4VP -> buildString {
        appendLine("=== Overview ===")
        appendLine("Type: OpenID4VP via W3C Digital Credentials API")
        appendLine("Time: $timestamp")
        appendLine("App ID: ${appId ?: "—"}")
        appendLine("Origin: ${origin.elideDetailSection()}")
        appendLine("Protocol: ${protocol.elideDetailSection()}")
        appendLine()
        appendLine("=== Requester / request summary ===")
        appendLine(presentmentData.tryFormatOverview())
        appendLine()
        appendLine("=== DC API request (JSON) ===")
        appendLine(requestJson.elideDetailSection())
        appendLine()
        appendLine("=== DC API response (JSON) ===")
        appendLine(responseJson.elideDetailSection())
        appendLine()
        appendLine("=== VP token ===")
        appendLine(vpToken.elideDetailSection())
        appendZkpBenchmarkSection(this@formatDetailText)
    }
    is EventPresentmentDigitalCredentialsMdocApi -> buildString {
        appendLine("=== Overview ===")
        appendLine("Type: ISO mdoc via Digital Credentials API")
        appendLine("Time: $timestamp")
        appendLine("App ID: ${appId ?: "—"}")
        appendLine("Origin: ${origin.elideDetailSection()}")
        appendLine("Protocol: ${protocol.elideDetailSection()}")
        appendLine()
        appendLine("=== Requester / request summary ===")
        appendLine(presentmentData.tryFormatOverview())
        appendLine()
        appendLine("=== DC API request (JSON) ===")
        appendLine(requestJson.elideDetailSection())
        appendLine()
        appendLine("=== DC API response (JSON) ===")
        appendLine(responseJson.elideDetailSection())
        appendLine()
        appendLine("=== Device response (CBOR diagnostic) ===")
        appendLine(deviceResponse.toDiagnosticsString())
        appendZkpBenchmarkSection(this@formatDetailText)
    }
    else -> buildString {
        appendLine("=== Overview ===")
        appendLine("Type: ${this@formatDetailText::class.simpleName}")
        appendLine("Time: $timestamp")
    }
}

private fun Appendable.appendZkpBenchmarkSection(event: Event) {
    val section = formatZkpBenchmarkSection(
        event = event,
        generationSnapshots = ZkpBenchmarkStore.snapshotsForPresentation(event),
        vpTokenBytes = vpTokenPayloadBytesForEvent(event),
    ) ?: return
    appendLine()
    appendLine(section)
}

/** Caps full detail string size before showing it in a single scrollable text block. */
private const val MAX_ACTIVITY_EVENT_DISPLAY_CHARS = 360_000

fun elideActivityEventBodyForDisplay(body: String): String {
    if (body.length <= MAX_ACTIVITY_EVENT_DISPLAY_CHARS) return body
    return body.take(MAX_ACTIVITY_EVENT_DISPLAY_CHARS) +
        "\n\n[… truncated for display: ${body.length} characters total …]"
}
