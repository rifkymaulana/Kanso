package com.kanso.feature.brain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kanso.core.model.NoteItem
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TaskPriority
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoDatePickerDialog
import com.kanso.core.ui.components.KansoFormBottomSheet
import com.kanso.core.ui.components.KansoOutlinedButton
import com.kanso.core.ui.theme.kansoColors
import java.time.LocalDate

@Composable
fun NoteEditorDialog(
    initial: NoteItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, tags: List<String>, isPinned: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember { mutableStateOf(initial?.content.orEmpty()) }
    var tagsText by remember { mutableStateOf(initial?.tags?.joinToString(", ").orEmpty()) }
    var isPinned by remember { mutableStateOf(initial?.isPinned ?: false) }
    var error by remember { mutableStateOf<String?>(null) }
    val brainAccent = MaterialTheme.kansoColors.brainAccent

    KansoFormBottomSheet(
        title = if (initial == null) "New Note" else "Edit Note",
        onDismiss = onDismiss,
        primaryLabel = if (initial == null) "Save Note" else "Update Note",
        primaryColor = brainAccent,
        badgeText = if (isPinned) "Pinned" else "Note",
        badgeColor = brainAccent,
        badgeIcon = Icons.Outlined.Description,
        secondaryLabel = if (onDelete != null) "Delete" else null,
        onSecondaryClick = onDelete,
        errorText = error,
        onPrimaryClick = {
            if (title.isBlank() && content.isBlank()) {
                error = "Enter a title or note content."
            } else {
                val resolvedTitle = title.trim().ifEmpty {
                    content.trim().lineSequence().first().take(40)
                }
                val tags = tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                onSave(resolvedTitle, content, tags, isPinned)
            }
        }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        OutlinedTextField(
            value = content,
            onValueChange = {
                content = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Content") },
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        OutlinedTextField(
            value = tagsText,
            onValueChange = { tagsText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tags (comma separated)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pin note", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isPinned, onCheckedChange = { isPinned = it })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorDialog(
    initial: TaskItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, priority: TaskPriority, dueDateEpochDay: Long?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var priority by remember { mutableStateOf(initial?.priority ?: TaskPriority.MEDIUM) }
    val today = remember { LocalDate.now() }
    val initialDueChoice = remember(initial, today) {
        val due = initial?.dueDateEpochDay
        val todayEpoch = today.toEpochDay()
        when (due) {
            null -> 4
            todayEpoch -> 0
            todayEpoch + 1L -> 1
            todayEpoch + 7L -> 2
            else -> 3
        }
    }
    var dueChoice by remember { mutableIntStateOf(initialDueChoice) }
    var customDueEpochDay by remember {
        mutableLongStateOf(initial?.dueDateEpochDay ?: today.plusDays(3).toEpochDay())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val taskAccent = Color(0xFF8B5CF6)

    KansoFormBottomSheet(
        title = if (initial == null) "New Task" else "Edit Task",
        onDismiss = onDismiss,
        primaryLabel = if (initial == null) "Save Task" else "Update Task",
        primaryColor = taskAccent,
        badgeText = priority.label,
        badgeColor = taskAccent,
        badgeIcon = Icons.Outlined.CheckCircle,
        secondaryLabel = if (onDelete != null) "Delete" else null,
        onSecondaryClick = onDelete,
        errorText = error,
        onPrimaryClick = {
            if (title.isBlank()) {
                error = "Enter a task title."
            } else {
                val dueDay = when (dueChoice) {
                    0 -> today.toEpochDay()
                    1 -> today.plusDays(1).toEpochDay()
                    2 -> today.plusDays(7).toEpochDay()
                    3 -> customDueEpochDay
                    else -> null
                }
                onSave(title, note, priority, dueDay)
            }
        }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Task") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Details (optional)") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        Text("Priority", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskPriority.entries.forEach { p ->
                FilterChip(
                    selected = priority == p,
                    onClick = { priority = p },
                    label = { Text(p.label) }
                )
            }
        }
        Text("Due date", style = MaterialTheme.typography.labelLarge)
        val labels = listOf("Today", "Tomorrow", "Next Week", "Pick Date", "None")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { idx, label ->
                val chipLabel = if (idx == 3 && dueChoice == 3) {
                    KansoDateFormatters.formatFullDate(customDueEpochDay)
                } else {
                    label
                }
                FilterChip(
                    selected = dueChoice == idx,
                    onClick = {
                        if (idx == 3) {
                            showDatePicker = true
                        }
                        dueChoice = idx
                    },
                    label = { Text(chipLabel) }
                )
            }
        }
        if (dueChoice == 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Due on ${KansoDateFormatters.formatFullDate(customDueEpochDay)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                KansoOutlinedButton(
                    text = "Change Date",
                    size = KansoButtonSize.COMPACT,
                    onClick = { showDatePicker = true }
                )
            }
        }
    }

    if (showDatePicker) {
        KansoDatePickerDialog(
            initialEpochDay = customDueEpochDay,
            onSelect = {
                customDueEpochDay = it
                dueChoice = 3
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SnippetEditorDialog(
    initial: SnippetItem?,
    onDismiss: () -> Unit,
    onSave: (label: String, value: String, category: SnippetCategory, isSensitive: Boolean, isPinned: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var value by remember { mutableStateOf(initial?.value.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: SnippetCategory.OTHER) }
    var isSensitive by remember { mutableStateOf(initial?.isSensitive ?: false) }
    var isPinned by remember { mutableStateOf(initial?.isPinned ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    KansoFormBottomSheet(
        title = if (initial == null) "New Snippet" else "Edit Snippet",
        onDismiss = onDismiss,
        primaryLabel = if (initial == null) "Save Snippet" else "Update Snippet",
        primaryColor = MaterialTheme.colorScheme.primary,
        badgeText = category.label,
        badgeColor = MaterialTheme.colorScheme.primary,
        badgeIcon = Icons.Outlined.ContentPaste,
        secondaryLabel = if (onDelete != null) "Delete" else null,
        onSecondaryClick = onDelete,
        errorText = error,
        onPrimaryClick = {
            if (label.isBlank() || value.isBlank()) {
                error = "Both label and value are required."
            } else {
                onSave(label, value, category, isSensitive, isPinned)
            }
        }
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {
                label = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Label") },
            placeholder = { Text("Home Address, Tax ID, Bank Account") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Value") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp)
        )
        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SnippetCategory.entries.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat.label) }
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mask value until revealed", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isSensitive, onCheckedChange = { isSensitive = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pin to Today screen", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
            }
        }
    }
}
