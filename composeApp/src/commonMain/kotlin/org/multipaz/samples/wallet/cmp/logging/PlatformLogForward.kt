package org.multipaz.samples.wallet.cmp.logging

import org.multipaz.util.Logger

internal expect fun forwardPlatformLog(
    level: Logger.LogPrinter.Level,
    tag: String,
    msg: String,
    throwable: Throwable?,
)
