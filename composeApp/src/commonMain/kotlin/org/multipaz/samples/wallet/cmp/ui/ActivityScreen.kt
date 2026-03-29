package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import meinewallet.composeapp.generated.resources.Res
import meinewallet.composeapp.generated.resources.activity
import meinewallet.composeapp.generated.resources.activity_empty_hint
import meinewallet.composeapp.generated.resources.activity_open_logs_button
import meinewallet.composeapp.generated.resources.activity_open_logs_hint
import meinewallet.composeapp.generated.resources.back
import org.jetbrains.compose.resources.stringResource
import org.multipaz.eventlogger.Event
import org.multipaz.eventlogger.SimpleEventLogger
import org.multipaz.samples.wallet.cmp.activity.ActivityListItem
import org.multipaz.samples.wallet.cmp.activity.IssuanceActivityRecord
import org.multipaz.samples.wallet.cmp.activity.IssuanceActivityStore
import org.multipaz.samples.wallet.cmp.activity.listSubtitle
import org.multipaz.samples.wallet.cmp.activity.listTitle
import org.multipaz.samples.wallet.cmp.activity.summarySubtitle
import org.multipaz.samples.wallet.cmp.activity.summaryTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    eventLogger: SimpleEventLogger,
    issuanceActivityStore: IssuanceActivityStore,
    onBack: () -> Unit,
    onOpenLogs: () -> Unit,
    onPresentationSelected: (String) -> Unit,
    onIssuanceSelected: (String) -> Unit,
) {
    var presentationEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var issuanceRecords by remember { mutableStateOf<List<IssuanceActivityRecord>>(emptyList()) }

    LaunchedEffect(eventLogger) {
        presentationEvents = eventLogger.getEvents()
        eventLogger.eventFlow.collect {
            presentationEvents = eventLogger.getEvents()
        }
    }

    LaunchedEffect(issuanceActivityStore) {
        issuanceRecords = issuanceActivityStore.getAllRecords()
        issuanceActivityStore.refreshFlow.collect {
            issuanceRecords = issuanceActivityStore.getAllRecords()
        }
    }

    val items: List<ActivityListItem> = remember(presentationEvents, issuanceRecords) {
        val p = presentationEvents.map { ActivityListItem.PresentationItem(it) }
        val i = issuanceRecords.map { ActivityListItem.IssuanceItem(it) }
        (p + i).sortedByDescending { it.sortKey }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.activity)) },
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
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ActivityOpenLogsCard(onOpenLogs = onOpenLogs)
                Text(
                    text = stringResource(Res.string.activity_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "open_logs") {
                    ActivityOpenLogsCard(onOpenLogs = onOpenLogs)
                }
                items(items, key = {
                    when (it) {
                        is ActivityListItem.PresentationItem -> "p:${it.event.identifier}"
                        is ActivityListItem.IssuanceItem -> "i:${it.record.id}"
                    }
                }) { item ->
                    when (item) {
                        is ActivityListItem.PresentationItem -> {
                            val event = item.event
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = event.summaryTitle(),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = event.summarySubtitle(),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onPresentationSelected(event.identifier)
                                },
                            )
                        }
                        is ActivityListItem.IssuanceItem -> {
                            val record = item.record
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = record.listTitle(),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = record.listSubtitle(),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onIssuanceSelected(record.id)
                                },
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ActivityOpenLogsCard(onOpenLogs: () -> Unit) {
    FilledTonalButton(
        onClick = onOpenLogs,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.activity_open_logs_button),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(Res.string.activity_open_logs_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.92f),
                )
            }
        }
    }
}
