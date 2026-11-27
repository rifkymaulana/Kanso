package com.kanso.app.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import com.kanso.app.MainActivity
import com.kanso.core.ui.components.QuickCaptureMode
import java.net.URI

data class SharedCapturePayload(
    val text: String = "",
    val subject: String = "",
    val isReceiptMedia: Boolean = false,
    val isScanning: Boolean = false,
    val previewBitmap: ImageBitmap? = null
)

sealed interface LaunchCommand {
    data class OpenQuickCapture(val mode: QuickCaptureMode) : LaunchCommand
    data object OpenReceiptScanner : LaunchCommand
    data object OpenSharedCapture : LaunchCommand
    data class OpenBrainTab(val tab: Int) : LaunchCommand
    data class OpenUtilitiesTab(val tab: Int) : LaunchCommand
    data object OpenHabits : LaunchCommand
    data class OpenLifeAdminTab(val tab: Int) : LaunchCommand
    data object OpenMoney : LaunchCommand
}

object KansoUris {
    const val QUICK_CAPTURE_EXPENSE = "kanso://quick-capture/expense"
    const val QUICK_CAPTURE_INCOME = "kanso://quick-capture/income"
    const val QUICK_CAPTURE_NOTE = "kanso://quick-capture/note"
    const val QUICK_CAPTURE_TASK = "kanso://quick-capture/task"
    const val QUICK_CAPTURE_SNIPPET = "kanso://quick-capture/snippet"
    const val QUICK_CAPTURE_EXPIRY = "kanso://quick-capture/expiry"
    const val QUICK_CAPTURE_SCAN_RECEIPT = "kanso://quick-capture/scan-receipt"
    const val BRAIN_NOTES = "kanso://brain/notes"
    const val BRAIN_TASKS = "kanso://brain/tasks"
    const val BRAIN_SNIPPETS = "kanso://brain/snippets"
    const val UTILITIES_QR = "kanso://utilities/qr"
    const val UTILITIES_CONVERT = "kanso://utilities/convert"
    const val UTILITIES_FOCUS = "kanso://utilities/focus"
    const val UTILITIES_DATES = "kanso://utilities/dates"
    const val HABITS = "kanso://habits"
    const val LIFEADMIN_EXPIRIES = "kanso://lifeadmin/expiries"
    const val LIFEADMIN_MAINTENANCE = "kanso://lifeadmin/maintenance"
    const val LIFEADMIN_VAULT = "kanso://lifeadmin/vault"
    const val MONEY = "kanso://money"
}

object LaunchCommandParser {

    fun parseUri(uriString: String?): LaunchCommand? {
        if (uriString.isNullOrBlank()) return null
        val parsed = runCatching { URI(uriString.trim()) }.getOrNull() ?: return null
        if (!parsed.scheme.equals("kanso", ignoreCase = true)) return null

        val host = parsed.host?.lowercase().orEmpty()
        val path = parsed.path?.trim('/')?.lowercase().orEmpty()

        return when (host) {
            "quick-capture" -> {
                if (path == "scan-receipt") {
                    return LaunchCommand.OpenReceiptScanner
                }
                val mode = when (path) {
                    "income" -> QuickCaptureMode.INCOME
                    "note" -> QuickCaptureMode.NOTE
                    "task" -> QuickCaptureMode.TASK
                    "snippet" -> QuickCaptureMode.SNIPPET
                    "expiry" -> QuickCaptureMode.EXPIRY
                    "", "expense" -> QuickCaptureMode.EXPENSE
                    else -> return null
                }
                LaunchCommand.OpenQuickCapture(mode)
            }

            "brain" -> when (path) {
                "", "notes" -> LaunchCommand.OpenBrainTab(0)
                "tasks" -> LaunchCommand.OpenBrainTab(1)
                "snippets" -> LaunchCommand.OpenBrainTab(2)
                else -> null
            }

            "utilities" -> when (path) {
                "", "qr" -> LaunchCommand.OpenUtilitiesTab(0)
                "convert" -> LaunchCommand.OpenUtilitiesTab(1)
                "focus" -> LaunchCommand.OpenUtilitiesTab(2)
                "dates" -> LaunchCommand.OpenUtilitiesTab(3)
                else -> null
            }

            "habits" -> if (path.isEmpty()) LaunchCommand.OpenHabits else null

            "lifeadmin" -> when (path) {
                "", "expiries" -> LaunchCommand.OpenLifeAdminTab(0)
                "maintenance" -> LaunchCommand.OpenLifeAdminTab(1)
                "vault" -> LaunchCommand.OpenLifeAdminTab(2)
                else -> null
            }

            "money" -> if (path.isEmpty()) LaunchCommand.OpenMoney else null

            else -> null
        }
    }
}

fun createLaunchIntent(context: Context, uriString: String): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse(uriString), context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
}
