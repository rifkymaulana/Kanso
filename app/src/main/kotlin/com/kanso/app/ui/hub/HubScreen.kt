package com.kanso.app.ui.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.ui.components.KansoDateFormatters

private data class HubMiniAppItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun HubRoute(
    onOpenMoney: () -> Unit,
    onOpenBrain: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenLifeAdmin: () -> Unit,
    onOpenUtilities: () -> Unit,
    viewModel: HubViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val items = remember(
        onOpenMoney,
        onOpenBrain,
        onOpenHabits,
        onOpenLifeAdmin,
        onOpenUtilities,
        onOpenSearch,
        onOpenSettings
    ) {
        listOf(
            HubMiniAppItem(
                title = "Money & Budgets",
                subtitle = "Expenses, limits, bills, IOUs",
                icon = Icons.Outlined.AccountBalanceWallet,
                onClick = onOpenMoney
            ),
            HubMiniAppItem(
                title = "Second Brain",
                subtitle = "Notes, tasks & 1-tap snippets",
                icon = Icons.Outlined.Psychology,
                onClick = onOpenBrain
            ),
            HubMiniAppItem(
                title = "Habits & Health",
                subtitle = "Streaks, sleep, water, mood",
                icon = Icons.Outlined.FavoriteBorder,
                onClick = onOpenHabits
            ),
            HubMiniAppItem(
                title = "Life Admin & Vault",
                subtitle = "Expiries, service & locked vault",
                icon = Icons.Outlined.Lock,
                onClick = onOpenLifeAdmin
            ),
            HubMiniAppItem(
                title = "Pocket Utilities",
                subtitle = "QR tools, converters, Pomodoro",
                icon = Icons.Outlined.Build,
                onClick = onOpenUtilities
            ),
            HubMiniAppItem(
                title = "Global Search",
                subtitle = "Search everything offline",
                icon = Icons.Outlined.Search,
                onClick = onOpenSearch
            ),
            HubMiniAppItem(
                title = "Backup & Settings",
                subtitle = "AES-256 export & preferences",
                icon = Icons.Outlined.Backup,
                onClick = onOpenSettings
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Hub",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Your personal modular mini-apps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "On-Device Storage",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatChip("Entries", uiState.stats.transactionCount)
                        StatChip("Notes", uiState.stats.noteCount)
                        StatChip("Tasks", uiState.stats.openTaskCount)
                        StatChip("Snippets", uiState.stats.snippetCount)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatChip("Habits", uiState.stats.habitCount)
                        StatChip("Expiries", uiState.stats.expiryCount)
                        StatChip("Vault", uiState.stats.vaultCount)
                    }
                    Text(
                        text = uiState.lastBackupMillis?.let {
                            "Last encrypted backup: ${KansoDateFormatters.formatFullDateTime(it)}"
                        } ?: "No encrypted backup created yet — tap Backup & Settings.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val rows = items.chunked(2)
        items(rows.size) { rowIndex ->
            val pair = rows[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { appItem ->
                    MiniAppCard(
                        item = appItem,
                        modifier = Modifier.weight(1f),
                        onClick = appItem.onClick
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MiniAppCard(
    item: HubMiniAppItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(138.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}
