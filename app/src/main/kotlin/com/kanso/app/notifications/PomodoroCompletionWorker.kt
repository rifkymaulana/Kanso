package com.kanso.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
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
import com.kanso.app.R
import com.kanso.app.navigation.KansoUris
import com.kanso.app.navigation.createLaunchIntent
import com.kanso.core.database.preferences.PomodoroTimerManager
import com.kanso.core.model.PomodoroMode
import com.kanso.core.model.PomodoroState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PomodoroEntryPoint {
    fun pomodoroTimerManager(): PomodoroTimerManager
}

class PomodoroCompletionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PomodoroEntryPoint::class.java
        )
        val completedMode = entryPoint.pomodoroTimerManager().reconcileWithClock()
        if (completedMode != null) {
            postCompletionNotification(applicationContext, completedMode)
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "kanso_pomodoro_completion"
        private const val CHANNEL_ID = "kanso_pomodoro_timer"
        private const val NOTIFICATION_ID = 2028

        fun syncScheduledWork(context: Context, state: PomodoroState) {
            val workManager = WorkManager.getInstance(context)
            val endMillis = state.endEpochMillis
            if (!state.isRunning || endMillis == null) {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }
            val delayMillis = (endMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
            val request = OneTimeWorkRequestBuilder<PomodoroCompletionWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
            workManager.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelScheduledWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        fun postCompletionNotification(context: Context, mode: PomodoroMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            ensureChannel(context)
            val title = when (mode) {
                PomodoroMode.FOCUS -> "Focus session complete"
                PomodoroMode.SHORT_BREAK -> "Short break finished"
                PomodoroMode.LONG_BREAK -> "Long break finished"
            }
            val body = when (mode) {
                PomodoroMode.FOCUS -> "Great work! Time to take a short break in Kanso."
                PomodoroMode.SHORT_BREAK, PomodoroMode.LONG_BREAK -> "Ready to start your next focus sprint."
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                createLaunchIntent(context, KansoUris.UTILITIES_FOCUS),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tile_kanso)
                .setColor(0xFF0F766E.toInt())
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        private fun ensureChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Focus Timer",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when a Kanso Pomodoro focus or break session finishes."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
