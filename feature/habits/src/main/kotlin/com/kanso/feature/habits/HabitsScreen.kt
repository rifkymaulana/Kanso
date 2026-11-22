package com.kanso.feature.habits

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.model.DailyHealthLog
import com.kanso.core.model.HabitItem
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoCheckCircle
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoEmptyState
import com.kanso.core.ui.components.KansoOutlinedButton
import com.kanso.core.ui.components.KansoPillBadge
import com.kanso.core.ui.components.KansoPrimaryButton
import com.kanso.core.ui.components.KansoProgressRing
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.core.ui.components.KansoSmallIconButton
import com.kanso.core.ui.components.KansoTonalButton
import com.kanso.core.ui.components.warningSemanticColor
import com.kanso.core.ui.theme.KansoTheme
import com.kanso.core.ui.theme.kansoColors
import java.time.LocalDate
import java.util.Locale

@Composable
fun HabitsRoute(
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showNewHabitDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitItem?>(null) }
    var heatmapHabitId by remember { mutableStateOf<Long?>(null) }

    HabitsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onAddHabit = { showNewHabitDialog = true },
        onEditHabit = { editingHabit = it },
        onOpenHeatmap = { heatmapHabitId = it.habit.id },
        onToggleHabitToday = viewModel::toggleHabitForToday,
        onPreviousDay = viewModel::previousDay,
        onNextDay = viewModel::nextDay,
        onAdjustWater = viewModel::adjustWater,
        onSaveHealthDetails = { sleep, weight, mood, workout ->
            viewModel.saveHealthDetails(sleep, weight, mood, workout)
            onShowMessage("Saved health log")
        }
    )

    if (showNewHabitDialog || editingHabit != null) {
        val current = editingHabit
        HabitEditorDialog(
            initial = current,
            onDismiss = {
                showNewHabitDialog = false
                editingHabit = null
            },
            onSave = { name, targetDays, hour, min ->
                viewModel.saveHabit(current, name, targetDays, hour, min)
                showNewHabitDialog = false
                editingHabit = null
            },
            onDelete = current?.let { habit ->
                {
                    viewModel.deleteHabit(habit.id)
                    editingHabit = null
                    onShowMessage("Habit deleted")
                }
            }
        )
    }

    heatmapHabitId?.let { targetId ->
        val model = uiState.habits.firstOrNull { it.habit.id == targetId }
        if (model != null) {
            HabitHeatmapDialog(
                model = model,
                onToggleDay = { day -> viewModel.toggleHabitForDay(targetId, day) },
                onDismiss = { heatmapHabitId = null }
            )
        } else {
            heatmapHabitId = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreenContent(
    uiState: HabitsUiState,
    onBack: () -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (HabitItem) -> Unit,
    onOpenHeatmap: (HabitRowUiModel) -> Unit,
    onToggleHabitToday: (Long) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onAdjustWater: (Int) -> Unit,
    onSaveHealthDetails: (sleepHours: Float?, weightKg: Float?, mood: Int?, workoutNote: String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Habits & Health", style = MaterialTheme.typography.titleLarge) },
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
            KansoSegmentedRow(
                options = listOf("Habits", "Health", "Insights"),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it }
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "habitsTab"
            ) { tab ->
                when (tab) {
                    0 -> HabitsTabContent(
                        uiState = uiState,
                        onAddHabit = onAddHabit,
                        onEditHabit = onEditHabit,
                        onOpenHeatmap = onOpenHeatmap,
                        onToggleHabitToday = onToggleHabitToday
                    )

                    1 -> HealthTabContent(
                        uiState = uiState,
                        onPreviousDay = onPreviousDay,
                        onNextDay = onNextDay,
                        onAdjustWater = onAdjustWater,
                        onSaveHealthDetails = onSaveHealthDetails
                    )

                    else -> InsightsTabContent(uiState = uiState)
                }
            }
        }
    }
}

