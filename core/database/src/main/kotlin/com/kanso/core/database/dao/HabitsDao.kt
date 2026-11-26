package com.kanso.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kanso.core.database.entity.DailyHealthLogEntity
import com.kanso.core.database.entity.HabitCompletionEntity
import com.kanso.core.database.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitsDao {

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAtMillis ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAtMillis ASC")
    suspend fun getAllHabits(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(entity: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(items: List<HabitEntity>)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("SELECT COUNT(*) FROM habits WHERE isArchived = 0")
    fun observeActiveHabitCount(): Flow<Int>

    @Query("SELECT * FROM habit_completions WHERE epochDay >= :minEpochDay")
    fun observeCompletionsSince(minEpochDay: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    suspend fun getCompletionsForHabit(habitId: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllCompletions(): List<HabitCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(entity: HabitCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletions(items: List<HabitCompletionEntity>)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteCompletion(habitId: Long, epochDay: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND epochDay = :epochDay)")
    suspend fun hasCompletion(habitId: Long, epochDay: Long): Boolean

    @Query("DELETE FROM habit_completions")
    suspend fun deleteAllCompletions()

    @Query("SELECT * FROM daily_health_logs WHERE epochDay = :epochDay LIMIT 1")
    fun observeHealthLog(epochDay: Long): Flow<DailyHealthLogEntity?>

    @Query("SELECT * FROM daily_health_logs ORDER BY epochDay DESC LIMIT :limit")
    fun observeRecentHealthLogs(limit: Int): Flow<List<DailyHealthLogEntity>>

    @Query("SELECT * FROM daily_health_logs")
    suspend fun getAllHealthLogs(): List<DailyHealthLogEntity>

    @Query("SELECT * FROM daily_health_logs WHERE epochDay = :epochDay LIMIT 1")
    suspend fun getHealthLog(epochDay: Long): DailyHealthLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHealthLog(entity: DailyHealthLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthLogs(items: List<DailyHealthLogEntity>)

    @Query("DELETE FROM daily_health_logs")
    suspend fun deleteAllHealthLogs()
}
