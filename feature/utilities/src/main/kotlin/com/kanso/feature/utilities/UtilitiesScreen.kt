package com.kanso.feature.utilities

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CountdownCalculations
import com.kanso.core.model.CountdownItem
import com.kanso.core.model.OfflineCurrencyConverter
import com.kanso.core.model.UnitConverter
import com.kanso.core.model.UnitDomain
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoDatePickerDialog
import com.kanso.core.ui.components.KansoEmptyState
import com.kanso.core.ui.components.KansoHeroCard
import com.kanso.core.ui.components.KansoOutlinedButton
import com.kanso.core.ui.components.KansoPillBadge
import com.kanso.core.ui.components.KansoPrimaryButton
import com.kanso.core.ui.components.KansoProgressRing
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.core.ui.components.KansoSmallIconButton
import com.kanso.core.ui.components.KansoTonalButton
import com.kanso.core.ui.theme.KansoTheme
import com.kanso.core.ui.theme.kansoColors
import java.text.DecimalFormat
import java.time.LocalDate
import java.util.Locale

@Composable
fun UtilitiesRoute(
    initialTab: Int = 0,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: UtilitiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    var showNewCountdown by remember { mutableStateOf(false) }
    var editingCountdown by remember { mutableStateOf<CountdownItem?>(null) }
    var showRatesDialog by remember { mutableStateOf(false) }

    UtilitiesScreenContent(
        uiState = uiState,
        initialTab = initialTab,
        onBack = onBack,
        onCopyText = { text, label ->
            clipboard.setText(AnnotatedString(text))
            onShowMessage("Copied $label")
        },
        onSelectTimerMode = viewModel::selectTimerMode,
        onToggleTimer = viewModel::toggleTimer,
        onResetTimer = viewModel::resetTimer,
        onOpenRatesDialog = { showRatesDialog = true },
        onAddCountdown = { showNewCountdown = true },
        onEditCountdown = { editingCountdown = it }
    )

    if (showNewCountdown || editingCountdown != null) {
        val current = editingCountdown
        CountdownEditorDialog(
            initial = current,
            onDismiss = {
                showNewCountdown = false
                editingCountdown = null
            },
            onSave = { title, targetDay, note ->
                viewModel.saveCountdown(current, title, targetDay, note)
                showNewCountdown = false
                editingCountdown = null
            },
            onDelete = current?.let { item ->
                {
                    viewModel.deleteCountdown(item.id)
                    editingCountdown = null
                    onShowMessage("Countdown deleted")
                }
            }
        )
    }

    if (showRatesDialog) {
        ExchangeRatesDialog(
            currentRates = uiState.effectiveRatesVsUsd,
            onDismiss = { showRatesDialog = false },
            onSave = { rates ->
                viewModel.updateCustomRates(rates)
                showRatesDialog = false
                onShowMessage("Offline exchange rates updated")
            },
            onReset = {
                viewModel.resetCustomRates()
                showRatesDialog = false
                onShowMessage("Reset to built-in reference rates")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilitiesScreenContent(
    uiState: UtilitiesUiState,
    initialTab: Int = 0,
    onBack: () -> Unit,
    onCopyText: (String, String) -> Unit,
    onSelectTimerMode: (PomodoroMode) -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onOpenRatesDialog: () -> Unit,
    onAddCountdown: () -> Unit,
    onEditCountdown: (CountdownItem) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    LaunchedEffect(initialTab) {
        if (initialTab in 0..3) {
            selectedTab = initialTab
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
                title = { Text("Pocket Utilities", style = MaterialTheme.typography.titleLarge) },
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
                options = listOf("QR", "Convert", "Focus", "Dates"),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it }
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "utilitiesTab"
            ) { tab ->
                when (tab) {
                    0 -> QrTabContent(onCopyText = onCopyText)
                    1 -> ConverterTabContent(
                        uiState = uiState,
                        onCopyText = onCopyText,
                        onOpenRatesDialog = onOpenRatesDialog
                    )
                    2 -> FocusTabContent(
                        pomodoro = uiState.pomodoro,
                        onSelectMode = onSelectTimerMode,
                        onToggle = onToggleTimer,
                        onReset = onResetTimer
                    )
                    else -> CountdownsTabContent(
                        countdowns = uiState.countdowns,
                        onAddCountdown = onAddCountdown,
                        onEditCountdown = onEditCountdown
                    )
                }
            }
        }
    }
}

@Composable
private fun QrTabContent(
    onCopyText: (String, String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var qrMode by rememberSaveable { mutableIntStateOf(0) } // 0=Generate, 1=Scan
    var qrInput by rememberSaveable { mutableStateOf("") }
    var scannedResult by remember { mutableStateOf<String?>(null) }
    var showWifiDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = qrMode == 0,
                    onClick = { qrMode = 0 },
                    label = { Text("Generate QR") }
                )
                FilterChip(
                    selected = qrMode == 1,
                    onClick = {
                        scannedResult = null
                        qrMode = 1
                    },
                    label = { Text("Scan QR") }
                )
            }
        }

        if (qrMode == 0) {
            item {
                OutlinedTextField(
                    value = qrInput,
                    onValueChange = { qrInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Text, URL, or Wi-Fi payload") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp)
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KansoOutlinedButton(
                        text = "Wi-Fi QR",
                        leadingIcon = Icons.Outlined.Wifi,
                        size = KansoButtonSize.COMPACT,
                        onClick = { showWifiDialog = true }
                    )
                    if (qrInput.isNotBlank()) {
                        KansoOutlinedButton(
                            text = "Copy Text",
                            leadingIcon = Icons.Outlined.ContentCopy,
                            size = KansoButtonSize.COMPACT,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCopyText(qrInput, "QR text")
                            }
                        )
                    }
                }
            }
            item {
                val bitmap = remember(qrInput) { QrCodeUtils.generateQrBitmap(qrInput, 560) }
                if (bitmap != null) {
                    KansoBentoCard(contentPadding = PaddingValues(20.dp)) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .padding(14.dp)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Generated QR code",
                                    modifier = Modifier.size(220.dp)
                                )
                            }
                        }
                    }
                } else {
                    KansoBentoCard(contentPadding = PaddingValues(18.dp)) {
                        Text(
                            text = "Type text or tap Wi-Fi QR above to generate a crisp offline QR code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                val result = scannedResult
                if (result == null) {
                    QrScannerCard(onScanned = { decoded -> scannedResult = decoded })
                } else {
                    KansoBentoCard(contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Scanned Result", style = MaterialTheme.typography.titleMedium)
                            Text(result, style = MaterialTheme.typography.bodyLarge)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KansoPrimaryButton(
                                    text = "Copy",
                                    leadingIcon = Icons.Outlined.ContentCopy,
                                    size = KansoButtonSize.COMPACT,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onCopyText(result, "scanned QR")
                                    }
                                )
                                if (result.startsWith("http://", ignoreCase = true) ||
                                    result.startsWith("https://", ignoreCase = true)
                                ) {
                                    KansoOutlinedButton(
                                        text = "Open",
                                        leadingIcon = Icons.Outlined.OpenInBrowser,
                                        size = KansoButtonSize.COMPACT,
                                        onClick = {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result)))
                                            }
                                        }
                                    )
                                }
                                TextButton(
                                    onClick = { scannedResult = null },
                                    modifier = Modifier.height(KansoButtonSize.COMPACT.height),
                                    shape = RoundedCornerShape(KansoButtonSize.COMPACT.cornerRadius),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Text("Scan Again", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWifiDialog) {
        WifiQrDialog(
            onDismiss = { showWifiDialog = false },
            onBuild = { payload ->
                qrInput = payload
                showWifiDialog = false
            }
        )
    }
}