@Composable
private fun HabitsTabContent(
    uiState: HabitsUiState,
    onAddHabit: () -> Unit,
    onEditHabit: (HabitItem) -> Unit,
    onOpenHeatmap: (HabitRowUiModel) -> Unit,
    onToggleHabitToday: (Long) -> Unit
) {
    val habitsAccent = MaterialTheme.kansoColors.habitsAccent
    val ratio = if (uiState.totalHabitsCount > 0) {
        (uiState.completedTodayCount.toFloat() / uiState.totalHabitsCount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            KansoBentoCard(contentPadding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KansoProgressRing(
                        progress = ratio,
                        color = habitsAccent,
                        size = 64.dp,
                        strokeWidth = 6.dp
                    ) {
                        Text(
                            text = "${(ratio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = habitsAccent
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Today's Habits", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${uiState.completedTodayCount} of ${uiState.totalHabitsCount} completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    KansoTonalButton(
                        text = "Add",
                        leadingIcon = Icons.Outlined.Add,
                        size = KansoButtonSize.COMPACT,
                        onClick = onAddHabit
                    )
                }
            }
        }

        if (uiState.habits.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.CheckCircle,
                    title = "No habits yet",
                    subtitle = "Start with 1 or 2 small daily habits like hydration, reading, or a short walk.",
                    actionLabel = "Add Habit",
                    onAction = onAddHabit
                )
            }
        } else {
            items(uiState.habits, key = { it.habit.id }) { row ->
                KansoBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenHeatmap(row) },
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KansoCheckCircle(
                            checked = row.isCompletedToday,
                            accentColor = habitsAccent,
                            onToggle = { onToggleHabitToday(row.habit.id) }
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = row.habit.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KansoPillBadge(
                                    text = "${row.currentStreak}d streak",
                                    color = warningSemanticColor(),
                                    leadingIcon = Icons.Outlined.LocalFireDepartment
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    row.last30DaysGrid.takeLast(7).forEach { (_, done) ->
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    if (done) habitsAccent else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        KansoSmallIconButton(
                            icon = Icons.Outlined.Edit,
                            contentDescription = "Edit Habit",
                            onClick = { onEditHabit(row.habit) }
                        )
                    }
                }
            }
        }
    }
}

