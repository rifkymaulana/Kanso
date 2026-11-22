package com.kanso.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kanso.core.model.ExpiryCategory
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.MaintenanceCategory
import com.kanso.core.model.MaintenanceLog
import com.kanso.core.model.VaultCategory

@Entity(
    tableName = "expiry_items",
    indices = [Index(value = ["expiryEpochDay"])]
)
data class ExpiryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val category: String,
    val referenceNumber: String,
    val expiryEpochDay: Long,
    val remindDaysBefore: Int,
    val note: String,
    val updatedAtMillis: Long
)

@Entity(
    tableName = "maintenance_logs",
    indices = [Index(value = ["serviceEpochDay"])]
)
data class MaintenanceLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val assetName: String,
    val category: String,
    val serviceDescription: String,
    val serviceEpochDay: Long,
    val odometerKm: Int?,
    val costCents: Long?,
    val nextDueEpochDay: Long?,
    val nextDueOdometerKm: Int?,
    val note: String
)

@Entity(tableName = "vault_items")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val category: String,
    val encryptedContent: String,
    val updatedAtMillis: Long
)

fun ExpiryEntity.toDomain(): ExpiryItem = ExpiryItem(
    id = id,
    title = title,
    category = runCatching { ExpiryCategory.valueOf(category) }.getOrDefault(ExpiryCategory.OTHER),
    referenceNumber = referenceNumber,
    expiryEpochDay = expiryEpochDay,
    remindDaysBefore = remindDaysBefore,
    note = note,
    updatedAtMillis = updatedAtMillis
)

fun ExpiryItem.toEntity(): ExpiryEntity = ExpiryEntity(
    id = id,
    title = title,
    category = category.name,
    referenceNumber = referenceNumber,
    expiryEpochDay = expiryEpochDay,
    remindDaysBefore = remindDaysBefore,
    note = note,
    updatedAtMillis = updatedAtMillis
)

fun MaintenanceLogEntity.toDomain(): MaintenanceLog = MaintenanceLog(
    id = id,
    assetName = assetName,
    category = runCatching { MaintenanceCategory.valueOf(category) }.getOrDefault(MaintenanceCategory.OTHER),
    serviceDescription = serviceDescription,
    serviceEpochDay = serviceEpochDay,
    odometerKm = odometerKm,
    costCents = costCents,
    nextDueEpochDay = nextDueEpochDay,
    nextDueOdometerKm = nextDueOdometerKm,
    note = note
)

fun MaintenanceLog.toEntity(): MaintenanceLogEntity = MaintenanceLogEntity(
    id = id,
    assetName = assetName,
    category = category.name,
    serviceDescription = serviceDescription,
    serviceEpochDay = serviceEpochDay,
    odometerKm = odometerKm,
    costCents = costCents,
    nextDueEpochDay = nextDueEpochDay,
    nextDueOdometerKm = nextDueOdometerKm,
    note = note
)

fun VaultEntity.parseCategory(): VaultCategory =
    runCatching { VaultCategory.valueOf(category) }.getOrDefault(VaultCategory.NOTE)
