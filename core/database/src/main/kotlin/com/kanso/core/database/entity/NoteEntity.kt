package com.kanso.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.kanso.core.model.NoteItem

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val content: String,
    val tagsCsv: String,
    val isPinned: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    val title: String,
    val content: String,
    val tagsCsv: String
)

fun NoteEntity.toDomain(): NoteItem = NoteItem(
    id = id,
    title = title,
    content = content,
    tags = tagsCsv.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() },
    isPinned = isPinned,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun NoteItem.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    tagsCsv = tags.joinToString(",") { it.trim() }.trim(','),
    isPinned = isPinned,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)
