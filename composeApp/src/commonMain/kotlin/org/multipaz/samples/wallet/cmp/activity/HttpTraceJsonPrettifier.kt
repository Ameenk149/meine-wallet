package org.multipaz.samples.wallet.cmp.activity

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private val parseJson = Json { ignoreUnknownKeys = true }

private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
}

/**
 * Walks [raw] and replaces each substring that is a balanced JSON object or array (respecting
 * strings and escapes) with a pretty-printed form when it parses as JSON.
 */
fun prettifyJsonFragmentsInHttpTrace(raw: String): String = buildString {
    var i = 0
    while (i < raw.length) {
        val c = raw[i]
        if (c != '{' && c != '[') {
            append(c)
            i++
            continue
        }
        val end = findMatchingJsonEnd(raw, i)
        if (end == -1) {
            append(c)
            i++
            continue
        }
        val slice = raw.substring(i, end + 1)
        val formatted = try {
            val el = parseJson.parseToJsonElement(slice)
            prettyJson.encodeToString(JsonElement.serializer(), el)
        } catch (_: Exception) {
            slice
        }
        append(formatted)
        i = end + 1
    }
}

private fun findMatchingJsonEnd(s: String, start: Int): Int {
    if (start >= s.length) return -1
    when (val first = s[start]) {
        '{', '[' -> Unit
        else -> return -1
    }
    val stack = ArrayDeque<Char>()
    stack.addLast(when (s[start]) {
        '{' -> '}'
        else -> ']'
    })
    var i = start + 1
    var inString = false
    var escape = false
    while (i < s.length && stack.isNotEmpty()) {
        val c = s[i]
        if (escape) {
            escape = false
            i++
            continue
        }
        if (inString) {
            when (c) {
                '\\' -> escape = true
                '"' -> inString = false
            }
            i++
            continue
        }
        when (c) {
            '"' -> {
                inString = true
                i++
            }
            '{' -> {
                stack.addLast('}')
                i++
            }
            '[' -> {
                stack.addLast(']')
                i++
            }
            '}', ']' -> {
                if (stack.lastOrNull() == c) {
                    stack.removeLast()
                    if (stack.isEmpty()) return i
                }
                i++
            }
            else -> i++
        }
    }
    return -1
}
