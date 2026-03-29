package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.multipaz.samples.wallet.cmp.activity.IssuanceApiSection
import org.multipaz.samples.wallet.cmp.activity.prettifyJsonFragmentsInHttpTrace

@Composable
fun CollapsibleDetailSection(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    titleMonospace: Boolean = false,
    prettifyJson: Boolean = false,
) {
    var expanded by remember(title, body) { mutableStateOf(false) }
    val displayBody = remember(expanded, body, prettifyJson) {
        if (!prettifyJson) body
        else if (expanded) prettifyJsonFragmentsInHttpTrace(body)
        else body
    }
    Column(modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp, horizontal = 4.dp),
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 2.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = if (titleMonospace) FontFamily.Monospace else FontFamily.Default,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(start = 40.dp, top = 2.dp),
            )
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
private fun CollapsibleHttpApiSection(index: Int, section: IssuanceApiSection) {
    var expanded by remember(index, section.content) { mutableStateOf(false) }
    val displayContent = remember(expanded, section.content) {
        if (expanded) prettifyJsonFragmentsInHttpTrace(section.content) else section.content
    }

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp, horizontal = 4.dp),
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 2.dp),
            )
            Text(
                text = section.header,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(start = 40.dp, top = 2.dp),
            )
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
