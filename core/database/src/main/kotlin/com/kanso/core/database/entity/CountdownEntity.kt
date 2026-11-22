package com.kanso.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kanso.core.model.CountdownItem

@Entity(tableName = "countdowns")
data class CountdownEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val targetEpochDay: Long,
    val note: String,
    val createdAtMillis: Long
)

fun CountdownEntity.toDomain(): CountdownItem = CountdownItem(
    id = id,
    title = title,
    targetEpochDay = targetEpochDay,
    note = note,
    createdAtMillis = createdAtMillis
)

fun CountdownItem.toEntity(): CountdownEntity = CountdownEntity(
    id = id,
    title = title,
    targetEpochDay = targetEpochDay,
    note = note,
    createdAtMillis = createdAtMillis
)
