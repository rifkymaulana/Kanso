package com.kanso.core.database.repository

import com.kanso.core.database.dao.BrainDao
import com.kanso.core.database.entity.toDomain
import com.kanso.core.database.entity.toEntity
import com.kanso.core.model.FtsQuerySanitizer
import com.kanso.core.model.NoteItem
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun toggleTaskCompleted(task: TaskItem) {
        val nowCompleted = !task.isCompleted
        brainDao.upsertTask(
            task.copy(
                isCompleted = nowCompleted,
                completedAtMillis = if (nowCompleted) System.currentTimeMillis() else null
            ).toEntity()
        )
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
