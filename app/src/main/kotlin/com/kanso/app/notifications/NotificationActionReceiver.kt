package com.kanso.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.kanso.app.widget.KansoWidgetUpdater
import com.kanso.core.database.dao.HabitsDao
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.HabitsRepository
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
    fun preferencesRepository(): PreferencesRepository
}

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_ADD_WATER_250) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
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

    companion object {
        const val ACTION_ADD_WATER_250 = "com.kanso.app.notifications.ACTION_ADD_WATER_250"
    }
}
