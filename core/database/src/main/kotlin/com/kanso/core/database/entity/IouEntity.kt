package com.kanso.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kanso.core.model.IouDirection
import com.kanso.core.model.IouEntry

@Entity(tableName = "ious")
data class IouEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val personName: String,
    val amountCents: Long,
    val direction: String,
    val note: String,
    val createdAtMillis: Long,
    val dueDateEpochDay: Long?,
    val isSettled: Boolean,
    val settledAtMillis: Long?
)

fun IouEntity.toDomain(): IouEntry = IouEntry(
    id = id,
    personName = personName,
    amountCents = amountCents,
    direction = runCatching { IouDirection.valueOf(direction) }.getOrDefault(IouDirection.OWES_ME),
    note = note,
    createdAtMillis = createdAtMillis,
    dueDateEpochDay = dueDateEpochDay,
    isSettled = isSettled,
    settledAtMillis = settledAtMillis
)

fun IouEntry.toEntity(): IouEntity = IouEntity(
    id = id,
    personName = personName,
    amountCents = amountCents,
    direction = direction.name,
    note = note,
    createdAtMillis = createdAtMillis,
    dueDateEpochDay = dueDateEpochDay,
    isSettled = isSettled,
    settledAtMillis = settledAtMillis
)
