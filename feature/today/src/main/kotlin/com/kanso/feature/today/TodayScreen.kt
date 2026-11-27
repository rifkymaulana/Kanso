package com.kanso.feature.today

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.BudgetAnchorSource
import com.kanso.core.model.CoachActionTarget
import com.kanso.core.model.CoachInsight
import com.kanso.core.model.CoachInsightTone
import com.kanso.core.model.CountdownCalculations
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.HabitItem
import com.kanso.core.model.LifeAdminCalculations
import com.kanso.core.model.NoteChecklistParser
import com.kanso.core.model.NoteItem
import com.kanso.core.model.PomodoroCalculations
import com.kanso.core.model.PomodoroMode
import com.kanso.core.model.PomodoroState
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.SafeToSpendReport
import com.kanso.core.model.SmartCoachCalculations
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.SnippetMasker
import com.kanso.core.model.SpendPaceStatus
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TaskPriority
import com.kanso.core.model.TaskRecurrence
import com.kanso.core.model.WalletType
import com.kanso.core.ui.components.KansoAccentIconBadge
import com.kanso.core.ui.components.KansoAnimatedCurrencyText
import com.kanso.core.ui.components.KansoAnimatedIntText
import com.kanso.core.ui.components.KansoAnimatedProgressBar
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoBounceOnChange
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoCheckCircle
import com.kanso.core.ui.components.KansoClipboard
import com.kanso.core.ui.components.KansoCompactEmptyHint
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoHeroCard
import com.kanso.core.ui.components.KansoMotion
import com.kanso.core.ui.components.KansoNoteChecklistPreview
import com.kanso.core.ui.components.KansoPillBadge
import com.kanso.core.ui.components.KansoPrivacyEyeButton
import com.kanso.core.ui.components.KansoProgressRing
import com.kanso.core.ui.components.KansoStaggeredEntrance
import com.kanso.core.ui.components.KansoTonalButton
import com.kanso.core.ui.components.MonthlyReviewSheet
import com.kanso.core.ui.components.SectionHeader
import com.kanso.core.ui.components.expenseSemanticColor
import com.kanso.core.ui.components.incomeSemanticColor
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
    onOpenIousTab: () -> Unit = onOpenMoney,
    onOpenBrainTab: (Int) -> Unit,
    onOpenNoteItem: (Long) -> Unit = { onOpenBrainTab(0) },
    onOpenTaskItem: (Long) -> Unit = { onOpenBrainTab(1) },
    onOpenHabits: () -> Unit = {},
    onOpenExpiries: () -> Unit = {},
    onOpenExpiryItem: (Long) -> Unit = { onOpenExpiries() },
    onOpenMaintenance: () -> Unit = {},
    onOpenMaintenanceItem: (Long) -> Unit = { onOpenMaintenance() },
    onOpenCountdowns: () -> Unit = {},
    onOpenCountdownItem: (Long) -> Unit = { onOpenCountdowns() },
    onOpenFocusTimer: () -> Unit = {},
    onShowMessage: (String) -> Unit,
    onShowUndoMessage: (String, () -> Unit) -> Unit = { msg, _ -> onShowMessage(msg) },
    onBalancesVisibilityChanged: () -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMonthlyReviewSheet by remember { mutableStateOf(false) }

    TodayScreenContent(
        uiState = uiState,
        onOpenQuickCapture = onOpenQuickCapture,
        onOpenSearch = onOpenSearch,
        onOpenSettings = onOpenSettings,
        onOpenMoney = onOpenMoney,
        onOpenIousTab = onOpenIousTab,
        onOpenMonthlyReview = { showMonthlyReviewSheet = true },
        onOpenBrainTab = onOpenBrainTab,
        onOpenNoteItem = onOpenNoteItem,
        onOpenTaskItem = onOpenTaskItem,
        onOpenHabits = onOpenHabits,
        onOpenExpiries = onOpenExpiries,
        onOpenExpiryItem = onOpenExpiryItem,
        onOpenMaintenance = onOpenMaintenance,
        onOpenMaintenanceItem = onOpenMaintenanceItem,
        onOpenCountdowns = onOpenCountdowns,
        onOpenCountdownItem = onOpenCountdownItem,
        onOpenFocusTimer = onOpenFocusTimer,
        onTogglePomodoro = viewModel::togglePomodoro,
        onToggleHideBalances = {
            viewModel.toggleHideBalances()
            onBalancesVisibilityChanged()
        },
        onToggleTask = { task ->
            viewModel.toggleTaskComplete(task) { previous, nextDueDay ->
                val nextLabel = KansoDateFormatters.formatEpochDayLabel(nextDueDay)
                    ?: KansoDateFormatters.formatFullDate(nextDueDay)
                onShowUndoMessage("Completed · Next due $nextLabel") {
                    viewModel.restoreTask(previous)
                }
            }
        },
        onToggleNoteChecklistLine = viewModel::toggleNoteChecklistLine,
        onToggleHabit = { habit ->
            viewModel.toggleHabitToday(habit.id)
            onShowMessage("Completed ${habit.name}")
        },
        onAddWater250 = {
            viewModel.addWater250ml()
            onShowMessage("+250 ml water logged")
        },
        onPayBill = { bill ->
            viewModel.payBillNow(bill) { record ->
                onShowUndoMessage("Logged payment for ${bill.name}") {
                    viewModel.undoPayBill(record)
                }
            }
        },
        onCopySnippet = { snippet ->
            KansoClipboard.copyText(
                context = context,
                label = snippet.label,
                text = snippet.value,
                isSensitive = snippet.isSensitive
            )
            onShowMessage("Copied ${snippet.label}")
        }
    )

    if (showMonthlyReviewSheet) {
        uiState.monthlyReview?.let { report ->
            MonthlyReviewSheet(
                report = report,
                currency = uiState.currency,
                hideBalances = uiState.hideBalances,
                onDismiss = { showMonthlyReviewSheet = false },
                onCopySummary = { summary ->
                    KansoClipboard.copyText(
                        context = context,
                        label = "Monthly Review",
                        text = summary,
                        isSensitive = uiState.hideBalances
                    )
                    onShowMessage("Monthly review copied")
                },
                onSaveToNotes = { title, content ->
                    viewModel.saveMonthlyReviewToNotes(title, content)
                    onShowMessage("Saved recap to Notes")
                }
            )
        }
    }
}

