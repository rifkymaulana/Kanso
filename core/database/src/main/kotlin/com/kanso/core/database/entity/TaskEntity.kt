package com.kanso.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TaskPriority
import com.kanso.core.model.TaskRecurrence

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val note: String,
    val priority: String,
    val dueDateEpochDay: Long?,
    val recurrence: String = "NONE",
    val isCompleted: Boolean,
    val createdAtMillis: Long,
    val completedAtMillis: Long?
)

fun TaskEntity.toDomain(): TaskItem = TaskItem(
    id = id,
    title = title,
    note = note,
    priority = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.MEDIUM),
    dueDateEpochDay = dueDateEpochDay,
    recurrence = runCatching { TaskRecurrence.valueOf(recurrence) }.getOrDefault(TaskRecurrence.NONE),
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
    completedAtMillis = completedAtMillis
)

fun TaskItem.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    note = note,
    priority = priority.name,
    dueDateEpochDay = dueDateEpochDay,
    recurrence = recurrence.name,
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
    completedAtMillis = completedAtMillis
)
