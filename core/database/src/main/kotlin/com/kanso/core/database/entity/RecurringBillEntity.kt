package com.kanso.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kanso.core.model.BillFrequency
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.WalletType

@Entity(tableName = "recurring_bills")
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val amountCents: Long,
    val category: String,
    val wallet: String,
    val dueDayOfMonth: Int,
    val frequency: String,
    val lastPaidYearMonth: String?,
    val isActive: Boolean
)

fun RecurringBillEntity.toDomain(): RecurringBill = RecurringBill(
    id = id,
    name = name,
    amountCents = amountCents,
    category = runCatching { ExpenseCategory.valueOf(category) }.getOrDefault(ExpenseCategory.BILLS),
    wallet = runCatching { WalletType.valueOf(wallet) }.getOrDefault(WalletType.BANK),
    dueDayOfMonth = dueDayOfMonth,
    frequency = runCatching { BillFrequency.valueOf(frequency) }.getOrDefault(BillFrequency.MONTHLY),
    lastPaidYearMonth = lastPaidYearMonth,
    isActive = isActive
)

fun RecurringBill.toEntity(): RecurringBillEntity = RecurringBillEntity(
    id = id,
    name = name,
    amountCents = amountCents,
    category = category.name,
    wallet = wallet.name,
    dueDayOfMonth = dueDayOfMonth,
    frequency = frequency.name,
    lastPaidYearMonth = lastPaidYearMonth,
    isActive = isActive
)
