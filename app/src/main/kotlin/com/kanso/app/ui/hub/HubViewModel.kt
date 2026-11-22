package com.kanso.app.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.StorageStatsRepository
import com.kanso.core.model.DriveSyncConfig
import com.kanso.core.model.StorageStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HubUiState(
    val stats: StorageStats = StorageStats(0, 0, 0, 0),
    val lastBackupMillis: Long? = null,
    val driveSyncConfig: DriveSyncConfig = DriveSyncConfig()
)

@HiltViewModel
class HubViewModel @Inject constructor(
    storageStatsRepository: StorageStatsRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<HubUiState> = combine(
        storageStatsRepository.observeStats(),
        preferencesRepository.preferences,
        preferencesRepository.driveSyncConfig
    ) { stats, prefs, driveConfig ->
        HubUiState(
            stats = stats,
            lastBackupMillis = prefs.lastBackupMillis,
            driveSyncConfig = driveConfig
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HubUiState()
    )
}
