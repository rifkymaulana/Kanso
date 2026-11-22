package com.kanso.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kanso.core.database.entity.NoteEntity
import com.kanso.core.database.entity.SnippetEntity
import com.kanso.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrainDao {

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAtMillis DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :ftsQuery
        ORDER BY notes.isPinned DESC, notes.updatedAtMillis DESC
        """
    )
    fun searchNotesFts(ftsQuery: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(entity: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(items: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("SELECT COUNT(*) FROM notes")
    fun observeNoteCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM tasks
        ORDER BY isCompleted ASC, dueDateEpochDay IS NULL ASC, dueDateEpochDay ASC, createdAtMillis DESC
        """
    )
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE title LIKE '%' || :query || '%'
           OR note LIKE '%' || :query || '%'
        ORDER BY isCompleted ASC, createdAtMillis DESC
        """
    )
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(entity: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(items: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun observeOpenTaskCount(): Flow<Int>

    @Query("SELECT * FROM snippets ORDER BY isPinned DESC, updatedAtMillis DESC")
    fun observeSnippets(): Flow<List<SnippetEntity>>

    @Query(
        """
        SELECT * FROM snippets
        WHERE label LIKE '%' || :query || '%'
           OR value LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, updatedAtMillis DESC
        """
    )
    fun searchSnippets(query: String): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets")
    suspend fun getAllSnippets(): List<SnippetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnippet(entity: SnippetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippets(items: List<SnippetEntity>)

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteSnippetById(id: Long)

    @Query("DELETE FROM snippets")
    suspend fun deleteAllSnippets()

    @Query("SELECT COUNT(*) FROM snippets")
    fun observeSnippetCount(): Flow<Int>
}
