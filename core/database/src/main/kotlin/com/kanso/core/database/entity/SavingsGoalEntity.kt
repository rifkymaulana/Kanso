package com.kanso.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kanso.core.model.GoalCategory
import com.kanso.core.model.SavingsGoal
import com.kanso.core.model.WalletType

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: String,
    val targetAmountCents: Long,
    val savedAmountCents: Long,
    val linkedWallet: String?,
    val targetEpochDay: Long?,
    val note: String,
    val isArchived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

fun SavingsGoalEntity.toDomain(): SavingsGoal = SavingsGoal(
    id = id,
    name = name,
    category = runCatching { GoalCategory.valueOf(category) }.getOrDefault(GoalCategory.OTHER),
    targetAmountCents = targetAmountCents,
    savedAmountCents = savedAmountCents,
    linkedWallet = linkedWallet?.let { raw -> runCatching { WalletType.valueOf(raw) }.getOrNull() },
    targetEpochDay = targetEpochDay,
    note = note,
    isArchived = isArchived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun SavingsGoal.toEntity(): SavingsGoalEntity = SavingsGoalEntity(
    id = id,
    name = name,
    category = category.name,
    targetAmountCents = targetAmountCents,
    savedAmountCents = savedAmountCents,
    linkedWallet = linkedWallet?.name,
    targetEpochDay = targetEpochDay,
    note = note,
    isArchived = isArchived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)
