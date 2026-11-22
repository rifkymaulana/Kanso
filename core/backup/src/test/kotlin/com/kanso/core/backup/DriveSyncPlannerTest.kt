package com.kanso.core.backup

import com.kanso.core.backup.drive.DriveRemoteFile
import com.kanso.core.backup.drive.DriveSyncDecision
import com.kanso.core.backup.drive.DriveSyncForceMode
import com.kanso.core.backup.drive.DriveSyncPlanner
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.KansoBackupSnapshot
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.NoteItem
import com.kanso.core.model.TransactionType
import com.kanso.core.model.UserPreferences
import com.kanso.core.model.WalletType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSyncPlannerTest {

    private fun remoteFile(
        id: String = "remote-1",
        modifiedMillis: Long = 1_700_000_000_000L,
        contentHash: String? = "hash-remote",
        itemCount: Int? = 12
    ) = DriveRemoteFile(
        id = id,
        name = "kanso-cloud-sync.kanso.enc",
        modifiedTimeMillis = modifiedMillis,
        sizeBytes = 2048L,
        contentHash = contentHash,
        itemCount = itemCount,
        deviceModel = "Pixel"
    )

    @Test
    fun `smart mode uploads initially when remote file does not exist`() {
        val decision = DriveSyncPlanner.decide(
            localContentHash = "hash-local",
            localItemCount = 5,
            lastSyncedContentHash = null,
            lastSyncedRemoteModifiedMillis = null,
            remoteFile = null
        )
        assertEquals(DriveSyncDecision.UploadInitial, decision)
    }

    @Test
    fun `smart mode pulls automatically on empty newly connected device`() {
        val remote = remoteFile(contentHash = "hash-remote", itemCount = 40)
        val decision = DriveSyncPlanner.decide(
            localContentHash = "hash-empty",
            localItemCount = 0,
            lastSyncedContentHash = null,
            lastSyncedRemoteModifiedMillis = null,
            remoteFile = remote
        )
        assertEquals(DriveSyncDecision.PullRemote(remote), decision)
    }

    @Test
    fun `smart mode flags conflict when first connecting a non-empty device with differing remote data`() {
        val remote = remoteFile(contentHash = "hash-remote", itemCount = 40)
        val decision = DriveSyncPlanner.decide(
            localContentHash = "hash-local",
            localItemCount = 7,
            lastSyncedContentHash = null,
            lastSyncedRemoteModifiedMillis = null,
            remoteFile = remote
        )
        assertEquals(DriveSyncDecision.Conflict(remote, localItemCount = 7), decision)
    }

    @Test
    fun `smart mode returns up to date when remote contentHash already matches local`() {
        val remote = remoteFile(contentHash = "same-hash")
        val decision = DriveSyncPlanner.decide(
            localContentHash = "same-hash",
            localItemCount = 7,
            lastSyncedContentHash = null,
            lastSyncedRemoteModifiedMillis = null,
            remoteFile = remote
        )
        assertEquals(DriveSyncDecision.UpToDate(remote), decision)
    }

    @Test
    fun `smart mode pushes when only local changed since last sync`() {
        val remote = remoteFile(id = "remote-9", contentHash = "hash-base", modifiedMillis = 5000L)
        val decision = DriveSyncPlanner.decide(
            localContentHash = "hash-new-local",
            localItemCount = 10,
            lastSyncedContentHash = "hash-base",
            lastSyncedRemoteModifiedMillis = 5000L,
            remoteFile = remote
        )
        assertEquals(DriveSyncDecision.PushLocal("remote-9"), decision)
    }

    @Test
    fun `smart mode pulls when only remote changed since last sync`() {
        val remote = remoteFile(id = "remote-9", contentHash = "hash-new-remote", modifiedMillis = 9000L)
        val decision = DriveSyncPlanner.decide(
            localContentHash = "hash-base",
            localItemCount = 10,
            lastSyncedContentHash = "hash-base",
            lastSyncedRemoteModifiedMillis = 5000L,
            remoteFile = remote
        )
        assertEquals(DriveSyncDecision.PullRemote(remote), decision)
    }

    @Test
    fun `smart mode detects conflict when both local and remote changed since last sync`() {
        val remote = remoteFile(contentHash = "hash-new-remote", modifiedMillis = 9000L)
        val decision = DriveSyncPlanner.decide(
            localContentHash = "hash-new-local",
            localItemCount = 14,
            lastSyncedContentHash = "hash-base",
            lastSyncedRemoteModifiedMillis = 5000L,
            remoteFile = remote
        )
        assertTrue(decision is DriveSyncDecision.Conflict)
    }

    @Test
    fun `force modes override smart conflict detection cleanly`() {
        val remote = remoteFile(id = "remote-7", contentHash = "other")
        assertEquals(
            DriveSyncDecision.PushLocal("remote-7"),
            DriveSyncPlanner.decide("local", 3, null, null, remote, DriveSyncForceMode.FORCE_UPLOAD)
        )
        assertEquals(
            DriveSyncDecision.PullRemote(remote),
            DriveSyncPlanner.decide("local", 3, null, null, remote, DriveSyncForceMode.FORCE_DOWNLOAD)
        )
        assertEquals(
            DriveSyncDecision.NoRemoteBackup,
            DriveSyncPlanner.decide("local", 3, null, null, null, DriveSyncForceMode.FORCE_DOWNLOAD)
        )
    }

    @Test
    fun `computeContentHash ignores volatile timestamps and ordering while detecting real changes`() {
        val tx1 = MoneyTransaction(
            id = 1L,
            type = TransactionType.EXPENSE,
            amountCents = 25000L,
            category = ExpenseCategory.FOOD,
            wallet = WalletType.CASH,
            note = "Lunch",
            timestampMillis = 1000L
        )
        val tx2 = MoneyTransaction(
            id = 2L,
            type = TransactionType.INCOME,
            amountCents = 100000L,
            category = ExpenseCategory.SALARY,
            wallet = WalletType.BANK,
            note = "Payday",
            timestampMillis = 2000L
        )
        val note = NoteItem(
            id = 1L,
            title = "Packing",
            content = "Passport & charger",
            tags = listOf("travel", "checklist"),
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L
        )

        val snapA = KansoBackupSnapshot(
            exportedAtMillis = 1111L,
            preferences = UserPreferences(
                displayName = "Rifky",
                currency = AppCurrency.IDR,
                lastBackupMillis = 1111L,
                lastUpdateCheckMillis = 1111L,
                customRatesVsUsd = mapOf("SGD" to 1.34, "EUR" to 0.92)
            ),
            transactions = listOf(tx1, tx2),
            budgets = emptyList(),
            recurringBills = emptyList(),
            ious = emptyList(),
            notes = listOf(note),
            tasks = emptyList(),
            snippets = emptyList()
        )
        val snapB = KansoBackupSnapshot(
            exportedAtMillis = 9999L,
            preferences = UserPreferences(
                displayName = "Rifky",
                currency = AppCurrency.IDR,
                lastBackupMillis = 8888L,
                lastUpdateCheckMillis = 7777L,
                customRatesVsUsd = mapOf("EUR" to 0.92, "SGD" to 1.34)
            ),
            transactions = listOf(tx2, tx1),
            budgets = emptyList(),
            recurringBills = emptyList(),
            ious = emptyList(),
            notes = listOf(note.copy(tags = listOf("checklist", "travel"))),
            tasks = emptyList(),
            snippets = emptyList()
        )

        val hashA = BackupSerializer.computeContentHash(snapA)
        val hashB = BackupSerializer.computeContentHash(snapB)
        assertEquals(hashA, hashB)

        val mutated = snapA.copy(transactions = listOf(tx1.copy(amountCents = 30000L), tx2))
        assertNotEquals(hashA, BackupSerializer.computeContentHash(mutated))
    }
}
