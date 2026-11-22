package com.kanso.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kanso.core.database.entity.CategoryBudgetEntity
import com.kanso.core.database.entity.IouEntity
import com.kanso.core.database.entity.RecurringBillEntity
import com.kanso.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyDao {

    @Query(
        """
        SELECT * FROM transactions
        WHERE timestampMillis >= :startMillis AND timestampMillis < :endMillis
        ORDER BY timestampMillis DESC, id DESC
        """
    )
    fun observeTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC, id DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE note LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
           OR wallet LIKE '%' || :query || '%'
        ORDER BY timestampMillis DESC, id DESC
        """
    )
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(entity: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(items: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeTransactionCount(): Flow<Int>

    @Query("SELECT * FROM category_budgets ORDER BY category ASC")
    fun observeBudgets(): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM category_budgets")
    suspend fun getAllBudgets(): List<CategoryBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(entity: CategoryBudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(items: List<CategoryBudgetEntity>)

    @Query("DELETE FROM category_budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    @Query("DELETE FROM category_budgets")
    suspend fun deleteAllBudgets()

    @Query("SELECT * FROM recurring_bills ORDER BY isActive DESC, dueDayOfMonth ASC, name ASC")
    fun observeBills(): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills")
    suspend fun getAllBills(): List<RecurringBillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBill(entity: RecurringBillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(items: List<RecurringBillEntity>)

    @Query("DELETE FROM recurring_bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    @Query("DELETE FROM recurring_bills")
    suspend fun deleteAllBills()

    @Query("SELECT * FROM ious ORDER BY isSettled ASC, createdAtMillis DESC")
    fun observeIous(): Flow<List<IouEntity>>

    @Query("SELECT * FROM ious")
    suspend fun getAllIous(): List<IouEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIou(entity: IouEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIous(items: List<IouEntity>)

    @Query("DELETE FROM ious WHERE id = :id")
    suspend fun deleteIouById(id: Long)

    @Query("DELETE FROM ious")
    suspend fun deleteAllIous()
}
