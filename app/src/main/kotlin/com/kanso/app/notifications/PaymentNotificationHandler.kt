package com.kanso.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kanso.app.MainActivity
import com.kanso.app.R
import com.kanso.app.navigation.KansoUris
import com.kanso.app.navigation.createLaunchIntent
import com.kanso.app.sync.DriveSyncScheduler
import com.kanso.app.widget.KansoWidgetUpdater
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.MoneyRepository
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.DetectedPaymentAlert
import com.kanso.core.model.KnownFinanceApps
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.PaymentAlertDeduplicator
import com.kanso.core.model.PaymentCaptureMode
import com.kanso.core.model.PaymentNotificationLogEntry
import com.kanso.core.model.PaymentNotificationParser
import com.kanso.core.model.PaymentParseResult
import com.kanso.core.model.PaymentParseSkipReason
import com.kanso.core.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentNotificationHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val moneyRepository: MoneyRepository
) {
    private val deduplicator = PaymentAlertDeduplicator()
    private val notificationIdCounter = AtomicInteger(NOTIFICATION_ID_START)
    private val logIdGenerator = AtomicLong(System.currentTimeMillis())
    private val logJson = Json { ignoreUnknownKeys = true }
    private val logPrefs by lazy {
        context.getSharedPreferences(LOG_PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val logLock = Any()

    private val _recentDetections = MutableStateFlow<List<DetectedPaymentAlert>>(emptyList())
    val recentDetections: StateFlow<List<DetectedPaymentAlert>> = _recentDetections.asStateFlow()

    private val _recentLogs = MutableStateFlow(loadPrunedLogs(System.currentTimeMillis()))
    val recentLogs: StateFlow<List<PaymentNotificationLogEntry>> = _recentLogs.asStateFlow()

    fun clearRecentLogs() {
        synchronized(logLock) {
            logPrefs.edit().remove(KEY_LOGS_JSON).apply()
            _recentLogs.value = emptyList()
        }
    }

    suspend fun processIncomingNotification(
        packageName: String,
        appLabel: String,
        title: String,
        text: String,
        timestampMillis: Long = System.currentTimeMillis(),
        isTestSimulation: Boolean = false
    ): Boolean {
        val prefs = preferencesRepository.getCurrentPreferences()
        if (!isTestSimulation) {
            if (!prefs.paymentLoggerEnabled) return false
            val shouldInspect = PaymentNotificationParser.shouldInspectPackage(
                packageName = packageName,
                selfPackageName = context.packageName,
                allowAllApps = prefs.paymentAllowAllApps,
                allowedPackages = prefs.paymentAllowedPackages
            )
            if (!shouldInspect) return false
        }

        val resolvedLabel = appLabel.trim().ifEmpty {
            KnownFinanceApps.findByPackage(packageName)?.displayName ?: packageName
        }

        val alert = when (
            val parseResult = PaymentNotificationParser.parseWithReason(
                packageName = packageName,
                appLabel = resolvedLabel,
                title = title,
                text = text,
                userCurrency = prefs.currency,
                customRatesVsUsd = prefs.customRatesVsUsd,
                timestampMillis = timestampMillis
            )
        ) {
            is PaymentParseResult.Skipped -> {
                appendLogEntry(
                    packageName = packageName,
                    sourceAppLabel = resolvedLabel,
                    title = title,
                    text = text,
                    timestampMillis = timestampMillis,
                    matchedAlert = null,
                    skipReason = parseResult.reason
                )
                return false
            }

            is PaymentParseResult.Matched -> parseResult.alert
        }

        if (!isTestSimulation && !deduplicator.shouldProcess(alert.fingerprint, timestampMillis)) {
            appendLogEntry(
                packageName = packageName,
                sourceAppLabel = resolvedLabel,
                title = title,
                text = text,
                timestampMillis = timestampMillis,
                matchedAlert = null,
                skipReason = PaymentParseSkipReason.DUPLICATE
            )
            return false
        }

        appendLogEntry(
            packageName = packageName,
            sourceAppLabel = resolvedLabel,
            title = title,
            text = text,
            timestampMillis = timestampMillis,
            matchedAlert = alert,
            skipReason = null
        )

        _recentDetections.update { existing ->
            (listOf(alert) + existing).take(MAX_RECENT_DETECTIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        ensureChannel(context)
        val notifId = nextNotificationId()
        val formattedAmount = CurrencyFormatter.format(alert.amountCents, prefs.currency)

        return when (prefs.paymentCaptureMode) {
            PaymentCaptureMode.PROMPT -> {
                postPromptNotification(notifId, formattedAmount, alert)
                true
            }

            PaymentCaptureMode.AUTO_SAVE -> {
                val savedId = moneyRepository.upsertTransaction(
                    MoneyTransaction(
                        type = alert.type,
                        amountCents = alert.amountCents,
                        category = alert.category,
                        wallet = alert.wallet,
                        note = alert.note,
                        timestampMillis = alert.timestampMillis
                    )
                )
                KansoWidgetUpdater.updateAll(context)
                val driveConfig = preferencesRepository.getDriveSyncConfig()
                if (driveConfig.isConnected && driveConfig.autoSyncEnabled && driveConfig.hasPassphrase) {
                    DriveSyncScheduler.enqueueBackgroundSync(context)
                }
                postAutoSavedNotification(notifId, savedId, formattedAmount, alert)
                true
            }
        }
    }

    private fun postPromptNotification(
        notifId: Int,
        formattedAmount: String,
        alert: DetectedPaymentAlert
    ) {
        val isIncome = alert.type == TransactionType.INCOME
        val prefix = if (isIncome) "Income " else ""
        val title = "Log $prefix$formattedAmount · ${alert.category.label}?"
        val body = "${alert.note} · ${alert.wallet.label} (via ${alert.sourceAppLabel})"

        val editIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_EDIT_DETECTED_PAYMENT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putNotificationExtras(notifId, alert)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId * 10 + 1,
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val editActionPendingIntent = PendingIntent.getActivity(
            context,
            notifId * 10 + 2,
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val saveIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SAVE_DETECTED_PAYMENT
            putNotificationExtras(notifId, alert)
        }
        val savePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 10 + 3,
            saveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_DETECTED_PAYMENT
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 10 + 4,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_kanso)
            .setColor(0xFF0F766E.toInt())
            .setContentTitle("Payment notification detected")
            .setContentText("Tap to review in Kanso")
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_kanso)
            .setColor(0xFF0F766E.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Save", savePendingIntent)
            .addAction(0, "Edit", editActionPendingIntent)
            .addAction(0, "Ignore", dismissPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    private fun postAutoSavedNotification(
        notifId: Int,
        savedTxId: Long,
        formattedAmount: String,
        alert: DetectedPaymentAlert
    ) {
        val title = "Auto-logged $formattedAmount · ${alert.note}"
        val body = "${alert.category.label} · ${alert.wallet.label} (via ${alert.sourceAppLabel})"

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId * 10 + 1,
            createLaunchIntent(context, KansoUris.MONEY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val undoIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_UNDO_AUTO_PAYMENT
            putExtra(EXTRA_SAVED_TX_ID, savedTxId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val undoPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 10 + 5,
            undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_kanso)
            .setColor(0xFF0F766E.toInt())
            .setContentTitle("Transaction auto-logged")
            .setContentText("Open Kanso to view")
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_kanso)
            .setColor(0xFF0F766E.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Undo", undoPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    private fun Intent.putNotificationExtras(notifId: Int, alert: DetectedPaymentAlert) {
        putExtra(EXTRA_NOTIFICATION_ID, notifId)
        putExtra(EXTRA_IS_INCOME, alert.type == TransactionType.INCOME)
        putExtra(EXTRA_AMOUNT_CENTS, alert.amountCents)
        putExtra(EXTRA_CATEGORY, alert.category.name)
        putExtra(EXTRA_WALLET, alert.wallet.name)
        putExtra(EXTRA_NOTE, alert.note)
        putExtra(EXTRA_TIMESTAMP_MILLIS, alert.timestampMillis)
    }

    private fun nextNotificationId(): Int {
        val current = notificationIdCounter.getAndIncrement()
        if (current >= NOTIFICATION_ID_END) {
            notificationIdCounter.compareAndSet(current + 1, NOTIFICATION_ID_START)
        }
        return NOTIFICATION_ID_START + ((current - NOTIFICATION_ID_START) % (NOTIFICATION_ID_END - NOTIFICATION_ID_START + 1))
    }

    private fun appendLogEntry(
        packageName: String,
        sourceAppLabel: String,
        title: String,
        text: String,
        timestampMillis: Long,
        matchedAlert: DetectedPaymentAlert?,
        skipReason: PaymentParseSkipReason?
    ) {
        val (safeTitle, safeText) = if (skipReason == PaymentParseSkipReason.OTP) {
            "OTP notification" to "Redacted for privacy"
        } else {
            title.trim().take(120) to text.trim().take(300)
        }
        val entry = PaymentNotificationLogEntry(
            id = logIdGenerator.incrementAndGet(),
            packageName = packageName,
            sourceAppLabel = sourceAppLabel,
            title = safeTitle,
            text = safeText,
            timestampMillis = timestampMillis,
            matchedAlert = matchedAlert,
            skipReason = skipReason
        )
        synchronized(logLock) {
            val existing = loadPrunedLogs(timestampMillis)
            val updated = (listOf(entry) + existing).take(MAX_LOG_ENTRIES)
            logPrefs.edit().putString(KEY_LOGS_JSON, logJson.encodeToString(updated)).apply()
            _recentLogs.value = updated
        }
    }

    private fun loadPrunedLogs(nowMillis: Long): List<PaymentNotificationLogEntry> {
        val raw = runCatching { logPrefs.getString(KEY_LOGS_JSON, null) }.getOrNull() ?: return emptyList()
        val parsed = runCatching {
            logJson.decodeFromString<List<PaymentNotificationLogEntry>>(raw)
        }.getOrNull().orEmpty()
        val cutoff = nowMillis - LOG_RETENTION_MS
        return parsed
            .filter { it.timestampMillis >= cutoff }
            .sortedByDescending { it.timestampMillis }
            .take(MAX_LOG_ENTRIES)
    }

    companion object {
        const val CHANNEL_ID = "kanso_payment_capture"
        private const val NOTIFICATION_ID_START = 4000
        private const val NOTIFICATION_ID_END = 4499
        private const val MAX_RECENT_DETECTIONS = 5
        private const val MAX_LOG_ENTRIES = 15
        private const val LOG_RETENTION_MS = 24L * 60L * 60L * 1000L
        private const val LOG_PREFS_NAME = "kanso_payment_logs"
        private const val KEY_LOGS_JSON = "recent_logs_json"

        const val ACTION_SAVE_DETECTED_PAYMENT = "com.kanso.app.notifications.ACTION_SAVE_DETECTED_PAYMENT"
        const val ACTION_DISMISS_DETECTED_PAYMENT = "com.kanso.app.notifications.ACTION_DISMISS_DETECTED_PAYMENT"
        const val ACTION_EDIT_DETECTED_PAYMENT = "com.kanso.app.notifications.ACTION_EDIT_DETECTED_PAYMENT"
        const val ACTION_UNDO_AUTO_PAYMENT = "com.kanso.app.notifications.ACTION_UNDO_AUTO_PAYMENT"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_IS_INCOME = "extra_is_income"
        const val EXTRA_AMOUNT_CENTS = "extra_amount_cents"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_WALLET = "extra_wallet"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_TIMESTAMP_MILLIS = "extra_timestamp_millis"
        const val EXTRA_SAVED_TX_ID = "extra_saved_tx_id"

        fun isNotificationAccessGranted(context: Context): Boolean {
            return NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }

        fun createNotificationAccessIntent(): Intent {
            return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                ?: return true
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }

        fun createBatteryOptimizationSettingsIntent(): Intent {
            return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Payment Auto-Logger",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "1-tap confirmation prompts and undo alerts for detected bank & e-wallet notifications."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
