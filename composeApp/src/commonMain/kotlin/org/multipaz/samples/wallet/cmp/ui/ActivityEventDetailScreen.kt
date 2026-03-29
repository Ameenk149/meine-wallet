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
import org.multipaz.samples.wallet.cmp.activity.formatDetailText
import org.multipaz.samples.wallet.cmp.activity.summaryTitle

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
            event = eventLogger.getEvents().find { it.identifier == eventId }
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
                    Text(
                        text = event?.summaryTitle()
                            ?: stringResource(Res.string.activity_detail_missing),
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
        val scroll = rememberScrollState()
        when (val e = event) {
            null -> Text(
                text = stringResource(Res.string.activity_detail_missing),
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            else -> Text(
                text = e.formatDetailText(),
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
