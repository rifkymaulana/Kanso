package com.kanso.core.database.repository

import com.kanso.core.database.dao.LifeAdminDao
import com.kanso.core.database.entity.VaultEntity
import com.kanso.core.database.entity.parseCategory
import com.kanso.core.database.entity.toDomain
import com.kanso.core.database.entity.toEntity
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.MaintenanceLog
import com.kanso.core.model.VaultItem
import com.kanso.core.security.VaultCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifeAdminRepository @Inject constructor(
    private val lifeAdminDao: LifeAdminDao,
    private val vaultCrypto: VaultCrypto
) {
    fun observeExpiries(): Flow<List<ExpiryItem>> {
        return lifeAdminDao.observeExpiries().map { list -> list.map { it.toDomain() } }
    }

    fun searchExpiries(query: String): Flow<List<ExpiryItem>> {
        return lifeAdminDao.searchExpiries(query.trim()).map { list -> list.map { it.toDomain() } }
    }

    suspend fun upsertExpiry(item: ExpiryItem): Long {
        return lifeAdminDao.upsertExpiry(item.toEntity())
    }

    suspend fun deleteExpiry(id: Long) {
        lifeAdminDao.deleteExpiryById(id)
    }

    fun observeMaintenanceLogs(): Flow<List<MaintenanceLog>> {
        return lifeAdminDao.observeMaintenanceLogs().map { list -> list.map { it.toDomain() } }
    }

    fun searchMaintenanceLogs(query: String): Flow<List<MaintenanceLog>> {
        return lifeAdminDao.searchMaintenanceLogs(query.trim()).map { list -> list.map { it.toDomain() } }
    }

    suspend fun upsertMaintenanceLog(log: MaintenanceLog): Long {
        return lifeAdminDao.upsertMaintenanceLog(log.toEntity())
    }

    suspend fun deleteMaintenanceLog(id: Long) {
        lifeAdminDao.deleteMaintenanceLogById(id)
    }

    fun observeVaultItems(): Flow<List<VaultItem>> {
        return lifeAdminDao.observeVaultItems().map { entities ->
            entities.map { entity ->
                val decrypted = runCatching { vaultCrypto.decrypt(entity.encryptedContent) }
                    .getOrElse { "[Unable to decrypt on this device keystore]" }
                VaultItem(
                    id = entity.id,
                    title = entity.title,
                    category = entity.parseCategory(),
                    secretContent = decrypted,
                    updatedAtMillis = entity.updatedAtMillis
                )
            }
        }
    }

    suspend fun upsertVaultItem(item: VaultItem): Long {
        val encrypted = vaultCrypto.encrypt(item.secretContent)
        return lifeAdminDao.upsertVaultItem(
            VaultEntity(
                id = item.id,
                title = item.title,
                category = item.category.name,
                encryptedContent = encrypted,
                updatedAtMillis = item.updatedAtMillis
            )
        )
    }

    suspend fun deleteVaultItem(id: Long) {
        lifeAdminDao.deleteVaultItemById(id)
    }
}
