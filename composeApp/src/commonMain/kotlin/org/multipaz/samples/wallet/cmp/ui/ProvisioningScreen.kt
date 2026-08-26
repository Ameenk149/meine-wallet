package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CompletableDeferred
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import meinewallet.composeapp.generated.resources.Res
import meinewallet.composeapp.generated.resources.cancel_provisioning
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.provisioning.ProvisioningBottomSheet
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.samples.wallet.cmp.ProvisioningSupport

@Composable
fun ProvisioningScreen(
    provisioningModel: ProvisioningModel,
    provisioningSupport: ProvisioningSupport,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProvisioningBottomSheet(
            provisioningModel = provisioningModel,
            waitForRedirectLinkInvocation = { state ->
                provisioningSupport.waitForAppLinkInvocation(state)
            },
            clientPreferences = CompletableDeferred(provisioningSupport.preferences),
            backend = CompletableDeferred(provisioningSupport.backend),
        )

        Spacer(Modifier.padding(12.dp))

        Button(onClick = onCancel) {
            Text(stringResource(Res.string.cancel_provisioning))
        }
    }
}