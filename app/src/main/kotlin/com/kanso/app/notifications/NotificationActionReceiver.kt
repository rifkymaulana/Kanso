package com.kanso.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.kanso.app.sync.DriveSyncScheduler
import com.kanso.app.widget.KansoWidgetUpdater
import com.kanso.core.database.dao.HabitsDao
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.HabitsRepository
import com.kanso.core.database.repository.MoneyRepository
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.TransactionType
import com.kanso.core.model.WalletType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationActionEntryPoint {
    fun habitsRepository(): HabitsRepository
    fun habitsDao(): HabitsDao
    fun moneyRepository(): MoneyRepository
    fun preferencesRepository(): PreferencesRepository
}

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appContext = context.applicationContext

        when (action) {
            ACTION_ADD_WATER_250 -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            appContext,
                            NotificationActionEntryPoint::class.java
                        )
                        val todayEpoch = LocalDate.now().toEpochDay()
                        entryPoint.habitsRepository().adjustWater(todayEpoch, 250)
                        KansoWidgetUpdater.updateAll(appContext)

                        val prefs = entryPoint.preferencesRepository().getCurrentPreferences()
                        val updatedWater = entryPoint.habitsDao().getHealthLog(todayEpoch)?.waterMl ?: 0
                        val activeHabits = entryPoint.habitsDao().getAllHabits().filter { !it.isArchived }
                        val completedIds = entryPoint.habitsDao().getAllCompletions()
                            .filter { it.epochDay == todayEpoch }
                            .map { it.habitId }
                            .toSet()
                        val pendingHabits = activeHabits.count { !completedIds.contains(it.id) }
                        if (updatedWater >= prefs.waterGoalMl && pendingHabits == 0) {
                            NotificationManagerCompat.from(appContext)
                                .cancel(DailyReminderWorker.NOTIFICATION_ID_EVENING)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            PaymentNotificationHandler.ACTION_SAVE_DETECTED_PAYMENT -> {
                val notifId = intent.getIntExtra(PaymentNotificationHandler.EXTRA_NOTIFICATION_ID, 0)
                val amountCents = intent.getLongExtra(PaymentNotificationHandler.EXTRA_AMOUNT_CENTS, 0L)
                if (amountCents <= 0L) {
                    if (notifId > 0) NotificationManagerCompat.from(appContext).cancel(notifId)
                    return
                }
                val isIncome = intent.getBooleanExtra(PaymentNotificationHandler.EXTRA_IS_INCOME, false)
                val categoryName = intent.getStringExtra(PaymentNotificationHandler.EXTRA_CATEGORY)
                val category = runCatching {
                    ExpenseCategory.valueOf(categoryName.orEmpty())
                }.getOrDefault(ExpenseCategory.OTHER)
                val walletName = intent.getStringExtra(PaymentNotificationHandler.EXTRA_WALLET)
                val wallet = runCatching {
                    WalletType.valueOf(walletName.orEmpty())
                }.getOrDefault(WalletType.EWALLET)
                val note = intent.getStringExtra(PaymentNotificationHandler.EXTRA_NOTE).orEmpty()
                val timestampMillis = intent.getLongExtra(
                    PaymentNotificationHandler.EXTRA_TIMESTAMP_MILLIS,
                    System.currentTimeMillis()
                )

                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            appContext,
                            NotificationActionEntryPoint::class.java
                        )
                        entryPoint.moneyRepository().upsertTransaction(
                            MoneyTransaction(
                                type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                                amountCents = amountCents,
                                category = category,
                                wallet = wallet,
                                note = note,
                                timestampMillis = timestampMillis
                            )
                        )
                        KansoWidgetUpdater.updateAll(appContext)
                        val driveConfig = entryPoint.preferencesRepository().getDriveSyncConfig()
                        if (driveConfig.isConnected && driveConfig.autoSyncEnabled && driveConfig.hasPassphrase) {
                            DriveSyncScheduler.enqueueBackgroundSync(appContext)
                        }
                        if (notifId > 0) {
                            NotificationManagerCompat.from(appContext).cancel(notifId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            PaymentNotificationHandler.ACTION_DISMISS_DETECTED_PAYMENT -> {
                val notifId = intent.getIntExtra(PaymentNotificationHandler.EXTRA_NOTIFICATION_ID, 0)
                if (notifId > 0) {
                    NotificationManagerCompat.from(appContext).cancel(notifId)
                }
            }

            PaymentNotificationHandler.ACTION_UNDO_AUTO_PAYMENT -> {
                val notifId = intent.getIntExtra(PaymentNotificationHandler.EXTRA_NOTIFICATION_ID, 0)
                val txId = intent.getLongExtra(PaymentNotificationHandler.EXTRA_SAVED_TX_ID, 0L)
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        if (txId > 0L) {
                            val entryPoint = EntryPointAccessors.fromApplication(
                                appContext,
                                NotificationActionEntryPoint::class.java
                            )
                            entryPoint.moneyRepository().deleteTransaction(txId)
                            KansoWidgetUpdater.updateAll(appContext)
                            val driveConfig = entryPoint.preferencesRepository().getDriveSyncConfig()
                            if (driveConfig.isConnected && driveConfig.autoSyncEnabled && driveConfig.hasPassphrase) {
                                DriveSyncScheduler.enqueueBackgroundSync(appContext)
                            }
                        }
                        if (notifId > 0) {
                            NotificationManagerCompat.from(appContext).cancel(notifId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_ADD_WATER_250 = "com.kanso.app.notifications.ACTION_ADD_WATER_250"
    }
}
