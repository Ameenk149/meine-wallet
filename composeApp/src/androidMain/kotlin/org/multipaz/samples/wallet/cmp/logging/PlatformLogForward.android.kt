package org.multipaz.samples.wallet.cmp.logging

import android.util.Log
import org.multipaz.util.Logger

internal actual fun forwardPlatformLog(
    level: Logger.LogPrinter.Level,
    tag: String,
    msg: String,
    throwable: Throwable?,
) {
    when (level) {
        Logger.LogPrinter.Level.DEBUG ->
            if (throwable != null) Log.d(tag, msg, throwable) else Log.d(tag, msg)
        Logger.LogPrinter.Level.INFO ->
            if (throwable != null) Log.i(tag, msg, throwable) else Log.i(tag, msg)
        Logger.LogPrinter.Level.WARNING ->
            if (throwable != null) Log.w(tag, msg, throwable) else Log.w(tag, msg)
        Logger.LogPrinter.Level.ERROR ->
            if (throwable != null) Log.e(tag, msg, throwable) else Log.e(tag, msg)
    }
}
