package com.kanso.app.ui.search

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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.IouDirection
import com.kanso.core.model.SnippetMasker
import com.kanso.core.ui.components.KansoAccentIconBadge
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoClipboard
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoEmptyState
import com.kanso.core.ui.components.KansoPillBadge
import com.kanso.core.ui.components.MoneyAmountBadge
import com.kanso.core.ui.components.SectionHeader
import com.kanso.core.ui.components.accentColor
import com.kanso.core.ui.components.icon
import com.kanso.core.ui.theme.LocalBalancesHidden
import com.kanso.core.ui.theme.kansoColors

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
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val kansoColors = MaterialTheme.kansoColors

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Global Search", style = MaterialTheme.typography.titleLarge) },
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
                placeholder = { Text("Search expenses, bills, IOUs, notes, tasks...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
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
                            subtitle = "Matches notes, tasks, snippets, expenses, expiries, and service logs offline (Private Vault stays locked)."
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
                            KansoBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    KansoClipboard.copyText(
                                        context = context,
                                        label = snippet.label,
                                        text = snippet.value,
                                        isSensitive = snippet.isSensitive
                                    )
                                    onShowMessage("Copied ${snippet.label}")
                                },
                                cornerRadius = 18.dp,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    KansoAccentIconBadge(
                                        icon = Icons.Outlined.ContentPaste,
                                        accent = kansoColors.brainAccent,
                                        size = 38.dp,
                                        iconSize = 19.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(snippet.label, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = if (snippet.isSensitive) {
                                                SnippetMasker.mask(snippet.value)
                                            } else {
                                                snippet.value
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
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
                            KansoBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenBrainTab(0) },
                                cornerRadius = 18.dp,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    KansoAccentIconBadge(
                                        icon = Icons.Outlined.Description,
                                        accent = kansoColors.brainAccent,
                                        size = 38.dp,
                                        iconSize = 19.dp
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
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
                    }

                    if (uiState.tasks.isNotEmpty()) {
                        item { SectionHeader(title = "Tasks (${uiState.tasks.size})") }
                        items(uiState.tasks, key = { "task-${it.id}" }) { task ->
                            KansoBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenBrainTab(1) },
                                cornerRadius = 18.dp,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KansoAccentIconBadge(
                                        icon = Icons.Outlined.TaskAlt,
                                        accent = kansoColors.brainAccent,
                                        size = 38.dp,
                                        iconSize = 19.dp
                                    )
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    KansoPillBadge(
                                        text = if (task.isCompleted) "Done" else "Open",
                                        color = if (task.isCompleted) kansoColors.income else kansoColors.brainAccent
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.transactions.isNotEmpty()) {
                        item { SectionHeader(title = "Transactions (${uiState.transactions.size})") }
                        items(uiState.transactions, key = { "tx-${it.id}" }) { tx ->
                            KansoBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onOpenMoney,
                                cornerRadius = 18.dp,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isTransfer = tx.type == com.kanso.core.model.TransactionType.TRANSFER
                                    val transferRoute = "${tx.wallet.label} → ${(tx.toWallet ?: tx.wallet).label}"
                                    KansoAccentIconBadge(
                                        icon = if (isTransfer) Icons.Outlined.SwapHoriz else tx.category.icon(),
                                        accent = if (isTransfer) MaterialTheme.colorScheme.primary else tx.category.accentColor(),
                                        size = 38.dp,
                                        iconSize = 19.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.note.ifBlank { if (isTransfer) transferRoute else tx.category.label },
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = if (isTransfer) {
                                                "Transfer · $transferRoute · ${
                                                    KansoDateFormatters.formatShortDate(tx.timestampMillis)
                                                }"
                                            } else {
                                                "${tx.category.label} · ${tx.wallet.label} · ${
                                                    KansoDateFormatters.formatShortDate(tx.timestampMillis)
                                                }"
                                            },
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

                    if (uiState.recurringBills.isNotEmpty()) {
                        item { SectionHeader(title = "Recurring Bills (${uiState.recurringBills.size})") }
                        items(uiState.recurringBills, key = { "bill-${it.id}" }) { bill ->
                            val hidden = LocalBalancesHidden.current
                            KansoBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onOpenMoney,
                                cornerRadius = 18.dp,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KansoAccentIconBadge(
                                        icon = Icons.Outlined.Payments,
                                        accent = bill.category.accentColor(),
                                        size = 38.dp,
                                        iconSize = 19.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bill.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "Day ${bill.dueDayOfMonth} · ${bill.frequency.label} · ${bill.wallet.label}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatOrMask(bill.amountCents, uiState.currency, hidden = hidden),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = kansoColors.moneyAccent
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.ious.isNotEmpty()) {
                        item { SectionHeader(title = "IOUs (${uiState.ious.size})") }
                        items(uiState.ious, key = { "iou-${it.id}" }) { iou ->
                            val hidden = LocalBalancesHidden.current
                            val iouAccent = if (iou.direction == IouDirection.OWES_ME) {
                                kansoColors.income
                            } else {
                                kansoColors.expense
                            }
                            KansoBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onOpenMoney,
                                cornerRadius = 18.dp,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KansoAccentIconBadge(
                                        icon = Icons.Outlined.Handshake,
                                        accent = iouAccent,
                                        size = 38.dp,
                                        iconSize = 19.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(iou.personName, style = MaterialTheme.typography.titleMedium)
                                        val statusLabel = if (iou.isSettled) "Settled" else iou.direction.label
                                        val sub = listOfNotNull(
                                            statusLabel,
                                            iou.note.takeIf { it.isNotBlank() }
                                        ).joinToString(" · ")
                                        Text(
                                            text = sub,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatOrMask(iou.amountCents, uiState.currency, hidden = hidden),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = iouAccent
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.savingsGoals.isNotEmpty()) {
                        item { SectionHeader(title = "Savings Goals (${uiState.savingsGoals.size})") }
                        items(uiState.savingsGoals, key = { "goal-${it.id}" }) { goal ->
                            val hidden = LocalBalancesHidden.current
                            val catAccent = goal.category.accentColor()
                            KansoBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onOpenMoney,
                                cornerRadius = 18.dp,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KansoAccentIconBadge(
                                        icon = goal.category.icon(),
                                        accent = catAccent,
                                        size = 38.dp,
                                        iconSize = 19.dp
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(goal.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${
                                                CurrencyFormatter.formatOrMask(goal.savedAmountCents, uiState.currency, hidden = hidden)
                                            } / ${
                                                CurrencyFormatter.formatOrMask(goal.targetAmountCents, uiState.currency, hidden = hidden)
                                            }",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val pct = if (goal.targetAmountCents > 0L) {
                                        ((goal.savedAmountCents.toFloat() / goal.targetAmountCents.toFloat()) * 100f)
                                            .toInt()
                                            .coerceIn(0, 100)
                                    } else {
                                        0
                                    }
                                    KansoPillBadge(text = "$pct%", color = catAccent)
                                }
                            }
                        }
                    }

                    if (uiState.expiries.isNotEmpty()) {
                        item { SectionHeader(title = "Expiries (${uiState.expiries.size})") }
                        items(uiState.expiries, key = { "exp-${it.id}" }) { expiry ->
                            KansoBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenLifeAdminTab(0) },
                                cornerRadius = 18.dp,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KansoAccentIconBadge(
                                        icon = Icons.Outlined.EventAvailable,
                                        accent = kansoColors.lifeAdminAccent,
                                        size = 38.dp,
                                        iconSize = 19.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(expiry.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${expiry.category.label} · ${KansoDateFormatters.formatFullDate(expiry.expiryEpochDay)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    KansoPillBadge(text = "View", color = kansoColors.lifeAdminAccent)
                                }
                            }
                        }
                    }

                    if (uiState.maintenanceLogs.isNotEmpty()) {
                        item { SectionHeader(title = "Maintenance (${uiState.maintenanceLogs.size})") }
                        items(uiState.maintenanceLogs, key = { "maint-${it.id}" }) { log ->
                            KansoBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenLifeAdminTab(1) },
                                cornerRadius = 18.dp,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KansoAccentIconBadge(
                                        icon = Icons.Outlined.Build,
                                        accent = kansoColors.lifeAdminAccent,
                                        size = 38.dp,
                                        iconSize = 19.dp
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "${log.assetName} — ${log.serviceDescription}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${log.category.label} · ${KansoDateFormatters.formatFullDate(log.serviceEpochDay)}",
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
}
