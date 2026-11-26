package com.kanso.core.database.repository

import com.kanso.core.database.dao.BrainDao
import com.kanso.core.database.entity.toDomain
import com.kanso.core.database.entity.toEntity
import com.kanso.core.model.FtsQuerySanitizer
import com.kanso.core.model.NoteChecklistParser
import com.kanso.core.model.NoteItem
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.TaskCalculations
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TaskRecurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed interface TaskToggleOutcome {
    data class Completed(val previous: TaskItem) : TaskToggleOutcome
    data class Reopened(val previous: TaskItem) : TaskToggleOutcome
    data class Rescheduled(val previous: TaskItem, val nextDueEpochDay: Long) : TaskToggleOutcome
}

@Singleton
class BrainRepository @Inject constructor(
    private val brainDao: BrainDao
) {
    fun observeNotes(): Flow<List<NoteItem>> {
        return brainDao.observeNotes().map { list -> list.map { it.toDomain() } }
    }

    fun searchNotes(rawQuery: String): Flow<List<NoteItem>> {
        val ftsQuery = FtsQuerySanitizer.toFtsQuery(rawQuery)
        return if (ftsQuery.isEmpty()) {
            observeNotes()
        } else {
            brainDao.searchNotesFts(ftsQuery).map { list -> list.map { it.toDomain() } }
        }
    }

    suspend fun upsertNote(note: NoteItem): Long {
        return brainDao.upsertNote(note.toEntity())
    }

    suspend fun togglePinNote(note: NoteItem) {
        brainDao.upsertNote(
            note.copy(
                isPinned = !note.isPinned,
                updatedAtMillis = System.currentTimeMillis()
            ).toEntity()
        )
    }

    suspend fun toggleNoteChecklistLine(note: NoteItem, lineIndex: Int) {
        val updatedContent = NoteChecklistParser.toggleLine(note.content, lineIndex)
        if (updatedContent == note.content) return
        brainDao.upsertNote(
            note.copy(
                content = updatedContent,
                updatedAtMillis = System.currentTimeMillis()
            ).toEntity()
        )
    }

    suspend fun deleteNote(id: Long) {
        brainDao.deleteNoteById(id)
    }

    fun observeTasks(): Flow<List<TaskItem>> {
        return brainDao.observeTasks().map { list -> list.map { it.toDomain() } }
    }

    fun searchTasks(query: String): Flow<List<TaskItem>> {
        return brainDao.searchTasks(query.trim()).map { list -> list.map { it.toDomain() } }
    }

    suspend fun upsertTask(task: TaskItem): Long {
        return brainDao.upsertTask(task.toEntity())
    }

    suspend fun toggleTaskCompleted(
        task: TaskItem,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): TaskToggleOutcome {
        val nowMillis = System.currentTimeMillis()
        if (!task.isCompleted && task.recurrence != TaskRecurrence.NONE) {
            val nextDue = TaskCalculations.nextRecurringDueEpochDay(
                currentDueEpochDay = task.dueDateEpochDay,
                recurrence = task.recurrence,
                todayEpochDay = todayEpochDay
            ) ?: (todayEpochDay + 1L)
            brainDao.upsertTask(
                task.copy(
                    dueDateEpochDay = nextDue,
                    isCompleted = false,
                    completedAtMillis = nowMillis
                ).toEntity()
            )
            return TaskToggleOutcome.Rescheduled(previous = task, nextDueEpochDay = nextDue)
        }
        val nowCompleted = !task.isCompleted
        brainDao.upsertTask(
            task.copy(
                isCompleted = nowCompleted,
                completedAtMillis = if (nowCompleted) nowMillis else null
            ).toEntity()
        )
        return if (nowCompleted) {
            TaskToggleOutcome.Completed(previous = task)
        } else {
            TaskToggleOutcome.Reopened(previous = task)
        }
    }

    suspend fun deleteTask(id: Long) {
        brainDao.deleteTaskById(id)
    }

    fun observeSnippets(): Flow<List<SnippetItem>> {
        return brainDao.observeSnippets().map { list -> list.map { it.toDomain() } }
    }

    fun searchSnippets(query: String): Flow<List<SnippetItem>> {
        return brainDao.searchSnippets(query.trim()).map { list -> list.map { it.toDomain() } }
    }

    suspend fun upsertSnippet(snippet: SnippetItem): Long {
        return brainDao.upsertSnippet(snippet.toEntity())
    }

    suspend fun togglePinSnippet(snippet: SnippetItem) {
        brainDao.upsertSnippet(
            snippet.copy(
                isPinned = !snippet.isPinned,
                updatedAtMillis = System.currentTimeMillis()
            ).toEntity()
        )
    }

    suspend fun deleteSnippet(id: Long) {
        brainDao.deleteSnippetById(id)
    }
}
