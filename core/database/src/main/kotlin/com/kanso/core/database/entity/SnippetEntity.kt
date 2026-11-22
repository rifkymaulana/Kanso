package com.kanso.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.SnippetItem

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val label: String,
    val value: String,
    val category: String,
    val isSensitive: Boolean,
    val isPinned: Boolean,
    val updatedAtMillis: Long
)

fun SnippetEntity.toDomain(): SnippetItem = SnippetItem(
    id = id,
    label = label,
    value = value,
    category = runCatching { SnippetCategory.valueOf(category) }.getOrDefault(SnippetCategory.OTHER),
    isSensitive = isSensitive,
    isPinned = isPinned,
    updatedAtMillis = updatedAtMillis
)

fun SnippetItem.toEntity(): SnippetEntity = SnippetEntity(
    id = id,
    label = label,
    value = value,
    category = category.name,
    isSensitive = isSensitive,
    isPinned = isPinned,
    updatedAtMillis = updatedAtMillis
)
