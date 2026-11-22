package com.kanso.app.update

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kanso.core.ui.components.KansoAccentIconBadge
import com.kanso.core.ui.components.KansoAnimatedProgressBar
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoPrimaryButton
import java.io.File

fun launchApkInstall(
    context: Context,
    apkFile: File,
    canInstallPackages: () -> Boolean,
    createInstallIntent: (File) -> Intent,
    createUnknownSourcesIntent: () -> Intent,
    onMessage: (String) -> Unit
) {
    if (canInstallPackages()) {
        runCatching {
            context.startActivity(createInstallIntent(apkFile))
        }.onFailure {
            onMessage("Could not launch Android installer.")
        }
    } else {
        onMessage("Allow Kanso to install updates, then tap Install again.")
        runCatching {
            context.startActivity(createUnknownSourcesIntent())
        }
    }
}

@Composable
fun KansoUpdateBanner(
    updateState: AppUpdateState,
    dismissedTag: String?,
    onDownload: (AppReleaseInfo) -> Unit,
    onInstall: (File) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTag = when (updateState) {
        is AppUpdateState.UpdateAvailable -> updateState.release.tagName
        is AppUpdateState.Downloading -> updateState.release.tagName
        is AppUpdateState.Verifying -> updateState.release.tagName
        is AppUpdateState.ReadyToInstall -> updateState.release.tagName
        else -> null
    }
    if (activeTag == null || activeTag == dismissedTag) return

    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (updateState) {
                is AppUpdateState.UpdateAvailable -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KansoAccentIconBadge(
                            icon = Icons.Outlined.SystemUpdate,
                            accent = accent,
                            size = 38.dp,
                            iconSize = 20.dp,
                            cornerRadius = 12.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Update ${updateState.release.tagName} ready",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Installed v${updateState.currentVersion}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onDismiss(updateState.release.tagName) },
                                modifier = Modifier.height(KansoButtonSize.COMPACT.height),
                                shape = RoundedCornerShape(KansoButtonSize.COMPACT.cornerRadius),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Text("Later", maxLines = 1, softWrap = false)
                            }
                            KansoPrimaryButton(
                                text = "Update",
                                size = KansoButtonSize.COMPACT,
                                onClick = { onDownload(updateState.release) }
                            )
                        }
                    }
                }

                is AppUpdateState.Downloading -> {
                    val total = updateState.totalBytes
                    val progress = if (total > 0L) {
                        (updateState.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    } else 0.15f

                    Text(
                        text = "Downloading ${updateState.release.tagName}...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    KansoAnimatedProgressBar(
                        progress = progress,
                        height = 7.dp,
                        color = accent,
                        trackColor = accent.copy(alpha = 0.2f)
                    )
                }

                is AppUpdateState.Verifying -> {
                    Text(
                        text = "Verifying ${updateState.release.tagName} SHA-256...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    KansoAnimatedProgressBar(
                        progress = 0.95f,
                        height = 7.dp,
                        color = accent,
                        trackColor = accent.copy(alpha = 0.2f)
                    )
                }

                is AppUpdateState.ReadyToInstall -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KansoAccentIconBadge(
                            icon = Icons.Outlined.SystemUpdate,
                            accent = accent,
                            size = 38.dp,
                            iconSize = 20.dp,
                            cornerRadius = 12.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${updateState.release.tagName} verified & ready",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Tap Install to update Kanso.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onDismiss(updateState.release.tagName) },
                                modifier = Modifier.height(KansoButtonSize.COMPACT.height),
                                shape = RoundedCornerShape(KansoButtonSize.COMPACT.cornerRadius),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Text("Later", maxLines = 1, softWrap = false)
                            }
                            KansoPrimaryButton(
                                text = "Install",
                                size = KansoButtonSize.COMPACT,
                                onClick = { onInstall(updateState.apkFile) }
                            )
                        }
                    }
                }

                else -> Unit
            }
        }
    }
}
