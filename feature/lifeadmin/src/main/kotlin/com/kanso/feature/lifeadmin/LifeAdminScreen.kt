package com.kanso.feature.lifeadmin

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ExpiryCategory
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.ExpiryUrgency
import com.kanso.core.model.LifeAdminCalculations
import com.kanso.core.model.MaintenanceCategory
import com.kanso.core.model.MaintenanceLog
import com.kanso.core.model.VaultCategory
import com.kanso.core.model.VaultItem
import com.kanso.core.security.BiometricAuthHelper
import com.kanso.core.security.BiometricAvailability
import com.kanso.core.ui.components.KansoAccentIconBadge
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoEmptyState
import com.kanso.core.ui.components.KansoHeroCard
import com.kanso.core.ui.components.KansoOutlinedButton
import com.kanso.core.ui.components.KansoPillBadge
import com.kanso.core.ui.components.KansoPrimaryButton
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.core.ui.components.KansoSmallIconButton
import com.kanso.core.ui.components.KansoTonalButton
import com.kanso.core.ui.components.expenseSemanticColor
import com.kanso.core.ui.components.incomeSemanticColor
import com.kanso.core.ui.components.warningSemanticColor
import com.kanso.core.ui.theme.KansoTheme
import com.kanso.core.ui.theme.kansoColors
import java.time.LocalDate

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
fun LifeAdminRoute(
    initialTab: Int = 0,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: LifeAdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = LocalClipboardManager.current

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    LaunchedEffect(initialTab) {
        if (initialTab in 0..2) {
            selectedTab = initialTab
        }
    }

    // Protect vault screens with FLAG_SECURE when Vault tab is active.
    DisposableEffect(selectedTab, activity) {
        val window = activity?.window
        if (selectedTab == 2 && window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (selectedTab == 2 && window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    // Auto-lock vault when app goes to background.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.lockVault()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.lockVault()
        }
    }

    var showNewExpiry by remember { mutableStateOf(false) }
    var editingExpiry by remember { mutableStateOf<ExpiryItem?>(null) }

    var showNewMaintenance by remember { mutableStateOf(false) }
    var editingMaintenance by remember { mutableStateOf<MaintenanceLog?>(null) }

    var showNewVault by remember { mutableStateOf(false) }
    var editingVault by remember { mutableStateOf<VaultItem?>(null) }

    LifeAdminScreenContent(
        uiState = uiState,
        selectedTab = selectedTab,
        onSelectTab = { selectedTab = it },
        onBack = onBack,
        onAddExpiry = { showNewExpiry = true },
        onEditExpiry = { editingExpiry = it },
        onCopyReference = { ref ->
            clipboard.setText(AnnotatedString(ref))
            onShowMessage("Copied reference number")
        },
        onAddMaintenance = { showNewMaintenance = true },
        onEditMaintenance = { editingMaintenance = it },
        onRequestUnlockVault = {
            val availability = BiometricAuthHelper.checkAvailability(context)
            when (availability) {
                BiometricAvailability.AVAILABLE -> {
                    if (activity != null) {
                        BiometricAuthHelper.authenticate(
                            activity = activity,
                            title = "Unlock Private Vault",
                            subtitle = "Verify identity to view encrypted secrets",
                            onSuccess = { viewModel.unlockVault() },
                            onError = { msg -> onShowMessage(msg) }
                        )
                    } else {
                        onShowMessage("Unable to open biometric prompt.")
                    }
                }
                BiometricAvailability.NOT_ENROLLED -> {
                    onShowMessage("Set up screen lock or biometrics in Android Settings first.")
                }
                BiometricAvailability.UNAVAILABLE -> {
                    onShowMessage("Biometric or device credential hardware unavailable.")
                }
            }
        },
        onOpenSecuritySettings = {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            }
        },
        onLockVault = viewModel::lockVault,
        onAddVaultItem = { showNewVault = true },
        onEditVaultItem = { editingVault = it },
        onCopyVaultSecret = { item ->
            clipboard.setText(AnnotatedString(item.secretContent))
            onShowMessage("Copied ${item.title}")
        }
    )

    if (showNewExpiry || editingExpiry != null) {
        val current = editingExpiry
        ExpiryEditorDialog(
            initial = current,
            onDismiss = {
                showNewExpiry = false
                editingExpiry = null
            },
            onSave = { title, cat, ref, epochDay, remindDays, note ->
                viewModel.saveExpiry(current, title, cat, ref, epochDay, remindDays, note)
                showNewExpiry = false
                editingExpiry = null
            },
            onDelete = current?.let { item ->
                {
                    viewModel.deleteExpiry(item.id)
                    editingExpiry = null
                    onShowMessage("Expiry alert deleted")
                }
            }
        )
    }

    if (showNewMaintenance || editingMaintenance != null) {
        val current = editingMaintenance
        MaintenanceEditorDialog(
            currency = uiState.currency,
            initial = current,
            onDismiss = {
                showNewMaintenance = false
                editingMaintenance = null
            },
            onSave = { asset, cat, desc, serviceDay, odo, cost, nextDay, nextOdo, note ->
                viewModel.saveMaintenanceLog(current, asset, cat, desc, serviceDay, odo, cost, nextDay, nextOdo, note)
                showNewMaintenance = false
                editingMaintenance = null
            },
            onDelete = current?.let { log ->
                {
                    viewModel.deleteMaintenanceLog(log.id)
                    editingMaintenance = null
                    onShowMessage("Service log deleted")
                }
            }
        )
    }

    if (showNewVault || editingVault != null) {
        val current = editingVault
        VaultEditorDialog(
            initial = current,
            onDismiss = {
                showNewVault = false
                editingVault = null
            },
            onSave = { title, cat, content ->
                viewModel.saveVaultItem(current, title, cat, content)
                showNewVault = false
                editingVault = null
            },
            onDelete = current?.let { item ->
                {
                    viewModel.deleteVaultItem(item.id)
                    editingVault = null
                    onShowMessage("Vault secret deleted")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeAdminScreenContent(
    uiState: LifeAdminUiState,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onBack: () -> Unit,
    onAddExpiry: () -> Unit,
    onEditExpiry: (ExpiryItem) -> Unit,
    onCopyReference: (String) -> Unit,
    onAddMaintenance: () -> Unit,
    onEditMaintenance: (MaintenanceLog) -> Unit,
    onRequestUnlockVault: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onLockVault: () -> Unit,
    onAddVaultItem: () -> Unit,
    onEditVaultItem: (VaultItem) -> Unit,
    onCopyVaultSecret: (VaultItem) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Life Admin & Vault", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KansoSegmentedRow(
                options = listOf("Expiries", "Maintenance", "Vault"),
                selectedIndex = selectedTab,
                onSelect = onSelectTab
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "lifeAdminTab"
            ) { tab ->
                when (tab) {
                    0 -> ExpiriesTabContent(
                        uiState = uiState,
                        onAddExpiry = onAddExpiry,
                        onEditExpiry = onEditExpiry,
                        onCopyReference = onCopyReference
                    )

                    1 -> MaintenanceTabContent(
                        uiState = uiState,
                        onAddMaintenance = onAddMaintenance,
                        onEditMaintenance = onEditMaintenance
                    )

                    else -> VaultTabContent(
                        uiState = uiState,
                        onRequestUnlockVault = onRequestUnlockVault,
                        onOpenSecuritySettings = onOpenSecuritySettings,
                        onLockVault = onLockVault,
                        onAddVaultItem = onAddVaultItem,
                        onEditVaultItem = onEditVaultItem,
                        onCopyVaultSecret = onCopyVaultSecret
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpiriesTabContent(
    uiState: LifeAdminUiState,
    onAddExpiry: () -> Unit,
    onEditExpiry: (ExpiryItem) -> Unit,
    onCopyReference: (String) -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()
    val warningColor = warningSemanticColor()
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("IDs, Taxes & Warranties", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = if (uiState.attentionExpiryCount > 0) {
                            "${uiState.attentionExpiryCount} item(s) need attention"
                        } else {
                            "All tracked documents up to date"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.attentionExpiryCount > 0) warningColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                KansoTonalButton(
                    text = "Add",
                    leadingIcon = Icons.Outlined.Add,
                    size = KansoButtonSize.COMPACT,
                    onClick = onAddExpiry
                )
            }
        }

        if (uiState.expiries.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.EventAvailable,
                    title = "No expiry dates tracked",
                    subtitle = "Track passport, driver's license, STNK/vehicle tax, or gadget warranties before they lapse.",
                    actionLabel = "Add Expiry",
                    onAction = onAddExpiry
                )
            }
        } else {
            items(uiState.expiries, key = { it.id }) { item ->
                val urgency = LifeAdminCalculations.urgency(item, todayEpoch)
                val delta = LifeAdminCalculations.daysUntilExpiry(item.expiryEpochDay, todayEpoch)
                val badgeColor = when (urgency) {
                    ExpiryUrgency.EXPIRED, ExpiryUrgency.CRITICAL_7_DAYS -> expenseSemanticColor()
                    ExpiryUrgency.UPCOMING -> warningColor
                    ExpiryUrgency.SAFE -> incomeSemanticColor()
                }
                val badgeText = when {
                    delta < 0L -> "Expired ${-delta}d ago"
                    delta == 0L -> "Expires today"
                    else -> "${delta}d left"
                }

                KansoBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEditExpiry(item) },
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KansoAccentIconBadge(
                            icon = Icons.Outlined.EventAvailable,
                            accent = badgeColor
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${item.category.label} · ${KansoDateFormatters.formatFullDate(item.expiryEpochDay)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.referenceNumber.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = item.referenceNumber,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    KansoSmallIconButton(
                                        icon = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy reference",
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onCopyReference(item.referenceNumber)
                                        }
                                    )
                                }
                            }
                        }

                        KansoPillBadge(text = badgeText, color = badgeColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceTabContent(
    uiState: LifeAdminUiState,
    onAddMaintenance: () -> Unit,
    onEditMaintenance: (MaintenanceLog) -> Unit
) {
    var categoryFilter by remember { mutableStateOf<MaintenanceCategory?>(null) }
    val filtered = remember(uiState.maintenanceLogs, categoryFilter) {
        if (categoryFilter == null) {
            uiState.maintenanceLogs
        } else {
            uiState.maintenanceLogs.filter { it.category == categoryFilter }
        }
    }
    val accent = MaterialTheme.kansoColors.lifeAdminAccent

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Maintenance History", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                KansoTonalButton(
                    text = "Add Log",
                    leadingIcon = Icons.Outlined.Add,
                    size = KansoButtonSize.COMPACT,
                    onClick = onAddMaintenance
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = categoryFilter == null,
                        onClick = { categoryFilter = null },
                        label = { Text("All") }
                    )
                }
                items(MaintenanceCategory.entries, key = { it.name }) { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                        label = { Text(cat.label) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.Build,
                    title = "No service logs yet",
                    subtitle = "Log oil changes, AC cleaning, tire swaps, or home repairs with odometer and cost.",
                    actionLabel = "Add Service Log",
                    onAction = onAddMaintenance
                )
            }
        } else {
            items(filtered, key = { it.id }) { log ->
                val odo = log.odometerKm
                val cost = log.costCents
                val nextDay = log.nextDueEpochDay
                val nextOdo = log.nextDueOdometerKm

                KansoBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEditMaintenance(log) },
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KansoAccentIconBadge(
                            icon = Icons.Outlined.Build,
                            accent = accent
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${log.assetName} — ${log.serviceDescription}",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = KansoDateFormatters.formatFullDate(log.serviceEpochDay),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val meta = buildList {
                                add(log.category.label)
                                if (odo != null) add("$odo km")
                                if (cost != null) add(CurrencyFormatter.format(cost, uiState.currency))
                            }.joinToString(" · ")
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (nextDay != null || nextOdo != null) {
                                val nextText = buildList {
                                    if (nextDay != null) add("Next: ${KansoDateFormatters.formatFullDate(nextDay)}")
                                    if (nextOdo != null) add("at $nextOdo km")
                                }.joinToString(" ")
                                KansoPillBadge(text = nextText, color = accent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultTabContent(
    uiState: LifeAdminUiState,
    onRequestUnlockVault: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onLockVault: () -> Unit,
    onAddVaultItem: () -> Unit,
    onEditVaultItem: (VaultItem) -> Unit,
    onCopyVaultSecret: (VaultItem) -> Unit
) {
    val context = LocalContext.current
    val availability = remember(context) { BiometricAuthHelper.checkAvailability(context) }
    val haptic = LocalHapticFeedback.current
    val kansoColors = MaterialTheme.kansoColors

    if (!uiState.isVaultUnlocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            KansoHeroCard(
                glowColor = kansoColors.lifeAdminAccent,
                contentPadding = PaddingValues(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFBBF24).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color(0xFFFBBF24)
                        )
                    }

                    Text(
                        text = "Private Vault Locked",
                        style = MaterialTheme.typography.headlineMedium,
                        color = kansoColors.heroOnSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${uiState.vaultItemCount} encrypted secret(s) · AES-256-GCM",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = kansoColors.heroOnSurface
                        )
                    }

                    Text(
                        text = "Backed by Android Keystore hardware encryption. Screenshots & screen recording are blocked while unlocked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = kansoColors.heroOnSurfaceMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (availability == BiometricAvailability.AVAILABLE) {
                        KansoPrimaryButton(
                            text = "Unlock with Biometrics / PIN",
                            leadingIcon = Icons.Outlined.Fingerprint,
                            size = KansoButtonSize.LARGE,
                            containerColor = Color(0xFF2DD4BF),
                            contentColor = Color(0xFF042F2E),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRequestUnlockVault()
                            }
                        )
                    } else {
                        Text(
                            text = "Set a device PIN, pattern, or biometric lock in Android Settings to unlock the Private Vault.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFCA5A5),
                            textAlign = TextAlign.Center
                        )
                        KansoOutlinedButton(
                            text = "Open Security Settings",
                            size = KansoButtonSize.LARGE,
                            contentColor = kansoColors.heroOnSurface,
                            borderColor = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onOpenSecuritySettings
                        )
                    }
                }
            }
        }
        return
    }

    val revealedIds = remember { mutableStateMapOf<Long, Boolean>() }
    var categoryFilter by remember { mutableStateOf<VaultCategory?>(null) }
    val filtered = remember(uiState.vaultItems, categoryFilter) {
        if (categoryFilter == null) {
            uiState.vaultItems
        } else {
            uiState.vaultItems.filter { it.category == categoryFilter }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            KansoBentoCard(
                containerColor = kansoColors.income.copy(alpha = 0.08f),
                borderColor = kansoColors.income.copy(alpha = 0.28f),
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = kansoColors.income
                        )
                        Text(
                            text = "Unlocked · Screenshots blocked",
                            style = MaterialTheme.typography.labelLarge,
                            color = kansoColors.income
                        )
                    }
                    TextButton(onClick = onLockVault) {
                        Text("Lock")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Encrypted Secrets", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                KansoTonalButton(
                    text = "Add",
                    leadingIcon = Icons.Outlined.Add,
                    size = KansoButtonSize.COMPACT,
                    onClick = onAddVaultItem
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = categoryFilter == null,
                        onClick = { categoryFilter = null },
                        label = { Text("All") }
                    )
                }
                items(VaultCategory.entries, key = { it.name }) { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                        label = { Text(cat.label) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.Lock,
                    title = "Vault is empty",
                    subtitle = "Save recovery codes, spare PINs, passport notes, or private credentials encrypted offline.",
                    actionLabel = "Add Secret",
                    onAction = onAddVaultItem
                )
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                val isRevealed = revealedIds[item.id] == true
                KansoBentoCard(
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                KansoPillBadge(text = item.category.label, color = kansoColors.lifeAdminAccent)
                            }
                            Row {
                                KansoSmallIconButton(
                                    icon = if (isRevealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (isRevealed) "Hide" else "Reveal",
                                    onClick = { revealedIds[item.id] = !isRevealed }
                                )
                                KansoSmallIconButton(
                                    icon = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onCopyVaultSecret(item)
                                    }
                                )
                                KansoSmallIconButton(
                                    icon = Icons.Outlined.Edit,
                                    contentDescription = "Edit",
                                    onClick = { onEditVaultItem(item) }
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = if (isRevealed) item.secretContent else "•••••••• ••••••••",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LifeAdminScreenPreview() {
    KansoTheme {
        val today = LocalDate.now().toEpochDay()
        LifeAdminScreenContent(
            uiState = LifeAdminUiState(
                expiries = listOf(
                    ExpiryItem(
                        id = 1L,
                        title = "Passport",
                        category = ExpiryCategory.PASSPORT,
                        referenceNumber = "X908123",
                        expiryEpochDay = today + 18L,
                        remindDaysBefore = 30,
                        updatedAtMillis = 1L
                    )
                ),
                attentionExpiryCount = 1
            ),
            selectedTab = 0,
            onSelectTab = {},
            onBack = {},
            onAddExpiry = {},
            onEditExpiry = {},
            onCopyReference = {},
            onAddMaintenance = {},
            onEditMaintenance = {},
            onRequestUnlockVault = {},
            onOpenSecuritySettings = {},
            onLockVault = {},
            onAddVaultItem = {},
            onEditVaultItem = {},
            onCopyVaultSecret = {}
        )
    }
}
