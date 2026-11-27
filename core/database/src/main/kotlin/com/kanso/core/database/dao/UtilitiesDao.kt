package com.kanso.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kanso.core.database.entity.CountdownEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilitiesDao {

    @Query("SELECT * FROM countdowns ORDER BY targetEpochDay ASC, id ASC")
    fun observeCountdowns(): Flow<List<CountdownEntity>>

    @Query("SELECT * FROM countdowns")
    suspend fun getAllCountdowns(): List<CountdownEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCountdown(entity: CountdownEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountdowns(items: List<CountdownEntity>)

    @Query("DELETE FROM countdowns WHERE id = :id")
    suspend fun deleteCountdownById(id: Long)

    @Query("DELETE FROM countdowns")
    suspend fun deleteAllCountdowns()

    @Query("SELECT COUNT(*) FROM countdowns")
    fun observeCountdownCount(): Flow<Int>
}
