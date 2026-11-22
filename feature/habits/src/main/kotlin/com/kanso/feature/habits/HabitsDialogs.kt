package com.kanso.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kanso.core.model.HabitItem
import com.kanso.core.ui.components.KansoDateFormatters
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitEditorDialog(
    initial: HabitItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, targetDaysPerWeek: Int, reminderHour: Int?, reminderMinute: Int?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var targetDays by remember { mutableIntStateOf(initial?.targetDaysPerWeek ?: 7) }
    var hasReminder by remember { mutableStateOf(initial?.reminderHour != null) }
    var reminderHour by remember { mutableIntStateOf(initial?.reminderHour ?: 20) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Habit" else "Edit Habit") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Habit name") },
                    placeholder = { Text("Read 20 pages, Walk 8k steps, Meditate") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Text("Weekly target (days)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { days ->
                        FilterChip(
                            selected = targetDays == days,
                            onClick = { targetDays = days },
                            label = { Text("$days") }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Include in daily reminders", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hasReminder, onCheckedChange = { hasReminder = it })
                }
                if (hasReminder) {
                    Text("Preferred reminder hour", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(7, 9, 12, 18, 20, 21).forEach { hr ->
                            FilterChip(
                                selected = reminderHour == hr,
                                onClick = { reminderHour = hr },
                                label = { Text("${hr.toString().padStart(2, '0')}:00") }
                            )
                        }
                    }
                }
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    error = "Enter a habit name."
                } else {
                    onSave(
                        name,
                        targetDays,
                        if (hasReminder) reminderHour else null,
                        if (hasReminder) 0 else null
                    )
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun HabitHeatmapDialog(
    model: HabitRowUiModel,
    onToggleDay: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(model.habit.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricPill("Streak", "${model.currentStreak}d")
                    MetricPill("Best", "${model.longestStreak}d")
                    MetricPill("30d Rate", "${(model.rate30d * 100).toInt()}%")
                }

                Text(
                    text = "Last 30 Days (tap any day to toggle)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    model.last30DaysGrid.chunked(6).forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowDays.forEach { (epochDay, completed) ->
                                val dayOfMonth = LocalDate.ofEpochDay(epochDay).dayOfMonth
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (completed) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .clickable { onToggleDay(epochDay) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (completed) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Target: ${model.habit.targetDaysPerWeek} days / week • Created ${
                        KansoDateFormatters.formatShortDate(model.habit.createdAtMillis)
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun MetricPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
