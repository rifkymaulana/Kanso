package com.kanso.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.TransactionType
import com.kanso.core.model.WalletType

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["timestampMillis"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String,
    val amountCents: Long,
    val category: String,
    val wallet: String,
    val toWallet: String? = null,
    val note: String,
    val timestampMillis: Long
)

fun TransactionEntity.toDomain(): MoneyTransaction = MoneyTransaction(
    id = id,
    type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
    amountCents = amountCents,
    category = runCatching { ExpenseCategory.valueOf(category) }.getOrDefault(ExpenseCategory.OTHER),
    wallet = runCatching { WalletType.valueOf(wallet) }.getOrDefault(WalletType.CASH),
    toWallet = toWallet?.let { raw -> runCatching { WalletType.valueOf(raw) }.getOrNull() },
    note = note,
    timestampMillis = timestampMillis
)

fun MoneyTransaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    type = type.name,
    amountCents = amountCents,
    category = category.name,
    wallet = wallet.name,
    toWallet = toWallet?.name,
    note = note,
    timestampMillis = timestampMillis
)