@Composable
private fun ConverterTabContent(
    uiState: UtilitiesUiState,
    onCopyText: (String, String) -> Unit,
    onOpenRatesDialog: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val utilitiesAccent = MaterialTheme.kansoColors.utilitiesAccent
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0=Units, 1=Currency

    var domain by remember { mutableStateOf(UnitDomain.LENGTH) }
    val domainUnits = remember(domain) { UnitConverter.unitsFor(domain) }
    var fromUnitId by remember(domain) { mutableStateOf(domainUnits.first().id) }
    var toUnitId by remember(domain) { mutableStateOf(domainUnits.getOrElse(1) { domainUnits.first() }.id) }
    var unitInput by rememberSaveable { mutableStateOf("1") }

    var fromCurrency by remember { mutableStateOf(AppCurrency.USD) }
    var toCurrency by remember { mutableStateOf(uiState.defaultCurrency) }
    var currencyInput by rememberSaveable { mutableStateOf("100") }

    val numberFormat = remember { DecimalFormat("#,##0.####") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Units") })
                FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Currency") })
            }
        }

        if (mode == 0) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(UnitDomain.entries, key = { it.name }) { d ->
                        FilterChip(
                            selected = domain == d,
                            onClick = { domain = d },
                            label = { Text(d.label) }
                        )
                    }
                }
            }
            item {
                val fromUnit = domainUnits.firstOrNull { it.id == fromUnitId } ?: domainUnits.first()
                val toUnit = domainUnits.firstOrNull { it.id == toUnitId } ?: domainUnits.last()
                val rawValue = unitInput.trim().replace(',', '.').toDoubleOrNull()
                val converted = rawValue?.let { UnitConverter.convert(it, fromUnit, toUnit) }

                KansoBentoCard(contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = unitInput,
                            onValueChange = { unitInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Value (${fromUnit.symbol})") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Text("From", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(domainUnits, key = { "from-${it.id}" }) { unit ->
                                FilterChip(
                                    selected = unit.id == fromUnit.id,
                                    onClick = { fromUnitId = unit.id },
                                    label = { Text(unit.symbol) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("To", style = MaterialTheme.typography.labelLarge)
                            KansoSmallIconButton(
                                icon = Icons.Outlined.SwapHoriz,
                                contentDescription = "Swap",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val prevFrom = fromUnit.id
                                    fromUnitId = toUnit.id
                                    toUnitId = prevFrom
                                }
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(domainUnits, key = { "to-${it.id}" }) { unit ->
                                FilterChip(
                                    selected = unit.id == toUnit.id,
                                    onClick = { toUnitId = unit.id },
                                    label = { Text(unit.symbol) }
                                )
                            }
                        }
                        val formatted = converted?.let { "${numberFormat.format(it)} ${toUnit.symbol}" } ?: "—"
                        KansoBentoCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = if (converted != null) {
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCopyText(formatted, "converted value")
                                }
                            } else null,
                            containerColor = utilitiesAccent.copy(alpha = 0.12f),
                            borderColor = utilitiesAccent.copy(alpha = 0.30f),
                            cornerRadius = 16.dp,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatted,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = utilitiesAccent
                                )
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = utilitiesAccent
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                val rawAmount = currencyInput.trim().replace(',', '.').toDoubleOrNull()
                val converted = rawAmount?.let {
                    OfflineCurrencyConverter.convert(
                        amount = it,
                        from = fromCurrency,
                        to = toCurrency,
                        customOverrides = uiState.effectiveRatesVsUsd
                    )
                }

                KansoBentoCard(contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Offline Currency Converter", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = onOpenRatesDialog) { Text("Rates") }
                        }
                        OutlinedTextField(
                            value = currencyInput,
                            onValueChange = { currencyInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Amount (${fromCurrency.code})") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Text("From", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AppCurrency.entries, key = { "fc-${it.code}" }) { c ->
                                FilterChip(
                                    selected = c == fromCurrency,
                                    onClick = { fromCurrency = c },
                                    label = { Text(c.code) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("To", style = MaterialTheme.typography.labelLarge)
                            KansoSmallIconButton(
                                icon = Icons.Outlined.SwapHoriz,
                                contentDescription = "Swap",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val prev = fromCurrency
                                    fromCurrency = toCurrency
                                    toCurrency = prev
                                }
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AppCurrency.entries, key = { "tc-${it.code}" }) { c ->
                                FilterChip(
                                    selected = c == toCurrency,
                                    onClick = { toCurrency = c },
                                    label = { Text(c.code) }
                                )
                            }
                        }

                        val resultFmt = if (converted != null) {
                            val pattern = if (toCurrency.decimalDigits == 0) "#,##0" else "#,##0.00"
                            "${toCurrency.symbol} ${DecimalFormat(pattern).format(converted)}"
                        } else "—"

                        KansoBentoCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = if (converted != null) {
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCopyText(resultFmt, "converted amount")
                                }
                            } else null,
                            containerColor = utilitiesAccent.copy(alpha = 0.12f),
                            borderColor = utilitiesAccent.copy(alpha = 0.30f),
                            cornerRadius = 16.dp,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = resultFmt,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = utilitiesAccent
                                )
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = utilitiesAccent
                                )
                            }
                        }
                        Text(
                            text = "Uses offline reference rates (tap Rates above to edit).",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusTabContent(
    pomodoro: PomodoroState,
    onSelectMode: (PomodoroMode) -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val kansoColors = MaterialTheme.kansoColors
    val ringColor = if (pomodoro.mode == PomodoroMode.FOCUS) Color(0xFF22D3EE) else Color(0xFF34D399)
    val mins = pomodoro.remainingSeconds / 60
    val secs = pomodoro.remainingSeconds % 60
    val timeLabel = String.format(Locale.US, "%02d:%02d", mins, secs)
    val progress = if (pomodoro.totalSeconds > 0) {
        pomodoro.remainingSeconds.toFloat() / pomodoro.totalSeconds.toFloat()
    } else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PomodoroMode.entries.forEach { mode ->
                    FilterChip(
                        selected = pomodoro.mode == mode,
                        onClick = { onSelectMode(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
        }

        item {
            KansoHeroCard(
                glowColor = ringColor,
                contentPadding = PaddingValues(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (pomodoro.isRunning) "Running · ${pomodoro.mode.label}" else "Ready · ${pomodoro.mode.label}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = kansoColors.heroOnSurface
                        )
                    }

                    KansoProgressRing(
                        progress = progress,
                        color = ringColor,
                        trackColor = Color.White.copy(alpha = 0.14f),
                        size = 220.dp,
                        strokeWidth = 12.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = kansoColors.heroOnSurface
                            )
                            Text(
                                text = "${pomodoro.completedFocusSessions} sessions today",
                                style = MaterialTheme.typography.labelMedium,
                                color = kansoColors.heroOnSurfaceMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KansoPrimaryButton(
                            text = if (pomodoro.isRunning) "Pause" else "Start",
                            size = KansoButtonSize.LARGE,
                            containerColor = ringColor,
                            contentColor = Color(0xFF042F2E),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggle()
                            }
                        )
                        KansoOutlinedButton(
                            text = "Reset",
                            size = KansoButtonSize.LARGE,
                            contentColor = kansoColors.heroOnSurface,
                            borderColor = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onReset()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownsTabContent(
    countdowns: List<CountdownItem>,
    onAddCountdown: () -> Unit,
    onEditCountdown: (CountdownItem) -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()
    val utilitiesAccent = MaterialTheme.kansoColors.utilitiesAccent

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
                Text("Date Countdowns", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                KansoTonalButton(
                    text = "Add",
                    leadingIcon = Icons.Outlined.Add,
                    size = KansoButtonSize.COMPACT,
                    onClick = onAddCountdown
                )
            }
        }

        if (countdowns.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.Event,
                    title = "No countdowns yet",
                    subtitle = "Track trips, birthdays, anniversaries, or project deadlines.",
                    actionLabel = "Add Countdown",
                    onAction = onAddCountdown
                )
            }
        } else {
            items(countdowns, key = { it.id }) { item ->
                val delta = CountdownCalculations.daysRemaining(item.targetEpochDay, todayEpoch)
                val deltaText = CountdownCalculations.formatDelta(delta)

                KansoBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEditCountdown(item) },
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = KansoDateFormatters.formatFullDate(item.targetEpochDay),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.note.isNotBlank()) {
                                Text(
                                    text = item.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        KansoPillBadge(text = deltaText, color = utilitiesAccent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WifiQrDialog(
    onDismiss: () -> Unit,
    onBuild: (String) -> Unit
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var security by remember { mutableStateOf("WPA") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Wi-Fi QR") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Network name (SSID)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = security != "nopass"
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("WPA", "WEP", "nopass").forEach { sec ->
                        FilterChip(
                            selected = security == sec,
                            onClick = { security = sec },
                            label = { Text(sec) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (ssid.isNotBlank()) {
                        onBuild(QrCodeUtils.buildWifiQrPayload(ssid, password, security))
                    }
                }
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ExchangeRatesDialog(
    currentRates: Map<String, Double>,
    onDismiss: () -> Unit,
    onSave: (Map<String, Double>) -> Unit,
    onReset: () -> Unit
) {
    val inputs = remember(currentRates) {
        mutableStateMapOf<String, String>().apply {
            AppCurrency.entries.filter { it != AppCurrency.USD }.forEach { c ->
                val value = currentRates[c.code] ?: OfflineCurrencyConverter.defaultRatesVsUsd[c.code] ?: 1.0
                put(c.code, value.toString())
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rates vs 1 USD") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppCurrency.entries.filter { it != AppCurrency.USD }.forEach { currency ->
                    OutlinedTextField(
                        value = inputs[currency.code].orEmpty(),
                        onValueChange = { inputs[currency.code] = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("1 USD in ${currency.code} (${currency.symbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = mutableMapOf<String, Double>()
                for ((code, raw) in inputs) {
                    val v = raw.trim().replace(',', '.').toDoubleOrNull()
                    if (v != null && v > 0.0) {
                        parsed[code] = v
                    }
                }
                onSave(parsed)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun CountdownEditorDialog(
    initial: CountdownItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, targetEpochDay: Long, note: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var targetEpochDay by remember {
        mutableLongStateOf(initial?.targetEpochDay ?: LocalDate.now().plusDays(14).toEpochDay())
    }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var showPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Countdown" else "Edit Countdown") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Event") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Date", style = MaterialTheme.typography.labelLarge)
                        Text(KansoDateFormatters.formatFullDate(targetEpochDay))
                    }
                    KansoOutlinedButton(
                        text = "Pick Date",
                        size = KansoButtonSize.COMPACT,
                        onClick = { showPicker = true }
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) {
                    error = "Enter an event title."
                } else {
                    onSave(title, targetEpochDay, note)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (showPicker) {
        KansoDatePickerDialog(
            initialEpochDay = targetEpochDay,
            onSelect = { targetEpochDay = it },
            onDismiss = { showPicker = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UtilitiesScreenPreview() {
    KansoTheme {
        UtilitiesScreenContent(
            uiState = UtilitiesUiState(),
            initialTab = 2,
            onBack = {},
            onCopyText = { _, _ -> },
            onSelectTimerMode = {},
            onToggleTimer = {},
            onResetTimer = {},
            onOpenRatesDialog = {},
            onAddCountdown = {},
            onEditCountdown = {}
        )
    }
}
