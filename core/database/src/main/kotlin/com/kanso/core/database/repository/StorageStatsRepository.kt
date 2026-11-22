package com.kanso.core.database.repository

import com.kanso.core.database.dao.BrainDao
import com.kanso.core.database.dao.HabitsDao
import com.kanso.core.database.dao.LifeAdminDao
import com.kanso.core.database.dao.MoneyDao
import com.kanso.core.model.StorageStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageStatsRepository @Inject constructor(
    private val moneyDao: MoneyDao,
    private val brainDao: BrainDao,
    private val habitsDao: HabitsDao,
    private val lifeAdminDao: LifeAdminDao
) {
    fun observeStats(): Flow<StorageStats> {
        val m1Flow = combine(
            moneyDao.observeTransactionCount(),
            brainDao.observeNoteCount(),
            brainDao.observeOpenTaskCount(),
            brainDao.observeSnippetCount()
        ) { txCount, noteCount, openTaskCount, snippetCount ->
            listOf(txCount, noteCount, openTaskCount, snippetCount)
        }

        val m2Flow = combine(
            habitsDao.observeActiveHabitCount(),
            lifeAdminDao.observeExpiryCount(),
            lifeAdminDao.observeVaultCount()
        ) { habitCount, expiryCount, vaultCount ->
            Triple(habitCount, expiryCount, vaultCount)
        }

        return combine(m1Flow, m2Flow) { m1, m2 ->
            StorageStats(
                transactionCount = m1[0],
                noteCount = m1[1],
                openTaskCount = m1[2],
                snippetCount = m1[3],
                habitCount = m2.first,
                expiryCount = m2.second,
                vaultCount = m2.third
            )
        }
    }
}
