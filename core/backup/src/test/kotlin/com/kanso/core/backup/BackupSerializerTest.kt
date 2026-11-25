package com.kanso.core.backup

import com.kanso.core.model.AppCurrency
import com.kanso.core.model.BillFrequency
import com.kanso.core.model.CategoryBudget
import com.kanso.core.model.CountdownItem
import com.kanso.core.model.DailyHealthLog
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.ExpiryCategory
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.HabitCompletion
import com.kanso.core.model.HabitItem
import com.kanso.core.model.IouDirection
import com.kanso.core.model.IouEntry
import com.kanso.core.model.KansoBackupSnapshot
import com.kanso.core.model.MaintenanceCategory
import com.kanso.core.model.MaintenanceLog
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.NoteItem
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TaskPriority
import com.kanso.core.model.ThemeMode
import com.kanso.core.model.TransactionType
import com.kanso.core.model.UserPreferences
import com.kanso.core.model.VaultCategory
import com.kanso.core.model.VaultItem
import com.kanso.core.model.WalletType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializerTest {

    @Test
    fun `full v2 snapshot encode encrypt decrypt decode round trip preserves all entities`() {
        val snapshot = KansoBackupSnapshot(
            schemaVersion = KansoBackupSnapshot.CURRENT_SCHEMA_VERSION,
            exportedAtMillis = 1_760_000_000_000L,
            preferences = UserPreferences(
                displayName = "Aria",
                currency = AppCurrency.IDR,
                themeMode = ThemeMode.DARK,
                useDynamicColor = false,
                overallMonthlyBudgetCents = 4_000_000_00L,
                lastBackupMillis = 1_760_000_000_000L,
                waterGoalMl = 2500,
                remindersEnabled = true,
                customRatesVsUsd = mapOf("IDR" to 16000.0),
                walletOpeningBalancesCents = mapOf("BANK" to 5_000_000_00L)
            ),
            transactions = listOf(
                MoneyTransaction(
                    id = 10L,
                    type = TransactionType.EXPENSE,
                    amountCents = 45_000_00L,
                    category = ExpenseCategory.FOOD,
                    wallet = WalletType.EWALLET,
                    note = "Lunch",
                    timestampMillis = 1_760_000_000_000L
                ),
                MoneyTransaction(
                    id = 11L,
                    type = TransactionType.TRANSFER,
                    amountCents = 500_000_00L,
                    category = ExpenseCategory.OTHER,
                    wallet = WalletType.BANK,
                    toWallet = WalletType.EWALLET,
                    note = "Top up",
                    timestampMillis = 1_760_000_100_000L
                )
            ),
            budgets = listOf(
                CategoryBudget(id = 1L, category = ExpenseCategory.FOOD, limitCents = 1_500_000_00L)
            ),
            recurringBills = listOf(
                RecurringBill(
                    id = 2L,
                    name = "Fiber Internet",
                    amountCents = 400_000_00L,
                    category = ExpenseCategory.BILLS,
                    wallet = WalletType.BANK,
                    dueDayOfMonth = 25,
                    frequency = BillFrequency.MONTHLY,
                    lastPaidYearMonth = "2026-11",
                    isActive = true
                )
            ),
            ious = listOf(
                IouEntry(
                    id = 3L,
                    personName = "Budi",
                    amountCents = 150_000_00L,
                    direction = IouDirection.OWES_ME,
                    note = "Dinner split",
                    createdAtMillis = 1_759_900_000_000L,
                    dueDateEpochDay = 20_400L,
                    isSettled = false
                )
            ),
            notes = listOf(
                NoteItem(
                    id = 4L,
                    title = "Kanso Ideas",
                    content = "Build milestone 2",
                    tags = listOf("personal", "app"),
                    isPinned = true,
                    createdAtMillis = 1_759_800_000_000L,
                    updatedAtMillis = 1_759_850_000_000L
                )
            ),
            tasks = listOf(
                TaskItem(
                    id = 5L,
                    title = "Renew vehicle tax",
                    note = "Bring STNK",
                    priority = TaskPriority.HIGH,
                    dueDateEpochDay = 20_420L,
                    isCompleted = false,
                    createdAtMillis = 1_759_700_000_000L
                )
            ),
            snippets = listOf(
                SnippetItem(
                    id = 6L,
                    label = "BCA Account",
                    value = "1234567890",
                    category = SnippetCategory.FINANCE,
                    isSensitive = true,
                    isPinned = true,
                    updatedAtMillis = 1_759_600_000_000L
                )
            ),
            habits = listOf(
                HabitItem(
                    id = 7L,
                    name = "Morning Run",
                    targetDaysPerWeek = 5,
                    reminderHour = 7,
                    reminderMinute = 0,
                    isArchived = false,
                    createdAtMillis = 1_759_500_000_000L
                )
            ),
            habitCompletions = listOf(
                HabitCompletion(habitId = 7L, epochDay = 20_410L)
            ),
            healthLogs = listOf(
                DailyHealthLog(
                    epochDay = 20_410L,
                    waterMl = 2250,
                    sleepHours = 7.5f,
                    weightKg = 68.2f,
                    mood = 4,
                    workoutNote = "5km jog",
                    updatedAtMillis = 1_759_550_000_000L
                )
            ),
            expiries = listOf(
                ExpiryItem(
                    id = 8L,
                    title = "Passport",
                    category = ExpiryCategory.PASSPORT,
                    referenceNumber = "X123456",
                    expiryEpochDay = 21_500L,
                    remindDaysBefore = 60,
                    note = "Renew 6 months early",
                    updatedAtMillis = 1_759_400_000_000L
                )
            ),
            maintenanceLogs = listOf(
                MaintenanceLog(
                    id = 9L,
                    assetName = "Honda Jazz",
                    category = MaintenanceCategory.CAR,
                    serviceDescription = "Oil & filter change",
                    serviceEpochDay = 20_400L,
                    odometerKm = 45_200,
                    costCents = 650_000_00L,
                    nextDueEpochDay = 20_580L,
                    nextDueOdometerKm = 55_000,
                    note = "Synthetic 0W-20"
                )
            ),
            vaultItems = listOf(
                VaultItem(
                    id = 10L,
                    title = "GitHub Recovery Codes",
                    category = VaultCategory.RECOVERY,
                    secretContent = "12345-67890",
                    updatedAtMillis = 1_759_300_000_000L
                )
            ),
            countdowns = listOf(
                CountdownItem(
                    id = 11L,
                    title = "Japan Trip",
                    targetEpochDay = 20_600L,
                    note = "Kyoto autumn",
                    createdAtMillis = 1_759_200_000_000L
                )
            )
        )

        val encoded = BackupSerializer.encode(snapshot)
        val encrypted = EncryptedBackupCrypto.encrypt(encoded, "kanso-test-pass".toCharArray())
        val decrypted = EncryptedBackupCrypto.decrypt(encrypted, "kanso-test-pass".toCharArray())
        val restored = BackupSerializer.decode(decrypted)

        assertEquals(snapshot, restored)
    }

    @Test
    fun `decodes milestone 1 v1 json cleanly with empty milestone 2 defaults`() {
        val v1Json = """
            {
              "schemaVersion": 1,
              "exportedAtMillis": 1760000000000,
              "preferences": {
                "displayName": "Aria",
                "currency": "IDR",
                "themeMode": "SYSTEM",
                "useDynamicColor": true,
                "overallMonthlyBudgetCents": 0,
                "lastBackupMillis": null
              },
              "transactions": [],
              "budgets": [],
              "recurringBills": [],
              "ious": [],
              "notes": [],
              "tasks": [],
              "snippets": []
            }
        """.trimIndent()

        val restored = BackupSerializer.decode(v1Json.encodeToByteArray())
        assertEquals(1, restored.schemaVersion)
        assertEquals("Aria", restored.preferences.displayName)
        assertEquals(2000, restored.preferences.waterGoalMl)
        assertTrue(restored.habits.isEmpty())
        assertTrue(restored.vaultItems.isEmpty())
        assertTrue(restored.countdowns.isEmpty())
    }

    @Test(expected = BackupDecryptionException::class)
    fun `rejects unsupported newer schema version`() {
        val futureJson = """{"schemaVersion":99,"exportedAtMillis":1,"preferences":{},"transactions":[],"budgets":[],"recurringBills":[],"ious":[],"notes":[],"tasks":[],"snippets":[]}"""
        BackupSerializer.decode(futureJson.encodeToByteArray())
    }
}
