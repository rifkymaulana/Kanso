package com.kanso.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kanso.core.model.CategoryBudget
import com.kanso.core.model.ExpenseCategory

@Entity(
    tableName = "category_budgets",
    indices = [Index(value = ["category"], unique = true)]
)
data class CategoryBudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val category: String,
    val limitCents: Long,
    val yearMonth: String
)

fun CategoryBudgetEntity.toDomain(): CategoryBudget = CategoryBudget(
    id = id,
    category = runCatching { ExpenseCategory.valueOf(category) }.getOrDefault(ExpenseCategory.OTHER),
    limitCents = limitCents,
    yearMonth = yearMonth
)

fun CategoryBudget.toEntity(): CategoryBudgetEntity = CategoryBudgetEntity(
    id = id,
    category = category.name,
    limitCents = limitCents,
    yearMonth = yearMonth
)
