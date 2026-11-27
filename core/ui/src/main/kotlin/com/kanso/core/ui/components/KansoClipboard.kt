package com.kanso.core.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle

/**
 * Copies text to the system clipboard and marks sensitive secrets so Android 13+
 * redacts them in the system clipboard preview.
 */
object KansoClipboard {

    private const val EXTRA_IS_SENSITIVE_KEY = "android.content.extra.IS_SENSITIVE"

    fun copyText(
        context: Context,
        label: String,
        text: String,
        isSensitive: Boolean = false
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        val clip = ClipData.newPlainText(label, text)
        if (isSensitive) {
            val sensitiveKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ClipDescription.EXTRA_IS_SENSITIVE
            } else {
                EXTRA_IS_SENSITIVE_KEY
            }
            clip.description.extras = PersistableBundle().apply {
                putBoolean(sensitiveKey, true)
            }
        }
        clipboard.setPrimaryClip(clip)
    }
}
