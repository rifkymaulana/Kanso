package com.kanso.app.update

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (updateState) {
                is AppUpdateState.UpdateAvailable -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Update ${updateState.release.tagName} available",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Current: v${updateState.currentVersion}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Row {
                            TextButton(onClick = { onDismiss(updateState.release.tagName) }) {
                                Text("Later")
                            }
                            Button(onClick = { onDownload(updateState.release) }) {
                                Text("Update")
                            }
                        }
                    }
                }

                is AppUpdateState.Downloading -> {
                    val total = updateState.totalBytes
                    val progress = if (total > 0L) {
                        (updateState.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    } else null

                    Text(
                        text = "Downloading ${updateState.release.tagName}...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                is AppUpdateState.Verifying -> {
                    Text(
                        text = "Verifying ${updateState.release.tagName} SHA-256...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is AppUpdateState.ReadyToInstall -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${updateState.release.tagName} verified & ready",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap Install to update Kanso.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Row {
                            TextButton(onClick = { onDismiss(updateState.release.tagName) }) {
                                Text("Later")
                            }
                            Button(onClick = { onInstall(updateState.apkFile) }) {
                                Text("Install")
                            }
                        }
                    }
                }

                else -> Unit
            }
        }
    }
}
