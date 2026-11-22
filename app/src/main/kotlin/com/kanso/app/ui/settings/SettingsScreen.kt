package com.kanso.app.ui.settings

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.app.BuildConfig
import com.kanso.app.notifications.KansoReminderScheduler
import com.kanso.app.update.AppUpdateManager
import com.kanso.app.update.AppUpdateState
import com.kanso.app.update.launchApkInstall
import com.kanso.app.widget.KansoTodayWidgetReceiver
import com.kanso.core.backup.EncryptedBackupCrypto
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ThemeMode
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.feature.money.OverallBudgetDialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    val context = LocalContext.current

    var showNameDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showWaterGoalDialog by remember { mutableStateOf(false) }
    var showGithubTokenDialog by remember { mutableStateOf(false) }
    var autoInstallPending by remember { mutableStateOf(false) }

    var showExportPassphraseDialog by remember { mutableStateOf(false) }
    var pendingExportPassphrase by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(uiState.updateState) {
        val ready = uiState.updateState as? AppUpdateState.ReadyToInstall
        if (ready != null && autoInstallPending) {
            autoInstallPending = false
            launchApkInstall(
                context = context,
                apkFile = ready.apkFile,
                canInstallPackages = viewModel::canInstallPackages,
                createInstallIntent = viewModel::createInstallIntent,
                createUnknownSourcesIntent = viewModel::createUnknownSourcesIntent,
                onMessage = onShowMessage
            )
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setRemindersEnabled(granted)
        KansoReminderScheduler.syncSchedule(context, granted)
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val passphrase = pendingExportPassphrase
        pendingExportPassphrase = null
        if (uri != null && !passphrase.isNullOrEmpty()) {
            viewModel.exportBackup(uri, passphrase)
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Settings & Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsRow(
                            title = "Display Name",
                            subtitle = prefs.displayName.ifBlank { "Not set (shows neutral greeting)" },
                            onClick = { showNameDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "Default Currency",
                            subtitle = "${prefs.currency.code} (${prefs.currency.symbol}) — ${prefs.currency.displayName}",
                            onClick = { showCurrencyDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "Overall Monthly Budget",
                            subtitle = if (prefs.overallMonthlyBudgetCents > 0L) {
                                CurrencyFormatter.format(prefs.overallMonthlyBudgetCents, prefs.currency)
                            } else {
                                "Disabled"
                            },
                            onClick = { showBudgetDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "Daily Hydration Goal",
                            subtitle = "${prefs.waterGoalMl} ml",
                            onClick = { showWaterGoalDialog = true }
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Appearance & Alerts", style = MaterialTheme.typography.titleMedium)
                        KansoSegmentedRow(
                            options = ThemeMode.entries.map { it.label },
                            selectedIndex = ThemeMode.entries.indexOf(prefs.themeMode),
                            onSelect = { idx -> viewModel.setThemeMode(ThemeMode.entries[idx]) }
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Material You Dynamic Color", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "Adapt palette from your wallpaper",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = prefs.useDynamicColor,
                                    onCheckedChange = viewModel::setUseDynamicColor
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Local Daily Reminders", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "WorkManager alerts for bills, expiries, and habits",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.remindersEnabled,
                                onCheckedChange = { wantEnabled ->
                                    if (wantEnabled &&
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setRemindersEnabled(wantEnabled)
                                        KansoReminderScheduler.syncSchedule(context, wantEnabled)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Encrypted Backup & Restore", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Exports your entire database (including Vault secrets and preferences) into a single AES-256-GCM passphrase-encrypted .kanso.enc file. Pick Google Drive or local storage directly in Android's file picker.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = prefs.lastBackupMillis?.let {
                                "Last backup: ${KansoDateFormatters.formatFullDateTime(it)}"
                            } ?: "Last backup: Never backed up",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        when (val status = uiState.backupStatus) {
                            BackupOperationStatus.Idle -> Unit
                            is BackupOperationStatus.InProgress -> {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Text(status.label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            is BackupOperationStatus.Success -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = status.message,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            is BackupOperationStatus.Error -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = status.message,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        val busy = uiState.backupStatus is BackupOperationStatus.InProgress
                        Button(
                            onClick = {
                                viewModel.clearBackupStatus()
                                showExportPassphraseDialog = true
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = null)
                            Text("  Export Encrypted Backup")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.clearBackupStatus()
                                openBackupLauncher.launch(arrayOf("*/*"))
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Restore, contentDescription = null)
                            Text("  Restore from Backup")
                        }
                    }
                }
            }

            item {
                AppUpdatesCard(
                    uiState = uiState,
                    onToggleAutoCheck = viewModel::setAutoCheckUpdates,
                    onEditGithubToken = { showGithubTokenDialog = true },
                    onCheckUpdates = viewModel::checkForUpdates,
                    onDownloadAndInstall = { release ->
                        autoInstallPending = true
                        viewModel.downloadUpdate(release)
                    },
                    onInstallApk = { apkFile ->
                        launchApkInstall(
                            context = context,
                            apkFile = apkFile,
                            canInstallPackages = viewModel::canInstallPackages,
                            createInstallIntent = viewModel::createInstallIntent,
                            createUnknownSourcesIntent = viewModel::createUnknownSourcesIntent,
                            onMessage = onShowMessage
                        )
                    },
                    onOpenReleasesPage = {
                        runCatching { context.startActivity(viewModel.createReleasesPageIntent()) }
                    }
                )
            }

            item {
                var widgetPinMessage by remember { mutableStateOf<String?>(null) }
                val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Android Widgets & Shortcuts", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Add the Kanso Today widget to your home screen, long-press the app icon for quick actions (Expense, Note, QR, Snippets), or add the \"Kanso Capture\" Quick Settings tile.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                    val provider = ComponentName(context, KansoTodayWidgetReceiver::class.java)
                                    val requested = appWidgetManager.requestPinAppWidget(provider, null, null)
                                    widgetPinMessage = if (requested) {
                                        "Follow your launcher prompt to place the Kanso Today widget."
                                    } else {
                                        "Could not trigger pin dialog; long-press your home screen to add Kanso."
                                    }
                                } else {
                                    widgetPinMessage = "Long-press your home screen -> Widgets -> Kanso to add it."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Widgets, contentDescription = null)
                            Text("  Pin Today Widget to Home Screen")
                        }
                        if (widgetPinMessage != null) {
                            Text(
                                text = widgetPinMessage.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Kanso v${BuildConfig.VERSION_NAME} • Offline-First + GitHub Updates",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showNameDialog) {
        DisplayNameDialog(
            currentName = prefs.displayName,
            onDismiss = { showNameDialog = false },
            onSave = { name ->
                viewModel.setDisplayName(name)
                showNameDialog = false
            }
        )
    }

    if (showCurrencyDialog) {
        CurrencyPickerDialog(
            current = prefs.currency,
            onDismiss = { showCurrencyDialog = false },
            onSelect = { currency ->
                viewModel.setCurrency(currency)
                showCurrencyDialog = false
            }
        )
    }

    if (showBudgetDialog) {
        OverallBudgetDialog(
            currency = prefs.currency,
            currentLimitCents = prefs.overallMonthlyBudgetCents,
            onDismiss = { showBudgetDialog = false },
            onSave = { limit ->
                viewModel.setOverallMonthlyBudget(limit)
                showBudgetDialog = false
            }
        )
    }

    if (showWaterGoalDialog) {
        WaterGoalDialog(
            currentGoalMl = prefs.waterGoalMl,
            onDismiss = { showWaterGoalDialog = false },
            onSave = { goal ->
                viewModel.setWaterGoalMl(goal)
                showWaterGoalDialog = false
            }
        )
    }

    if (showGithubTokenDialog) {
        GithubTokenDialog(
            hasExistingToken = uiState.hasGithubToken,
            onDismiss = { showGithubTokenDialog = false },
            onSave = { token ->
                viewModel.setGithubToken(token)
                showGithubTokenDialog = false
                onShowMessage(if (token.isNullOrBlank()) "GitHub token cleared" else "GitHub token saved (Keystore encrypted)")
            }
        )
    }

    if (showExportPassphraseDialog) {
        ExportPassphraseDialog(
            onDismiss = { showExportPassphraseDialog = false },
            onConfirm = { passphrase ->
                showExportPassphraseDialog = false
                pendingExportPassphrase = passphrase
                val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
                createBackupLauncher.launch("kanso-backup-$stamp.kanso.enc")
            }
        )
    }

    pendingRestoreUri?.let { uri ->
        RestorePassphraseDialog(
            onDismiss = { pendingRestoreUri = null },
            onConfirm = { passphrase ->
                pendingRestoreUri = null
                viewModel.restoreBackup(uri, passphrase)
            }
        )
    }
}

@Composable
private fun AppUpdatesCard(
    uiState: SettingsUiState,
    onToggleAutoCheck: (Boolean) -> Unit,
    onEditGithubToken: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadAndInstall: (com.kanso.app.update.AppReleaseInfo) -> Unit,
    onInstallApk: (java.io.File) -> Unit,
    onOpenReleasesPage: () -> Unit
) {
    val prefs = uiState.preferences
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("App Updates", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Installed: v${BuildConfig.VERSION_NAME} • ${AppUpdateManager.REPO_OWNER}/${AppUpdateManager.REPO_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-check on launch", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = prefs.lastUpdateCheckMillis?.let {
                            "Last checked: ${KansoDateFormatters.formatFullDateTime(it)}"
                        } ?: "Checks every 6h when GitHub token is configured",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = prefs.autoCheckUpdates,
                    onCheckedChange = onToggleAutoCheck
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditGithubToken),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "GitHub Access Token (Private Repo)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = if (uiState.hasGithubToken) {
                            "Configured (Android Keystore encrypted) — tap to update"
                        } else {
                            "Not set — tap to add a read-only token for rifkymaulana/Kanso"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.hasGithubToken) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            when (val status = uiState.updateState) {
                AppUpdateState.Idle -> Unit
                AppUpdateState.Checking -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Checking GitHub Releases...", style = MaterialTheme.typography.labelMedium)
                    }
                }

                is AppUpdateState.UpToDate -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "You are on the latest version (v${status.currentVersion}).",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                is AppUpdateState.UpdateAvailable -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "New version ${status.release.tagName} available",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (status.release.releaseNotes.isNotBlank()) {
                                Text(
                                    text = status.release.releaseNotes.take(280),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Button(
                                onClick = { onDownloadAndInstall(status.release) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.InstallMobile, contentDescription = null)
                                Text("  Download & Install ${status.release.tagName}")
                            }
                        }
                    }
                }

                is AppUpdateState.Downloading -> {
                    val total = status.totalBytes
                    val progress = if (total > 0L) {
                        (status.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    } else null
                    val mbDownloaded = String.format(java.util.Locale.US, "%.1f", status.downloadedBytes / 1_048_576.0)
                    val mbTotal = if (total > 0L) {
                        String.format(java.util.Locale.US, "%.1f MB", total / 1_048_576.0)
                    } else "—"
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (progress != null) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            text = "Downloading ${status.release.tagName}: $mbDownloaded / $mbTotal",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                is AppUpdateState.Verifying -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "Verifying ${status.release.tagName} SHA-256 checksum...",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                is AppUpdateState.ReadyToInstall -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${status.release.tagName} verified and ready to install.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Button(
                                onClick = { onInstallApk(status.apkFile) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.InstallMobile, contentDescription = null)
                                Text("  Install APK")
                            }
                        }
                    }
                }

                is AppUpdateState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = status.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (status.needsToken) {
                                OutlinedButton(onClick = onEditGithubToken) {
                                    Text("Set GitHub Token")
                                }
                            }
                        }
                    }
                }
            }

            val busy = uiState.updateState is AppUpdateState.Checking ||
                uiState.updateState is AppUpdateState.Downloading ||
                uiState.updateState is AppUpdateState.Verifying

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCheckUpdates,
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Check for Updates")
                }
                OutlinedButton(
                    onClick = onOpenReleasesPage,
                    modifier = Modifier.weight(0.9f)
                ) {
                    Text("Open Releases")
                }
            }
        }
    }
}

