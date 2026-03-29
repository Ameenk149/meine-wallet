package org.multipaz.samples.wallet.cmp.logging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.multipaz.util.Logger

/**
 * Captures [Logger] output (multipaz’s shared [org.multipaz.util.Logger]) and keeps a ring buffer for the UI.
 * Installs [Logger.logPrinter] to forward to the platform logger (Logcat / Xcode console) and append formatted lines.
 */
object AppLogCollector {
    private const val MAX_LINES = 2_000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val logSequential = Dispatchers.Default.limitedParallelism(1)
    private val installMutex = Mutex()

    private val buffer = ArrayDeque<String>()
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private var installed = false

    suspend fun install() {
        installMutex.withLock {
            if (installed) {
                return
            }
            installed = true
            Logger.logPrinter = Logger.LogPrinter { level, tag, msg, throwable ->
                forwardPlatformLog(level, tag, msg, throwable)
                val line = formatAppLogLine(level, tag, msg, throwable)
                scope.launch(logSequential) {
                    buffer.addLast(line)
                    while (buffer.size > MAX_LINES) {
                        buffer.removeFirst()
                    }
                    _lines.value = buffer.toList()
                }
            }
        }
    }

    fun clear() {
        scope.launch(logSequential) {
            buffer.clear()
            _lines.value = emptyList()
        }
    }
}
