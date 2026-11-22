package com.kanso.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.model.DailyHealthLog
import com.kanso.core.model.HabitItem
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoEmptyState
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.core.ui.theme.KansoTheme
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Habits & Health") },
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

            when (selectedTab) {
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

@Composable
private fun HabitsTabContent(
    uiState: HabitsUiState,
    onAddHabit: () -> Unit,
    onEditHabit: (HabitItem) -> Unit,
    onOpenHeatmap: (HabitRowUiModel) -> Unit,
    onToggleHabitToday: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Today's Habits", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${uiState.completedTodayCount} of ${uiState.totalHabitsCount} completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = onAddHabit) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" Add")
                        }
                    }
                    if (uiState.totalHabitsCount > 0) {
                        val ratio = uiState.completedTodayCount.toFloat() / uiState.totalHabitsCount.toFloat()
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenHeatmap(row) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = row.isCompletedToday,
                            onCheckedChange = { onToggleHabitToday(row.habit.id) }
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = row.habit.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocalFireDepartment,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "${row.currentStreak}d streak",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                // Last 7 days mini dots
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    row.last30DaysGrid.takeLast(7).forEach { (_, done) ->
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (done) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { onEditHabit(row.habit) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit Habit")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousDay) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous day")
                }
                Text(uiState.selectedDateLabel, style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = onNextDay,
                    enabled = !uiState.isSelectedDateToday
                ) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Next day")
                }
            }
        }

        item {
            val waterMl = log?.waterMl ?: 0
            val goal = uiState.waterGoalMl.coerceAtLeast(250)
            val progress = (waterMl.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
            Card(
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Hydration", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "$waterMl / $goal ml",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAdjustWater(-250) },
                            modifier = Modifier.weight(1f),
                            enabled = waterMl >= 250
                        ) {
                            Text("-250 ml")
                        }
                        FilledTonalButton(
                            onClick = { onAdjustWater(250) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+250 ml")
                        }
                        Button(
                            onClick = { onAdjustWater(500) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+500 ml")
                        }
                    }
                }
            }
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
                    Text("Daily Check-In", style = MaterialTheme.typography.titleMedium)

                    Text("Mood", style = MaterialTheme.typography.labelLarge)
                    val moods = listOf(1 to "Low", 2 to "Tired", 3 to "Okay", 4 to "Good", 5 to "Great")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        moods.forEach { (score, label) ->
                            FilterChip(
                                selected = selectedMood == score,
                                onClick = { selectedMood = if (selectedMood == score) null else score },
                                label = { Text("$score · $label") }
                            )
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Weight (kg)") },
                            placeholder = { Text("68.0") },
                            singleLine = true,
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
                        keyboardOptions = KeyboardCapitalization.Sentences.let {
                            KeyboardOptions(capitalization = it)
                        }
                    )

                    Button(
                        onClick = {
                            onSaveHealthDetails(
                                sleepText.trim().replace(',', '.').toFloatOrNull(),
                                weightText.trim().replace(',', '.').toFloatOrNull(),
                                selectedMood,
                                workoutNote
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Daily Check-In")
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsTabContent(uiState: HabitsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("7-Day Averages", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InsightStat("Water", "${uiState.avgWaterMl7d} ml")
                        InsightStat(
                            "Sleep",
                            uiState.avgSleepHours7d?.let { String.format(Locale.US, "%.1fh", it) } ?: "—"
                        )
                        InsightStat(
                            "Mood",
                            uiState.avgMood7d?.let { String.format(Locale.US, "%.1f/5", it) } ?: "—"
                        )
                    }
                }
            }
        }

        item {
            Text("Recent Health Logs", style = MaterialTheme.typography.titleMedium)
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
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = KansoDateFormatters.formatEpochDayLabel(item.epochDay)
                                    ?: KansoDateFormatters.formatFullDate(item.epochDay),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${item.waterMl} ml water",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val details = buildList {
                            item.sleepHours?.let { add("Sleep: ${String.format(Locale.US, "%.1f", it)}h") }
                            item.weightKg?.let { add("Weight: ${String.format(Locale.US, "%.1f", it)} kg") }
                            item.mood?.let { add("Mood: $it/5") }
                        }.joinToString(" • ")
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
private fun InsightStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
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