@Composable
private fun GithubTokenDialog(
    hasExistingToken: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var tokenText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub Access Token") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Because rifkymaulana/Kanso is private, create a Fine-Grained Personal Access Token on GitHub scoped only to Repository: Kanso with Contents: Read-only. It is stored locally encrypted with Android Keystore and never included in backups.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = tokenText,
                    onValueChange = { tokenText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (hasExistingToken) "New token (leave blank to clear)" else "github_pat_...") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(tokenText.trim().ifEmpty { null })
            }) {
                Text(if (tokenText.isBlank() && hasExistingToken) "Clear Token" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DisplayNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Display Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun WaterGoalDialog(
    currentGoalMl: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by remember { mutableStateOf(currentGoalMl.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Hydration Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it.filter { ch -> ch.isDigit() }
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Goal (ml)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = text.toIntOrNull()
                if (parsed == null || parsed !in 250..10_000) {
                    error = "Enter a goal between 250 and 10,000 ml."
                } else {
                    onSave(parsed)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CurrencyPickerDialog(
    current: AppCurrency,
    onDismiss: () -> Unit,
    onSelect: (AppCurrency) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default Currency") },
        text = {
            Column {
                AppCurrency.entries.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = item == current, onClick = { onSelect(item) })
                        Column {
                            Text("${item.code} (${item.symbol})", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                item.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ExportPassphraseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encrypt Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose a passphrase (at least ${EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH} characters). Keep it safe — backups cannot be decrypted without it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = {
                        confirm = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    passphrase.length < EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH ->
                        error = "Passphrase must be at least ${EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH} characters."
                    passphrase != confirm ->
                        error = "Passphrases do not match."
                    else -> onConfirm(passphrase)
                }
            }) {
                Text("Choose Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RestorePassphraseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Restoring replaces all current local data (money, brain, habits, life admin, vault, and countdowns). Enter your backup passphrase to proceed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Backup passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (passphrase.length < EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH) {
                    error = "Enter your backup passphrase."
                } else {
                    onConfirm(passphrase)
                }
            }) {
                Text("Restore Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
