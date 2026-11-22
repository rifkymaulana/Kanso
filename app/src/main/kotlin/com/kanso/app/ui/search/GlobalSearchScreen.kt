package com.kanso.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.model.SnippetMasker
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoEmptyState
import com.kanso.core.ui.components.MoneyAmountBadge
import com.kanso.core.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchRoute(
    onBack: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenBrainTab: (Int) -> Unit,
    onOpenLifeAdminTab: (Int) -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Global Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search expenses, notes, tasks, expiries...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.query.isBlank()) {
                    item {
                        KansoEmptyState(
                            icon = Icons.Outlined.Search,
                            title = "Search your entire Kanso",
                            subtitle = "Matches notes via SQLite FTS4, tasks, snippets, expenses, expiries, and service logs offline (Private Vault stays locked)."
                        )
                    }
                } else if (uiState.totalMatches == 0) {
                    item {
                        KansoEmptyState(
                            icon = Icons.Outlined.Search,
                            title = "No results for \"${uiState.query.trim()}\"",
                            subtitle = "Try another keyword or tag."
                        )
                    }
                } else {
                    if (uiState.snippets.isNotEmpty()) {
                        item { SectionHeader(title = "Snippets (${uiState.snippets.size})") }
                        items(uiState.snippets, key = { "snip-${it.id}" }) { snippet ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        clipboard.setText(AnnotatedString(snippet.value))
                                        onShowMessage("Copied ${snippet.label}")
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(snippet.label, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = if (snippet.isSensitive) {
                                                SnippetMasker.mask(snippet.value)
                                            } else {
                                                snippet.value
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.notes.isNotEmpty()) {
                        item { SectionHeader(title = "Notes (${uiState.notes.size})") }
                        items(uiState.notes, key = { "note-${it.id}" }) { note ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenBrainTab(0) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(note.title, style = MaterialTheme.typography.titleMedium)
                                    if (note.content.isNotBlank()) {
                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.tasks.isNotEmpty()) {
                        item { SectionHeader(title = "Tasks (${uiState.tasks.size})") }
                        items(uiState.tasks, key = { "task-${it.id}" }) { task ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenBrainTab(1) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (task.isCompleted) "Done" else "Open",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.transactions.isNotEmpty()) {
                        item { SectionHeader(title = "Transactions (${uiState.transactions.size})") }
                        items(uiState.transactions, key = { "tx-${it.id}" }) { tx ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onOpenMoney),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.note.ifBlank { tx.category.label },
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${tx.category.label} • ${tx.wallet.label} • ${
                                                KansoDateFormatters.formatShortDate(tx.timestampMillis)
                                            }",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    MoneyAmountBadge(
                                        amountCents = tx.amountCents,
                                        currency = uiState.currency,
                                        type = tx.type
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.expiries.isNotEmpty()) {
                        item { SectionHeader(title = "Expiries (${uiState.expiries.size})") }
                        items(uiState.expiries, key = { "exp-${it.id}" }) { expiry ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenLifeAdminTab(0) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(expiry.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${expiry.category.label} • ${KansoDateFormatters.formatFullDate(expiry.expiryEpochDay)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "View",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.maintenanceLogs.isNotEmpty()) {
                        item { SectionHeader(title = "Maintenance (${uiState.maintenanceLogs.size})") }
                        items(uiState.maintenanceLogs, key = { "maint-${it.id}" }) { log ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenLifeAdminTab(1) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${log.assetName} — ${log.serviceDescription}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${log.category.label} • ${KansoDateFormatters.formatFullDate(log.serviceEpochDay)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
