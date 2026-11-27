package com.kanso.core.database.repository

import com.kanso.core.database.dao.BrainDao
import com.kanso.core.database.dao.HabitsDao
import com.kanso.core.database.dao.LifeAdminDao
import com.kanso.core.database.dao.MoneyDao
import com.kanso.core.database.dao.UtilitiesDao
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
    private val lifeAdminDao: LifeAdminDao,
    private val utilitiesDao: UtilitiesDao
) {
    fun observeStats(): Flow<StorageStats> {
        val m1Flow = combine(
            moneyDao.observeTransactionCount(),
            moneyDao.observeActiveBillCount(),
            moneyDao.observeOpenIouCount(),
            moneyDao.observeActiveGoalCount()
        ) { txCount, billCount, iouCount, goalCount ->
            listOf(txCount, billCount, iouCount, goalCount)
        }

        val m2Flow = combine(
            brainDao.observeNoteCount(),
            brainDao.observeOpenTaskCount(),
            brainDao.observeTotalTaskCount(),
            brainDao.observeSnippetCount()
        ) { noteCount, openTaskCount, totalTaskCount, snippetCount ->
            listOf(noteCount, openTaskCount, totalTaskCount, snippetCount)
        }

        val m3Flow = combine(
            habitsDao.observeActiveHabitCount(),
            lifeAdminDao.observeExpiryCount(),
            lifeAdminDao.observeMaintenanceCount(),
            lifeAdminDao.observeVaultCount(),
            utilitiesDao.observeCountdownCount()
        ) { habitCount, expiryCount, maintenanceCount, vaultCount, countdownCount ->
            listOf(habitCount, expiryCount, maintenanceCount, vaultCount, countdownCount)
        }

        return combine(m1Flow, m2Flow, m3Flow) { money, brain, lifeUtil ->
            StorageStats(
                transactionCount = money[0],
                noteCount = brain[0],
                openTaskCount = brain[1],
                snippetCount = brain[3],
                habitCount = lifeUtil[0],
                expiryCount = lifeUtil[1],
                vaultCount = lifeUtil[3],
                maintenanceCount = lifeUtil[2],
                activeBillCount = money[1],
                openIouCount = money[2],
                activeGoalCount = money[3],
                totalTaskCount = brain[2],
                countdownCount = lifeUtil[4]
            )
        }
    }
}
