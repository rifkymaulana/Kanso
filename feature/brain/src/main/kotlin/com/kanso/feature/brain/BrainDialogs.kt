package com.kanso.feature.brain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kanso.core.model.NoteItem
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TaskPriority
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Note" else "Edit Note") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tags (comma separated)") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pin note", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank() && content.isBlank()) {
                    error = "Enter a title or note content."
                } else {
                    val resolvedTitle = title.trim().ifEmpty {
                        content.trim().lineSequence().first().take(40)
                    }
                    val tags = tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(resolvedTitle, content, tags, isPinned)
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
    val initialDueChoice = remember(initial) {
        val due = initial?.dueDateEpochDay
        val today = LocalDate.now().toEpochDay()
        when (due) {
            null -> 3
            today -> 0
            today + 1 -> 1
            else -> 2
        }
    }
    var dueChoice by remember { mutableIntStateOf(initialDueChoice) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Task" else "Edit Task") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Details (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) }
                        )
                    }
                }
                Text("Due date", style = MaterialTheme.typography.labelLarge)
                val labels = listOf("Today", "Tomorrow", "Next Week", "None")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    labels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = dueChoice == idx,
                            onClick = { dueChoice = idx },
                            label = { Text(label) }
                        )
                    }
                }
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) {
                    error = "Enter a task title."
                } else {
                    val today = LocalDate.now()
                    val dueDay = when (dueChoice) {
                        0 -> today.toEpochDay()
                        1 -> today.plusDays(1).toEpochDay()
                        2 -> if (initial != null && initialDueChoice == 2) initial.dueDateEpochDay else today.plusDays(7).toEpochDay()
                        else -> null
                    }
                    onSave(title, note, priority, dueDay)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Snippet" else "Edit Snippet") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    maxLines = 4
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SnippetCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
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
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (label.isBlank() || value.isBlank()) {
                    error = "Both label and value are required."
                } else {
                    onSave(label, value, category, isSensitive, isPinned)
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
