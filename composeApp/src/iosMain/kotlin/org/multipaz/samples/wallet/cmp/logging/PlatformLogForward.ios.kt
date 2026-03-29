package org.multipaz.samples.wallet.cmp.logging

import org.multipaz.util.Logger

internal actual fun forwardPlatformLog(
    level: Logger.LogPrinter.Level,
    tag: String,
    msg: String,
    throwable: Throwable?,
) {
    println(formatAppLogLine(level, tag, msg, throwable))
}
