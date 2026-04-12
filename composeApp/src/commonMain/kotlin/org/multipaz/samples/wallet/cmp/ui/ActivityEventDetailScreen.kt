package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import meinewallet.composeapp.generated.resources.Res
import meinewallet.composeapp.generated.resources.activity_detail_missing
import meinewallet.composeapp.generated.resources.back
import org.jetbrains.compose.resources.stringResource
import org.multipaz.eventlogger.Event
import org.multipaz.eventlogger.SimpleEventLogger
import org.multipaz.samples.wallet.cmp.activity.elideActivityEventBodyForDisplay
import org.multipaz.samples.wallet.cmp.activity.formatDetailText
import org.multipaz.samples.wallet.cmp.activity.summaryTitle
import org.multipaz.util.Logger

private const val TAG = "ActivityEventDetail"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityEventDetailScreen(
    eventId: String,
    eventLogger: SimpleEventLogger,
    onBack: () -> Unit,
) {
    var event by remember { mutableStateOf<Event?>(null) }

    LaunchedEffect(eventId, eventLogger) {
        suspend fun load() {
            try {
                val events = eventLogger.getEvents()
                event = events.find { it.identifier == eventId }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load activity events", e)
                event = null
            }
        }
        load()
        eventLogger.eventFlow.collect {
            load()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (val ev = event) {
                        null -> stringResource(Res.string.activity_detail_missing)
                        else -> runCatching { ev.summaryTitle() }.getOrElse { err ->
                            Logger.w(TAG, "summaryTitle failed for ${ev::class.simpleName}: ${err.message}")
                            stringResource(Res.string.activity_detail_missing)
                        }
                    }
                    Text(
                        text = titleText,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val e = event) {
            null -> Text(
                text = stringResource(Res.string.activity_detail_missing),
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            else -> key(e.identifier) {
                val scroll = rememberScrollState()
                val body = runCatching { e.formatDetailText() }.getOrElse { err ->
                    Logger.w(TAG, "formatDetailText failed: ${err.message}")
                    buildString {
                        appendLine("Could not render this activity event.")
                        appendLine()
                        appendLine(err.message ?: err.toString())
                    }
                }
                Text(
                    text = elideActivityEventBodyForDisplay(body),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .verticalScroll(scroll),
                )
            }
        }
    }
}
