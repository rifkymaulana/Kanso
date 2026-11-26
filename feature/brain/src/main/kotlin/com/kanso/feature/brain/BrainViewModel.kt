package com.kanso.feature.brain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.repository.BrainRepository
import com.kanso.core.model.NoteItem
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TaskPriority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class TaskFilter(val label: String) {
    ACTIVE("Active"),
    TODAY("Today"),
    COMPLETED("Completed")
}

data class BrainUiState(
    val noteSearchQuery: String = "",
    val selectedTag: String? = null,
    val availableTags: List<String> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val taskFilter: TaskFilter = TaskFilter.ACTIVE,
    val tasks: List<TaskItem> = emptyList(),
    val activeTaskCount: Int = 0,
    val snippetCategoryFilter: SnippetCategory? = null,
    val snippets: List<SnippetItem> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrainViewModel @Inject constructor(
    private val brainRepository: BrainRepository
) : ViewModel() {

    private val noteQueryFlow = MutableStateFlow("")
    private val selectedTagFlow = MutableStateFlow<String?>(null)
    private val taskFilterFlow = MutableStateFlow(TaskFilter.ACTIVE)
    private val snippetCategoryFlow = MutableStateFlow<SnippetCategory?>(null)

    private val searchedNotesFlow = noteQueryFlow.flatMapLatest { query ->
        brainRepository.searchNotes(query)
    }

    private val notesCombinedFlow = combine(
        noteQueryFlow,
        selectedTagFlow,
        brainRepository.observeNotes(),
        searchedNotesFlow
    ) { query, selectedTag, allNotes, matchedNotes ->
        val tags = allNotes.flatMap { it.tags }.distinct().sorted()
        val filtered = if (selectedTag == null) {
            matchedNotes
        } else {
            matchedNotes.filter { note -> note.tags.any { it.equals(selectedTag, ignoreCase = true) } }
        }
        Triple(query, selectedTag, Pair(tags, filtered))
    }

    private val tasksCombinedFlow = combine(
        taskFilterFlow,
        brainRepository.observeTasks()
    ) { filter, allTasks ->
        val todayEpoch = LocalDate.now().toEpochDay()
        val activeCount = allTasks.count { !it.isCompleted }
        val filtered = when (filter) {
            TaskFilter.ACTIVE -> allTasks.filter { !it.isCompleted }
            TaskFilter.TODAY -> allTasks.filter { task ->
                val dueDay = task.dueDateEpochDay
                !task.isCompleted && dueDay != null && dueDay <= todayEpoch
            }
            TaskFilter.COMPLETED -> allTasks.filter { it.isCompleted }
        }
        Triple(filter, activeCount, filtered)
    }

    private val snippetsCombinedFlow = combine(
        snippetCategoryFlow,
        brainRepository.observeSnippets()
    ) { categoryFilter, allSnippets ->
        val filtered = if (categoryFilter == null) {
            allSnippets
        } else {
            allSnippets.filter { it.category == categoryFilter }
        }
        Pair(categoryFilter, filtered)
    }

    val uiState: StateFlow<BrainUiState> = combine(
        notesCombinedFlow,
        tasksCombinedFlow,
        snippetsCombinedFlow
    ) { notesTriple, tasksTriple, snippetsPair ->
        val (query, selectedTag, tagAndNotes) = notesTriple
        val (tags, notes) = tagAndNotes
        val (taskFilter, activeCount, tasks) = tasksTriple
        val (snippetCat, snippets) = snippetsPair

        BrainUiState(
            noteSearchQuery = query,
            selectedTag = selectedTag,
            availableTags = tags,
            notes = notes,
            taskFilter = taskFilter,
            tasks = tasks,
            activeTaskCount = activeCount,
            snippetCategoryFilter = snippetCat,
            snippets = snippets
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BrainUiState()
    )

    fun setNoteSearchQuery(query: String) {
        noteQueryFlow.value = query
    }

    fun selectTag(tag: String?) {
        selectedTagFlow.value = if (selectedTagFlow.value == tag) null else tag
    }

    fun setTaskFilter(filter: TaskFilter) {
        taskFilterFlow.value = filter
    }

    fun setSnippetCategory(category: SnippetCategory?) {
        snippetCategoryFlow.value = category
    }

    fun saveNote(
        existing: NoteItem?,
        title: String,
        content: String,
        tags: List<String>,
        isPinned: Boolean
    ) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            brainRepository.upsertNote(
                NoteItem(
                    id = existing?.id ?: 0L,
                    title = title.trim(),
                    content = content.trim(),
                    tags = tags,
                    isPinned = isPinned,
                    createdAtMillis = existing?.createdAtMillis ?: now,
                    updatedAtMillis = now
                )
            )
        }
    }

    fun togglePinNote(note: NoteItem) {
        viewModelScope.launch {
            brainRepository.togglePinNote(note)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            brainRepository.deleteNote(id)
        }
    }

    fun restoreNote(note: NoteItem) {
        viewModelScope.launch {
            brainRepository.upsertNote(note)
        }
    }

    fun saveTask(
        existing: TaskItem?,
        title: String,
        note: String,
        priority: TaskPriority,
        dueDateEpochDay: Long?
    ) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            brainRepository.upsertTask(
                TaskItem(
                    id = existing?.id ?: 0L,
                    title = title.trim(),
                    note = note.trim(),
                    priority = priority,
                    dueDateEpochDay = dueDateEpochDay,
                    isCompleted = existing?.isCompleted ?: false,
                    createdAtMillis = existing?.createdAtMillis ?: now,
                    completedAtMillis = existing?.completedAtMillis
                )
            )
        }
    }

    fun toggleTaskCompleted(task: TaskItem) {
        viewModelScope.launch {
            brainRepository.toggleTaskCompleted(task)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            brainRepository.deleteTask(id)
        }
    }

    fun restoreTask(task: TaskItem) {
        viewModelScope.launch {
            brainRepository.upsertTask(task)
        }
    }

    fun saveSnippet(
        existing: SnippetItem?,
        label: String,
        value: String,
        category: SnippetCategory,
        isSensitive: Boolean,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            brainRepository.upsertSnippet(
                SnippetItem(
                    id = existing?.id ?: 0L,
                    label = label.trim(),
                    value = value.trim(),
                    category = category,
                    isSensitive = isSensitive,
                    isPinned = isPinned,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun togglePinSnippet(snippet: SnippetItem) {
        viewModelScope.launch {
            brainRepository.togglePinSnippet(snippet)
        }
    }

    fun deleteSnippet(id: Long) {
        viewModelScope.launch {
            brainRepository.deleteSnippet(id)
        }
    }

    fun restoreSnippet(snippet: SnippetItem) {
        viewModelScope.launch {
            brainRepository.upsertSnippet(snippet)
        }
    }
}
