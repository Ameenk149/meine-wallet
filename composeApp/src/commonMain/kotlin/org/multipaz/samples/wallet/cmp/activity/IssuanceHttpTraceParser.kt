package org.multipaz.samples.wallet.cmp.activity

data class IssuanceApiSection(
    /** Full endpoint URL from the Ktor `REQUEST:` line, or a placeholder for pre-request log text. */
    val header: String,
    val content: String,
)

private val urlInRequestLine = Regex("https?://\\S+")

/**
 * Split Ktor [io.ktor.client.plugins.logging.Logging] output into one block per `REQUEST:` so each
 * can be shown in a collapsible row in the UI.
 */
fun splitHttpTraceIntoSections(trace: String): List<IssuanceApiSection> {
    if (trace.isBlank()) return emptyList()
    val trimmed = trace.trim()
    val rawParts = trimmed.split(Regex("\\r?\\n(?=REQUEST:)", RegexOption.IGNORE_CASE))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (rawParts.isEmpty()) return emptyList()

    return rawParts.map { part ->
        val header = extractRequestUrlFromChunk(part)
            ?: if (part.contains("REQUEST:", ignoreCase = true)) {
                "Request (URL not parsed)"
            } else {
                "Pre-session"
            }
        IssuanceApiSection(
            header = header,
            content = part,
        )
    }
}

private fun extractRequestUrlFromChunk(chunk: String): String? {
    for (line in chunk.lineSequence().map { it.trim() }) {
        if (!line.startsWith("REQUEST", ignoreCase = true)) continue
        val afterRequest = line.substringAfter("REQUEST", "").trimStart().removePrefix(":").trim()
        if (afterRequest.isEmpty()) continue
        urlInRequestLine.find(afterRequest)?.value?.let { return it }
    }
    return null
}
