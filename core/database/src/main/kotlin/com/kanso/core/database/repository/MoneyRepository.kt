package com.kanso.core.database.repository

import androidx.room.withTransaction
import com.kanso.core.database.KansoDatabase
import com.kanso.core.database.dao.MoneyDao
import com.kanso.core.database.entity.toDomain
import com.kanso.core.database.entity.toEntity
import com.kanso.core.model.CategoryBudget
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.IouDirection
import com.kanso.core.model.IouEntry
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.SavingsGoal
import com.kanso.core.model.TransactionType
import com.kanso.core.model.WalletType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoneyRepository @Inject constructor(
    private val database: KansoDatabase,
    private val moneyDao: MoneyDao
) {
    fun observeTransactionsForMonth(yearMonth: YearMonth): Flow<List<MoneyTransaction>> {
        val zone = ZoneId.systemDefault()
        val startMillis = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return moneyDao.observeTransactionsInRange(startMillis, endMillis)
            .map { entities -> entities.map { it.toDomain() } }
    }

    fun observeAllTransactions(): Flow<List<MoneyTransaction>> {
        return moneyDao.observeAllTransactions()
            .map { entities -> entities.map { it.toDomain() } }
    }

    fun observeRecentTransactions(limit: Int = 20): Flow<List<MoneyTransaction>> {
        return moneyDao.observeRecentTransactions(limit)
            .map { entities -> entities.map { it.toDomain() } }
    }

    fun searchTransactions(query: String): Flow<List<MoneyTransaction>> {
        val trimmed = query.trim()
        return moneyDao.searchTransactions(trimmed)
            .map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun upsertTransaction(transaction: MoneyTransaction): Long {
        return moneyDao.upsertTransaction(transaction.toEntity())
    }

    suspend fun recordTransfer(
        fromWallet: WalletType,
        toWallet: WalletType,
        amountCents: Long,
        feeCents: Long = 0L,
        note: String = "",
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        require(fromWallet != toWallet) { "Source and destination wallets must be different." }
        database.withTransaction {
            val trimmedNote = note.trim()
            val transferTx = MoneyTransaction(
                type = TransactionType.TRANSFER,
                amountCents = amountCents,
                category = com.kanso.core.model.ExpenseCategory.OTHER,
                wallet = fromWallet,
                toWallet = toWallet,
                note = trimmedNote,
                timestampMillis = timestampMillis
            )
            moneyDao.upsertTransaction(transferTx.toEntity())
            if (feeCents > 0L) {
                val feeNote = if (trimmedNote.isNotBlank()) {
                    "Transfer fee: $trimmedNote"
                } else {
                    "Transfer fee (${fromWallet.label} → ${toWallet.label})"
                }
                val feeTx = MoneyTransaction(
                    type = TransactionType.EXPENSE,
                    amountCents = feeCents,
                    category = com.kanso.core.model.ExpenseCategory.BILLS,
                    wallet = fromWallet,
                    note = feeNote,
                    timestampMillis = timestampMillis
                )
                moneyDao.upsertTransaction(feeTx.toEntity())
            }
        }
    }

    suspend fun deleteTransaction(id: Long) {
        moneyDao.deleteTransactionById(id)
    }

    fun observeBudgets(): Flow<List<CategoryBudget>> {
        return moneyDao.observeBudgets().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun upsertBudget(budget: CategoryBudget): Long {
        return moneyDao.upsertBudget(budget.toEntity())
    }

    suspend fun deleteBudget(id: Long) {
        moneyDao.deleteBudgetById(id)
    }

    fun observeBills(): Flow<List<RecurringBill>> {
        return moneyDao.observeBills().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun upsertBill(bill: RecurringBill): Long {
        return moneyDao.upsertBill(bill.toEntity())
    }

    suspend fun deleteBill(id: Long) {
        moneyDao.deleteBillById(id)
    }

    suspend fun payRecurringBill(
        bill: RecurringBill,
        yearMonth: YearMonth = YearMonth.now(),
        nowMillis: Long = System.currentTimeMillis()
    ) {
        database.withTransaction {
            val tx = MoneyTransaction(
                type = TransactionType.EXPENSE,
                amountCents = bill.amountCents,
                category = bill.category,
                wallet = bill.wallet,
                note = "Bill: ${bill.name}",
                timestampMillis = nowMillis
            )
            moneyDao.upsertTransaction(tx.toEntity())
            moneyDao.upsertBill(
                bill.copy(lastPaidYearMonth = yearMonth.toString()).toEntity()
            )
        }
    }

    fun observeIous(): Flow<List<IouEntry>> {
        return moneyDao.observeIous().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun upsertIou(iou: IouEntry): Long {
        return moneyDao.upsertIou(iou.toEntity())
    }

    suspend fun deleteIou(id: Long) {
        moneyDao.deleteIouById(id)
    }

    suspend fun settleIou(
        iou: IouEntry,
        recordTransaction: Boolean,
        wallet: WalletType,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        database.withTransaction {
            moneyDao.upsertIou(
                iou.copy(isSettled = true, settledAtMillis = nowMillis).toEntity()
            )
            if (recordTransaction) {
                val txType = when (iou.direction) {
                    IouDirection.OWES_ME -> TransactionType.INCOME
                    IouDirection.I_OWE -> TransactionType.EXPENSE
                }
                val prefix = when (iou.direction) {
                    IouDirection.OWES_ME -> "IOU Repaid by"
                    IouDirection.I_OWE -> "IOU Paid to"
                }
                val note = if (iou.note.isNotBlank()) {
                    "$prefix ${iou.personName} (${iou.note})"
                } else {
                    "$prefix ${iou.personName}"
                }
                val tx = MoneyTransaction(
                    type = txType,
                    amountCents = iou.amountCents,
                    category = com.kanso.core.model.ExpenseCategory.OTHER,
                    wallet = wallet,
                    note = note,
                    timestampMillis = nowMillis
                )
                moneyDao.upsertTransaction(tx.toEntity())
            }
        }
    }

    fun observeSavingsGoals(): Flow<List<SavingsGoal>> {
        return moneyDao.observeSavingsGoals().map { entities -> entities.map { it.toDomain() } }
    }

    fun searchSavingsGoals(query: String): Flow<List<SavingsGoal>> {
        val trimmed = query.trim()
        return moneyDao.searchSavingsGoals(trimmed).map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun upsertSavingsGoal(goal: SavingsGoal): Long {
        return moneyDao.upsertSavingsGoal(goal.toEntity())
    }

    suspend fun deleteSavingsGoal(id: Long) {
        moneyDao.deleteSavingsGoalById(id)
    }

    suspend fun adjustSavingsGoal(
        goalId: Long,
        deltaCents: Long,
        recordExpense: Boolean = false,
        expenseCategory: ExpenseCategory = ExpenseCategory.OTHER,
        expenseWallet: WalletType = WalletType.BANK,
        note: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ) {
        database.withTransaction {
            val existing = moneyDao.getSavingsGoalById(goalId) ?: return@withTransaction
            val updatedSaved = (existing.savedAmountCents + deltaCents).coerceAtLeast(0L)
            moneyDao.upsertSavingsGoal(
                existing.copy(
                    savedAmountCents = updatedSaved,
                    updatedAtMillis = nowMillis
                )
            )
            if (deltaCents < 0L && recordExpense) {
                val txNote = note.trim().ifBlank { "Goal spent: ${existing.name}" }
                val tx = MoneyTransaction(
                    type = TransactionType.EXPENSE,
                    amountCents = -deltaCents,
                    category = expenseCategory,
                    wallet = expenseWallet,
                    note = txNote,
                    timestampMillis = nowMillis
                )
                moneyDao.upsertTransaction(tx.toEntity())
            }
        }
    }
}
