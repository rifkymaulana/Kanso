package com.kanso.feature.brain

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SnippetFolder
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.model.NoteItem
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.SnippetMasker
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TaskPriority
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoEmptyState
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.core.ui.theme.KansoTheme
import java.time.LocalDate

@Composable
fun BrainRoute(
    initialTab: Int = 0,
    onShowMessage: (String) -> Unit,
    viewModel: BrainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    var editingNote by remember { mutableStateOf<NoteItem?>(null) }
    var showNewNoteDialog by remember { mutableStateOf(false) }

    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var showNewTaskDialog by remember { mutableStateOf(false) }

    var editingSnippet by remember { mutableStateOf<SnippetItem?>(null) }
    var showNewSnippetDialog by remember { mutableStateOf(false) }

    BrainScreenContent(
        uiState = uiState,
        initialTab = initialTab,
        onQueryChange = viewModel::setNoteSearchQuery,
        onSelectTag = viewModel::selectTag,
        onAddNote = { showNewNoteDialog = true },
        onEditNote = { editingNote = it },
        onTogglePinNote = viewModel::togglePinNote,
        onSelectTaskFilter = viewModel::setTaskFilter,
        onAddTask = { showNewTaskDialog = true },
        onEditTask = { editingTask = it },
        onToggleTask = viewModel::toggleTaskCompleted,
        onSelectSnippetCategory = viewModel::setSnippetCategory,
        onAddSnippet = { showNewSnippetDialog = true },
        onEditSnippet = { editingSnippet = it },
        onTogglePinSnippet = viewModel::togglePinSnippet,
        onCopySnippet = { snippet ->
            clipboard.setText(AnnotatedString(snippet.value))
            onShowMessage("Copied ${snippet.label}")
        }
    )

    if (showNewNoteDialog || editingNote != null) {
        val current = editingNote
        NoteEditorDialog(
            initial = current,
            onDismiss = {
                showNewNoteDialog = false
                editingNote = null
            },
            onSave = { title, content, tags, isPinned ->
                viewModel.saveNote(current, title, content, tags, isPinned)
                showNewNoteDialog = false
                editingNote = null
            },
            onDelete = current?.let { note ->
                {
                    viewModel.deleteNote(note.id)
                    editingNote = null
                    onShowMessage("Note deleted")
                }
            }
        )
    }

    if (showNewTaskDialog || editingTask != null) {
        val current = editingTask
        TaskEditorDialog(
            initial = current,
            onDismiss = {
                showNewTaskDialog = false
                editingTask = null
            },
            onSave = { title, note, priority, dueDay ->
                viewModel.saveTask(current, title, note, priority, dueDay)
                showNewTaskDialog = false
                editingTask = null
            },
            onDelete = current?.let { task ->
                {
                    viewModel.deleteTask(task.id)
                    editingTask = null
                    onShowMessage("Task deleted")
                }
            }
        )
    }

    if (showNewSnippetDialog || editingSnippet != null) {
        val current = editingSnippet
        SnippetEditorDialog(
            initial = current,
            onDismiss = {
                showNewSnippetDialog = false
                editingSnippet = null
            },
            onSave = { label, value, cat, sensitive, pinned ->
                viewModel.saveSnippet(current, label, value, cat, sensitive, pinned)
                showNewSnippetDialog = false
                editingSnippet = null
            },
            onDelete = current?.let { snippet ->
                {
                    viewModel.deleteSnippet(snippet.id)
                    editingSnippet = null
                    onShowMessage("Snippet deleted")
                }
            }
        )
    }
}

