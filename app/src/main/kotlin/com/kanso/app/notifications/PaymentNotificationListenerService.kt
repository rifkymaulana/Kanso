package com.kanso.app.notifications

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kanso.core.model.KnownFinanceApps
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PaymentNotificationEntryPoint {
    fun paymentNotificationHandler(): PaymentNotificationHandler
}

class PaymentNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.isOngoing) return
        val notification = sbn.notification ?: return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val pkg = sbn.packageName?.takeIf { it.isNotBlank() } ?: return
        if (pkg.equals(packageName, ignoreCase = true)) return

        val extras = notification.extras ?: Bundle.EMPTY
        val title = (extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE))
            ?.toString()
            .orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val baseText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()

        val primaryText = bigText.ifBlank { baseText }
        val bodyText = if (subText.isNotBlank() && !primaryText.contains(subText, ignoreCase = true)) {
            listOf(primaryText, subText).filter { it.isNotBlank() }.joinToString("\n")
        } else {
            primaryText
        }

        if (title.isBlank() && bodyText.isBlank()) return

        val appLabel = KnownFinanceApps.findByPackage(pkg)?.displayName ?: runCatching {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(pkg)

        val timestamp = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()

        serviceScope.launch {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                PaymentNotificationEntryPoint::class.java
            )
            entryPoint.paymentNotificationHandler().processIncomingNotification(
                packageName = pkg,
                appLabel = appLabel,
                title = title,
                text = bodyText,
                timestampMillis = timestamp
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
