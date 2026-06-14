package org.multipaz.samples.wallet.cmp.benchmark

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.util.fromBase64Url

/**
 * Total decoded byte length of CBOR payloads embedded in an OpenID4VP [vp_token] JSON object
 * (Table 3.1: len(base64url.decode(vp_token))).
 */
fun measureVpTokenPayloadBytes(vpTokenJson: String): Long? {
    return runCatching {
        val root = Json.parseToJsonElement(vpTokenJson)
        sumDecodedBase64UrlBytes(root)
    }.getOrNull()
}

private fun sumDecodedBase64UrlBytes(element: JsonElement): Long = when (element) {
    is JsonObject -> element.values.sumOf { sumDecodedBase64UrlBytes(it) }
    is JsonArray -> element.sumOf { sumDecodedBase64UrlBytes(it) }
    is JsonPrimitive -> {
        if (!element.isString) {
            0L
        } else {
            val text = element.content
            if (looksLikeBase64UrlPayload(text)) {
                text.fromBase64Url().size.toLong()
            } else {
                0L
            }
        }
    }
}

private fun looksLikeBase64UrlPayload(value: String): Boolean {
    if (value.length < 16) {
        return false
    }
    return value.all { ch ->
        ch.isLetterOrDigit() || ch == '-' || ch == '_'
    }
}