private data class MoodOption(
    val score: Int,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun HealthTabContent(
    uiState: HabitsUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onAdjustWater: (Int) -> Unit,
    onSaveHealthDetails: (sleepHours: Float?, weightKg: Float?, mood: Int?, workoutNote: String) -> Unit
) {
    val log = uiState.selectedHealthLog
    var sleepText by remember(uiState.selectedDate, log) {
        mutableStateOf(log?.sleepHours?.let { String.format(Locale.US, "%.1f", it) }.orEmpty())
    }
    var weightText by remember(uiState.selectedDate, log) {
        mutableStateOf(log?.weightKg?.let { String.format(Locale.US, "%.1f", it) }.orEmpty())
    }
    var selectedMood by remember(uiState.selectedDate, log) { mutableStateOf(log?.mood) }
    var workoutNote by remember(uiState.selectedDate, log) { mutableStateOf(log?.workoutNote.orEmpty()) }

    LaunchedEffect(uiState.selectedDate, log?.updatedAtMillis) {
        sleepText = log?.sleepHours?.let { String.format(Locale.US, "%.1f", it) }.orEmpty()
        weightText = log?.weightKg?.let { String.format(Locale.US, "%.1f", it) }.orEmpty()
        selectedMood = log?.mood
        workoutNote = log?.workoutNote.orEmpty()
    }

    val haptic = LocalHapticFeedback.current
    val kansoColors = MaterialTheme.kansoColors
    val hydrationAccent = kansoColors.hydrationAccent

    val moodOptions = remember(kansoColors) {
        listOf(
            MoodOption(1, "Low", Icons.Outlined.SentimentVeryDissatisfied, kansoColors.expense),
            MoodOption(2, "Tired", Icons.Outlined.SentimentDissatisfied, kansoColors.warning),
            MoodOption(3, "Okay", Icons.Outlined.SentimentNeutral, Color(0xFF64748B)),
            MoodOption(4, "Good", Icons.Outlined.SentimentSatisfied, kansoColors.hydrationAccent),
            MoodOption(5, "Great", Icons.Outlined.SentimentVerySatisfied, kansoColors.income)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onPreviousDay),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous day")
                        }
                        Text(
                            text = uiState.selectedDateLabel,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !uiState.isSelectedDateToday, onClick = onNextDay),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = "Next day",
                                tint = if (uiState.isSelectedDateToday) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            val waterMl = log?.waterMl ?: 0
            val goal = uiState.waterGoalMl.coerceAtLeast(250)
            val progress = (waterMl.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
            KansoBentoCard(contentPadding = PaddingValues(18.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = hydrationAccent)
                            Text("Hydration", style = MaterialTheme.typography.titleMedium)
                        }
                        KansoPillBadge(text = "${(progress * 100).toInt()}% of goal", color = hydrationAccent)
                    }

                    KansoProgressRing(
                        progress = progress,
                        color = hydrationAccent,
                        size = 124.dp,
                        strokeWidth = 10.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$waterMl",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "/ $goal ml",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KansoOutlinedButton(
                            text = "-250 ml",
                            size = KansoButtonSize.COMPACT,
                            modifier = Modifier.weight(1f),
                            enabled = waterMl >= 250,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAdjustWater(-250)
                            }
                        )
                        KansoTonalButton(
                            text = "+250 ml",
                            size = KansoButtonSize.COMPACT,
                            modifier = Modifier.weight(1f),
                            containerColor = hydrationAccent.copy(alpha = 0.14f),
                            contentColor = hydrationAccent,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAdjustWater(250)
                            }
                        )
                        KansoPrimaryButton(
                            text = "+500 ml",
                            size = KansoButtonSize.COMPACT,
                            modifier = Modifier.weight(1f),
                            containerColor = hydrationAccent,
                            contentColor = Color.White,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAdjustWater(500)
                            }
                        )
                    }
                }
            }
        }

        item {
            KansoBentoCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Daily Check-In", style = MaterialTheme.typography.titleMedium)

                    Text("Mood", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        moodOptions.forEach { mood ->
                            val selected = selectedMood == mood.score
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedMood = if (selected) null else mood.score
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) mood.color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) mood.color else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = mood.icon,
                                        contentDescription = mood.label,
                                        tint = if (selected) mood.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = mood.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) mood.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = sleepText,
                            onValueChange = { sleepText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Sleep (hrs)") },
                            placeholder = { Text("7.5") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Weight (kg)") },
                            placeholder = { Text("68.0") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }

                    OutlinedTextField(
                        value = workoutNote,
                        onValueChange = { workoutNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Workout / activity note") },
                        placeholder = { Text("30m run, gym upper body...") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    KansoPrimaryButton(
                        text = "Save Daily Check-In",
                        size = KansoButtonSize.LARGE,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSaveHealthDetails(
                                sleepText.trim().replace(',', '.').toFloatOrNull(),
                                weightText.trim().replace(',', '.').toFloatOrNull(),
                                selectedMood,
                                workoutNote
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsTabContent(uiState: HabitsUiState) {
    val kansoColors = MaterialTheme.kansoColors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "7-DAY AVERAGES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InsightKpiTile(
                    label = "Water",
                    value = "${uiState.avgWaterMl7d} ml",
                    accent = kansoColors.hydrationAccent,
                    modifier = Modifier.weight(1f)
                )
                InsightKpiTile(
                    label = "Sleep",
                    value = uiState.avgSleepHours7d?.let { String.format(Locale.US, "%.1fh", it) } ?: "—",
                    accent = kansoColors.brainAccent,
                    modifier = Modifier.weight(1f)
                )
                InsightKpiTile(
                    label = "Mood",
                    value = uiState.avgMood7d?.let { String.format(Locale.US, "%.1f/5", it) } ?: "—",
                    accent = kansoColors.warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text("Recent Health Logs", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        if (uiState.recentHealthLogs.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = "No health logs yet",
                    subtitle = "Track water, sleep, mood, or workouts in the Health tab."
                )
            }
        } else {
            items(uiState.recentHealthLogs, key = { it.epochDay }) { item ->
                KansoBentoCard(
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = KansoDateFormatters.formatEpochDayLabel(item.epochDay)
                                    ?: KansoDateFormatters.formatFullDate(item.epochDay),
                                style = MaterialTheme.typography.titleMedium
                            )
                            KansoPillBadge(
                                text = "${item.waterMl} ml water",
                                color = kansoColors.hydrationAccent
                            )
                        }
                        val details = buildList {
                            item.sleepHours?.let { add("Sleep ${String.format(Locale.US, "%.1f", it)}h") }
                            item.weightKg?.let { add("Weight ${String.format(Locale.US, "%.1f", it)} kg") }
                            item.mood?.let { add("Mood $it/5") }
                        }.joinToString(" · ")
                        if (details.isNotEmpty()) {
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.workoutNote.isNotBlank()) {
                            Text(
                                text = item.workoutNote,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightKpiTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    KansoBentoCard(
        modifier = modifier,
        containerColor = accent.copy(alpha = 0.08f),
        borderColor = accent.copy(alpha = 0.25f),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(12.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HabitsScreenPreview() {
    KansoTheme {
        val today = LocalDate.now().toEpochDay()
        HabitsScreenContent(
            uiState = HabitsUiState(
                habits = listOf(
                    HabitRowUiModel(
                        habit = HabitItem(id = 1L, name = "Morning Walk 20m", createdAtMillis = 1L),
                        isCompletedToday = true,
                        currentStreak = 6,
                        longestStreak = 12,
                        last30DaysGrid = (0L..29L).map { (today - 29 + it) to (it % 2L == 0L) },
                        rate30d = 0.5f
                    )
                ),
                completedTodayCount = 1,
                totalHabitsCount = 1,
                selectedHealthLog = DailyHealthLog(epochDay = today, waterMl = 1250, updatedAtMillis = 1L)
            ),
            onBack = {},
            onAddHabit = {},
            onEditHabit = {},
            onOpenHeatmap = {},
            onToggleHabitToday = {},
            onPreviousDay = {},
            onNextDay = {},
            onAdjustWater = {},
            onSaveHealthDetails = { _, _, _, _ -> }
        )
    }
}
