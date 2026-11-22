package com.kanso.feature.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.HabitItem
import com.kanso.core.model.LifeAdminCalculations
import com.kanso.core.model.NoteItem
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.SnippetMasker
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TaskPriority
import com.kanso.core.model.WalletType
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoEmptyState
import com.kanso.core.ui.components.SectionHeader
import com.kanso.core.ui.components.expenseSemanticColor
import com.kanso.core.ui.components.incomeSemanticColor
import com.kanso.core.ui.theme.KansoTheme
import java.time.LocalDate

@Composable
fun TodayRoute(
    onOpenQuickCapture: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenBrainTab: (Int) -> Unit,
    onOpenHabits: () -> Unit = {},
    onOpenExpiries: () -> Unit = {},
    onShowMessage: (String) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    TodayScreenContent(
        uiState = uiState,
        onOpenQuickCapture = onOpenQuickCapture,
        onOpenSearch = onOpenSearch,
        onOpenSettings = onOpenSettings,
        onOpenMoney = onOpenMoney,
        onOpenBrainTab = onOpenBrainTab,
        onOpenHabits = onOpenHabits,
        onOpenExpiries = onOpenExpiries,
        onToggleTask = viewModel::toggleTaskComplete,
        onToggleHabit = { habit ->
            viewModel.toggleHabitToday(habit.id)
            onShowMessage("Completed ${habit.name}")
        },
        onAddWater250 = {
            viewModel.addWater250ml()
            onShowMessage("+250 ml water logged")
        },
        onPayBill = { bill ->
            viewModel.payBillNow(bill)
            onShowMessage("Logged payment for ${bill.name}")
        },
        onCopySnippet = { snippet ->
            clipboard.setText(AnnotatedString(snippet.value))
            onShowMessage("Copied ${snippet.label}")
        }
    )
}

