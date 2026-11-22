package com.kanso.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kanso.core.model.DailyHealthLog
import com.kanso.core.model.HabitCompletion
import com.kanso.core.model.HabitItem

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val targetDaysPerWeek: Int,
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val isArchived: Boolean,
    val createdAtMillis: Long
)

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["epochDay"]), Index(value = ["habitId"])]
)
data class HabitCompletionEntity(
    val habitId: Long,
    val epochDay: Long
)

@Entity(tableName = "daily_health_logs")
data class DailyHealthLogEntity(
    @PrimaryKey
    val epochDay: Long,
    val waterMl: Int,
    val sleepHours: Float?,
    val weightKg: Float?,
    val mood: Int?,
    val workoutNote: String,
    val updatedAtMillis: Long
)

fun HabitEntity.toDomain(): HabitItem = HabitItem(
    id = id,
    name = name,
    targetDaysPerWeek = targetDaysPerWeek,
    reminderHour = reminderHour,
    reminderMinute = reminderMinute,
    isArchived = isArchived,
    createdAtMillis = createdAtMillis
)

fun HabitItem.toEntity(): HabitEntity = HabitEntity(
    id = id,
    name = name,
    targetDaysPerWeek = targetDaysPerWeek,
    reminderHour = reminderHour,
    reminderMinute = reminderMinute,
    isArchived = isArchived,
    createdAtMillis = createdAtMillis
)

fun HabitCompletionEntity.toDomain(): HabitCompletion = HabitCompletion(
    habitId = habitId,
    epochDay = epochDay
)

fun HabitCompletion.toEntity(): HabitCompletionEntity = HabitCompletionEntity(
    habitId = habitId,
    epochDay = epochDay
)

fun DailyHealthLogEntity.toDomain(): DailyHealthLog = DailyHealthLog(
    epochDay = epochDay,
    waterMl = waterMl,
    sleepHours = sleepHours,
    weightKg = weightKg,
    mood = mood,
    workoutNote = workoutNote,
    updatedAtMillis = updatedAtMillis
)

fun DailyHealthLog.toEntity(): DailyHealthLogEntity = DailyHealthLogEntity(
    epochDay = epochDay,
    waterMl = waterMl,
    sleepHours = sleepHours,
    weightKg = weightKg,
    mood = mood,
    workoutNote = workoutNote,
    updatedAtMillis = updatedAtMillis
)
