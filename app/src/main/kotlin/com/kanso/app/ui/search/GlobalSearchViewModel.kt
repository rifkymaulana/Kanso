package com.kanso.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.BrainRepository
import com.kanso.core.database.repository.LifeAdminRepository
import com.kanso.core.database.repository.MoneyRepository
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.MaintenanceLog
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.NoteItem
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.TaskItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class GlobalSearchUiState(
    val query: String = "",
    val currency: AppCurrency = AppCurrency.IDR,
    val snippets: List<SnippetItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val transactions: List<MoneyTransaction> = emptyList(),
    val expiries: List<ExpiryItem> = emptyList(),
    val maintenanceLogs: List<MaintenanceLog> = emptyList()
) {
    val totalMatches: Int
        get() = snippets.size + notes.size + tasks.size + transactions.size + expiries.size + maintenanceLogs.size
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
    private val brainRepository: BrainRepository,
    private val lifeAdminRepository: LifeAdminRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    private val resultsFlow = queryFlow.flatMapLatest { raw ->
        val q = raw.trim()
        if (q.isEmpty()) {
            flowOf(SearchBundle())
        } else {
            val m1Flow = combine(
                brainRepository.searchSnippets(q),
                brainRepository.searchNotes(q),
                brainRepository.searchTasks(q),
                moneyRepository.searchTransactions(q)
            ) { snippets, notes, tasks, txs ->
                SearchBundle(snippets, notes, tasks, txs)
            }
            val m2Flow = combine(
                lifeAdminRepository.searchExpiries(q),
                lifeAdminRepository.searchMaintenanceLogs(q)
            ) { expiries, maintenance ->
                Pair(expiries, maintenance)
            }
            combine(m1Flow, m2Flow) { base, extra ->
                base.copy(expiries = extra.first, maintenanceLogs = extra.second)
            }
        }
    }

    val uiState: StateFlow<GlobalSearchUiState> = combine(
        queryFlow,
        preferencesRepository.preferences,
        resultsFlow
    ) { query, prefs, results ->
        GlobalSearchUiState(
            query = query,
            currency = prefs.currency,
            snippets = results.snippets,
            notes = results.notes,
            tasks = results.tasks,
            transactions = results.transactions,
            expiries = results.expiries,
            maintenanceLogs = results.maintenanceLogs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GlobalSearchUiState()
    )

    fun setQuery(query: String) {
        queryFlow.value = query
    }

    private data class SearchBundle(
        val snippets: List<SnippetItem> = emptyList(),
        val notes: List<NoteItem> = emptyList(),
        val tasks: List<TaskItem> = emptyList(),
        val transactions: List<MoneyTransaction> = emptyList(),
        val expiries: List<ExpiryItem> = emptyList(),
        val maintenanceLogs: List<MaintenanceLog> = emptyList()
    )
}
