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

private fun DataItem.toDiagnosticsString(): String {
    val encoded = Cbor.encode(this)
    return Cbor.toDiagnostics(
        encoded,
        setOf(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR),
    )
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
}

fun Event.summarySubtitle(): String {
    val p = this as? EventPresentment ?: return timestamp.toString()
    val name = p.presentmentData.requesterName ?: "Unknown requester"
    return "$name · $timestamp"
}

fun Event.formatDetailText(): String = when (this) {
    is EventPresentmentIso18013Proximity -> buildString {
        appendLine("=== Overview ===")
        appendLine("Type: ISO/IEC 18013-5 proximity (NFC)")
        appendLine("Time: $timestamp")
        appendLine()
        appendLine("=== Requester / request summary ===")
        appendLine(presentmentData.formatOverview())
        appendLine()
        appendLine("=== Request (CBOR diagnostic) ===")
        appendLine(request.toDiagnosticsString())
        appendLine()
        appendLine("=== Response (CBOR diagnostic) ===")
        appendLine(response.toDiagnosticsString())
        appendLine()
        appendLine("=== Session transcript (CBOR diagnostic) ===")
        appendLine(sessionTranscript.toDiagnosticsString())
    }
    is EventPresentmentIso18013AnnexA -> buildString {
        appendLine("=== Overview ===")
        appendLine("Type: ISO/IEC 18013-7 Annex A")
        appendLine("Time: $timestamp")
        appendLine("URI: $uri")
        appendLine("App ID: ${appId ?: "—"}")
        appendLine("Origin: ${origin ?: "—"}")
        appendLine()
        appendLine("=== Requester / request summary ===")
        appendLine(presentmentData.formatOverview())
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
    }
    is EventPresentmentUriSchemeOpenID4VP -> buildString {
        appendLine("=== Overview ===")
        appendLine("Type: OpenID4VP via URI scheme")
        appendLine("Time: $timestamp")
        appendLine("URI: $uri")
        appendLine("App ID: ${appId ?: "—"}")
        appendLine("Origin: ${origin ?: "—"}")
        appendLine("Redirect URI: $redirectUri")
        appendLine()
        appendLine("=== Requester / request summary ===")
        appendLine(presentmentData.formatOverview())
        appendLine()
        appendLine("=== Authorization request JWT ===")
        appendLine(requestJwt)
        appendLine()
        appendLine("=== VP token ===")
        appendLine(vpToken)
    }
    is EventPresentmentDigitalCredentialsOpenID4VP -> buildString {
        appendLine("=== Overview ===")
        appendLine("Type: OpenID4VP via W3C Digital Credentials API")
        appendLine("Time: $timestamp")
        appendLine("App ID: ${appId ?: "—"}")
        appendLine("Origin: $origin")
        appendLine("Protocol: $protocol")
        appendLine()
        appendLine("=== Requester / request summary ===")
        appendLine(presentmentData.formatOverview())
        appendLine()
        appendLine("=== DC API request (JSON) ===")
        appendLine(requestJson)
        appendLine()
        appendLine("=== DC API response (JSON) ===")
        appendLine(responseJson)
        appendLine()
        appendLine("=== VP token ===")
        appendLine(vpToken)
    }
    is EventPresentmentDigitalCredentialsMdocApi -> buildString {
        appendLine("=== Overview ===")
        appendLine("Type: ISO mdoc via Digital Credentials API")
        appendLine("Time: $timestamp")
        appendLine("App ID: ${appId ?: "—"}")
        appendLine("Origin: $origin")
        appendLine("Protocol: $protocol")
        appendLine()
        appendLine("=== Requester / request summary ===")
        appendLine(presentmentData.formatOverview())
        appendLine()
        appendLine("=== DC API request (JSON) ===")
        appendLine(requestJson)
        appendLine()
        appendLine("=== DC API response (JSON) ===")
        appendLine(responseJson)
        appendLine()
        appendLine("=== Device response (CBOR diagnostic) ===")
        appendLine(deviceResponse.toDiagnosticsString())
    }
}
