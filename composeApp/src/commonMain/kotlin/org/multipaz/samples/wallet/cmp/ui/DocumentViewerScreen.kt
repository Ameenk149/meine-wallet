package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import meinewallet.composeapp.generated.resources.Res
import meinewallet.composeapp.generated.resources.more_options
import org.jetbrains.compose.resources.stringResource
import org.multipaz.claim.Claim
import org.multipaz.claim.JsonClaim
import org.multipaz.compose.claim.RenderClaimValue
import org.multipaz.compose.document.DocumentInfo
import org.multipaz.compose.document.DocumentModel
import org.multipaz.presentment.PresentmentSource
import org.multipaz.prompt.PromptModel
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    documentId: String,
    documentModel: DocumentModel,
    promptModel: PromptModel,
    presentmentSource: PresentmentSource,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val documentInfo = documentModel.documentInfos.collectAsState().value.find {
        it.document.identifier == documentId
    }
    var claims by remember { mutableStateOf<List<Claim>>(emptyList()) }

    LaunchedEffect(documentId, documentInfo) {
        val document = documentInfo?.document ?: return@LaunchedEffect
        val credentials = document.getCertifiedCredentials()
        val credential = credentials.firstOrNull() ?: return@LaunchedEffect
        claims = credential.getClaims(presentmentSource.documentTypeRepository)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(Res.string.more_options)
                        )
                    }
                }
            )
        }
    ) { padding ->
        documentInfo?.let { info ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                DocumentCard(info)

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val jsonIgnoredClaims = setOf("iss", "vct", "iat", "nbf", "exp", "cnf", "status")

                    claims.forEachIndexed { index, claim ->
                        if (claim is JsonClaim) {
                            val name = claim.claimPath[0].jsonPrimitive.content
                            if (jsonIgnoredClaims.contains(name)) {
                                return@forEachIndexed
                            }
                        }

                        androidx.compose.material3.Text(
                            text = claim.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RenderClaimValue(claim = claim)
                        if (index != claims.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    documentInfo: DocumentInfo
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Image(
            bitmap = documentInfo.cardArt,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
