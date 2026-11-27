package com.kanso.feature.lifeadmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.LifeAdminRepository
import com.kanso.core.database.repository.MoneyRepository
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.ExpiryCategory
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.LifeAdminCalculations
import com.kanso.core.model.MaintenanceCategory
import com.kanso.core.model.MaintenanceLog
import com.kanso.core.model.MoneyCalculations
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.TransactionType
import com.kanso.core.model.VaultCategory
import com.kanso.core.model.VaultItem
import com.kanso.core.model.WalletType
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
    private val moneyRepository: MoneyRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    data class MaintenanceSaveResult(
        val logId: Long,
        val expenseTransactionId: Long?
    )

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

    fun restoreExpiry(item: ExpiryItem) {
        viewModelScope.launch {
            lifeAdminRepository.upsertExpiry(item)
        }
    }

    fun renewExpiryOneYear(
        item: ExpiryItem,
        onRenewed: (previous: ExpiryItem, newExpiryEpochDay: Long) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val todayEpoch = LocalDate.now().toEpochDay()
            val newExpiryEpochDay = LifeAdminCalculations.renewedExpiryEpochDay(
                currentExpiryEpochDay = item.expiryEpochDay,
                todayEpochDay = todayEpoch
            )
            lifeAdminRepository.upsertExpiry(
                item.copy(
                    expiryEpochDay = newExpiryEpochDay,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            onRenewed(item, newExpiryEpochDay)
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
        note: String,
        recordAsExpense: Boolean = false,
        expenseWallet: WalletType = WalletType.EWALLET,
        onSaved: (MaintenanceSaveResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            val trimmedAsset = assetName.trim()
            val trimmedDesc = serviceDescription.trim()
            val logId = lifeAdminRepository.upsertMaintenanceLog(
                MaintenanceLog(
                    id = existing?.id ?: 0L,
                    assetName = trimmedAsset,
                    category = category,
                    serviceDescription = trimmedDesc,
                    serviceEpochDay = serviceEpochDay,
                    odometerKm = odometerKm,
                    costCents = costCents,
                    nextDueEpochDay = nextDueEpochDay,
                    nextDueOdometerKm = nextDueOdometerKm,
                    note = note.trim()
                )
            )
            val txId = if (existing == null && recordAsExpense && costCents != null && costCents > 0L) {
                val txTimestamp = MoneyCalculations.resolveTransactionTimestampMillis(
                    selectedEpochDay = serviceEpochDay,
                    existingTimestampMillis = null,
                    nowMillis = System.currentTimeMillis()
                )
                moneyRepository.upsertTransaction(
                    MoneyTransaction(
                        type = TransactionType.EXPENSE,
                        amountCents = costCents,
                        category = LifeAdminCalculations.mapMaintenanceExpenseCategory(category),
                        wallet = expenseWallet,
                        note = "Service: $trimmedAsset — $trimmedDesc",
                        timestampMillis = txTimestamp
                    )
                )
            } else {
                null
            }
            onSaved(MaintenanceSaveResult(logId = logId, expenseTransactionId = txId))
        }
    }

    fun undoNewMaintenanceWithExpense(result: MaintenanceSaveResult) {
        viewModelScope.launch {
            lifeAdminRepository.deleteMaintenanceLog(result.logId)
            if (result.expenseTransactionId != null) {
                moneyRepository.deleteTransaction(result.expenseTransactionId)
            }
        }
    }

    fun deleteMaintenanceLog(id: Long) {
        viewModelScope.launch {
            lifeAdminRepository.deleteMaintenanceLog(id)
        }
    }

    fun restoreMaintenanceLog(log: MaintenanceLog) {
        viewModelScope.launch {
            lifeAdminRepository.upsertMaintenanceLog(log)
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

    fun restoreVaultItem(item: VaultItem) {
        viewModelScope.launch {
            lifeAdminRepository.upsertVaultItem(item)
        }
    }
}
