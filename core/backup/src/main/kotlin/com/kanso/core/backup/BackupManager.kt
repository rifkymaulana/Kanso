package com.kanso.core.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.kanso.core.database.KansoDatabase
import com.kanso.core.database.dao.BrainDao
import com.kanso.core.database.dao.HabitsDao
import com.kanso.core.database.dao.LifeAdminDao
import com.kanso.core.database.dao.MoneyDao
import com.kanso.core.database.dao.UtilitiesDao
import com.kanso.core.database.entity.VaultEntity
import com.kanso.core.database.entity.parseCategory
import com.kanso.core.database.entity.toDomain
import com.kanso.core.database.entity.toEntity
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.model.KansoBackupSnapshot
import com.kanso.core.model.RestoreSummary
import com.kanso.core.model.VaultItem
import com.kanso.core.security.VaultCrypto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: KansoDatabase,
    private val moneyDao: MoneyDao,
    private val brainDao: BrainDao,
    private val habitsDao: HabitsDao,
    private val lifeAdminDao: LifeAdminDao,
    private val utilitiesDao: UtilitiesDao,
    private val vaultCrypto: VaultCrypto,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun exportToUri(uri: Uri, passphrase: String): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val prefs = preferencesRepository.getCurrentPreferences()
            val decryptedVault = lifeAdminDao.getAllVaultItems().map { entity ->
                VaultItem(
                    id = entity.id,
                    title = entity.title,
                    category = entity.parseCategory(),
                    secretContent = vaultCrypto.decrypt(entity.encryptedContent),
                    updatedAtMillis = entity.updatedAtMillis
                )
            }

            val snapshot = KansoBackupSnapshot(
                schemaVersion = KansoBackupSnapshot.CURRENT_SCHEMA_VERSION,
                exportedAtMillis = now,
                preferences = prefs.copy(lastBackupMillis = now),
                transactions = moneyDao.getAllTransactions().map { it.toDomain() },
                budgets = moneyDao.getAllBudgets().map { it.toDomain() },
                recurringBills = moneyDao.getAllBills().map { it.toDomain() },
                ious = moneyDao.getAllIous().map { it.toDomain() },
                notes = brainDao.getAllNotes().map { it.toDomain() },
                tasks = brainDao.getAllTasks().map { it.toDomain() },
                snippets = brainDao.getAllSnippets().map { it.toDomain() },
                habits = habitsDao.getAllHabits().map { it.toDomain() },
                habitCompletions = habitsDao.getAllCompletions().map { it.toDomain() },
                healthLogs = habitsDao.getAllHealthLogs().map { it.toDomain() },
                expiries = lifeAdminDao.getAllExpiries().map { it.toDomain() },
                maintenanceLogs = lifeAdminDao.getAllMaintenanceLogs().map { it.toDomain() },
                vaultItems = decryptedVault,
                countdowns = utilitiesDao.getAllCountdowns().map { it.toDomain() }
            )

            val plainBytes = BackupSerializer.encode(snapshot)
            val passChars = passphrase.toCharArray()
            val encryptedBytes = try {
                EncryptedBackupCrypto.encrypt(plainBytes, passChars)
            } finally {
                passChars.fill('\u0000')
            }

            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Unable to open destination file for writing.")
            outputStream.use { it.write(encryptedBytes) }

            preferencesRepository.setLastBackupMillis(now)
            now
        }
    }

    suspend fun importFromUri(uri: Uri, passphrase: String): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Unable to open backup file for reading.")
            val payload = inputStream.use { it.readBytes() }

            val passChars = passphrase.toCharArray()
            val plainBytes = try {
                EncryptedBackupCrypto.decrypt(payload, passChars)
            } finally {
                passChars.fill('\u0000')
            }
            val snapshot = BackupSerializer.decode(plainBytes)

            val reEncryptedVault = snapshot.vaultItems.map { item ->
                VaultEntity(
                    id = item.id,
                    title = item.title,
                    category = item.category.name,
                    encryptedContent = vaultCrypto.encrypt(item.secretContent),
                    updatedAtMillis = item.updatedAtMillis
                )
            }

            database.withTransaction {
                moneyDao.deleteAllTransactions()
                moneyDao.deleteAllBudgets()
                moneyDao.deleteAllBills()
                moneyDao.deleteAllIous()
                brainDao.deleteAllNotes()
                brainDao.deleteAllTasks()
                brainDao.deleteAllSnippets()
                habitsDao.deleteAllCompletions()
                habitsDao.deleteAllHabits()
                habitsDao.deleteAllHealthLogs()
                lifeAdminDao.deleteAllExpiries()
                lifeAdminDao.deleteAllMaintenanceLogs()
                lifeAdminDao.deleteAllVaultItems()
                utilitiesDao.deleteAllCountdowns()

                if (snapshot.transactions.isNotEmpty()) {
                    moneyDao.insertTransactions(snapshot.transactions.map { it.toEntity() })
                }
                if (snapshot.budgets.isNotEmpty()) {
                    moneyDao.insertBudgets(snapshot.budgets.map { it.toEntity() })
                }
                if (snapshot.recurringBills.isNotEmpty()) {
                    moneyDao.insertBills(snapshot.recurringBills.map { it.toEntity() })
                }
                if (snapshot.ious.isNotEmpty()) {
                    moneyDao.insertIous(snapshot.ious.map { it.toEntity() })
                }
                if (snapshot.notes.isNotEmpty()) {
                    brainDao.insertNotes(snapshot.notes.map { it.toEntity() })
                }
                if (snapshot.tasks.isNotEmpty()) {
                    brainDao.insertTasks(snapshot.tasks.map { it.toEntity() })
                }
                if (snapshot.snippets.isNotEmpty()) {
                    brainDao.insertSnippets(snapshot.snippets.map { it.toEntity() })
                }
                if (snapshot.habits.isNotEmpty()) {
                    habitsDao.insertHabits(snapshot.habits.map { it.toEntity() })
                }
                if (snapshot.habitCompletions.isNotEmpty()) {
                    habitsDao.insertCompletions(snapshot.habitCompletions.map { it.toEntity() })
                }
                if (snapshot.healthLogs.isNotEmpty()) {
                    habitsDao.insertHealthLogs(snapshot.healthLogs.map { it.toEntity() })
                }
                if (snapshot.expiries.isNotEmpty()) {
                    lifeAdminDao.insertExpiries(snapshot.expiries.map { it.toEntity() })
                }
                if (snapshot.maintenanceLogs.isNotEmpty()) {
                    lifeAdminDao.insertMaintenanceLogs(snapshot.maintenanceLogs.map { it.toEntity() })
                }
                if (reEncryptedVault.isNotEmpty()) {
                    lifeAdminDao.insertVaultItems(reEncryptedVault)
                }
                if (snapshot.countdowns.isNotEmpty()) {
                    utilitiesDao.insertCountdowns(snapshot.countdowns.map { it.toEntity() })
                }
            }

            preferencesRepository.restorePreferences(snapshot.preferences)

            RestoreSummary(
                transactionsCount = snapshot.transactions.size,
                budgetsCount = snapshot.budgets.size,
                billsCount = snapshot.recurringBills.size,
                iousCount = snapshot.ious.size,
                notesCount = snapshot.notes.size,
                tasksCount = snapshot.tasks.size,
                snippetsCount = snapshot.snippets.size,
                habitsCount = snapshot.habits.size,
                expiriesCount = snapshot.expiries.size,
                maintenanceCount = snapshot.maintenanceLogs.size,
                vaultCount = snapshot.vaultItems.size,
                countdownsCount = snapshot.countdowns.size
            )
        }
    }
}
