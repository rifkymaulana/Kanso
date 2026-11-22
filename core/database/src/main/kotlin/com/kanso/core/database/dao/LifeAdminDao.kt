package com.kanso.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kanso.core.database.entity.ExpiryEntity
import com.kanso.core.database.entity.MaintenanceLogEntity
import com.kanso.core.database.entity.VaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeAdminDao {

    @Query("SELECT * FROM expiry_items ORDER BY expiryEpochDay ASC")
    fun observeExpiries(): Flow<List<ExpiryEntity>>

    @Query(
        """
        SELECT * FROM expiry_items
        WHERE title LIKE '%' || :query || '%'
           OR referenceNumber LIKE '%' || :query || '%'
           OR note LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
        ORDER BY expiryEpochDay ASC
        """
    )
    fun searchExpiries(query: String): Flow<List<ExpiryEntity>>

    @Query("SELECT * FROM expiry_items ORDER BY expiryEpochDay ASC")
    suspend fun getAllExpiries(): List<ExpiryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpiry(entity: ExpiryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpiries(items: List<ExpiryEntity>)

    @Query("DELETE FROM expiry_items WHERE id = :id")
    suspend fun deleteExpiryById(id: Long)

    @Query("DELETE FROM expiry_items")
    suspend fun deleteAllExpiries()

    @Query("SELECT COUNT(*) FROM expiry_items")
    fun observeExpiryCount(): Flow<Int>

    @Query("SELECT * FROM maintenance_logs ORDER BY serviceEpochDay DESC, id DESC")
    fun observeMaintenanceLogs(): Flow<List<MaintenanceLogEntity>>

    @Query(
        """
        SELECT * FROM maintenance_logs
        WHERE assetName LIKE '%' || :query || '%'
           OR serviceDescription LIKE '%' || :query || '%'
           OR note LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
        ORDER BY serviceEpochDay DESC, id DESC
        """
    )
    fun searchMaintenanceLogs(query: String): Flow<List<MaintenanceLogEntity>>

    @Query("SELECT * FROM maintenance_logs ORDER BY serviceEpochDay DESC")
    suspend fun getAllMaintenanceLogs(): List<MaintenanceLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaintenanceLog(entity: MaintenanceLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceLogs(items: List<MaintenanceLogEntity>)

    @Query("DELETE FROM maintenance_logs WHERE id = :id")
    suspend fun deleteMaintenanceLogById(id: Long)

    @Query("DELETE FROM maintenance_logs")
    suspend fun deleteAllMaintenanceLogs()

    @Query("SELECT * FROM vault_items ORDER BY updatedAtMillis DESC")
    fun observeVaultItems(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_items")
    suspend fun getAllVaultItems(): List<VaultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVaultItem(entity: VaultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItems(items: List<VaultEntity>)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItemById(id: Long)

    @Query("DELETE FROM vault_items")
    suspend fun deleteAllVaultItems()

    @Query("SELECT COUNT(*) FROM vault_items")
    fun observeVaultCount(): Flow<Int>
}
