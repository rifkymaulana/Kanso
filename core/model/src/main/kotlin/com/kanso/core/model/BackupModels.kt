package com.kanso.core.model

import kotlinx.serialization.Serializable

@Serializable
data class KansoBackupSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtMillis: Long,
    val preferences: UserPreferences,
    val transactions: List<MoneyTransaction>,
    val budgets: List<CategoryBudget>,
    val recurringBills: List<RecurringBill>,
    val ious: List<IouEntry>,
    val notes: List<NoteItem>,
    val tasks: List<TaskItem>,
    val snippets: List<SnippetItem>,
    val habits: List<HabitItem> = emptyList(),
    val habitCompletions: List<HabitCompletion> = emptyList(),
    val healthLogs: List<DailyHealthLog> = emptyList(),
    val expiries: List<ExpiryItem> = emptyList(),
    val maintenanceLogs: List<MaintenanceLog> = emptyList(),
    val vaultItems: List<VaultItem> = emptyList(),
    val countdowns: List<CountdownItem> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
    }
}

data class RestoreSummary(
    val transactionsCount: Int,
    val budgetsCount: Int,
    val billsCount: Int,
    val iousCount: Int,
    val notesCount: Int,
    val tasksCount: Int,
    val snippetsCount: Int,
    val habitsCount: Int = 0,
    val expiriesCount: Int = 0,
    val maintenanceCount: Int = 0,
    val vaultCount: Int = 0,
    val countdownsCount: Int = 0
)

data class StorageStats(
    val transactionCount: Int,
    val noteCount: Int,
    val openTaskCount: Int,
    val snippetCount: Int,
    val habitCount: Int = 0,
    val expiryCount: Int = 0,
    val vaultCount: Int = 0
)
