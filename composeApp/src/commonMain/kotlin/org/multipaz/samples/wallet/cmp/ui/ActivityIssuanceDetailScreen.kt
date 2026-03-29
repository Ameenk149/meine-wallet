package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import meinewallet.composeapp.generated.resources.Res
import meinewallet.composeapp.generated.resources.activity_detail_missing
import meinewallet.composeapp.generated.resources.back
import meinewallet.composeapp.generated.resources.issuance_detail_section_http
import meinewallet.composeapp.generated.resources.issuance_detail_section_state
import meinewallet.composeapp.generated.resources.issuance_detail_section_summary
import org.jetbrains.compose.resources.stringResource
import org.multipaz.samples.wallet.cmp.activity.IssuanceActivityRecord
import org.multipaz.samples.wallet.cmp.activity.IssuanceActivityStore
import org.multipaz.samples.wallet.cmp.activity.issuanceHttpEmptySectionText
import org.multipaz.samples.wallet.cmp.activity.issuanceStateSectionText
import org.multipaz.samples.wallet.cmp.activity.issuanceSummarySectionText
import org.multipaz.samples.wallet.cmp.activity.listTitle
import org.multipaz.samples.wallet.cmp.activity.splitHttpTraceIntoSections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityIssuanceDetailScreen(
    recordId: String,
    issuanceActivityStore: IssuanceActivityStore,
    onBack: () -> Unit,
) {
    var detail by remember { mutableStateOf<IssuanceActivityRecord?>(null) }

    LaunchedEffect(recordId, issuanceActivityStore) {
        suspend fun load() {
            detail = issuanceActivityStore.getRecord(recordId)
        }
        load()
        issuanceActivityStore.refreshFlow.collect {
            load()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.listTitle()
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
        when (val r = detail) {
            null -> Text(
                text = stringResource(Res.string.activity_detail_missing),
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            else -> {
                val httpSections = splitHttpTraceIntoSections(r.httpTrace)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .verticalScroll(scroll),
                ) {
                    CollapsibleDetailSection(
                        title = stringResource(Res.string.issuance_detail_section_summary),
                        body = r.issuanceSummarySectionText(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CollapsibleDetailSection(
                        title = stringResource(Res.string.issuance_detail_section_state),
                        body = r.issuanceStateSectionText(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (r.httpTrace.isBlank()) {
                        CollapsibleDetailSection(
                            title = stringResource(Res.string.issuance_detail_section_http),
                            body = r.issuanceHttpEmptySectionText(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else if (httpSections.isNotEmpty()) {
                        CollapsibleIssuanceHttpTrace(
                            sections = httpSections,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        CollapsibleDetailSection(
                            title = stringResource(Res.string.issuance_detail_section_http),
                            body = r.httpTrace,
                            modifier = Modifier.fillMaxWidth(),
                            prettifyJson = true,
                        )
                    }
                }
            }
        }
    }
}
