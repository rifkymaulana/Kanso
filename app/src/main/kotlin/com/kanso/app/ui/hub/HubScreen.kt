package com.kanso.app.ui.hub

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.ui.components.KansoAccentIconBadge
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoPillBadge
import com.kanso.core.ui.components.KansoStaggeredEntrance
import com.kanso.core.ui.theme.kansoColors

private data class HubMiniAppItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val badgeText: String,
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
    val kansoColors = MaterialTheme.kansoColors
    val stats = uiState.stats

    val items = remember(
        stats,
        uiState.lastBackupMillis,
        uiState.driveSyncConfig.isConnected,
        kansoColors,
        onOpenMoney,
        onOpenBrain,
        onOpenHabits,
        onOpenLifeAdmin,
        onOpenUtilities,
        onOpenSettings
    ) {
        val moneyExtras = stats.activeBillCount + stats.openIouCount + stats.activeGoalCount
        val moneyTotal = stats.transactionCount + moneyExtras
        val moneyBadge = when {
            stats.transactionCount > 0 && moneyExtras == 0 ->
                if (stats.transactionCount == 1) "1 entry" else "${stats.transactionCount} entries"
            moneyTotal == 1 -> "1 item"
            else -> "$moneyTotal items"
        }
        val brainTotal = stats.noteCount + stats.openTaskCount + stats.snippetCount
        listOf(
            HubMiniAppItem(
                title = "Money & Budgets",
                subtitle = "Wallets, goals, caps, bills, IOUs",
                icon = Icons.Outlined.AccountBalanceWallet,
                accent = kansoColors.moneyAccent,
                badgeText = moneyBadge,
                onClick = onOpenMoney
            ),
            HubMiniAppItem(
                title = "Second Brain",
                subtitle = "Notes, tasks & 1-tap snippets",
                icon = Icons.Outlined.Psychology,
                accent = kansoColors.brainAccent,
                badgeText = if (brainTotal == 1) "1 item" else "$brainTotal items",
                onClick = onOpenBrain
            ),
            HubMiniAppItem(
                title = "Habits & Health",
                subtitle = "Streaks, sleep, water, mood",
                icon = Icons.Outlined.FavoriteBorder,
                accent = kansoColors.habitsAccent,
                badgeText = if (stats.habitCount == 1) "1 habit" else "${stats.habitCount} habits",
                onClick = onOpenHabits
            ),
            HubMiniAppItem(
                title = "Life Admin & Vault",
                subtitle = "Expiries, service & locked vault",
                icon = Icons.Outlined.Lock,
                accent = kansoColors.lifeAdminAccent,
                badgeText = "${stats.expiryCount + stats.maintenanceCount + stats.vaultCount} tracked",
                onClick = onOpenLifeAdmin
            ),
            HubMiniAppItem(
                title = "Pocket Utilities",
                subtitle = "QR, converter, focus & dates",
                icon = Icons.Outlined.Build,
                accent = kansoColors.utilitiesAccent,
                badgeText = when (stats.countdownCount) {
                    0 -> "4 tools"
                    1 -> "1 date"
                    else -> "${stats.countdownCount} dates"
                },
                onClick = onOpenUtilities
            ),
            HubMiniAppItem(
                title = "Backup & Settings",
                subtitle = "Google Drive sync, export & updates",
                icon = Icons.Outlined.Backup,
                accent = kansoColors.settingsAccent,
                badgeText = when {
                    uiState.driveSyncConfig.isConnected -> "Cloud Sync"
                    uiState.lastBackupMillis != null -> "Backed up"
                    else -> "Setup"
                },
                onClick = onOpenSettings
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            KansoStaggeredEntrance(index = 0) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Hub",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Kanso · Personal Life OS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            KansoStaggeredEntrance(index = 1) {
                KansoBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenSearch,
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Search expenses, bills, IOUs, notes...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                pair.forEachIndexed { colIndex, appItem ->
                    KansoStaggeredEntrance(
                        index = 2 + rowIndex * 2 + colIndex,
                        modifier = Modifier.weight(1f)
                    ) {
                        MiniAppCard(
                            item = appItem,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = appItem.onClick
                        )
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            val driveConfig = uiState.driveSyncConfig
            val hasBackupOrSync = driveConfig.isConnected || uiState.lastBackupMillis != null
            val backupAccent = if (hasBackupOrSync) kansoColors.income else kansoColors.warning
            val statusIcon = if (driveConfig.isConnected) Icons.Outlined.CloudDone else Icons.Outlined.Shield
            val statusTitle = when {
                driveConfig.isConnected -> "Google Drive Sync Active"
                uiState.lastBackupMillis != null -> "Encrypted Backup Ready"
                else -> "Create an Encrypted Backup"
            }
            val statusSubtitle = when {
                driveConfig.isConnected && driveConfig.lastSyncMillis != null -> {
                    val syncedAt = driveConfig.lastSyncMillis ?: 0L
                    "Last synced: ${KansoDateFormatters.formatFullDateTime(syncedAt)}"
                }
                driveConfig.isConnected ->
                    driveConfig.accountEmail ?: "Connected — tap to sync across devices"
                uiState.lastBackupMillis != null -> {
                    val backedUpAt = uiState.lastBackupMillis ?: 0L
                    "Last backup: ${KansoDateFormatters.formatFullDateTime(backedUpAt)}"
                }
                else -> "Tap to set up Drive sync or export your database"
            }
            KansoStaggeredEntrance(index = 8) {
                KansoBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenSettings,
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KansoAccentIconBadge(
                            icon = statusIcon,
                            accent = backupAccent,
                            size = 40.dp,
                            iconSize = 20.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = statusSubtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAppCard(
    item: HubMiniAppItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    KansoBentoCard(
        modifier = modifier.height(152.dp),
        onClick = onClick,
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                KansoAccentIconBadge(
                    icon = item.icon,
                    accent = item.accent,
                    size = 42.dp,
                    iconSize = 21.dp,
                    cornerRadius = 14.dp
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopEnd
                ) {
                    KansoPillBadge(
                        text = item.badgeText,
                        color = item.accent
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
