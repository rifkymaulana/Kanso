package com.kanso.core.backup

import com.kanso.core.model.AppLockTimeout
import com.kanso.core.model.KansoBackupSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

object BackupSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(snapshot: KansoBackupSnapshot): ByteArray {
        return json.encodeToString(snapshot).encodeToByteArray()
    }

    fun decode(bytes: ByteArray): KansoBackupSnapshot {
        val snapshot = try {
            json.decodeFromString<KansoBackupSnapshot>(bytes.decodeToString())
        } catch (e: Exception) {
            throw BackupDecryptionException("Corrupted backup data.", e)
        }
        if (snapshot.schemaVersion > KansoBackupSnapshot.CURRENT_SCHEMA_VERSION) {
            throw BackupDecryptionException(
                "Backup schema v${snapshot.schemaVersion} is newer than this app supports."
            )
        }
        return snapshot
    }

    fun totalItemCount(snapshot: KansoBackupSnapshot): Int {
        return snapshot.transactions.size +
            snapshot.budgets.size +
            snapshot.recurringBills.size +
            snapshot.ious.size +
            snapshot.notes.size +
            snapshot.tasks.size +
            snapshot.snippets.size +
            snapshot.habits.size +
            snapshot.habitCompletions.size +
            snapshot.healthLogs.size +
            snapshot.expiries.size +
            snapshot.maintenanceLogs.size +
            snapshot.vaultItems.size +
            snapshot.countdowns.size
    }

    /**
     * Computes a deterministic SHA-256 hash over user preferences and all data lists,
     * ignoring volatile snapshot/backup/update-check timestamps and list ordering.
     */
    fun computeContentHash(snapshot: KansoBackupSnapshot): String {
        val canonical = snapshot.copy(
            exportedAtMillis = 0L,
            preferences = snapshot.preferences.copy(
                lastBackupMillis = null,
                lastUpdateCheckMillis = null,
                customRatesVsUsd = snapshot.preferences.customRatesVsUsd.toSortedMap(),
                walletOpeningBalancesCents = snapshot.preferences.walletOpeningBalancesCents.toSortedMap(),
                appLockEnabled = false,
                appLockTimeout = AppLockTimeout.IMMEDIATE,
                hideRecentAppsPreview = false,
                hideBalances = false
            ),
            transactions = snapshot.transactions.sortedWith(
                compareBy({ it.id }, { it.timestampMillis }, { it.category.name }, { it.toWallet?.name.orEmpty() })
            ),
            budgets = snapshot.budgets.sortedWith(
                compareBy({ it.id }, { it.category.name }, { it.yearMonth })
            ),
            recurringBills = snapshot.recurringBills.sortedWith(
                compareBy({ it.id }, { it.name })
            ),
            ious = snapshot.ious.sortedWith(
                compareBy({ it.id }, { it.createdAtMillis }, { it.personName })
            ),
            notes = snapshot.notes
                .map { it.copy(tags = it.tags.sorted()) }
                .sortedWith(compareBy({ it.id }, { it.createdAtMillis }, { it.title })),
            tasks = snapshot.tasks.sortedWith(
                compareBy({ it.id }, { it.createdAtMillis }, { it.title })
            ),
            snippets = snapshot.snippets.sortedWith(
                compareBy({ it.id }, { it.updatedAtMillis }, { it.label })
            ),
            habits = snapshot.habits.sortedWith(
                compareBy({ it.id }, { it.createdAtMillis }, { it.name })
            ),
            habitCompletions = snapshot.habitCompletions.sortedWith(
                compareBy({ it.habitId }, { it.epochDay })
            ),
            healthLogs = snapshot.healthLogs.sortedBy { it.epochDay },
            expiries = snapshot.expiries.sortedWith(
                compareBy({ it.id }, { it.expiryEpochDay }, { it.title })
            ),
            maintenanceLogs = snapshot.maintenanceLogs.sortedWith(
                compareBy({ it.id }, { it.serviceEpochDay }, { it.assetName })
            ),
            vaultItems = snapshot.vaultItems.sortedWith(
                compareBy({ it.id }, { it.updatedAtMillis }, { it.title })
            ),
            countdowns = snapshot.countdowns.sortedWith(
                compareBy({ it.id }, { it.targetEpochDay }, { it.title })
            )
        )
        val digest = MessageDigest.getInstance("SHA-256").digest(encode(canonical))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