@Composable
fun TodayScreenContent(
    uiState: TodayUiState,
    onOpenQuickCapture: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenIousTab: () -> Unit = onOpenMoney,
    onOpenMonthlyReview: () -> Unit = {},
    onOpenBrainTab: (Int) -> Unit,
    onOpenNoteItem: (Long) -> Unit = { onOpenBrainTab(0) },
    onOpenTaskItem: (Long) -> Unit = { onOpenBrainTab(1) },
    onOpenHabits: () -> Unit = {},
    onOpenExpiries: () -> Unit = {},
    onOpenExpiryItem: (Long) -> Unit = { onOpenExpiries() },
    onOpenMaintenance: () -> Unit = {},
    onOpenMaintenanceItem: (Long) -> Unit = { onOpenMaintenance() },
    onOpenCountdowns: () -> Unit = {},
    onOpenCountdownItem: (Long) -> Unit = { onOpenCountdowns() },
    onOpenFocusTimer: () -> Unit = {},
    onTogglePomodoro: () -> Unit = {},
    onToggleHideBalances: () -> Unit = {},
    onToggleTask: (TaskItem) -> Unit,
    onToggleNoteChecklistLine: (NoteItem, Int) -> Unit = { _, _ -> },
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
            KansoStaggeredEntrance(index = 0) {
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
                        KansoPrivacyEyeButton(
                            hidden = uiState.hideBalances,
                            onToggle = onToggleHideBalances
                        )
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
        }

        item {
            KansoStaggeredEntrance(index = 1) {
                MoneyHeroPulseCard(
                    uiState = uiState,
                    onClick = onOpenMoney,
                    onOpenMonthlyReview = onOpenMonthlyReview
                )
            }
        }

        if (uiState.hasAttentionItems) {
            item {
                KansoStaggeredEntrance(index = 2) {
                    AttentionBentoCard(
                        uiState = uiState,
                        onPayBill = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPayBill(it)
                        },
                        onOpenIousTab = onOpenIousTab,
                        onOpenTasks = { onOpenBrainTab(1) },
                        onOpenExpiries = onOpenExpiries,
                        onOpenExpiryItem = onOpenExpiryItem,
                        onOpenMaintenance = onOpenMaintenance,
                        onOpenMaintenanceItem = onOpenMaintenanceItem,
                        onOpenCountdowns = onOpenCountdowns,
                        onOpenCountdownItem = onOpenCountdownItem
                    )
                }
            }
        }

        if (uiState.safeToSpend != null || uiState.coachInsights.isNotEmpty()) {
            item {
                KansoStaggeredEntrance(index = 2) {
                    SmartCoachBentoCard(
                        uiState = uiState,
                        onOpenMoney = onOpenMoney,
                        onOpenHabits = onOpenHabits,
                        onAddWater250 = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddWater250()
                        }
                    )
                }
            }
        }

        item {
            KansoStaggeredEntrance(index = 3) {
                BentoPulseRow(
                    uiState = uiState,
                    onOpenHabits = onOpenHabits,
                    onAddWater250 = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAddWater250()
                    }
                )
            }
        }

        if (uiState.pendingTodayHabits.isNotEmpty()) {
            item {
                KansoStaggeredEntrance(index = 4) {
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
                                        .animateItem()
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
        }

        uiState.activePomodoro?.let { pomodoro ->
            item {
                KansoStaggeredEntrance(index = 4) {
                    ActiveFocusTimerCard(
                        pomodoro = pomodoro,
                        onClick = onOpenFocusTimer,
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePomodoro()
                        }
                    )
                }
            }
        }

        item {
            KansoStaggeredEntrance(index = 5) {
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
                            Column(modifier = Modifier.animateContentSize(animationSpec = KansoMotion.SizeSpring)) {
                                uiState.focusTasks.forEachIndexed { index, task ->
                                    FocusTaskRow(
                                        task = task,
                                        accentColor = kansoColors.brainAccent,
                                        onToggle = { onToggleTask(task) },
                                        onOpenTasks = { onOpenTaskItem(task.id) }
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
            }
        }

        if (uiState.pinnedSnippets.isNotEmpty()) {
            item {
                KansoStaggeredEntrance(index = 6) {
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
                                    modifier = Modifier.animateItem(),
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
        }

        item {
            KansoStaggeredEntrance(index = 7) {
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
                                onClick = { onOpenNoteItem(note.id) },
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
                                        val parsed = remember(note.content) { NoteChecklistParser.parse(note.content) }
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
                                            if (parsed.hasChecklist) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                KansoPillBadge(
                                                    text = "${parsed.checkedCount}/${parsed.totalCount}",
                                                    color = if (parsed.isAllChecked) kansoColors.income else kansoColors.brainAccent
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = KansoDateFormatters.formatShortDate(note.updatedAtMillis),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (parsed.hasChecklist) {
                                            val plainText = parsed.plainPreviewLines.take(1).joinToString("\n")
                                            if (plainText.isNotBlank()) {
                                                Text(
                                                    text = plainText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            KansoNoteChecklistPreview(
                                                items = parsed.items,
                                                accentColor = kansoColors.brainAccent,
                                                onToggleLine = { lineIdx -> onToggleNoteChecklistLine(note, lineIdx) },
                                                maxVisible = 4
                                            )
                                        } else if (note.content.isNotBlank()) {
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
}

@Composable
private fun MoneyHeroPulseCard(
    uiState: TodayUiState,
    onClick: () -> Unit,
    onOpenMonthlyReview: () -> Unit = {}
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenMonthlyReview),
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "Review →",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF5EEAD4),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        KansoAnimatedCurrencyText(
                            amountCents = uiState.todayExpenseCents,
                            currency = uiState.currency,
                            prefix = "Today · ",
                            style = MaterialTheme.typography.labelMedium,
                            color = kansoColors.heroOnSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Spent this month",
                    style = MaterialTheme.typography.labelMedium,
                    color = kansoColors.heroOnSurfaceMuted
                )
                KansoAnimatedCurrencyText(
                    amountCents = uiState.monthExpenseCents,
                    currency = uiState.currency,
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
                    amountCents = uiState.monthIncomeCents,
                    currency = uiState.currency,
                    dotColor = Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )
                HeroStatMiniPill(
                    label = "Net",
                    amountCents = netCents,
                    currency = uiState.currency,
                    showSign = true,
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
                        KansoAnimatedIntText(
                            value = (rawProgress * 100).toInt(),
                            format = { "$it% of budget" },
                            style = MaterialTheme.typography.labelMedium,
                            color = kansoColors.heroOnSurfaceMuted
                        )
                        Text(
                            text = CurrencyFormatter.formatOrMask(uiState.monthlyBudgetCents, uiState.currency, hidden = uiState.hideBalances),
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
    amountCents: Long,
    currency: AppCurrency,
    dotColor: Color,
    modifier: Modifier = Modifier,
    showSign: Boolean = false
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
            KansoAnimatedCurrencyText(
                amountCents = amountCents,
                currency = currency,
                showSign = showSign,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.kansoColors.heroOnSurface
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
                        KansoBounceOnChange(trigger = uiState.todayWaterMl) {
                            Icon(
                                imageVector = Icons.Outlined.WaterDrop,
                                contentDescription = null,
                                tint = kansoColors.hydrationAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    KansoAnimatedIntText(
                        value = uiState.todayWaterMl,
                        format = { "$it ml" },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = kansoColors.hydrationAccent
                    )
                    KansoAnimatedIntText(
                        value = (waterProgress * 100).toInt(),
                        format = { "Goal $goal ml · $it%" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                KansoTonalButton(
                    text = "+250 ml",
                    size = KansoButtonSize.COMPACT,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = kansoColors.hydrationAccent.copy(alpha = 0.14f),
                    contentColor = kansoColors.hydrationAccent,
                    onClick = onAddWater250
                )
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
                        KansoBounceOnChange(trigger = uiState.todayHabitsDone) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = kansoColors.habitsAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (habitTotal > 0) {
                        KansoAnimatedIntText(
                            value = uiState.todayHabitsDone,
                            format = { "$it/$habitTotal done" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "No habits",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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

                KansoTonalButton(
                    text = if (habitTotal == 0) "Create habit →" else "Open tracker →",
                    size = KansoButtonSize.COMPACT,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = kansoColors.habitsAccent.copy(alpha = 0.14f),
                    contentColor = kansoColors.habitsAccent,
                    onClick = onOpenHabits
                )
            }
        }
    }
}

@Composable
private fun AttentionBentoCard(
    uiState: TodayUiState,
    onPayBill: (RecurringBill) -> Unit,
    onOpenIousTab: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenExpiries: () -> Unit,
    onOpenExpiryItem: (Long) -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenMaintenanceItem: (Long) -> Unit,
    onOpenCountdowns: () -> Unit,
    onOpenCountdownItem: (Long) -> Unit
) {
    val warning = warningSemanticColor()
    val count = uiState.dueBills.size +
        uiState.dueOrOverdueTasksCount +
        uiState.totalExpiringCount +
        uiState.totalDueMaintenanceCount +
        uiState.totalUpcomingCountdownsCount +
        uiState.dueSoonIousCount

    KansoBentoCard(
        containerColor = warning.copy(alpha = 0.08f),
        borderColor = warning.copy(alpha = 0.30f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.animateContentSize(animationSpec = KansoMotion.SizeSpring),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                            text = CurrencyFormatter.formatOrMask(bill.amountCents, uiState.currency, hidden = uiState.hideBalances),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    KansoTonalButton(
                        text = "Pay",
                        size = KansoButtonSize.COMPACT,
                        onClick = { onPayBill(bill) }
                    )
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
                val todayEpoch = uiState.todayEpochDay
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
                            .clickable { onOpenExpiryItem(expiry.id) }
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
                if (uiState.totalExpiringCount > uiState.expiringItems.size) {
                    val remainingExpiring = uiState.totalExpiringCount - uiState.expiringItems.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenExpiries)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+$remainingExpiring more expiring soon",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "See all →",
                            style = MaterialTheme.typography.labelMedium,
                            color = warning
                        )
                    }
                }
            }

            if (uiState.dueMaintenanceLogs.isNotEmpty()) {
                val todayEpoch = uiState.todayEpochDay
                uiState.dueMaintenanceLogs.forEach { log ->
                    val dueDay = log.nextDueEpochDay ?: todayEpoch
                    val delta = LifeAdminCalculations.daysUntilExpiry(dueDay, todayEpoch)
                    val label = when {
                        delta < 0L -> "${log.assetName} · ${log.serviceDescription} overdue ${-delta}d"
                        delta == 0L -> "${log.assetName} · ${log.serviceDescription} due today"
                        else -> "${log.assetName} · ${log.serviceDescription} due in ${delta}d"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenMaintenanceItem(log.id) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Service →",
                            style = MaterialTheme.typography.labelLarge,
                            color = warning
                        )
                    }
                }
                if (uiState.totalDueMaintenanceCount > uiState.dueMaintenanceLogs.size) {
                    val remainingServices = uiState.totalDueMaintenanceCount - uiState.dueMaintenanceLogs.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenMaintenance)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+$remainingServices more service due",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "See all →",
                            style = MaterialTheme.typography.labelMedium,
                            color = warning
                        )
                    }
                }
            }

            if (uiState.upcomingCountdowns.isNotEmpty()) {
                val todayEpoch = uiState.todayEpochDay
                uiState.upcomingCountdowns.forEach { countdown ->
                    val delta = CountdownCalculations.daysRemaining(countdown.targetEpochDay, todayEpoch)
                    val deltaLabel = CountdownCalculations.formatDelta(delta)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenCountdownItem(countdown.id) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${countdown.title} · $deltaLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Dates →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.kansoColors.utilitiesAccent
                        )
                    }
                }
                if (uiState.totalUpcomingCountdownsCount > uiState.upcomingCountdowns.size) {
                    val remainingDates = uiState.totalUpcomingCountdownsCount - uiState.upcomingCountdowns.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenCountdowns)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+$remainingDates more upcoming dates",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "See all →",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.kansoColors.utilitiesAccent
                        )
                    }
                }
            }

            if (uiState.dueSoonIousCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenIousTab)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iouSummary = buildString {
                        append("${uiState.dueSoonIousCount} IOU(s) due soon")
                        append(" · Net ")
                        append(
                            CurrencyFormatter.formatOrMask(
                                uiState.openIouNetCents,
                                uiState.currency,
                                hidden = uiState.hideBalances,
                                showSign = true
                            )
                        )
                    }
                    Text(
                        text = iouSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "IOUs →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveFocusTimerCard(
    pomodoro: PomodoroState,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    val accent = if (pomodoro.mode == PomodoroMode.FOCUS) {
        MaterialTheme.kansoColors.utilitiesAccent
    } else {
        incomeSemanticColor()
    }
    val progress = if (pomodoro.totalSeconds > 0) {
        pomodoro.remainingSeconds.toFloat() / pomodoro.totalSeconds.toFloat()
    } else {
        0f
    }

    KansoBentoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = accent.copy(alpha = 0.08f),
        borderColor = accent.copy(alpha = 0.28f),
        contentPadding = PaddingValues(14.dp)
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
                        icon = Icons.Outlined.Timer,
                        accent = accent,
                        size = 36.dp,
                        iconSize = 18.dp,
                        cornerRadius = 10.dp
                    )
                    Column {
                        Text(
                            text = if (pomodoro.isRunning) {
                                "${pomodoro.mode.label} · Running"
                            } else {
                                "${pomodoro.mode.label} · Paused"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = accent
                        )
                        Text(
                            text = PomodoroCalculations.formatRemainingMmSs(pomodoro.remainingSeconds),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                KansoTonalButton(
                    text = if (pomodoro.isRunning) "Pause" else "Resume",
                    leadingIcon = if (pomodoro.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    size = KansoButtonSize.COMPACT,
                    containerColor = accent.copy(alpha = 0.16f),
                    contentColor = accent,
                    onClick = onToggle
                )
            }
            KansoAnimatedProgressBar(
                progress = progress.coerceIn(0f, 1f),
                height = 5.dp,
                color = accent
            )
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
            if (dueLabel != null || task.priority == TaskPriority.HIGH || task.recurrence != TaskRecurrence.NONE) {
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
                    if (task.recurrence != TaskRecurrence.NONE) {
                        KansoPillBadge(
                            text = task.recurrence.shortLabel,
                            color = accentColor,
                            leadingIcon = Icons.Outlined.Repeat
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
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    KansoBentoCard(
        modifier = modifier.width(186.dp),
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

@Composable
private fun coachToneColor(tone: CoachInsightTone): Color = when (tone) {
    CoachInsightTone.POSITIVE -> incomeSemanticColor()
    CoachInsightTone.NEUTRAL -> MaterialTheme.colorScheme.primary
    CoachInsightTone.WARNING -> warningSemanticColor()
    CoachInsightTone.CRITICAL -> expenseSemanticColor()
}

@Composable
private fun SmartCoachBentoCard(
    uiState: TodayUiState,
    onOpenMoney: () -> Unit,
    onOpenHabits: () -> Unit,
    onAddWater250: () -> Unit
) {
    val report = uiState.safeToSpend
    val statusColor = when (report?.paceStatus) {
        SpendPaceStatus.UNDER_PACE -> incomeSemanticColor()
        SpendPaceStatus.ON_TRACK -> MaterialTheme.kansoColors.moneyAccent
        SpendPaceStatus.WATCH_PACE -> warningSemanticColor()
        SpendPaceStatus.OVER_BUDGET -> expenseSemanticColor()
        else -> MaterialTheme.colorScheme.primary
    }

    KansoBentoCard(
        onClick = onOpenMoney,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        icon = Icons.Outlined.AutoAwesome,
                        accent = statusColor,
                        size = 34.dp,
                        iconSize = 18.dp,
                        cornerRadius = 10.dp
                    )
                    Text(
                        text = "Smart Coach",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (report != null) {
                    KansoPillBadge(
                        text = report.paceStatus.badgeLabel,
                        color = statusColor
                    )
                }
            }

            if (report != null && report.hasAnchor) {
                val isExceeded = report.todayRemainingAllowanceCents < 0L
                val usageRatio = if (report.dailySafeAllowanceCents > 0L) {
                    (report.todayExpenseCents.toFloat() / report.dailySafeAllowanceCents.toFloat()).coerceIn(0f, 1f)
                } else {
                    1f
                }
                val ringColor = if (isExceeded) expenseSemanticColor() else statusColor
                val anchorTag = if (report.anchorSource == BudgetAnchorSource.MONTHLY_INCOME) {
                    " · Income-based"
                } else {
                    ""
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (isExceeded) "TODAY'S ALLOWANCE EXCEEDED" else "SAFE TO SPEND TODAY",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isExceeded) expenseSemanticColor() else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isExceeded) {
                                Text(
                                    text = "Over by ${CurrencyFormatter.formatOrMask(-report.todayRemainingAllowanceCents, uiState.currency, hidden = uiState.hideBalances)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = expenseSemanticColor(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                KansoAnimatedCurrencyText(
                                    amountCents = report.todayRemainingAllowanceCents.coerceAtLeast(0L),
                                    currency = uiState.currency,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${CurrencyFormatter.formatOrMask(report.dailySafeAllowanceCents, uiState.currency, hidden = uiState.hideBalances)}/day · ${report.daysRemainingIncludingToday}d left$anchorTag",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        KansoProgressRing(
                            progress = usageRatio,
                            color = ringColor,
                            size = 44.dp,
                            strokeWidth = 5.dp
                        ) {
                            Text(
                                text = "${(usageRatio * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ringColor
                            )
                        }
                    }
                }
            }

            if (uiState.coachInsights.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.coachInsights.forEach { insight ->
                        val toneColor = coachToneColor(insight.tone)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(toneColor)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = insight.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = insight.body,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (insight.actionLabel != null && insight.actionTarget != null) {
                                KansoTonalButton(
                                    text = insight.actionLabel!!,
                                    size = KansoButtonSize.COMPACT,
                                    onClick = {
                                        when (insight.actionTarget) {
                                            CoachActionTarget.ADD_WATER_250 -> onAddWater250()
                                            CoachActionTarget.OPEN_HABITS -> onOpenHabits()
                                            CoachActionTarget.OPEN_MONEY_BUDGETS -> onOpenMoney()
                                            null -> Unit
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TodayScreenPreview() {
    val sampleReport = SmartCoachCalculations.computeSafeToSpend(
        targetMonth = java.time.YearMonth.of(2026, 11),
        today = LocalDate.of(2026, 11, 22),
        monthlyBudgetCents = 4_000_000_00L,
        monthIncomeCents = 8_500_000_00L,
        monthExpenseCents = 2_450_000_00L,
        todayExpenseCents = 65_000_00L,
        bills = listOf(
            RecurringBill(
                id = 1L,
                name = "Fiber Internet",
                amountCents = 385_000_00L,
                category = ExpenseCategory.BILLS,
                wallet = WalletType.BANK,
                dueDayOfMonth = 25
            )
        )
    )
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
                safeToSpend = sampleReport,
                coachInsights = listOf(
                    CoachInsight(
                        id = "bills_reserved",
                        title = "Rp 385.000 reserved for 1 unpaid bill",
                        body = "Already deducted from your Safe-to-Spend allowance.",
                        tone = CoachInsightTone.NEUTRAL
                    )
                ),
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
