package com.kanso.feature.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
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
import com.kanso.core.ui.components.KansoAccentIconBadge
import com.kanso.core.ui.components.KansoAnimatedProgressBar
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoCheckCircle
import com.kanso.core.ui.components.KansoCompactEmptyHint
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoHeroCard
import com.kanso.core.ui.components.KansoPillBadge
import com.kanso.core.ui.components.KansoProgressRing
import com.kanso.core.ui.components.SectionHeader
import com.kanso.core.ui.components.expenseSemanticColor
import com.kanso.core.ui.components.warningSemanticColor
import com.kanso.core.ui.theme.KansoTheme
import com.kanso.core.ui.theme.kansoColors
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
    val haptic = LocalHapticFeedback.current
    val kansoColors = MaterialTheme.kansoColors

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = uiState.formattedDate.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenSearch),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Global Search",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenSettings),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            MoneyHeroPulseCard(
                uiState = uiState,
                onClick = onOpenMoney
            )
        }

        if (uiState.hasAttentionItems) {
            item {
                AttentionBentoCard(
                    uiState = uiState,
                    onPayBill = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPayBill(it)
                    },
                    onOpenMoney = onOpenMoney,
                    onOpenTasks = { onOpenBrainTab(1) },
                    onOpenExpiries = onOpenExpiries
                )
            }
        }

        item {
            BentoPulseRow(
                uiState = uiState,
                onOpenHabits = onOpenHabits,
                onAddWater250 = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddWater250()
                }
            )
        }

        if (uiState.pendingTodayHabits.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(
                        title = "Quick Habits",
                        actionLabel = "All",
                        onAction = onOpenHabits
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.pendingTodayHabits, key = { it.id }) { habit ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onToggleHabit(habit)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, kansoColors.habitsAccent.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .border(1.75.dp, kansoColors.habitsAccent, CircleShape)
                                    )
                                    Text(
                                        text = habit.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(
                    title = "Focus Tasks",
                    actionLabel = "See all",
                    onAction = { onOpenBrainTab(1) }
                )
                if (uiState.focusTasks.isEmpty()) {
                    KansoCompactEmptyHint(
                        icon = Icons.Outlined.TaskAlt,
                        title = "All clear for today",
                        subtitle = "Tap + below anytime to add a task.",
                        accentColor = kansoColors.brainAccent,
                        actionLabel = "Add",
                        onAction = onOpenQuickCapture
                    )
                } else {
                    KansoBentoCard(
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        uiState.focusTasks.forEachIndexed { index, task ->
                            FocusTaskRow(
                                task = task,
                                accentColor = kansoColors.brainAccent,
                                onToggle = { onToggleTask(task) },
                                onOpenTasks = { onOpenBrainTab(1) }
                            )
                            if (index < uiState.focusTasks.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
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
                                onCopy = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCopySnippet(snippet)
                                }
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
                    KansoCompactEmptyHint(
                        icon = Icons.Outlined.Description,
                        title = "No notes yet",
                        subtitle = "Capture thoughts & quick references in Second Brain.",
                        accentColor = kansoColors.brainAccent,
                        actionLabel = "Open",
                        onAction = { onOpenBrainTab(0) }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.recentNotes.forEach { note ->
                            KansoBentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenBrainTab(0) },
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    if (note.isPinned) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(kansoColors.brainAccent)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
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
                                            Spacer(modifier = Modifier.width(8.dp))
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
        }
    }
}

@Composable
private fun MoneyHeroPulseCard(
    uiState: TodayUiState,
    onClick: () -> Unit
) {
    val kansoColors = MaterialTheme.kansoColors
    val netCents = uiState.monthIncomeCents - uiState.monthExpenseCents

    KansoHeroCard(
        onClick = onClick,
        glowColor = kansoColors.moneyAccent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "THIS MONTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = kansoColors.heroOnSurfaceMuted
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Today · ${CurrencyFormatter.format(uiState.todayExpenseCents, uiState.currency)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = kansoColors.heroOnSurface
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Spent this month",
                    style = MaterialTheme.typography.labelMedium,
                    color = kansoColors.heroOnSurfaceMuted
                )
                Text(
                    text = CurrencyFormatter.format(uiState.monthExpenseCents, uiState.currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = kansoColors.heroOnSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroStatMiniPill(
                    label = "Income",
                    value = CurrencyFormatter.format(uiState.monthIncomeCents, uiState.currency),
                    dotColor = Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )
                HeroStatMiniPill(
                    label = "Net",
                    value = CurrencyFormatter.format(netCents, uiState.currency, showSign = true),
                    dotColor = if (netCents >= 0L) Color(0xFF34D399) else Color(0xFFF87171),
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.monthlyBudgetCents > 0L) {
                val rawProgress = uiState.monthExpenseCents.toFloat() / uiState.monthlyBudgetCents.toFloat()
                val barColor = when {
                    rawProgress > 1f -> Color(0xFFF87171)
                    rawProgress >= 0.8f -> Color(0xFFFBBF24)
                    else -> Color(0xFF2DD4BF)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    KansoAnimatedProgressBar(
                        progress = rawProgress.coerceIn(0f, 1f),
                        height = 7.dp,
                        color = barColor,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(rawProgress * 100).toInt()}% of budget",
                            style = MaterialTheme.typography.labelMedium,
                            color = kansoColors.heroOnSurfaceMuted
                        )
                        Text(
                            text = CurrencyFormatter.format(uiState.monthlyBudgetCents, uiState.currency),
                            style = MaterialTheme.typography.labelMedium,
                            color = kansoColors.heroOnSurface
                        )
                    }
                }
            } else {
                Text(
                    text = "Tap to log expenses or set a monthly budget →",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF5EEAD4)
                )
            }
        }
    }
}

@Composable
private fun HeroStatMiniPill(
    label: String,
    value: String,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.kansoColors.heroOnSurfaceMuted
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.kansoColors.heroOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BentoPulseRow(
    uiState: TodayUiState,
    onOpenHabits: () -> Unit,
    onAddWater250: () -> Unit
) {
    val kansoColors = MaterialTheme.kansoColors
    val goal = uiState.waterGoalMl.coerceAtLeast(250)
    val waterProgress = (uiState.todayWaterMl.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val habitTotal = uiState.todayHabitsTotal
    val habitProgress = if (habitTotal > 0) {
        (uiState.todayHabitsDone.toFloat() / habitTotal.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KansoBentoCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onClick = onOpenHabits,
            contentPadding = PaddingValues(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hydration",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    KansoProgressRing(
                        progress = waterProgress,
                        color = kansoColors.hydrationAccent,
                        size = 46.dp,
                        strokeWidth = 5.dp
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            tint = kansoColors.hydrationAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${uiState.todayWaterMl} ml",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = kansoColors.hydrationAccent
                    )
                    Text(
                        text = "Goal $goal ml · ${(waterProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAddWater250),
                    shape = RoundedCornerShape(12.dp),
                    color = kansoColors.hydrationAccent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "+250 ml",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = kansoColors.hydrationAccent,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        KansoBentoCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onClick = onOpenHabits,
            contentPadding = PaddingValues(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Habits",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    KansoProgressRing(
                        progress = habitProgress,
                        color = kansoColors.habitsAccent,
                        size = 46.dp,
                        strokeWidth = 5.dp
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = kansoColors.habitsAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (habitTotal > 0) "${uiState.todayHabitsDone}/$habitTotal done" else "No habits",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val remaining = (habitTotal - uiState.todayHabitsDone).coerceAtLeast(0)
                    Text(
                        text = when {
                            habitTotal == 0 -> "Build a streak"
                            remaining == 0 -> "All done today!"
                            else -> "$remaining left today"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (habitTotal > 0 && remaining == 0) {
                            kansoColors.income
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = kansoColors.habitsAccent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (habitTotal == 0) "Create habit →" else "Open tracker →",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = kansoColors.habitsAccent,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AttentionBentoCard(
    uiState: TodayUiState,
    onPayBill: (RecurringBill) -> Unit,
    onOpenMoney: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenExpiries: () -> Unit
) {
    val warning = warningSemanticColor()
    val count = uiState.dueBills.size +
        (if (uiState.dueOrOverdueTasksCount > 0) 1 else 0) +
        uiState.expiringItems.size +
        (if (uiState.openIouCount > 0) 1 else 0)

    KansoBentoCard(
        containerColor = warning.copy(alpha = 0.08f),
        borderColor = warning.copy(alpha = 0.30f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KansoAccentIconBadge(
                        icon = Icons.Outlined.NotificationsActive,
                        accent = warning,
                        size = 34.dp,
                        iconSize = 18.dp,
                        cornerRadius = 10.dp
                    )
                    Text(
                        text = "Attention Needed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                KansoPillBadge(text = "$count", color = warning)
            }

            uiState.dueBills.forEach { bill ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${bill.name} · Day ${bill.dueDayOfMonth}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = CurrencyFormatter.format(bill.amountCents, uiState.currency),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { onPayBill(bill) },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Pay")
                    }
                }
            }

            if (uiState.dueOrOverdueTasksCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenTasks)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.dueOrOverdueTasksCount} task(s) due today or overdue",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "View →",
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
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenExpiries)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Review →",
                            style = MaterialTheme.typography.labelLarge,
                            color = warning
                        )
                    }
                }
            }

            if (uiState.openIouCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenMoney)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.openIouCount} open IOU(s) · Net ${
                            CurrencyFormatter.format(uiState.openIouNetCents, uiState.currency, showSign = true)
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Money →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusTaskRow(
    task: TaskItem,
    accentColor: Color,
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
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KansoCheckCircle(
            checked = task.isCompleted,
            accentColor = accentColor,
            onToggle = onToggle
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (task.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (dueLabel != null || task.priority == TaskPriority.HIGH) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dueLabel != null) {
                        KansoPillBadge(
                            text = dueLabel,
                            color = if (isOverdue) expenseSemanticColor() else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (task.priority == TaskPriority.HIGH) {
                        KansoPillBadge(
                            text = "High",
                            color = warningSemanticColor()
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
    KansoBentoCard(
        modifier = Modifier.width(186.dp),
        onClick = onCopy,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = snippet.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (snippet.isSensitive) SnippetMasker.mask(snippet.value) else snippet.value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
                todayWaterMl = 1250,
                waterGoalMl = 2000,
                todayHabitsDone = 2,
                todayHabitsTotal = 3,
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
