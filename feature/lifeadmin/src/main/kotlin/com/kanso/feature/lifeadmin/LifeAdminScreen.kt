package com.kanso.feature.lifeadmin

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoEmptyState
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.core.ui.components.expenseSemanticColor
import com.kanso.core.ui.components.incomeSemanticColor
import com.kanso.core.ui.theme.KansoTheme
import com.kanso.core.ui.theme.WarningAmber
import com.kanso.core.ui.theme.WarningAmberDark
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Life Admin & Vault") },
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

            when (selectedTab) {
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

@Composable
private fun ExpiriesTabContent(
    uiState: LifeAdminUiState,
    onAddExpiry: () -> Unit,
    onEditExpiry: (ExpiryItem) -> Unit,
    onCopyReference: (String) -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()
    val warningColor = if (isSystemInDarkTheme()) WarningAmberDark else WarningAmber

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
                    Text("IDs, Taxes & Warranties", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (uiState.attentionExpiryCount > 0) {
                            "${uiState.attentionExpiryCount} item(s) need attention"
                        } else {
                            "All tracked documents up to date"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.attentionExpiryCount > 0) {
                            warningColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Button(onClick = onAddExpiry) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add")
                }
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

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onEditExpiry(item) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${item.category.label} • ${KansoDateFormatters.formatFullDate(item.expiryEpochDay)}",
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { onCopyReference(item.referenceNumber) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy reference",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = badgeColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = badgeColor
                            )
                        }
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
                Text("Maintenance History", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onAddMaintenance) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add Log")
                }
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

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onEditMaintenance(log) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                        }.joinToString(" • ")
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
                            Text(
                                text = nextText,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
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

    if (!uiState.isVaultUnlocked) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Private Vault Locked",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${uiState.vaultItemCount} encrypted secret(s) stored with Android Keystore AES-256-GCM. Screenshots are blocked while open.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (availability == BiometricAvailability.AVAILABLE) {
                Button(onClick = onRequestUnlockVault) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                    Text("  Unlock with Biometrics / PIN")
                }
            } else {
                Text(
                    text = "Set a device PIN, pattern, or biometric lock in Android Settings to unlock the Private Vault.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenSecuritySettings) {
                    Text("Open Security Settings")
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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Unlocked • Screenshots blocked",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                Text("Encrypted Secrets", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onAddVaultItem) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add")
                }
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
                    subtitle = "Save recovery codes, spare PINs, passport scans notes, or private credentials encrypted offline.",
                    actionLabel = "Add Secret",
                    onAction = onAddVaultItem
                )
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                val isRevealed = revealedIds[item.id] == true
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = item.category.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row {
                                IconButton(onClick = { revealedIds[item.id] = !isRevealed }) {
                                    Icon(
                                        imageVector = if (isRevealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (isRevealed) "Hide" else "Reveal"
                                    )
                                }
                                IconButton(onClick = { onCopyVaultSecret(item) }) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
                                }
                                IconButton(onClick = { onEditVaultItem(item) }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                                }
                            }
                        }

                        Text(
                            text = if (isRevealed) item.secretContent else "•••••••• ••••••••",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
