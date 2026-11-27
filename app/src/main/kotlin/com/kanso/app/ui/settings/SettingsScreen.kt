package com.kanso.app.ui.settings

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.app.BuildConfig
import com.kanso.app.navigation.SharedCapturePayload
import com.kanso.app.notifications.KansoReminderScheduler
import com.kanso.app.notifications.PaymentNotificationHandler
import com.kanso.app.update.AppUpdateManager
import com.kanso.app.update.AppUpdateState
import com.kanso.app.update.launchApkInstall
import com.kanso.app.widget.KansoTodayWidgetReceiver
import com.kanso.app.widget.KansoWidgetUpdater
import com.kanso.core.backup.EncryptedBackupCrypto
import com.kanso.core.backup.drive.DriveSyncState
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.AppLockTimeout
import com.kanso.core.model.BriefingCalculations
import com.kanso.core.model.BriefingSlot
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.KnownFinanceApps
import com.kanso.core.model.ParsedShareDraft
import com.kanso.core.model.PaymentCaptureMode
import com.kanso.core.model.PaymentNotificationLogEntry
import com.kanso.core.model.PaymentParseSkipReason
import com.kanso.core.model.SharedCaptureTarget
import com.kanso.core.model.ThemeMode
import com.kanso.core.model.TransactionType
import com.kanso.core.security.BiometricAuthHelper
import com.kanso.core.security.BiometricAvailability
import com.kanso.core.ui.components.ConfirmDeleteDialog
import com.kanso.core.ui.components.KansoAccentIconBadge
import com.kanso.core.ui.components.KansoAnimatedProgressBar
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoClipboard
import com.kanso.core.ui.components.KansoCompactEmptyHint
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoFormBottomSheet
import com.kanso.core.ui.components.KansoMotion
import com.kanso.core.ui.components.KansoOutlinedButton
import com.kanso.core.ui.components.KansoPillBadge
import com.kanso.core.ui.components.KansoPrimaryButton
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.core.ui.components.KansoTonalButton
import com.kanso.core.ui.components.incomeSemanticColor
import com.kanso.core.ui.components.warningSemanticColor
import com.kanso.core.ui.theme.kansoColors
import com.kanso.feature.money.OverallBudgetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    initialAction: String? = null,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit = {},
    onAppLockActivated: () -> Unit = {},
    onBiometricPromptActive: (Boolean) -> Unit = {},
    onReopenOnboarding: () -> Unit = {},
    onOpenQuickCaptureDraft: (SharedCapturePayload) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val fragmentActivity = remember(context) { context.findFragmentActivity() }
    val listState = rememberLazyListState()

    var showNameDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showWaterGoalDialog by remember { mutableStateOf(false) }
    var showGithubTokenDialog by remember { mutableStateOf(false) }
    var editingBriefingSlot by remember { mutableStateOf<BriefingSlot?>(null) }
    var autoInstallPending by remember { mutableStateOf(false) }

    var showExportPassphraseDialog by remember { mutableStateOf(false) }
    var pendingExportPassphrase by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    var showDrivePassphraseDialog by remember { mutableStateOf(false) }
    var resumeDriveAfterPassphrase by remember { mutableStateOf(false) }
    var showDriveRestoreConfirm by remember { mutableStateOf(false) }
    var showDriveDisconnectConfirm by remember { mutableStateOf(false) }
    var showAllowedPaymentAppsSheet by remember { mutableStateOf(false) }
    var showRecentPaymentLogsSheet by remember { mutableStateOf(false) }

    var autoLoggerExpanded by rememberSaveable { mutableStateOf(false) }
    var appearanceExpanded by rememberSaveable { mutableStateOf(false) }
    var securityExpanded by rememberSaveable { mutableStateOf(false) }
    var cloudBackupExpanded by rememberSaveable(initialAction) {
        mutableStateOf(initialAction == "drive" || initialAction == "restore_file")
    }
    var updatesToolsExpanded by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var hasNotificationAccess by remember {
        mutableStateOf(PaymentNotificationHandler.isNotificationAccessGranted(context))
    }
    var isBatteryUnrestricted by remember {
        mutableStateOf(PaymentNotificationHandler.isIgnoringBatteryOptimizations(context))
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = PaymentNotificationHandler.isNotificationAccessGranted(context)
                isBatteryUnrestricted = PaymentNotificationHandler.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val driveConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDriveConsentResult(
            resultCode = result.resultCode,
            data = result.data,
            activity = activity
        )
    }

    LaunchedEffect(uiState.driveSyncState) {
        when (val driveState = uiState.driveSyncState) {
            is DriveSyncState.NeedsConsent -> {
                val request = IntentSenderRequest.Builder(driveState.pendingIntent.intentSender).build()
                viewModel.consumeDriveConsentPrompt()
                driveConsentLauncher.launch(request)
            }

            is DriveSyncState.NeedsPassphrase -> {
                resumeDriveAfterPassphrase = true
                showDrivePassphraseDialog = true
            }

            is DriveSyncState.Synced -> {
                KansoWidgetUpdater.updateAll(context)
            }

            else -> Unit
        }
    }

    LaunchedEffect(prefs.hideBalances) {
        KansoWidgetUpdater.updateAll(context)
    }

    LaunchedEffect(uiState.backupStatus) {
        if (uiState.backupStatus is BackupOperationStatus.Success) {
            KansoWidgetUpdater.updateAll(context)
        }
    }

    LaunchedEffect(uiState.updateState) {
        val update = uiState.updateState
        if (update is AppUpdateState.UpdateAvailable ||
            update is AppUpdateState.Downloading ||
            update is AppUpdateState.Verifying ||
            update is AppUpdateState.ReadyToInstall
        ) {
            updatesToolsExpanded = true
        }
        val ready = update as? AppUpdateState.ReadyToInstall
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

    LaunchedEffect(uiState.driveSyncState, uiState.backupStatus) {
        if (uiState.driveSyncState is DriveSyncState.Authorizing ||
            uiState.driveSyncState is DriveSyncState.Syncing ||
            uiState.driveSyncState is DriveSyncState.Conflict ||
            uiState.driveSyncState is DriveSyncState.Error ||
            uiState.backupStatus is BackupOperationStatus.InProgress ||
            uiState.backupStatus is BackupOperationStatus.Error ||
            uiState.backupStatus is BackupOperationStatus.Success
        ) {
            cloudBackupExpanded = true
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setRemindersEnabled(granted)
        KansoReminderScheduler.syncSchedule(
            context = context,
            prefs = prefs.copy(remindersEnabled = granted),
            replaceExisting = true
        )
    }

    val paymentNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onShowMessage("Notification permission granted")
        } else {
            onShowMessage("Allow notifications to see 1-tap payment prompts")
        }
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

    var handledInitialAction by rememberSaveable(initialAction) { mutableStateOf(false) }
    LaunchedEffect(initialAction, handledInitialAction) {
        if (!handledInitialAction && !initialAction.isNullOrBlank()) {
            handledInitialAction = true
            when (initialAction) {
                "drive" -> {
                    cloudBackupExpanded = true
                    listState.animateScrollToItem(4)
                    viewModel.connectOrSyncDrive(activity)
                }
                "restore_file" -> {
                    cloudBackupExpanded = true
                    listState.animateScrollToItem(4)
                    viewModel.clearBackupStatus()
                    openBackupLauncher.launch(arrayOf("*/*"))
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Settings & Backup", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        val kansoColors = MaterialTheme.kansoColors
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "general") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsSectionOverline("GENERAL")
                    KansoBentoCard(contentPadding = PaddingValues(0.dp)) {
                        SettingsRow(
                            title = "Display Name",
                            subtitle = prefs.displayName.ifBlank { "Not set (shows neutral greeting)" },
                            onClick = { showNameDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsRow(
                            title = "Default Currency",
                            subtitle = "${prefs.currency.code} (${prefs.currency.symbol}) — ${prefs.currency.displayName}",
                            onClick = { showCurrencyDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsRow(
                            title = "Overall Monthly Budget",
                            subtitle = if (prefs.overallMonthlyBudgetCents > 0L) {
                                CurrencyFormatter.format(prefs.overallMonthlyBudgetCents, prefs.currency)
                            } else {
                                "Disabled"
                            },
                            onClick = { showBudgetDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsRow(
                            title = "Daily Hydration Goal",
                            subtitle = "${prefs.waterGoalMl} ml",
                            onClick = { showWaterGoalDialog = true }
                        )
                    }
                }
            }

            item(key = "auto_logger_hub") {
                val (loggerBadgeText, loggerBadgeColor) = when {
                    prefs.paymentLoggerEnabled && hasNotificationAccess && isBatteryUnrestricted ->
                        "Active" to incomeSemanticColor()
                    prefs.paymentLoggerEnabled && hasNotificationAccess ->
                        "Check Battery" to warningSemanticColor()
                    prefs.paymentLoggerEnabled ->
                        "Setup Needed" to warningSemanticColor()
                    else ->
                        "Off" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                val loggerSubtitle = when {
                    prefs.paymentLoggerEnabled && hasNotificationAccess ->
                        "${prefs.paymentCaptureMode.label} · ${
                            if (prefs.paymentAllowAllApps) "All apps" else "${prefs.paymentAllowedPackages.size} apps"
                        }"
                    else -> "Offline bank, QRIS & e-wallet alerts"
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsSectionOverline("CONTROL CENTER")
                    CollapsibleSettingsHubCard(
                        title = "Payment Auto-Logger",
                        subtitle = loggerSubtitle,
                        icon = Icons.Outlined.NotificationsActive,
                        accent = if (prefs.paymentLoggerEnabled) kansoColors.moneyAccent else MaterialTheme.colorScheme.primary,
                        badgeText = loggerBadgeText,
                        badgeColor = loggerBadgeColor,
                        expanded = autoLoggerExpanded,
                        onToggle = { autoLoggerExpanded = !autoLoggerExpanded }
                    ) {
                        PaymentAutoLoggerContent(
                            uiState = uiState,
                            hasNotificationAccess = hasNotificationAccess,
                            isBatteryUnrestricted = isBatteryUnrestricted,
                            onOpenNotificationAccess = {
                                runCatching {
                                    context.startActivity(PaymentNotificationHandler.createNotificationAccessIntent())
                                }.onFailure {
                                    onShowMessage("Unable to open Notification Access settings")
                                }
                            },
                            onOpenBatterySettings = {
                                runCatching {
                                    context.startActivity(PaymentNotificationHandler.createBatteryOptimizationSettingsIntent())
                                }.onFailure {
                                    onShowMessage("Unable to open Battery settings")
                                }
                            },
                            onToggleEnabled = { wantEnabled ->
                                if (wantEnabled) {
                                    viewModel.setPaymentLoggerEnabled(true)
                                    if (!hasNotificationAccess) {
                                        onShowMessage("Grant Notification Access to Kanso, then return here")
                                        runCatching {
                                            context.startActivity(PaymentNotificationHandler.createNotificationAccessIntent())
                                        }
                                    } else if (
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        paymentNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    viewModel.setPaymentLoggerEnabled(false)
                                }
                            },
                            onSelectMode = viewModel::setPaymentCaptureMode,
                            onToggleAllowAllApps = viewModel::setPaymentAllowAllApps,
                            onManageAllowedApps = { showAllowedPaymentAppsSheet = true },
                            onOpenRecentLogs = { showRecentPaymentLogsSheet = true },
                            onSendTestAlert = {
                                if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    paymentNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.sendTestPaymentAlert { posted ->
                                        onShowMessage(
                                            if (posted) {
                                                "Test payment notification posted — check your shade!"
                                            } else {
                                                "Could not post test notification"
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            item(key = "appearance_hub") {
                val appearanceSubtitle = "${prefs.themeMode.label} · ${
                    if (prefs.remindersEnabled) "Daily briefings on" else "Reminders off"
                }"
                CollapsibleSettingsHubCard(
                    title = "Appearance & Reminders",
                    subtitle = appearanceSubtitle,
                    icon = Icons.Outlined.Palette,
                    accent = kansoColors.utilitiesAccent,
                    badgeText = if (prefs.remindersEnabled) "Reminders On" else null,
                    badgeColor = kansoColors.utilitiesAccent,
                    expanded = appearanceExpanded,
                    onToggle = { appearanceExpanded = !appearanceExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Theme Mode", style = MaterialTheme.typography.titleMedium)
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
                                        text = "Override Kanso Bento palette with wallpaper colors",
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
                                    text = "Briefings for bills, expiries, maintenance, tasks & habits",
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
                                        KansoReminderScheduler.syncSchedule(
                                            context = context,
                                            prefs = prefs.copy(remindersEnabled = wantEnabled),
                                            replaceExisting = true
                                        )
                                    }
                                }
                            )
                        }
                        AnimatedVisibility(
                            visible = prefs.remindersEnabled,
                            enter = expandVertically(KansoMotion.SizeSpring) + fadeIn(tween(180)),
                            exit = shrinkVertically() + fadeOut(tween(140))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                BriefingScheduleRow(
                                    title = "Morning Briefing",
                                    subtitle = "Bills, tasks, expiries, maintenance & IOUs",
                                    enabled = prefs.morningReminderEnabled,
                                    timeText = BriefingCalculations.formatClockTime(
                                        prefs.morningReminderHour,
                                        prefs.morningReminderMinute
                                    ),
                                    onToggle = viewModel::setMorningReminderEnabled,
                                    onPickTime = { editingBriefingSlot = BriefingSlot.MORNING }
                                )
                                BriefingScheduleRow(
                                    title = "Evening Check-In",
                                    subtitle = "Pending habits, hydration goal & open tasks",
                                    enabled = prefs.eveningReminderEnabled,
                                    timeText = BriefingCalculations.formatClockTime(
                                        prefs.eveningReminderHour,
                                        prefs.eveningReminderMinute
                                    ),
                                    onToggle = viewModel::setEveningReminderEnabled,
                                    onPickTime = { editingBriefingSlot = BriefingSlot.EVENING }
                                )
                            }
                        }
                    }
                }
            }

            item(key = "security_hub") {
                val isProtected = prefs.appLockEnabled || prefs.hideRecentAppsPreview || prefs.hideBalances
                val securitySummary = buildList {
                    if (prefs.appLockEnabled) add("App Lock")
                    if (prefs.hideRecentAppsPreview) add("Preview Blur")
                    if (prefs.hideBalances) add("Masked Balances")
                }.joinToString(" · ").ifEmpty { "App lock, preview blur & balance masking" }

                CollapsibleSettingsHubCard(
                    title = "Security & Privacy",
                    subtitle = securitySummary,
                    icon = Icons.Outlined.Shield,
                    accent = kansoColors.lifeAdminAccent,
                    badgeText = if (isProtected) "Protected" else "Standard",
                    badgeColor = if (isProtected) incomeSemanticColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                    expanded = securityExpanded,
                    onToggle = { securityExpanded = !securityExpanded }
                ) {
                    Column(
                        modifier = Modifier.animateContentSize(animationSpec = KansoMotion.SizeSpring),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("App Lock", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "Require fingerprint, face, or device PIN when opening Kanso",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.appLockEnabled,
                                onCheckedChange = { wantEnabled ->
                                    val availability = BiometricAuthHelper.checkAvailability(context)
                                    if (wantEnabled) {
                                        when (availability) {
                                            BiometricAvailability.AVAILABLE -> {
                                                if (fragmentActivity != null) {
                                                    onBiometricPromptActive(true)
                                                    BiometricAuthHelper.authenticate(
                                                        activity = fragmentActivity,
                                                        title = "Enable App Lock",
                                                        subtitle = "Confirm identity to lock Kanso",
                                                        onSuccess = {
                                                            onBiometricPromptActive(false)
                                                            onAppLockActivated()
                                                            viewModel.setAppLockEnabled(true)
                                                            onShowMessage("App Lock enabled")
                                                        },
                                                        onError = { msg ->
                                                            onBiometricPromptActive(false)
                                                            onShowMessage(msg)
                                                        },
                                                        onCancel = { onBiometricPromptActive(false) }
                                                    )
                                                } else {
                                                    onShowMessage("Unable to open biometric prompt.")
                                                }
                                            }
                                            BiometricAvailability.NOT_ENROLLED -> {
                                                onShowMessage("Set up biometrics or a screen lock in Android Settings first.")
                                            }
                                            BiometricAvailability.UNAVAILABLE -> {
                                                onShowMessage("Biometric or device credential hardware unavailable.")
                                            }
                                        }
                                    } else if (availability == BiometricAvailability.AVAILABLE && fragmentActivity != null) {
                                        onBiometricPromptActive(true)
                                        BiometricAuthHelper.authenticate(
                                            activity = fragmentActivity,
                                            title = "Disable App Lock",
                                            subtitle = "Confirm identity to turn off App Lock",
                                            onSuccess = {
                                                onBiometricPromptActive(false)
                                                viewModel.setAppLockEnabled(false)
                                                onShowMessage("App Lock disabled")
                                            },
                                            onError = { msg ->
                                                onBiometricPromptActive(false)
                                                onShowMessage(msg)
                                            },
                                            onCancel = { onBiometricPromptActive(false) }
                                        )
                                    } else {
                                        viewModel.setAppLockEnabled(false)
                                    }
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = prefs.appLockEnabled,
                            enter = expandVertically(KansoMotion.SizeSpring) + fadeIn(tween(180)),
                            exit = shrinkVertically() + fadeOut(tween(140))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Lock After Leaving", style = MaterialTheme.typography.labelLarge)
                                KansoSegmentedRow(
                                    options = AppLockTimeout.entries.map { it.label },
                                    selectedIndex = AppLockTimeout.entries.indexOf(prefs.appLockTimeout),
                                    onSelect = { idx -> viewModel.setAppLockTimeout(AppLockTimeout.entries[idx]) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hide Recent-Apps Preview", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "Blur Kanso in Recent Apps & block screenshots across all screens",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.hideRecentAppsPreview,
                                onCheckedChange = viewModel::setHideRecentAppsPreview
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hide Currency Balances", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "Mask money amounts with •••••• across Today, Money & Widget",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.hideBalances,
                                onCheckedChange = viewModel::setHideBalances
                            )
                        }
                    }
                }
            }

            item(key = "cloud_backup_hub") {
                val driveConfig = uiState.driveSyncConfig
                val driveState = uiState.driveSyncState
                val (syncBadgeText, syncBadgeColor) = when {
                    driveState is DriveSyncState.Authorizing || driveState is DriveSyncState.Syncing ->
                        "Syncing" to MaterialTheme.colorScheme.primary
                    driveState is DriveSyncState.Conflict ->
                        "Conflict" to warningSemanticColor()
                    driveState is DriveSyncState.Error ->
                        "Error" to MaterialTheme.colorScheme.error
                    driveConfig.isConnected ->
                        "Cloud Sync" to incomeSemanticColor()
                    prefs.lastBackupMillis != null ->
                        "Backed Up" to incomeSemanticColor()
                    else ->
                        "Not Set" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                val syncSubtitle = when {
                    driveConfig.isConnected && driveConfig.lastSyncMillis != null ->
                        "Synced ${KansoDateFormatters.formatFullDateTime(driveConfig.lastSyncMillis ?: 0L)}"
                    driveConfig.isConnected ->
                        driveConfig.accountEmail ?: "Connected to Google Drive"
                    prefs.lastBackupMillis != null ->
                        "Local backup ${KansoDateFormatters.formatFullDateTime(prefs.lastBackupMillis ?: 0L)}"
                    else -> "Encrypted Drive sync & .kanso.enc export"
                }

                CollapsibleSettingsHubCard(
                    title = "Cloud Sync & Backup",
                    subtitle = syncSubtitle,
                    icon = if (driveConfig.isConnected) Icons.Outlined.CloudDone else Icons.Outlined.CloudSync,
                    accent = if (driveConfig.isConnected) incomeSemanticColor() else MaterialTheme.colorScheme.primary,
                    badgeText = syncBadgeText,
                    badgeColor = syncBadgeColor,
                    expanded = cloudBackupExpanded,
                    onToggle = { cloudBackupExpanded = !cloudBackupExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        GoogleDriveSyncContent(
                            uiState = uiState,
                            onConnectOrSync = { viewModel.connectOrSyncDrive(activity) },
                            onForceUpload = { viewModel.forceUploadToDrive(activity) },
                            onRequestForceRestore = { showDriveRestoreConfirm = true },
                            onToggleAutoSync = viewModel::setDriveAutoSync,
                            onEditPassphrase = {
                                resumeDriveAfterPassphrase = false
                                showDrivePassphraseDialog = true
                            },
                            onRequestDisconnect = { showDriveDisconnectConfirm = true },
                            onOpenConflictDialog = { viewModel.connectOrSyncDrive(activity) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(
                            modifier = Modifier.animateContentSize(animationSpec = KansoMotion.SizeSpring),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Local Encrypted Backup", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = prefs.lastBackupMillis?.let {
                                    "Last backup: ${KansoDateFormatters.formatFullDateTime(it)}"
                                } ?: "Exports an AES-256-GCM encrypted .kanso.enc file",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            AnimatedContent(
                                targetState = uiState.backupStatus,
                                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                                label = "backupStatus"
                            ) { status ->
                                when (status) {
                                    BackupOperationStatus.Idle -> Unit
                                    is BackupOperationStatus.InProgress -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            KansoAnimatedProgressBar(progress = 0.65f, height = 7.dp)
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
                            }

                            val busy = uiState.backupStatus is BackupOperationStatus.InProgress
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                KansoPrimaryButton(
                                    text = "Export",
                                    leadingIcon = Icons.Outlined.CloudUpload,
                                    size = KansoButtonSize.COMPACT,
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.clearBackupStatus()
                                        showExportPassphraseDialog = true
                                    }
                                )
                                KansoOutlinedButton(
                                    text = "Restore",
                                    leadingIcon = Icons.Outlined.Restore,
                                    size = KansoButtonSize.COMPACT,
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.clearBackupStatus()
                                        openBackupLauncher.launch(arrayOf("*/*"))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item(key = "updates_tools_hub") {
                val hasUpdate = uiState.updateState is AppUpdateState.UpdateAvailable ||
                    uiState.updateState is AppUpdateState.Downloading ||
                    uiState.updateState is AppUpdateState.Verifying ||
                    uiState.updateState is AppUpdateState.ReadyToInstall
                var widgetPinMessage by remember { mutableStateOf<String?>(null) }
                val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }

                CollapsibleSettingsHubCard(
                    title = "Updates & Tools",
                    subtitle = "App updates, home widget & Quick Start",
                    icon = Icons.Outlined.SystemUpdate,
                    accent = kansoColors.settingsAccent,
                    badgeText = if (hasUpdate) "Update" else "v${BuildConfig.VERSION_NAME}",
                    badgeColor = if (hasUpdate) incomeSemanticColor() else MaterialTheme.colorScheme.primary,
                    expanded = updatesToolsExpanded,
                    onToggle = { updatesToolsExpanded = !updatesToolsExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AppUpdatesContent(
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

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Widgets & Quick Start", style = MaterialTheme.typography.titleMedium)
                            KansoOutlinedButton(
                                text = "Pin Today Widget to Home Screen",
                                leadingIcon = Icons.Outlined.Widgets,
                                size = KansoButtonSize.COMPACT,
                                modifier = Modifier.fillMaxWidth(),
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
                                }
                            )
                            if (widgetPinMessage != null) {
                                Text(
                                    text = widgetPinMessage.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onReopenOnboarding),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Quick Start Setup",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                        )
                                        Text(
                                            text = "Re-open welcome & starter habits sheet",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Open",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Kanso v${BuildConfig.VERSION_NAME} • Personal Life OS • Offline-First",
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

    editingBriefingSlot?.let { slot ->
        val (initHour, initMinute) = when (slot) {
            BriefingSlot.MORNING -> prefs.morningReminderHour to prefs.morningReminderMinute
            BriefingSlot.EVENING -> prefs.eveningReminderHour to prefs.eveningReminderMinute
        }
        ReminderTimePickerDialog(
            slot = slot,
            initialHour = initHour,
            initialMinute = initMinute,
            onDismiss = { editingBriefingSlot = null },
            onConfirm = { hour, minute ->
                when (slot) {
                    BriefingSlot.MORNING -> viewModel.setMorningReminderTime(hour, minute)
                    BriefingSlot.EVENING -> viewModel.setEveningReminderTime(hour, minute)
                }
                editingBriefingSlot = null
                onShowMessage(
                    "${if (slot == BriefingSlot.MORNING) "Morning briefing" else "Evening check-in"} set to ${
                        BriefingCalculations.formatClockTime(hour, minute)
                    }"
                )
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

    if (showDrivePassphraseDialog) {
        DrivePassphraseDialog(
            hasExisting = uiState.driveSyncConfig.hasPassphrase,
            onDismiss = {
                showDrivePassphraseDialog = false
                if (uiState.driveSyncState is DriveSyncState.NeedsPassphrase) {
                    viewModel.clearDriveSyncStatus()
                }
            },
            onSave = { passphrase ->
                showDrivePassphraseDialog = false
                viewModel.saveDriveSyncPassphrase(
                    passphrase = passphrase,
                    resumeSync = resumeDriveAfterPassphrase,
                    activity = activity
                )
            }
        )
    }

    val conflictState = uiState.driveSyncState as? DriveSyncState.Conflict
    if (conflictState != null) {
        DriveConflictDialog(
            conflict = conflictState,
            onKeepCloud = { viewModel.resolveDriveConflict(keepCloud = true, activity = activity) },
            onKeepDevice = { viewModel.resolveDriveConflict(keepCloud = false, activity = activity) },
            onDismiss = viewModel::clearDriveSyncStatus
        )
    }

    if (showDriveRestoreConfirm) {
        ConfirmDeleteDialog(
            title = "Restore from Google Drive?",
            message = "This replaces all current local data (Money, Brain, Habits, Life Admin, Vault, and Countdowns) with the encrypted snapshot stored in your Google Drive.",
            confirmLabel = "Restore Cloud Data",
            onConfirm = {
                showDriveRestoreConfirm = false
                viewModel.forceRestoreFromDrive(activity)
            },
            onDismiss = { showDriveRestoreConfirm = false }
        )
    }

    if (showDriveDisconnectConfirm) {
        ConfirmDeleteDialog(
            title = "Disconnect Google Drive?",
            message = "Your local data and existing encrypted backup inside Google Drive will remain intact, and automatic sync on this device will stop.",
            confirmLabel = "Disconnect",
            onConfirm = {
                showDriveDisconnectConfirm = false
                viewModel.disconnectDrive()
                onShowMessage("Google Drive sync disconnected")
            },
            onDismiss = { showDriveDisconnectConfirm = false }
        )
    }

    if (showAllowedPaymentAppsSheet) {
        AllowedPaymentAppsSheet(
            allowedPackages = prefs.paymentAllowedPackages,
            allowAllApps = prefs.paymentAllowAllApps,
            onTogglePackage = viewModel::togglePaymentAllowedPackage,
            onResetDefaults = {
                viewModel.resetPaymentAllowedPackages()
                onShowMessage("Reset to ${KnownFinanceApps.defaultPackages.size} finance apps")
            },
            onDismiss = { showAllowedPaymentAppsSheet = false }
        )
    }

    if (showRecentPaymentLogsSheet) {
        RecentPaymentAlertsSheet(
            logs = uiState.recentPaymentLogs,
            currency = prefs.currency,
            onClear = {
                viewModel.clearRecentPaymentLogs()
                onShowMessage("Cleared recent alert logs")
            },
            onCopy = { entry ->
                val body = listOf(entry.sourceAppLabel, entry.title, entry.text)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                KansoClipboard.copyText(
                    context = context,
                    label = "${entry.sourceAppLabel} alert",
                    text = body,
                    isSensitive = false
                )
                onShowMessage("Copied alert wording")
            },
            onLogEntry = { entry ->
                showRecentPaymentLogsSheet = false
                val matched = entry.matchedAlert
                val payload = if (matched != null) {
                    SharedCapturePayload(
                        prefilledDraft = ParsedShareDraft(
                            target = if (matched.type == TransactionType.INCOME) {
                                SharedCaptureTarget.INCOME
                            } else {
                                SharedCaptureTarget.EXPENSE
                            },
                            amountCents = matched.amountCents,
                            detectedCurrency = matched.detectedCurrency,
                            wasConvertedCurrency = matched.wasConvertedCurrency,
                            suggestedCategory = matched.category,
                            suggestedWallet = matched.wallet,
                            transactionNote = matched.note
                        )
                    )
                } else {
                    SharedCapturePayload(
                        text = listOf(entry.title, entry.text)
                            .filter { it.isNotBlank() }
                            .joinToString("\n"),
                        subject = entry.sourceAppLabel,
                        isReceiptMedia = true
                    )
                }
                onOpenQuickCaptureDraft(payload)
            },
            onDismiss = { showRecentPaymentLogsSheet = false }
        )
    }
}

@Composable
private fun CollapsibleSettingsHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    badgeText: String?,
    badgeColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    KansoBentoCard(contentPadding = PaddingValues(14.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggle)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KansoAccentIconBadge(
                    icon = icon,
                    accent = accent,
                    size = 38.dp,
                    iconSize = 19.dp,
                    cornerRadius = 12.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!badgeText.isNullOrBlank()) {
                    KansoPillBadge(
                        text = badgeText,
                        color = badgeColor
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(KansoMotion.SizeSpring) + fadeIn(tween(180)),
                exit = shrinkVertically() + fadeOut(tween(140))
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    content()
                }
            }
        }
    }
}

@Composable
private fun PaymentAutoLoggerContent(
    uiState: SettingsUiState,
    hasNotificationAccess: Boolean,
    isBatteryUnrestricted: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectMode: (PaymentCaptureMode) -> Unit,
    onToggleAllowAllApps: (Boolean) -> Unit,
    onManageAllowedApps: () -> Unit,
    onOpenRecentLogs: () -> Unit,
    onSendTestAlert: () -> Unit
) {
    val prefs = uiState.preferences
    Column(
        modifier = Modifier.animateContentSize(animationSpec = KansoMotion.SizeSpring),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable Auto-Logger", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Filters out promos, OTPs & unpaid reminders",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = prefs.paymentLoggerEnabled,
                onCheckedChange = onToggleEnabled
            )
        }

        if (!hasNotificationAccess) {
            KansoPrimaryButton(
                text = "Grant Notification Access",
                leadingIcon = Icons.Outlined.NotificationsActive,
                size = KansoButtonSize.LARGE,
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenNotificationAccess
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenNotificationAccess),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Access",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Granted — tap to manage in Android Settings",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    KansoPillBadge(text = "Granted", color = incomeSemanticColor())
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenBatterySettings),
            shape = RoundedCornerShape(12.dp),
            color = if (isBatteryUnrestricted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                warningSemanticColor().copy(alpha = 0.12f)
            },
            border = if (isBatteryUnrestricted) {
                null
            } else {
                BorderStroke(1.dp, warningSemanticColor().copy(alpha = 0.35f))
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = if (isBatteryUnrestricted) incomeSemanticColor() else warningSemanticColor()
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Background Battery",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = if (isBatteryUnrestricted) {
                            "Unrestricted — won't sleep in background"
                        } else {
                            "Tap & set Kanso to Unrestricted so alerts aren't missed"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (isBatteryUnrestricted) "Unrestricted" else "Set",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isBatteryUnrestricted) incomeSemanticColor() else warningSemanticColor()
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Capture Mode", style = MaterialTheme.typography.labelLarge)
            KansoSegmentedRow(
                options = PaymentCaptureMode.entries.map { it.label },
                selectedIndex = PaymentCaptureMode.entries.indexOf(prefs.paymentCaptureMode),
                onSelect = { idx -> onSelectMode(PaymentCaptureMode.entries[idx]) }
            )
            Text(
                text = if (prefs.paymentCaptureMode == PaymentCaptureMode.PROMPT) {
                    "Posts a notification with [Save], [Edit], and [Ignore] actions."
                } else {
                    "Logs immediately and shows a confirmation alert with [Undo]."
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Scan All Installed Apps", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Inspect notifications from any app",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = prefs.paymentAllowAllApps,
                onCheckedChange = onToggleAllowAllApps
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onManageAllowedApps),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Whitelisted Finance Apps",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = if (prefs.paymentAllowAllApps) {
                            "${prefs.paymentAllowedPackages.size} selected (Scan All is on)"
                        } else {
                            "${prefs.paymentAllowedPackages.size} apps selected"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Choose",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenRecentLogs),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recent Alerts Log (24h)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = if (uiState.recentPaymentLogs.isEmpty()) {
                            "Inspect matched & skipped notifications"
                        } else {
                            val matchedCount = uiState.recentPaymentLogs.count { it.isMatched }
                            "${uiState.recentPaymentLogs.size} alerts ($matchedCount matched)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Inspect",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        KansoTonalButton(
            text = "Send Test Alert",
            leadingIcon = Icons.Outlined.NotificationsActive,
            size = KansoButtonSize.COMPACT,
            modifier = Modifier.fillMaxWidth(),
            onClick = onSendTestAlert
        )
    }
}

@Composable
private fun RecentPaymentAlertsSheet(
    logs: List<PaymentNotificationLogEntry>,
    currency: AppCurrency,
    onClear: () -> Unit,
    onCopy: (PaymentNotificationLogEntry) -> Unit,
    onLogEntry: (PaymentNotificationLogEntry) -> Unit,
    onDismiss: () -> Unit
) {
    KansoFormBottomSheet(
        title = "Recent Payment Alerts",
        onDismiss = onDismiss,
        primaryLabel = "Done",
        onPrimaryClick = onDismiss,
        badgeText = "${logs.size} logged",
        badgeColor = MaterialTheme.colorScheme.primary,
        badgeIcon = Icons.Outlined.History
    ) {
        if (logs.isEmpty()) {
            KansoCompactEmptyHint(
                icon = Icons.Outlined.History,
                title = "No alerts captured yet",
                subtitle = "Matched & skipped notifications from watched apps are kept locally for 24 hours so you can verify wording or log missed spends.",
                accentColor = MaterialTheme.colorScheme.primary
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last 24 hours from watched apps (OTPs redacted)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                logs.forEach { entry ->
                    val isOtp = entry.skipReason == PaymentParseSkipReason.OTP
                    val statusText = if (entry.isMatched) {
                        "Matched · ${
                            CurrencyFormatter.format(
                                entry.matchedAlert!!.amountCents,
                                currency
                            )
                        }"
                    } else {
                        entry.skipReason?.label ?: "Skipped"
                    }
                    val statusColor = when {
                        entry.isMatched -> incomeSemanticColor()
                        isOtp -> warningSemanticColor()
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    KansoBentoCard(
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${entry.sourceAppLabel} · ${KansoDateFormatters.formatTime(entry.timestampMillis)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                                KansoPillBadge(
                                    text = statusText,
                                    color = statusColor
                                )
                            }

                            if (entry.title.isNotBlank()) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (entry.text.isNotBlank()) {
                                Text(
                                    text = entry.text,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!isOtp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    KansoOutlinedButton(
                                        text = "Copy",
                                        leadingIcon = Icons.Outlined.ContentCopy,
                                        size = KansoButtonSize.COMPACT,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onCopy(entry) }
                                    )
                                    KansoTonalButton(
                                        text = "Log",
                                        leadingIcon = Icons.Outlined.Add,
                                        size = KansoButtonSize.COMPACT,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onLogEntry(entry) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PaymentAppOptionRow(
    val packageName: String,
    val displayName: String,
    val groupLabel: String,
    val isInstalled: Boolean,
    val isPreset: Boolean
)

@Composable
private fun AllowedPaymentAppsSheet(
    allowedPackages: Set<String>,
    allowAllApps: Boolean,
    onTogglePackage: (String) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var installedLaunchableApps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var installedPackageSet by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(context) {
        val loaded = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(launcherIntent, 0)
            val appPairs = resolved.mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg.equals(context.packageName, ignoreCase = true)) return@mapNotNull null
                val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
                pkg to label
            }
                .distinctBy { it.first }
                .sortedBy { it.second.lowercase() }
            val installedSet = appPairs.mapTo(HashSet()) { it.first }
            appPairs to installedSet
        }
        installedLaunchableApps = loaded.first
        installedPackageSet = loaded.second
    }

    val allRows = remember(installedLaunchableApps, installedPackageSet) {
        val presetRows = KnownFinanceApps.presets.map { preset ->
            PaymentAppOptionRow(
                packageName = preset.packageName,
                displayName = preset.displayName,
                groupLabel = preset.groupLabel,
                isInstalled = installedPackageSet.contains(preset.packageName),
                isPreset = true
            )
        }.sortedWith(
            compareByDescending<PaymentAppOptionRow> { it.isInstalled }
                .thenBy { it.groupLabel }
                .thenBy { it.displayName.lowercase() }
        )

        val presetPkgSet = KnownFinanceApps.defaultPackages
        val extraInstalledRows = installedLaunchableApps
            .filter { (pkg, _) -> !presetPkgSet.contains(pkg) }
            .map { (pkg, label) ->
                PaymentAppOptionRow(
                    packageName = pkg,
                    displayName = label,
                    groupLabel = "Other Installed Apps",
                    isInstalled = true,
                    isPreset = false
                )
            }

        presetRows + extraInstalledRows
    }

    val filteredRows = remember(allRows, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            allRows
        } else {
            allRows.filter {
                it.displayName.lowercase().contains(q) ||
                    it.packageName.lowercase().contains(q) ||
                    it.groupLabel.lowercase().contains(q)
            }
        }
    }

    KansoFormBottomSheet(
        title = "Whitelisted Finance Apps",
        onDismiss = onDismiss,
        primaryLabel = "Done",
        onPrimaryClick = onDismiss,
        badgeText = "${allowedPackages.size} active",
        badgeColor = MaterialTheme.colorScheme.primary,
        badgeIcon = Icons.Outlined.NotificationsActive
    ) {
        if (allowAllApps) {
            Text(
                text = "Note: \"Scan All Installed Apps\" is currently ON, so Kanso inspects all non-system apps regardless of this list.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                label = { Text("Search apps or package ID") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            TextButton(onClick = onResetDefaults) {
                Text("Reset Defaults")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filteredRows.forEach { row ->
                val checked = allowedPackages.contains(row.packageName)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTogglePackage(row.packageName) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (checked) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (checked) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (row.isInstalled) {
                                    KansoPillBadge(
                                        text = "Installed",
                                        color = MaterialTheme.kansoColors.income
                                    )
                                }
                            }
                            Text(
                                text = "${row.groupLabel} · ${row.packageName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = { onTogglePackage(row.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleDriveSyncContent(
    uiState: SettingsUiState,
    onConnectOrSync: () -> Unit,
    onForceUpload: () -> Unit,
    onRequestForceRestore: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onEditPassphrase: () -> Unit,
    onRequestDisconnect: () -> Unit,
    onOpenConflictDialog: () -> Unit
) {
    val config = uiState.driveSyncConfig
    val driveState = uiState.driveSyncState
    val kansoColors = MaterialTheme.kansoColors
    val busy = driveState is DriveSyncState.Authorizing || driveState is DriveSyncState.Syncing

    Column(
        modifier = Modifier.animateContentSize(animationSpec = KansoMotion.SizeSpring),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KansoAccentIconBadge(
                icon = if (config.isConnected) Icons.Outlined.CloudDone else Icons.Outlined.CloudSync,
                accent = if (config.isConnected) kansoColors.income else MaterialTheme.colorScheme.primary,
                size = 38.dp,
                iconSize = 19.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Google Drive Sync", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = config.accountEmail ?: "End-to-end encrypted cross-device sync",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            KansoPillBadge(
                text = if (config.isConnected) "Connected" else "Off",
                color = if (config.isConnected) kansoColors.income else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Stores an AES-256-GCM encrypted snapshot inside your private Google Drive App Data folder — invisible in My Drive and unreadable without your Sync Passphrase.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = config.lastSyncMillis?.let {
                "Last synced: ${KansoDateFormatters.formatFullDateTime(it)}"
            } ?: "Last synced: Not synced yet",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        if (config.isConnected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-sync across devices", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Syncs on app launch, background & every 6 hours",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.autoSyncEnabled,
                    onCheckedChange = onToggleAutoSync
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onEditPassphrase),
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
                        text = "Sync Encryption Passphrase",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = if (config.hasPassphrase) {
                            "Configured (Android Keystore encrypted) — tap to change"
                        } else {
                            "Not set yet — tap to configure"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (config.hasPassphrase) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        AnimatedContent(
            targetState = driveState,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "driveSyncStatus"
        ) { currentDriveState ->
            when (currentDriveState) {
                DriveSyncState.Idle,
                is DriveSyncState.NeedsPassphrase,
                is DriveSyncState.NeedsConsent -> Unit

                DriveSyncState.Authorizing -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KansoAnimatedProgressBar(progress = 0.35f, height = 7.dp)
                        Text("Authorizing with Google Drive...", style = MaterialTheme.typography.labelMedium)
                    }
                }

                is DriveSyncState.Syncing -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KansoAnimatedProgressBar(progress = 0.72f, height = 7.dp)
                        Text(currentDriveState.label, style = MaterialTheme.typography.labelMedium)
                    }
                }

                is DriveSyncState.Synced -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = currentDriveState.message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                is DriveSyncState.Conflict -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = kansoColors.warning.copy(alpha = 0.14f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Both this device and Google Drive have changes. Choose which copy to keep.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            KansoOutlinedButton(
                                text = "Resolve Conflict",
                                size = KansoButtonSize.COMPACT,
                                onClick = onOpenConflictDialog
                            )
                        }
                    }
                }

                is DriveSyncState.Error -> {
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
                                text = currentDriveState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (currentDriveState.isPassphraseError) {
                                KansoOutlinedButton(
                                    text = "Update Sync Passphrase",
                                    size = KansoButtonSize.COMPACT,
                                    onClick = onEditPassphrase
                                )
                            } else if (currentDriveState.isConfigError) {
                                Text(
                                    text = "In Google Cloud Console: enable Google Drive API, configure OAuth consent screen (drive.appdata + test user), and create an Android OAuth 2.0 Client ID for com.kanso.app using this APK's SHA-1 certificate fingerprint.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!config.isConnected) {
            KansoPrimaryButton(
                text = "Connect Google Drive",
                leadingIcon = Icons.Outlined.CloudSync,
                size = KansoButtonSize.LARGE,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = onConnectOrSync
            )
        } else {
            KansoPrimaryButton(
                text = "Sync Now",
                leadingIcon = Icons.Outlined.CloudSync,
                size = KansoButtonSize.LARGE,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = onConnectOrSync
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KansoTonalButton(
                    text = "Upload",
                    leadingIcon = Icons.Outlined.CloudUpload,
                    size = KansoButtonSize.COMPACT,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    onClick = onForceUpload
                )
                KansoOutlinedButton(
                    text = "Restore",
                    leadingIcon = Icons.Outlined.CloudDownload,
                    size = KansoButtonSize.COMPACT,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    onClick = onRequestForceRestore
                )
            }

            TextButton(
                onClick = onRequestDisconnect,
                enabled = !busy,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Disconnect Google Drive",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DrivePassphraseDialog(
    hasExisting: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    KansoFormBottomSheet(
        title = if (hasExisting) "Update Sync Passphrase" else "Set Sync Encryption Passphrase",
        onDismiss = onDismiss,
        primaryLabel = "Save & Encrypt",
        badgeText = "E2EE",
        badgeColor = MaterialTheme.colorScheme.primary,
        badgeIcon = Icons.Outlined.Lock,
        errorText = error,
        onPrimaryClick = {
            when {
                passphrase.length < EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH ->
                    error = "Passphrase must be at least ${EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH} characters."
                passphrase != confirm ->
                    error = "Passphrases do not match."
                else -> onSave(passphrase)
            }
        }
    ) {
        Text(
            text = "All Google Drive sync data is end-to-end encrypted with this passphrase before leaving your device. Use the exact same passphrase on all your devices (min ${EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH} chars). Stored locally via Android Keystore.",
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
            label = { Text("Sync passphrase") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
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
            shape = RoundedCornerShape(14.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
    }
}

@Composable
private fun DriveConflictDialog(
    conflict: DriveSyncState.Conflict,
    onKeepCloud: () -> Unit,
    onKeepDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    val remote = conflict.remoteFile
    val remoteTime = if (remote.modifiedTimeMillis > 0L) {
        KansoDateFormatters.formatFullDateTime(remote.modifiedTimeMillis)
    } else {
        "Unknown time"
    }
    val remoteItemsLabel = remote.itemCount?.let { "$it items" } ?: "Encrypted backup"
    val remoteDeviceLabel = remote.deviceModel?.let { " from $it" }.orEmpty()
    val conflictColor = warningSemanticColor()

    KansoFormBottomSheet(
        title = "Sync Conflict Detected",
        onDismiss = onDismiss,
        primaryLabel = "Keep Cloud",
        onPrimaryClick = onKeepCloud,
        badgeText = "Conflict",
        badgeColor = conflictColor,
        badgeIcon = Icons.Outlined.CloudSync,
        secondaryLabel = "Keep Device",
        onSecondaryClick = onKeepDevice,
        secondaryIsDestructive = false
    ) {
        Text(
            text = "Both this device and Google Drive contain different changes. Choose which snapshot should become the source of truth:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "This device: ${conflict.localItemCount} items",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "Google Drive: $remoteItemsLabel$remoteDeviceLabel",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "Cloud updated: $remoteTime",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionOverline(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun AppUpdatesContent(
    uiState: SettingsUiState,
    onToggleAutoCheck: (Boolean) -> Unit,
    onEditGithubToken: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadAndInstall: (com.kanso.app.update.AppReleaseInfo) -> Unit,
    onInstallApk: (java.io.File) -> Unit,
    onOpenReleasesPage: () -> Unit
) {
    val prefs = uiState.preferences
    Column(
        modifier = Modifier.animateContentSize(animationSpec = KansoMotion.SizeSpring),
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
                        } ?: "Checks GitHub Releases automatically every 6 hours",
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
                    .clip(RoundedCornerShape(12.dp))
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
                        text = "GitHub Access Token (Optional)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = if (uiState.hasGithubToken) {
                            "Configured (Android Keystore encrypted) — tap to update"
                        } else {
                            "Not set — optional for public repo (raises GitHub API rate limit)"
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

            AnimatedContent(
                targetState = uiState.updateState,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "appUpdateStatus"
            ) { status ->
                when (status) {
                    AppUpdateState.Idle -> Unit
                    AppUpdateState.Checking -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            KansoAnimatedProgressBar(progress = 0.45f, height = 7.dp)
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
                                KansoPrimaryButton(
                                    text = "Download & Install ${status.release.tagName}",
                                    leadingIcon = Icons.Outlined.InstallMobile,
                                    size = KansoButtonSize.LARGE,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onDownloadAndInstall(status.release) }
                                )
                            }
                        }
                    }

                    is AppUpdateState.Downloading -> {
                        val total = status.totalBytes
                        val progress = if (total > 0L) {
                            (status.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        } else 0.15f
                        val mbDownloaded = String.format(java.util.Locale.US, "%.1f", status.downloadedBytes / 1_048_576.0)
                        val mbTotal = if (total > 0L) {
                            String.format(java.util.Locale.US, "%.1f MB", total / 1_048_576.0)
                        } else "—"
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            KansoAnimatedProgressBar(progress = progress, height = 7.dp)
                            Text(
                                text = "Downloading ${status.release.tagName}: $mbDownloaded / $mbTotal",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    is AppUpdateState.Verifying -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            KansoAnimatedProgressBar(progress = 0.95f, height = 7.dp)
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
                                KansoPrimaryButton(
                                    text = "Install APK",
                                    leadingIcon = Icons.Outlined.InstallMobile,
                                    size = KansoButtonSize.LARGE,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onInstallApk(status.apkFile) }
                                )
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
                                    KansoOutlinedButton(
                                        text = "Set GitHub Token",
                                        size = KansoButtonSize.COMPACT,
                                        onClick = onEditGithubToken
                                    )
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
                KansoPrimaryButton(
                    text = "Check Updates",
                    size = KansoButtonSize.LARGE,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    onClick = onCheckUpdates
                )
                KansoOutlinedButton(
                    text = "Open Releases",
                    size = KansoButtonSize.LARGE,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    onClick = onOpenReleasesPage
                )
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

    KansoFormBottomSheet(
        title = "GitHub Access Token",
        onDismiss = onDismiss,
        primaryLabel = if (tokenText.isBlank() && hasExistingToken) "Clear Token" else "Save Token",
        badgeText = "Optional",
        badgeColor = MaterialTheme.colorScheme.primary,
        badgeIcon = Icons.Outlined.Lock,
        onPrimaryClick = {
            onSave(tokenText.trim().ifEmpty { null })
        }
    ) {
        Text(
            text = "rifkymaulana/Kanso is public, so in-app updates work automatically without a token. Only add an optional read-only Fine-Grained Personal Access Token if you hit GitHub's unauthenticated API rate limit or make the repository private again. Stored locally encrypted with Android Keystore and never included in backups.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = tokenText,
            onValueChange = { tokenText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (hasExistingToken) "New token (leave blank to clear)" else "github_pat_...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
    }
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
    KansoFormBottomSheet(
        title = "Display Name",
        onDismiss = onDismiss,
        primaryLabel = "Save Name",
        badgeText = "Profile",
        badgeColor = MaterialTheme.colorScheme.primary,
        onPrimaryClick = { onSave(name) }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your name") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
    }
}

@Composable
private fun WaterGoalDialog(
    currentGoalMl: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by remember { mutableStateOf(currentGoalMl.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val habitsAccent = MaterialTheme.kansoColors.habitsAccent

    KansoFormBottomSheet(
        title = "Daily Hydration Goal",
        onDismiss = onDismiss,
        primaryLabel = "Save Goal",
        primaryColor = habitsAccent,
        badgeText = "${text.ifBlank { currentGoalMl.toString() }} ml",
        badgeColor = habitsAccent,
        badgeIcon = Icons.Outlined.WaterDrop,
        errorText = error,
        onPrimaryClick = {
            val parsed = text.toIntOrNull()
            if (parsed == null || parsed !in 250..10_000) {
                error = "Enter a goal between 250 and 10,000 ml."
            } else {
                onSave(parsed)
            }
        }
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it.filter { ch -> ch.isDigit() }
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Goal (ml)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(1500, 2000, 2500, 3000), key = { it }) { presetMl ->
                AssistChip(
                    onClick = {
                        text = presetMl.toString()
                        error = null
                    },
                    label = { Text("$presetMl ml") },
                    shape = RoundedCornerShape(50)
                )
            }
        }
    }
}

@Composable
private fun CurrencyPickerDialog(
    current: AppCurrency,
    onDismiss: () -> Unit,
    onSelect: (AppCurrency) -> Unit
) {
    KansoFormBottomSheet(
        title = "Default Currency",
        onDismiss = onDismiss,
        primaryLabel = "Done",
        onPrimaryClick = onDismiss,
        badgeText = current.code,
        badgeColor = MaterialTheme.colorScheme.primary,
        badgeIcon = Icons.Outlined.Payments
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppCurrency.entries.forEach { item ->
                val selected = item == current
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(item) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected, onClick = { onSelect(item) })
                        Column {
                            Text(
                                text = "${item.code} (${item.symbol})",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            )
                            Text(
                                text = item.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportPassphraseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    KansoFormBottomSheet(
        title = "Encrypt Backup",
        onDismiss = onDismiss,
        primaryLabel = "Choose Location",
        badgeText = "AES-256",
        badgeColor = MaterialTheme.colorScheme.primary,
        badgeIcon = Icons.Outlined.CloudUpload,
        errorText = error,
        onPrimaryClick = {
            when {
                passphrase.length < EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH ->
                    error = "Passphrase must be at least ${EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH} characters."
                passphrase != confirm ->
                    error = "Passphrases do not match."
                else -> onConfirm(passphrase)
            }
        }
    ) {
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
            shape = RoundedCornerShape(14.dp),
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
            shape = RoundedCornerShape(14.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
    }
}

@Composable
private fun RestorePassphraseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    KansoFormBottomSheet(
        title = "Restore Backup",
        onDismiss = onDismiss,
        primaryLabel = "Restore Data",
        primaryColor = MaterialTheme.colorScheme.error,
        primaryContentColor = MaterialTheme.colorScheme.onError,
        badgeText = "Overwrite",
        badgeColor = MaterialTheme.colorScheme.error,
        badgeIcon = Icons.Outlined.Restore,
        errorText = error,
        onPrimaryClick = {
            if (passphrase.length < EncryptedBackupCrypto.MIN_PASSPHRASE_LENGTH) {
                error = "Enter your backup passphrase."
            } else {
                onConfirm(passphrase)
            }
        }
    ) {
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
            shape = RoundedCornerShape(14.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
    }
}

@Composable
private fun BriefingScheduleRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    timeText: String,
    onToggle: (Boolean) -> Unit,
    onPickTime: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = enabled, onClick = onPickTime),
            shape = RoundedCornerShape(10.dp),
            color = if (enabled) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            border = BorderStroke(
                1.dp,
                if (enabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            )
        ) {
            Text(
                text = timeText,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    slot: BriefingSlot,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val isMorning = slot == BriefingSlot.MORNING
    val presets = remember(slot) {
        if (isMorning) {
            listOf(6 to 30, 7 to 30, 8 to 0, 9 to 0)
        } else {
            listOf(19 to 0, 20 to 0, 20 to 30, 21 to 30)
        }
    }
    var chosenHour by remember(slot, initialHour) { mutableIntStateOf(initialHour) }
    var chosenMinute by remember(slot, initialMinute) { mutableIntStateOf(initialMinute) }
    var presetVersion by remember { mutableIntStateOf(0) }

    key(presetVersion) {
        val pickerState = rememberTimePickerState(
            initialHour = chosenHour,
            initialMinute = chosenMinute,
            is24Hour = true
        )
        KansoFormBottomSheet(
            title = if (isMorning) "Morning Briefing Time" else "Evening Check-In Time",
            onDismiss = onDismiss,
            primaryLabel = "Save Schedule",
            onPrimaryClick = { onConfirm(pickerState.hour, pickerState.minute) },
            badgeText = BriefingCalculations.formatClockTime(pickerState.hour, pickerState.minute),
            badgeIcon = Icons.Outlined.Schedule
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets, key = { "${it.first}:${it.second}" }) { (h, m) ->
                    FilterChip(
                        selected = pickerState.hour == h && pickerState.minute == m,
                        onClick = {
                            chosenHour = h
                            chosenMinute = m
                            presetVersion++
                        },
                        label = { Text(BriefingCalculations.formatClockTime(h, m)) }
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimeInput(state = pickerState)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
