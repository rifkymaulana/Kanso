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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kanso.app.MainActivity
import com.kanso.app.R
import com.kanso.app.navigation.KansoUris
import com.kanso.app.navigation.createLaunchIntent
import com.kanso.app.widget.KansoWidgetUpdater
import com.kanso.core.database.dao.BrainDao
import com.kanso.core.database.dao.HabitsDao
import com.kanso.core.database.dao.LifeAdminDao
import com.kanso.core.database.dao.MoneyDao
import com.kanso.core.database.entity.toDomain
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.model.BriefingCalculations
import com.kanso.core.model.BriefingSlot
import com.kanso.core.model.LifeAdminCalculations
import com.kanso.core.model.MoneyCalculations
import com.kanso.core.model.UserPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyReminderEntryPoint {
    fun moneyDao(): MoneyDao
    fun brainDao(): BrainDao
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
        val slot = inputData.getString(KEY_SLOT)?.let { raw ->
            runCatching { BriefingSlot.valueOf(raw) }.getOrNull()
        } ?: BriefingSlot.MORNING

        val slotEnabled = when (slot) {
            BriefingSlot.MORNING -> prefs.remindersEnabled && prefs.morningReminderEnabled
            BriefingSlot.EVENING -> prefs.remindersEnabled && prefs.eveningReminderEnabled
        }

        try {
            if (!slotEnabled) return Result.success()

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

            val openDueTasks = entryPoint.brainDao().getAllTasks()
                .map { it.toDomain() }
                .count { task ->
                    val due = task.dueDateEpochDay
                    !task.isCompleted && due != null && due <= todayEpoch
                }

            val activeHabits = entryPoint.habitsDao().getAllHabits()
                .filter { !it.isArchived }
            val completedIds = entryPoint.habitsDao().getAllCompletions()
                .filter { it.epochDay == todayEpoch }
                .map { it.habitId }
                .toSet()

            val briefing = when (slot) {
                BriefingSlot.MORNING -> {
                    val dueBills = entryPoint.moneyDao().getAllBills()
                        .map { it.toDomain() }
                        .count { MoneyCalculations.isBillDueSoon(it, today, withinDays = 5) }

                    val dueIous = entryPoint.moneyDao().getAllIous()
                        .map { it.toDomain() }
                        .count { MoneyCalculations.isIouDueSoon(it, todayEpoch, withinDays = 3) }

                    val attentionExpiries = entryPoint.lifeAdminDao().getAllExpiries()
                        .map { it.toDomain() }
                        .count { LifeAdminCalculations.needsAttention(it, todayEpoch) }

                    val dueMaintenance = LifeAdminCalculations.activeDueMaintenanceLogs(
                        logs = entryPoint.lifeAdminDao().getAllMaintenanceLogs().map { it.toDomain() },
                        todayEpochDay = todayEpoch,
                        withinDays = 14
                    ).size

                    val morningHabits = activeHabits.count { habit ->
                        !completedIds.contains(habit.id) &&
                            habit.reminderHour != null &&
                            habit.reminderHour!! <= 12
                    }

                    BriefingCalculations.buildMorningBriefing(
                        dueBillsCount = dueBills,
                        dueTasksCount = openDueTasks,
                        expiringCount = attentionExpiries,
                        dueMaintenanceCount = dueMaintenance,
                        dueIousCount = dueIous,
                        morningHabitsCount = morningHabits
                    )
                }

                BriefingSlot.EVENING -> {
                    val pendingHabits = activeHabits.count { !completedIds.contains(it.id) }
                    val todayWaterMl = entryPoint.habitsDao().getHealthLog(todayEpoch)?.waterMl ?: 0
                    val waterRemainingMl = (prefs.waterGoalMl - todayWaterMl).coerceAtLeast(0)

                    BriefingCalculations.buildEveningCheckIn(
                        pendingHabitsCount = pendingHabits,
                        waterRemainingMl = waterRemainingMl,
                        dueTasksCount = openDueTasks
                    )
                }
            }

            if (briefing != null) {
                ensureChannel(applicationContext)
                val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    if (slot == BriefingSlot.MORNING) 1001 else 1002,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val addWaterIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_ADD_WATER_250
                }
                val addWaterPendingIntent = PendingIntent.getBroadcast(
                    applicationContext,
                    1010,
                    addWaterIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val secondaryLabel: String
                val secondaryPendingIntent: PendingIntent
                if (slot == BriefingSlot.MORNING) {
                    secondaryLabel = "Quick Capture"
                    secondaryPendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        1011,
                        createLaunchIntent(applicationContext, KansoUris.QUICK_CAPTURE_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                } else {
                    secondaryLabel = "Open Habits"
                    secondaryPendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        1012,
                        createLaunchIntent(applicationContext, KansoUris.HABITS),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentTitle(briefing.title)
                    .setContentText(briefing.body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(briefing.body))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .addAction(0, "+250ml Water", addWaterPendingIntent)
                    .addAction(0, secondaryLabel, secondaryPendingIntent)

                if (prefs.appLockEnabled || prefs.hideBalances) {
                    val publicNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle(briefing.title)
                        .setContentText(briefing.publicRedactedBody)
                        .setAutoCancel(true)
                        .setContentIntent(contentPendingIntent)
                        .build()
                    builder
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        .setPublicVersion(publicNotification)
                } else {
                    builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                }

                val notificationId = when (slot) {
                    BriefingSlot.MORNING -> NOTIFICATION_ID_MORNING
                    BriefingSlot.EVENING -> NOTIFICATION_ID_EVENING
                }
                NotificationManagerCompat.from(applicationContext).notify(notificationId, builder.build())
            }

            KansoWidgetUpdater.updateAll(applicationContext)
        } finally {
            if (slotEnabled) {
                KansoReminderScheduler.scheduleNextOccurrenceAfterRun(
                    context = applicationContext,
                    slot = slot,
                    prefs = prefs
                )
            }
        }

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "kanso_daily_reminders"
        const val KEY_SLOT = "briefing_slot"
        const val NOTIFICATION_ID_MORNING = 2026
        const val NOTIFICATION_ID_EVENING = 2027

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
                    description = "Offline briefings for upcoming bills, expiring documents, maintenance, tasks, and habits."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}

object KansoReminderScheduler {
    private const val LEGACY_UNIQUE_WORK_NAME = "kanso_daily_reminder_work"
    private const val MORNING_WORK_NAME = "kanso_morning_briefing_work"
    private const val EVENING_WORK_NAME = "kanso_evening_checkin_work"

    fun syncSchedule(
        context: Context,
        prefs: UserPreferences,
        replaceExisting: Boolean = false
    ) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(LEGACY_UNIQUE_WORK_NAME)

        if (!prefs.remindersEnabled) {
            workManager.cancelUniqueWork(MORNING_WORK_NAME)
            workManager.cancelUniqueWork(EVENING_WORK_NAME)
            return
        }

        DailyReminderWorker.ensureChannel(context)
        val policy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

        if (prefs.morningReminderEnabled) {
            enqueueSlot(
                workManager = workManager,
                slot = BriefingSlot.MORNING,
                hour = prefs.morningReminderHour,
                minute = prefs.morningReminderMinute,
                policy = policy
            )
        } else {
            workManager.cancelUniqueWork(MORNING_WORK_NAME)
        }

        if (prefs.eveningReminderEnabled) {
            enqueueSlot(
                workManager = workManager,
                slot = BriefingSlot.EVENING,
                hour = prefs.eveningReminderHour,
                minute = prefs.eveningReminderMinute,
                policy = policy
            )
        } else {
            workManager.cancelUniqueWork(EVENING_WORK_NAME)
        }
    }

    internal fun scheduleNextOccurrenceAfterRun(
        context: Context,
        slot: BriefingSlot,
        prefs: UserPreferences
    ) {
        val workManager = WorkManager.getInstance(context)
        val (hour, minute) = when (slot) {
            BriefingSlot.MORNING -> prefs.morningReminderHour to prefs.morningReminderMinute
            BriefingSlot.EVENING -> prefs.eveningReminderHour to prefs.eveningReminderMinute
        }
        enqueueSlot(
            workManager = workManager,
            slot = slot,
            hour = hour,
            minute = minute,
            policy = ExistingWorkPolicy.APPEND_OR_REPLACE
        )
    }

    private fun enqueueSlot(
        workManager: WorkManager,
        slot: BriefingSlot,
        hour: Int,
        minute: Int,
        policy: ExistingWorkPolicy
    ) {
        val delayMillis = BriefingCalculations.computeDelayUntilNextTriggerMillis(
            now = LocalDateTime.now(),
            targetHour = hour,
            targetMinute = minute
        )
        val workName = when (slot) {
            BriefingSlot.MORNING -> MORNING_WORK_NAME
            BriefingSlot.EVENING -> EVENING_WORK_NAME
        }
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(DailyReminderWorker.KEY_SLOT to slot.name))
            .build()
        workManager.enqueueUniqueWork(workName, policy, request)
    }
}
