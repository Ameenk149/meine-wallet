package org.multipaz.samples.wallet.cmp.logging

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.multipaz.util.Logger
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun formatAppLogLine(
    level: Logger.LogPrinter.Level,
    tag: String,
    msg: String,
    throwable: Throwable?,
): String {
    val sb = StringBuilder()
    val now = Clock.System.now()
    val dt = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val timeStamp = dt.format(LocalDateTime.Formats.ISO)
    sb.append(timeStamp)
    sb.append(": ")
    when (level) {
        Logger.LogPrinter.Level.DEBUG -> sb.append("DEBUG")
        Logger.LogPrinter.Level.INFO -> sb.append("INFO")
        Logger.LogPrinter.Level.WARNING -> sb.append("WARNING")
        Logger.LogPrinter.Level.ERROR -> sb.append("ERROR")
    }
    sb.append(": ")
    sb.append(tag)
    sb.append(": ")
    sb.append(msg)
    if (throwable != null) {
        sb.append("\nEXCEPTION: ")
        sb.append(throwable)
    }
    return sb.toString()
}
