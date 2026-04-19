package org.multipaz.samples.wallet.cmp.activity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import org.multipaz.document.DocumentAdded
import org.multipaz.document.DocumentEvent
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.storage.Storage
import org.multipaz.storage.StorageTableSpec
import org.multipaz.util.UUID
import kotlin.concurrent.Volatile

/**
 * Persists OpenID4VCI issuance / provisioning sessions for the Activity screen.
 * (Multipaz [org.multipaz.eventlogger.Event] does not yet include provisioning events.)
 */
class IssuanceActivityStore(
    private val storage: Storage,
) {
    private val httpLogScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val initMutex = Mutex()
    private val mutex = Mutex()

    @Volatile
    private var tableInitialized = false

    private val _refresh = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val refreshFlow: SharedFlow<Unit> = _refresh.asSharedFlow()

    @Volatile
    private var pendingOfferUri: String? = null

    @Volatile
    private var activeSessionId: String? = null

    /** True between [prepareOfferForIssuance] and first [startNewSessionLocked]; buffers early HTTP log lines. */
    @Volatile
    private var expectingIssuance: Boolean = false

    private val preSessionHttpBuffer = StringBuilder()

    /**
     * Call immediately before [org.multipaz.provisioning.ProvisioningModel.launchOpenID4VCIProvisioning]
     * (same thread is fine). Clears any buffered HTTP lines from a previous attempt.
     */
    suspend fun prepareOfferForIssuance(uri: String) {
        mutex.withLock {
            pendingOfferUri = uri
            expectingIssuance = true
            preSessionHttpBuffer.clear()
        }
    }

    /**
     * Appends one Ktor [Logging] message line/block for the active issuance session.
     * Called from the HTTP client thread; work is serialized on [mutex] with other store updates.
     */
    fun appendProvisioningHttpLog(message: String) {
        httpLogScope.launch {
            mutex.withLock {
                val sid = activeSessionId
                if (sid == null) {
                    if (expectingIssuance) {
                        if (preSessionHttpBuffer.isNotEmpty()) {
                            preSessionHttpBuffer.append('\n')
                        }
                        preSessionHttpBuffer.append(message)
                        if (preSessionHttpBuffer.length > MAX_HTTP_TRACE_CHARS) {
                            preSessionHttpBuffer.setLength(MAX_HTTP_TRACE_CHARS)
                            preSessionHttpBuffer.append("\n[… truncated …]")
                        }
                    }
                    return@withLock
                }
                val existing = loadRecordLocked(sid) ?: return@withLock
                val addition = if (existing.httpTrace.isEmpty()) {
                    message
                } else {
                    existing.httpTrace + "\n" + message
                }
                val capped = if (addition.length > MAX_HTTP_TRACE_CHARS) {
                    addition.take(MAX_HTTP_TRACE_CHARS) + "\n[… HTTP trace truncated …]"
                } else {
                    addition
                }
                saveRecordLocked(existing.copy(httpTrace = capped))
            }
        }
    }

    suspend fun onProvisioningState(
        previous: ProvisioningModel.State,
        current: ProvisioningModel.State,
    ) {
        mutex.withLock {
            when (current) {
                is ProvisioningModel.Initial -> {
                    activeSessionId?.let { sid ->
                        appendTraceLocked(sid, transitionLine(previous, current))
                    }
                    activeSessionId = null
                    startNewSessionLocked()
                }
                is ProvisioningModel.CredentialsIssued -> {
                    activeSessionId?.let { sid ->
                        appendTraceLocked(sid, transitionLine(previous, current))
                    }
                    completeSuccessLocked()
                }
                is ProvisioningModel.Error -> {
                    activeSessionId?.let { sid ->
                        appendTraceLocked(sid, transitionLine(previous, current))
                    }
                    completeFailedLocked(current.err)
                }
                is ProvisioningModel.Idle -> {
                    activeSessionId?.let { sid ->
                        appendTraceLocked(sid, transitionLine(previous, current))
                    }
                    cancelIfStillInProgressLocked()
                    activeSessionId = null
                }
                else -> {
                    activeSessionId?.let { sid ->
                        appendTraceLocked(sid, transitionLine(previous, current))
                    }
                }
            }
        }
    }

    suspend fun onDocumentEvent(event: DocumentEvent) {
        if (event !is DocumentAdded) return
        mutex.withLock {
            ensureTableLocked()
            val rows = storage.getTable(tableSpec).enumerateWithData(limit = 10_000)
            val records = rows.mapNotNull { (_, bytes) ->
                try {
                    json.decodeFromString<IssuanceActivityRecord>(bytes.toByteArray().decodeToString())
                } catch (_: Exception) {
                    null
                }
            }
            val target = records
                .filter {
                    it.documentId == null &&
                        (it.outcome == IssuanceOutcome.IN_PROGRESS || it.outcome == IssuanceOutcome.SUCCESS)
                }
                .maxByOrNull { it.startedAtEpochMs }
                ?: return@withLock
            saveRecordLocked(target.copy(documentId = event.documentId))
        }
    }

    private fun transitionLine(
        previous: ProvisioningModel.State,
        current: ProvisioningModel.State,
    ): String {
        val p = previous::class.simpleName ?: "?"
        val c = current::class.simpleName ?: "?"
        return "$p → $c"
    }

    private suspend fun startNewSessionLocked() {
        ensureTableLocked()
        val id = UUID.randomUUID().toString()
        val offer = pendingOfferUri.also { pendingOfferUri = null }
        val preHttp = preSessionHttpBuffer.toString().also { preSessionHttpBuffer.clear() }
        expectingIssuance = false
        activeSessionId = id
        val record = IssuanceActivityRecord(
            id = id,
            startedAtEpochMs = System.currentTimeMillis(),
            offerUri = offer,
            outcome = IssuanceOutcome.IN_PROGRESS,
            stateTrace = emptyList(),
            httpTrace = preHttp,
        )
        storage.getTable(tableSpec).insert(
            key = id,
            data = ByteString(json.encodeToString(record).encodeToByteArray()),
        )
        _refresh.tryEmit(Unit)
    }

    private suspend fun appendTraceLocked(sessionId: String, line: String) {
        val existing = loadRecordLocked(sessionId) ?: return
        saveRecordLocked(existing.copy(stateTrace = existing.stateTrace + line))
    }

    private suspend fun completeSuccessLocked() {
        val sid = activeSessionId ?: return
        val existing = loadRecordLocked(sid) ?: return
        saveRecordLocked(
            existing.copy(
                endedAtEpochMs = System.currentTimeMillis(),
                outcome = IssuanceOutcome.SUCCESS,
            ),
        )
    }

    private suspend fun completeFailedLocked(err: Throwable) {
        val sid = activeSessionId ?: return
        activeSessionId = null
        val existing = loadRecordLocked(sid) ?: return
        saveRecordLocked(
            existing.copy(
                endedAtEpochMs = System.currentTimeMillis(),
                outcome = IssuanceOutcome.FAILED,
                errorMessage = err.message ?: err.toString(),
            ),
        )
    }

    private suspend fun cancelIfStillInProgressLocked() {
        val sid = activeSessionId ?: return
        val existing = loadRecordLocked(sid) ?: return
        if (existing.outcome != IssuanceOutcome.IN_PROGRESS) {
            return
        }
        activeSessionId = null
        saveRecordLocked(
            existing.copy(
                endedAtEpochMs = System.currentTimeMillis(),
                outcome = IssuanceOutcome.CANCELLED,
            ),
        )
    }

    private suspend fun loadRecordLocked(sessionId: String): IssuanceActivityRecord? {
        ensureTableLocked()
        val data = storage.getTable(tableSpec).get(sessionId) ?: return null
        return try {
            json.decodeFromString(data.toByteArray().decodeToString())
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun saveRecordLocked(record: IssuanceActivityRecord) {
        ensureTableLocked()
        storage.getTable(tableSpec).update(
            key = record.id,
            data = ByteString(json.encodeToString(record).encodeToByteArray()),
        )
        _refresh.tryEmit(Unit)
    }

    suspend fun getRecord(id: String): IssuanceActivityRecord? {
        mutex.withLock { return loadRecordLocked(id) }
    }

    suspend fun getAllRecords(): List<IssuanceActivityRecord> {
        mutex.withLock {
            ensureTableLocked()
            val rows = storage.getTable(tableSpec).enumerateWithData(limit = 10_000)
            return rows.mapNotNull { (_, bytes) ->
                try {
                    json.decodeFromString<IssuanceActivityRecord>(bytes.toByteArray().decodeToString())
                } catch (_: Exception) {
                    null
                }
            }.sortedByDescending { it.startedAtEpochMs }
        }
    }

    private suspend fun ensureTableLocked() {
        if (tableInitialized) return
        initMutex.withLock {
            if (tableInitialized) return
            storage.getTable(tableSpec)
            tableInitialized = true
        }
    }

    companion object {
        private const val MAX_HTTP_TRACE_CHARS = 400_000

        private val tableSpec = StorageTableSpec(
            name = "IssuanceActivityHistory",
            supportPartitions = false,
            supportExpiration = false,
            schemaVersion = 0L,
        )
    }
}