@Composable
fun TodayScreenContent(
    uiState: TodayUiState,
    onOpenQuickCapture: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenBrainTab: (Int) -> Unit,
    onOpenHabits: () -> Unit = {},
    onOpenExpiries: () -> Unit = {},
    onToggleTask: (TaskItem) -> Unit,
    onToggleHabit: (HabitItem) -> Unit = {},
    onAddWater250: () -> Unit = {},
    onPayBill: (RecurringBill) -> Unit,
    onCopySnippet: (SnippetItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = uiState.formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Global Search"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenQuickCapture),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Capture expense, note, task, or snippet...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (uiState.hasAttentionItems) {
            item {
                AttentionCard(
                    uiState = uiState,
                    onPayBill = onPayBill,
                    onOpenMoney = onOpenMoney,
                    onOpenTasks = { onOpenBrainTab(1) },
                    onOpenExpiries = onOpenExpiries
                )
            }
        }

        item {
            MoneyPulseCard(
                uiState = uiState,
                onClick = onOpenMoney
            )
        }

        item {
            HabitsHydrationPulseCard(
                uiState = uiState,
                onOpenHabits = onOpenHabits,
                onToggleHabit = onToggleHabit,
                onAddWater250 = onAddWater250
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(
                    title = "Focus Tasks",
                    actionLabel = "See all",
                    onAction = { onOpenBrainTab(1) }
                )
                if (uiState.focusTasks.isEmpty()) {
                    KansoEmptyState(
                        icon = Icons.Outlined.TaskAlt,
                        title = "All clear for today",
                        subtitle = "Capture a task with + whenever something needs your focus.",
                        actionLabel = "Add Task",
                        onAction = onOpenQuickCapture
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            uiState.focusTasks.forEachIndexed { index, task ->
                                FocusTaskRow(
                                    task = task,
                                    onToggle = { onToggleTask(task) },
                                    onOpenTasks = { onOpenBrainTab(1) }
                                )
                                if (index < uiState.focusTasks.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.pinnedSnippets.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(
                        title = "1-Tap Snippets",
                        actionLabel = "All",
                        onAction = { onOpenBrainTab(2) }
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.pinnedSnippets, key = { it.id }) { snippet ->
                            SnippetQuickCard(
                                snippet = snippet,
                                onCopy = { onCopySnippet(snippet) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(
                    title = "Recent Notes",
                    actionLabel = "All",
                    onAction = { onOpenBrainTab(0) }
                )
                if (uiState.recentNotes.isEmpty()) {
                    KansoEmptyState(
                        icon = Icons.Outlined.Description,
                        title = "No notes yet",
                        subtitle = "Save quick thoughts, checklists, or ideas in Second Brain.",
                        actionLabel = "Open Notes",
                        onAction = { onOpenBrainTab(0) }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.recentNotes.forEach { note ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenBrainTab(0) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = note.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = KansoDateFormatters.formatShortDate(note.updatedAtMillis),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun AttentionCard(
    uiState: TodayUiState,
    onPayBill: (RecurringBill) -> Unit,
    onOpenMoney: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenExpiries: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Attention Needed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            uiState.dueBills.forEach { bill ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${bill.name} (Day ${bill.dueDayOfMonth})",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = CurrencyFormatter.format(bill.amountCents, uiState.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = { onPayBill(bill) }) {
                        Text("Pay")
                    }
                }
            }

            if (uiState.dueOrOverdueTasksCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenTasks),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.dueOrOverdueTasksCount} task(s) due today or overdue",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "View",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (uiState.expiringItems.isNotEmpty()) {
                val todayEpoch = LocalDate.now().toEpochDay()
                uiState.expiringItems.forEach { expiry ->
                    val delta = LifeAdminCalculations.daysUntilExpiry(expiry.expiryEpochDay, todayEpoch)
                    val label = when {
                        delta < 0L -> "${expiry.title} expired ${-delta}d ago"
                        delta == 0L -> "${expiry.title} expires today"
                        else -> "${expiry.title} expires in ${delta}d"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenExpiries),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Review",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (uiState.openIouCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenMoney),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.openIouCount} open IOU(s) • Net ${
                            CurrencyFormatter.format(uiState.openIouNetCents, uiState.currency, showSign = true)
                        }",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Money",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MoneyPulseCard(
    uiState: TodayUiState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This Month",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Today: ${CurrencyFormatter.format(uiState.todayExpenseCents, uiState.currency)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(uiState.monthExpenseCents, uiState.currency),
                        style = MaterialTheme.typography.headlineMedium,
                        color = expenseSemanticColor()
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(uiState.monthIncomeCents, uiState.currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = incomeSemanticColor()
                    )
                }
            }

            if (uiState.monthlyBudgetCents > 0L) {
                val rawProgress = uiState.monthExpenseCents.toFloat() / uiState.monthlyBudgetCents.toFloat()
                val clamped = rawProgress.coerceIn(0f, 1f)
                val barColor = if (rawProgress > 1f) {
                    expenseSemanticColor()
                } else {
                    MaterialTheme.colorScheme.primary
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { clamped },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${(rawProgress * 100).toInt()}% of ${
                            CurrencyFormatter.format(uiState.monthlyBudgetCents, uiState.currency)
                        } monthly budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Tap to view expenses or set a monthly budget",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitsHydrationPulseCard(
    uiState: TodayUiState,
    onOpenHabits: () -> Unit,
    onToggleHabit: (HabitItem) -> Unit,
    onAddWater250: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenHabits),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Habits & Hydration", style = MaterialTheme.typography.titleMedium)
                    val habitSummary = if (uiState.todayHabitsTotal > 0) {
                        "Habits: ${uiState.todayHabitsDone}/${uiState.todayHabitsTotal} • Water: ${uiState.todayWaterMl}/${uiState.waterGoalMl} ml"
                    } else {
                        "Water today: ${uiState.todayWaterMl}/${uiState.waterGoalMl} ml"
                    }
                    Text(
                        text = habitSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(onClick = onAddWater250) {
                    Icon(
                        imageVector = Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(" +250ml")
                }
            }

            if (uiState.pendingTodayHabits.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.pendingTodayHabits.forEach { habit ->
                        AssistChip(
                            onClick = { onToggleHabit(habit) },
                            label = { Text("✓ ${habit.name}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusTaskRow(
    task: TaskItem,
    onToggle: () -> Unit,
    onOpenTasks: () -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()
    val dueDay = task.dueDateEpochDay
    val isOverdue = dueDay != null && dueDay < todayEpoch
    val dueLabel = KansoDateFormatters.formatEpochDayLabel(dueDay)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenTasks)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (dueLabel != null || task.priority == TaskPriority.HIGH) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dueLabel != null) {
                        Text(
                            text = dueLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOverdue) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (task.priority == TaskPriority.HIGH) {
                        Text(
                            text = "High",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnippetQuickCard(
    snippet: SnippetItem,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(176.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onCopy),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = snippet.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = if (snippet.isSensitive) SnippetMasker.mask(snippet.value) else snippet.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TodayScreenPreview() {
    KansoTheme {
        TodayScreenContent(
            uiState = TodayUiState(
                greeting = "Good evening, Aria",
                formattedDate = "Sunday, 22 Nov",
                currency = AppCurrency.IDR,
                monthExpenseCents = 2_450_000_00L,
                monthIncomeCents = 8_500_000_00L,
                todayExpenseCents = 65_000_00L,
                monthlyBudgetCents = 4_000_000_00L,
                dueBills = listOf(
                    RecurringBill(
                        id = 1L,
                        name = "Fiber Internet",
                        amountCents = 385_000_00L,
                        category = ExpenseCategory.BILLS,
                        wallet = WalletType.BANK,
                        dueDayOfMonth = 25
                    )
                ),
                dueOrOverdueTasksCount = 1,
                openIouCount = 1,
                openIouNetCents = 150_000_00L,
                focusTasks = listOf(
                    TaskItem(
                        id = 1L,
                        title = "Pay vehicle tax before Friday",
                        priority = TaskPriority.HIGH,
                        dueDateEpochDay = LocalDate.now().toEpochDay(),
                        createdAtMillis = 1L
                    )
                ),
                pinnedSnippets = listOf(
                    SnippetItem(
                        id = 1L,
                        label = "BCA Account",
                        value = "1234567890",
                        category = SnippetCategory.FINANCE,
                        isSensitive = true,
                        isPinned = true,
                        updatedAtMillis = 1L
                    )
                ),
                recentNotes = listOf(
                    NoteItem(
                        id = 1L,
                        title = "Weekend grocery list",
                        content = "Coffee beans, oat milk, eggs, bananas",
                        isPinned = true,
                        createdAtMillis = 1L,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
            ),
            onOpenQuickCapture = {},
            onOpenSearch = {},
            onOpenSettings = {},
            onOpenMoney = {},
            onOpenBrainTab = {},
            onToggleTask = {},
            onPayBill = {},
            onCopySnippet = {}
        )
    }
}
