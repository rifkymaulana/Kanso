package com.kanso.core.database.repository

import androidx.room.withTransaction
import com.kanso.core.database.KansoDatabase
import com.kanso.core.database.dao.HabitsDao
import com.kanso.core.database.entity.DailyHealthLogEntity
import com.kanso.core.database.entity.HabitCompletionEntity
import com.kanso.core.database.entity.toDomain
import com.kanso.core.database.entity.toEntity
import com.kanso.core.model.DailyHealthLog
import com.kanso.core.model.HabitCompletion
import com.kanso.core.model.HabitItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitsRepository @Inject constructor(
    private val database: KansoDatabase,
    private val habitsDao: HabitsDao
) {
    fun observeActiveHabits(): Flow<List<HabitItem>> {
        return habitsDao.observeActiveHabits().map { list -> list.map { it.toDomain() } }
    }

    suspend fun upsertHabit(habit: HabitItem): Long {
        return habitsDao.upsertHabit(habit.toEntity())
    }

    suspend fun deleteHabit(id: Long) {
        habitsDao.deleteHabitById(id)
    }

    fun observeCompletionsSince(minEpochDay: Long): Flow<List<HabitCompletion>> {
        return habitsDao.observeCompletionsSince(minEpochDay).map { list -> list.map { it.toDomain() } }
    }

    suspend fun toggleCompletion(habitId: Long, epochDay: Long) {
        database.withTransaction {
            if (habitsDao.hasCompletion(habitId, epochDay)) {
                habitsDao.deleteCompletion(habitId, epochDay)
            } else {
                habitsDao.insertCompletion(HabitCompletionEntity(habitId = habitId, epochDay = epochDay))
            }
        }
    }

    fun observeHealthLog(epochDay: Long): Flow<DailyHealthLog?> {
        return habitsDao.observeHealthLog(epochDay).map { it?.toDomain() }
    }

    fun observeRecentHealthLogs(limit: Int = 14): Flow<List<DailyHealthLog>> {
        return habitsDao.observeRecentHealthLogs(limit).map { list -> list.map { it.toDomain() } }
    }

    suspend fun adjustWater(epochDay: Long, deltaMl: Int) {
        database.withTransaction {
            val current = habitsDao.getHealthLog(epochDay)
            val now = System.currentTimeMillis()
            val updatedWater = ((current?.waterMl ?: 0) + deltaMl).coerceIn(0, 15_000)
            val entity = current?.copy(
                waterMl = updatedWater,
                updatedAtMillis = now
            ) ?: DailyHealthLogEntity(
                epochDay = epochDay,
                waterMl = updatedWater,
                sleepHours = null,
                weightKg = null,
                mood = null,
                workoutNote = "",
                updatedAtMillis = now
            )
            habitsDao.upsertHealthLog(entity)
        }
    }

    suspend fun upsertHealthLog(log: DailyHealthLog) {
        habitsDao.upsertHealthLog(log.toEntity())
    }
}
