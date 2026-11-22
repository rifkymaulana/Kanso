package com.kanso.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kanso.app.MainActivity
import com.kanso.app.R
import com.kanso.app.widget.KansoWidgetUpdater
import com.kanso.core.database.dao.HabitsDao
import com.kanso.core.database.dao.LifeAdminDao
import com.kanso.core.database.dao.MoneyDao
import com.kanso.core.database.entity.toDomain
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.model.LifeAdminCalculations
import com.kanso.core.model.MoneyCalculations
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyReminderEntryPoint {
    fun moneyDao(): MoneyDao
    fun habitsDao(): HabitsDao
    fun lifeAdminDao(): LifeAdminDao
    fun preferencesRepository(): PreferencesRepository
}

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DailyReminderEntryPoint::class.java
        )
        val prefs = entryPoint.preferencesRepository().getCurrentPreferences()
        if (!prefs.remindersEnabled) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()

        val dueBills = entryPoint.moneyDao().getAllBills()
            .map { it.toDomain() }
            .count { MoneyCalculations.isBillDueSoon(it, today, withinDays = 5) }

        val attentionExpiries = entryPoint.lifeAdminDao().getAllExpiries()
            .map { it.toDomain() }
            .count { LifeAdminCalculations.needsAttention(it, todayEpoch) }

        val activeHabits = entryPoint.habitsDao().getAllHabits()
            .filter { !it.isArchived }
        val completedIds = entryPoint.habitsDao().getAllCompletions()
            .filter { it.epochDay == todayEpoch }
            .map { it.habitId }
            .toSet()
        val pendingHabits = activeHabits.count { !completedIds.contains(it.id) }

        val summaryParts = buildList {
            if (dueBills > 0) add("$dueBills recurring bill(s) due")
            if (attentionExpiries > 0) add("$attentionExpiries expiring item(s)")
            if (pendingHabits > 0) add("$pendingHabits habit(s) left today")
        }

        if (summaryParts.isNotEmpty()) {
            ensureChannel(applicationContext)
            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                1001,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Kanso Daily Check")
                .setContentText(summaryParts.joinToString(" • "))
                .setStyle(NotificationCompat.BigTextStyle().bigText(summaryParts.joinToString(" • ")))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }

        KansoWidgetUpdater.updateAll(applicationContext)

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "kanso_daily_reminders"
        private const val NOTIFICATION_ID = 2026

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Daily Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Offline alerts for expiring documents, upcoming bills, and daily habits."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}

object KansoReminderScheduler {
    private const val UNIQUE_WORK_NAME = "kanso_daily_reminder_work"

    fun syncSchedule(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }
        DailyReminderWorker.ensureChannel(context)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(12, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
