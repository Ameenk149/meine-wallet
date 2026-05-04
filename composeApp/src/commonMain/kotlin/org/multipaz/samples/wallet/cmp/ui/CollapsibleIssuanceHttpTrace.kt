package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import meinewallet.composeapp.generated.resources.Res
import meinewallet.composeapp.generated.resources.copy_to_clipboard
import org.jetbrains.compose.resources.stringResource
import org.multipaz.samples.wallet.cmp.activity.IssuanceApiSection
import org.multipaz.samples.wallet.cmp.activity.prettifyJsonFragmentsInHttpTrace

// LocalClipboardManager is deprecated in favor of suspend LocalClipboard + ClipEntry; plain-text
// clip construction is still platform-specific, so we keep using ClipboardManager.setText here.
@Composable
@Suppress("DEPRECATION")
fun CollapsibleDetailSection(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    titleMonospace: Boolean = false,
    prettifyJson: Boolean = false,
) {
    val clipboardManager = LocalClipboardManager.current
    var expanded by remember(title, body) { mutableStateOf(false) }
    val displayBody = remember(expanded, body, prettifyJson) {
        if (!prettifyJson) body
        else if (expanded) prettifyJsonFragmentsInHttpTrace(body)
        else body
    }
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = if (titleMonospace) FontFamily.Monospace else FontFamily.Default,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(body)) },
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = stringResource(Res.string.copy_to_clipboard),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = displayBody,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .padding(start = 40.dp, end = 8.dp, bottom = 12.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
fun CollapsibleIssuanceHttpTrace(
    sections: List<IssuanceApiSection>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        sections.forEachIndexed { index, section ->
            CollapsibleHttpApiSection(index = index, section = section)
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun CollapsibleHttpApiSection(index: Int, section: IssuanceApiSection) {
    val clipboardManager = LocalClipboardManager.current
    var expanded by remember(index, section.content) { mutableStateOf(false) }
    val displayContent = remember(expanded, section.content) {
        if (expanded) prettifyJsonFragmentsInHttpTrace(section.content) else section.content
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = section.header,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(section.content)) },
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = stringResource(Res.string.copy_to_clipboard),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = displayContent,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .padding(start = 40.dp, end = 8.dp, bottom = 12.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
}
