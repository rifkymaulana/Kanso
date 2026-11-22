package com.kanso.core.database.repository

import com.kanso.core.database.dao.UtilitiesDao
import com.kanso.core.database.entity.toDomain
import com.kanso.core.database.entity.toEntity
import com.kanso.core.model.CountdownItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UtilitiesRepository @Inject constructor(
    private val utilitiesDao: UtilitiesDao
) {
    fun observeCountdowns(): Flow<List<CountdownItem>> {
        return utilitiesDao.observeCountdowns().map { list -> list.map { it.toDomain() } }
    }

    suspend fun upsertCountdown(item: CountdownItem): Long {
        return utilitiesDao.upsertCountdown(item.toEntity())
    }

    suspend fun deleteCountdown(id: Long) {
        utilitiesDao.deleteCountdownById(id)
    }
}
