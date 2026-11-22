package com.kanso.feature.lifeadmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.LifeAdminRepository
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.ExpiryCategory
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.LifeAdminCalculations
import com.kanso.core.model.MaintenanceCategory
import com.kanso.core.model.MaintenanceLog
import com.kanso.core.model.VaultCategory
import com.kanso.core.model.VaultItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LifeAdminUiState(
    val currency: AppCurrency = AppCurrency.IDR,
    val expiries: List<ExpiryItem> = emptyList(),
    val attentionExpiryCount: Int = 0,
    val maintenanceLogs: List<MaintenanceLog> = emptyList(),
    val isVaultUnlocked: Boolean = false,
    val vaultItems: List<VaultItem> = emptyList(),
    val vaultItemCount: Int = 0
)

@HiltViewModel
class LifeAdminViewModel @Inject constructor(
    private val lifeAdminRepository: LifeAdminRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val isVaultUnlockedFlow = MutableStateFlow(false)

    private val dataFlow = combine(
        lifeAdminRepository.observeExpiries(),
        lifeAdminRepository.observeMaintenanceLogs(),
        lifeAdminRepository.observeVaultItems()
    ) { expiries, logs, vaultItems ->
        Triple(expiries, logs, vaultItems)
    }

    val uiState: StateFlow<LifeAdminUiState> = combine(
        dataFlow,
        isVaultUnlockedFlow,
        preferencesRepository.preferences
    ) { triple, unlocked, prefs ->
        val (expiries, logs, vaultItems) = triple
        val todayEpoch = LocalDate.now().toEpochDay()
        val attentionCount = expiries.count { LifeAdminCalculations.needsAttention(it, todayEpoch) }

        LifeAdminUiState(
            currency = prefs.currency,
            expiries = expiries,
            attentionExpiryCount = attentionCount,
            maintenanceLogs = logs,
            isVaultUnlocked = unlocked,
            vaultItems = if (unlocked) vaultItems else emptyList(),
            vaultItemCount = vaultItems.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LifeAdminUiState()
    )

    fun unlockVault() {
        isVaultUnlockedFlow.value = true
    }

    fun lockVault() {
        isVaultUnlockedFlow.value = false
    }

    fun saveExpiry(
        existing: ExpiryItem?,
        title: String,
        category: ExpiryCategory,
        referenceNumber: String,
        expiryEpochDay: Long,
        remindDaysBefore: Int,
        note: String
    ) {
        viewModelScope.launch {
            lifeAdminRepository.upsertExpiry(
                ExpiryItem(
                    id = existing?.id ?: 0L,
                    title = title.trim(),
                    category = category,
                    referenceNumber = referenceNumber.trim(),
                    expiryEpochDay = expiryEpochDay,
                    remindDaysBefore = remindDaysBefore,
                    note = note.trim(),
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteExpiry(id: Long) {
        viewModelScope.launch {
            lifeAdminRepository.deleteExpiry(id)
        }
    }

    fun saveMaintenanceLog(
        existing: MaintenanceLog?,
        assetName: String,
        category: MaintenanceCategory,
        serviceDescription: String,
        serviceEpochDay: Long,
        odometerKm: Int?,
        costCents: Long?,
        nextDueEpochDay: Long?,
        nextDueOdometerKm: Int?,
        note: String
    ) {
        viewModelScope.launch {
            lifeAdminRepository.upsertMaintenanceLog(
                MaintenanceLog(
                    id = existing?.id ?: 0L,
                    assetName = assetName.trim(),
                    category = category,
                    serviceDescription = serviceDescription.trim(),
                    serviceEpochDay = serviceEpochDay,
                    odometerKm = odometerKm,
                    costCents = costCents,
                    nextDueEpochDay = nextDueEpochDay,
                    nextDueOdometerKm = nextDueOdometerKm,
                    note = note.trim()
                )
            )
        }
    }

    fun deleteMaintenanceLog(id: Long) {
        viewModelScope.launch {
            lifeAdminRepository.deleteMaintenanceLog(id)
        }
    }

    fun saveVaultItem(
        existing: VaultItem?,
        title: String,
        category: VaultCategory,
        secretContent: String
    ) {
        viewModelScope.launch {
            lifeAdminRepository.upsertVaultItem(
                VaultItem(
                    id = existing?.id ?: 0L,
                    title = title.trim(),
                    category = category,
                    secretContent = secretContent,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteVaultItem(id: Long) {
        viewModelScope.launch {
            lifeAdminRepository.deleteVaultItem(id)
        }
    }
}
