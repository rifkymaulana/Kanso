package com.kanso.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.BrainRepository
import com.kanso.core.database.repository.LifeAdminRepository
import com.kanso.core.database.repository.MoneyRepository
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.ExpiryCategory
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.NoteItem
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.TaskItem
import com.kanso.core.model.TaskPriority
import com.kanso.core.model.TransactionType
import com.kanso.core.model.UserPreferences
import com.kanso.core.model.WalletType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    private val moneyRepository: MoneyRepository,
    private val brainRepository: BrainRepository,
    private val lifeAdminRepository: LifeAdminRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    fun saveQuickTransaction(
        type: TransactionType,
        amountCents: Long,
        category: ExpenseCategory,
        wallet: WalletType,
        note: String
    ) {
        viewModelScope.launch {
            moneyRepository.upsertTransaction(
                MoneyTransaction(
                    type = type,
                    amountCents = amountCents,
                    category = category,
                    wallet = wallet,
                    note = note,
                    timestampMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveQuickNote(
        title: String,
        content: String,
        tags: List<String>,
        isPinned: Boolean
    ) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            brainRepository.upsertNote(
                NoteItem(
                    title = title,
                    content = content,
                    tags = tags,
                    isPinned = isPinned,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
        }
    }

    fun saveQuickTask(
        title: String,
        priority: TaskPriority,
        dueDateEpochDay: Long?
    ) {
        viewModelScope.launch {
            brainRepository.upsertTask(
                TaskItem(
                    title = title,
                    priority = priority,
                    dueDateEpochDay = dueDateEpochDay,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveQuickSnippet(
        label: String,
        value: String,
        category: SnippetCategory,
        isSensitive: Boolean,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            brainRepository.upsertSnippet(
                SnippetItem(
                    label = label,
                    value = value,
                    category = category,
                    isSensitive = isSensitive,
                    isPinned = isPinned,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveQuickExpiry(
        title: String,
        category: ExpiryCategory,
        expiryEpochDay: Long,
        remindDaysBefore: Int
    ) {
        viewModelScope.launch {
            lifeAdminRepository.upsertExpiry(
                ExpiryItem(
                    title = title,
                    category = category,
                    expiryEpochDay = expiryEpochDay,
                    remindDaysBefore = remindDaysBefore,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }
}