@Composable
fun BrainScreenContent(
    uiState: BrainUiState,
    initialTab: Int = 0,
    onQueryChange: (String) -> Unit,
    onSelectTag: (String?) -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (NoteItem) -> Unit,
    onTogglePinNote: (NoteItem) -> Unit,
    onSelectTaskFilter: (TaskFilter) -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onSelectSnippetCategory: (SnippetCategory?) -> Unit,
    onAddSnippet: () -> Unit,
    onEditSnippet: (SnippetItem) -> Unit,
    onTogglePinSnippet: (SnippetItem) -> Unit,
    onCopySnippet: (SnippetItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    LaunchedEffect(initialTab) {
        if (initialTab in 0..2) {
            selectedTab = initialTab
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Second Brain",
            style = MaterialTheme.typography.headlineMedium
        )

        KansoSegmentedRow(
            options = listOf("Notes", "Tasks (${uiState.activeTaskCount})", "Snippets"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )

        when (selectedTab) {
            0 -> NotesTabContent(
                uiState = uiState,
                onQueryChange = onQueryChange,
                onSelectTag = onSelectTag,
                onAddNote = onAddNote,
                onEditNote = onEditNote,
                onTogglePinNote = onTogglePinNote
            )

            1 -> TasksTabContent(
                uiState = uiState,
                onSelectTaskFilter = onSelectTaskFilter,
                onAddTask = onAddTask,
                onEditTask = onEditTask,
                onToggleTask = onToggleTask
            )

            else -> SnippetsTabContent(
                uiState = uiState,
                onSelectCategory = onSelectSnippetCategory,
                onAddSnippet = onAddSnippet,
                onEditSnippet = onEditSnippet,
                onTogglePinSnippet = onTogglePinSnippet,
                onCopySnippet = onCopySnippet
            )
        }
    }
}

@Composable
private fun NotesTabContent(
    uiState: BrainUiState,
    onQueryChange: (String) -> Unit,
    onSelectTag: (String?) -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (NoteItem) -> Unit,
    onTogglePinNote: (NoteItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.noteSearchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search notes (FTS)...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.noteSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Button(onClick = onAddNote) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" New")
                }
            }
        }

        if (uiState.availableTags.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.selectedTag == null,
                            onClick = { onSelectTag(null) },
                            label = { Text("All") }
                        )
                    }
                    items(uiState.availableTags, key = { it }) { tag ->
                        FilterChip(
                            selected = uiState.selectedTag.equals(tag, ignoreCase = true),
                            onClick = { onSelectTag(tag) },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }
        }

        if (uiState.notes.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.Description,
                    title = if (uiState.noteSearchQuery.isNotBlank()) "No matching notes" else "Your brain inbox is empty",
                    subtitle = "Save ideas, reading notes, or quick reference docs with instant full-text search.",
                    actionLabel = "Create Note",
                    onAction = onAddNote
                )
            }
        } else {
            items(uiState.notes, key = { it.id }) { note ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onEditNote(note) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            IconButton(
                                onClick = { onTogglePinNote(note) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (note.isPinned) "Unpin" else "Pin",
                                    tint = if (note.isPinned) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (note.content.isNotBlank()) {
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                note.tags.take(3).forEach { tag ->
                                    AssistChip(
                                        onClick = { onSelectTag(tag) },
                                        label = { Text("#$tag", style = MaterialTheme.typography.labelMedium) }
                                    )
                                }
                            }
                            Text(
                                text = KansoDateFormatters.formatShortDate(note.updatedAtMillis),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksTabContent(
    uiState: BrainUiState,
    onSelectTaskFilter: (TaskFilter) -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onToggleTask: (TaskItem) -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.taskFilter == filter,
                            onClick = { onSelectTaskFilter(filter) },
                            label = { Text(filter.label) }
                        )
                    }
                }
                Button(onClick = onAddTask) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add")
                }
            }
        }

        if (uiState.tasks.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.TaskAlt,
                    title = "No ${uiState.taskFilter.label.lowercase()} tasks",
                    subtitle = "Break bigger goals into small checkable actions.",
                    actionLabel = "Add Task",
                    onAction = onAddTask
                )
            }
        } else {
            items(uiState.tasks, key = { it.id }) { task ->
                val dueDay = task.dueDateEpochDay
                val isOverdue = !task.isCompleted && dueDay != null && dueDay < todayEpoch
                val dueLabel = KansoDateFormatters.formatEpochDayLabel(dueDay)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onEditTask(task) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleTask(task) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (task.isCompleted) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (task.note.isNotBlank()) {
                                Text(
                                    text = task.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (dueLabel != null || task.priority != TaskPriority.MEDIUM) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            text = "High priority",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    } else if (task.priority == TaskPriority.LOW) {
                                        Text(
                                            text = "Low",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun SnippetsTabContent(
    uiState: BrainUiState,
    onSelectCategory: (SnippetCategory?) -> Unit,
    onAddSnippet: () -> Unit,
    onEditSnippet: (SnippetItem) -> Unit,
    onTogglePinSnippet: (SnippetItem) -> Unit,
    onCopySnippet: (SnippetItem) -> Unit
) {
    val revealedIds = remember { mutableStateMapOf<Long, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1-Tap Copy Snippets", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onAddSnippet) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add")
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = uiState.snippetCategoryFilter == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("All") }
                    )
                }
                items(SnippetCategory.entries, key = { it.name }) { category ->
                    FilterChip(
                        selected = uiState.snippetCategoryFilter == category,
                        onClick = {
                            onSelectCategory(
                                if (uiState.snippetCategoryFilter == category) null else category
                            )
                        },
                        label = { Text(category.label) }
                    )
                }
            }
        }

        if (uiState.snippets.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.SnippetFolder,
                    title = "No snippets saved",
                    subtitle = "Store addresses, bank numbers, tax IDs, or email templates and copy them in 1 tap.",
                    actionLabel = "Add Snippet",
                    onAction = onAddSnippet
                )
            }
        } else {
            items(uiState.snippets, key = { it.id }) { snippet ->
                val isRevealed = revealedIds[snippet.id] == true
                val displayValue = if (snippet.isSensitive && !isRevealed) {
                    SnippetMasker.mask(snippet.value)
                } else {
                    snippet.value
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onCopySnippet(snippet) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = snippet.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = snippet.category.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = displayValue,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (snippet.isSensitive) {
                            IconButton(
                                onClick = { revealedIds[snippet.id] = !isRevealed }
                            ) {
                                Icon(
                                    imageVector = if (isRevealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (isRevealed) "Hide" else "Reveal"
                                )
                            }
                        }

                        IconButton(onClick = { onTogglePinSnippet(snippet) }) {
                            Icon(
                                imageVector = if (snippet.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin",
                                tint = if (snippet.isPinned) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        IconButton(onClick = { onEditSnippet(snippet) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                        }

                        IconButton(onClick = { onCopySnippet(snippet) }) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BrainScreenPreview() {
    KansoTheme {
        BrainScreenContent(
            uiState = BrainUiState(
                availableTags = listOf("personal", "work"),
                notes = listOf(
                    NoteItem(
                        id = 1L,
                        title = "Personal super app roadmap",
                        content = "Milestone 1: Money + Second Brain + Encrypted Backup",
                        tags = listOf("personal"),
                        isPinned = true,
                        createdAtMillis = 1L,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                ),
                activeTaskCount = 2
            ),
            onQueryChange = {},
            onSelectTag = {},
            onAddNote = {},
            onEditNote = {},
            onTogglePinNote = {},
            onSelectTaskFilter = {},
            onAddTask = {},
            onEditTask = {},
            onToggleTask = {},
            onSelectSnippetCategory = {},
            onAddSnippet = {},
            onEditSnippet = {},
            onTogglePinSnippet = {},
            onCopySnippet = {}
        )
    }
}
