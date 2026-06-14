package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import meinewallet.composeapp.generated.resources.Res
import meinewallet.composeapp.generated.resources.activity_zkp_benchmark_title
import meinewallet.composeapp.generated.resources.activity_zkp_last_peak_cpu_title
import meinewallet.composeapp.generated.resources.activity_zkp_last_peak_memory_title
import meinewallet.composeapp.generated.resources.activity_zkp_last_proof_none
import meinewallet.composeapp.generated.resources.activity_zkp_last_proof_title
import meinewallet.composeapp.generated.resources.activity_zkp_last_vp_token_none
import meinewallet.composeapp.generated.resources.activity_zkp_last_vp_token_title
import meinewallet.composeapp.generated.resources.activity_zkp_peak_resource_none
import org.jetbrains.compose.resources.stringResource
import org.multipaz.samples.wallet.cmp.benchmark.ZkpBenchmarkStore
import org.multipaz.samples.wallet.cmp.benchmark.formatPeakCpuPercent
import org.multipaz.samples.wallet.cmp.benchmark.formatPeakMemoryKb
import org.multipaz.samples.wallet.cmp.benchmark.formatProofGenerationDuration
import org.multipaz.samples.wallet.cmp.benchmark.formatVpTokenPayloadSize

@Composable
fun ZkpLastProofGenerationCard(
    modifier: Modifier = Modifier,
) {
    val lastProof by ZkpBenchmarkStore.lastProofGeneration.collectAsState()
    val lastVpToken by ZkpBenchmarkStore.lastVpTokenPayload.collectAsState()
    val peakResourceNone = stringResource(Res.string.activity_zkp_peak_resource_none)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.activity_zkp_benchmark_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
            )

            BenchmarkMetricSection(
                title = stringResource(Res.string.activity_zkp_last_proof_title),
                emptyText = stringResource(Res.string.activity_zkp_last_proof_none),
                hasValue = lastProof != null,
            ) {
                val snapshot = lastProof!!
                Text(
                    text = formatProofGenerationDuration(snapshot.proofGenerationNanos),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "${snapshot.proofGenerationNanos} ns · SystemClock.elapsedRealtimeNanos()",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                )
                Text(
                    text = "${snapshot.zkSystemName} · ${snapshot.zkSystemSpecId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                )
            }

            BenchmarkDivider()

            BenchmarkMetricSection(
                title = stringResource(Res.string.activity_zkp_last_peak_memory_title),
                emptyText = peakResourceNone,
                hasValue = lastProof != null,
            ) {
                val snapshot = lastProof!!
                Text(
                    text = formatPeakMemoryKb(snapshot.peakMemoryKb),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "${snapshot.peakMemoryKb} KB · Debug.MemoryInfo (sampled every 50 ms during proof generation)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                )
            }

            BenchmarkDivider()

            BenchmarkMetricSection(
                title = stringResource(Res.string.activity_zkp_last_peak_cpu_title),
                emptyText = peakResourceNone,
                hasValue = lastProof != null,
            ) {
                val snapshot = lastProof!!
                Text(
                    text = formatPeakCpuPercent(snapshot.peakCpuPercent),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = String.format(
                        "%.1f%% · /proc/self/stat (sampled every 50 ms during proof generation)",
                        snapshot.peakCpuPercent,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                )
            }

            BenchmarkDivider()

            BenchmarkMetricSection(
                title = stringResource(Res.string.activity_zkp_last_vp_token_title),
                emptyText = stringResource(Res.string.activity_zkp_last_vp_token_none),
                hasValue = lastVpToken != null,
            ) {
                val snapshot = lastVpToken!!
                Text(
                    text = formatVpTokenPayloadSize(snapshot.payloadBytes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "${snapshot.payloadBytes} bytes · len(base64url.decode(vp_token))",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun BenchmarkDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
    )
}

@Composable
private fun BenchmarkMetricSection(
    title: String,
    emptyText: String,
    hasValue: Boolean,
    content: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    if (hasValue) {
        content()
    } else {
        Text(
            text = emptyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
        )
    }
}
