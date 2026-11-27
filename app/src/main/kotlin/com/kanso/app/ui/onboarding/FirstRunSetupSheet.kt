package com.kanso.app.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.OnboardingHelper
import com.kanso.core.model.StarterHabitPreset
import com.kanso.core.model.UserPreferences
import com.kanso.core.ui.components.CurrencyAmountVisualTransformation
import com.kanso.core.ui.components.KansoAccentIconBadge
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoOutlinedButton
import com.kanso.core.ui.components.KansoPrimaryButton
import com.kanso.core.ui.components.KansoTonalButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FirstRunSetupSheet(
    currentPreferences: UserPreferences,
    isReopen: Boolean,
    onSkip: () -> Unit,
    onRestoreFromDrive: () -> Unit,
    onImportBackupFile: () -> Unit,
    onComplete: (
        displayName: String,
        currency: AppCurrency,
        monthlyBudgetCents: Long,
        starterHabits: Set<StarterHabitPreset>,
        remindersEnabled: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialCurrency = remember(isReopen, currentPreferences.currency) {
        if (isReopen) currentPreferences.currency else OnboardingHelper.suggestCurrencyForLocale()
    }
    var name by rememberSaveable(isReopen) {
        mutableStateOf(if (isReopen) currentPreferences.displayName else "")
    }
    var selectedCurrency by rememberSaveable(isReopen) {
        mutableStateOf(initialCurrency)
    }
    var budgetInput by rememberSaveable(isReopen) {
        mutableStateOf(
            if (isReopen && currentPreferences.overallMonthlyBudgetCents > 0L) {
                CurrencyFormatter.formatCompactInput(
                    currentPreferences.overallMonthlyBudgetCents,
                    currentPreferences.currency
                )
            } else {
                ""
            }
        )
    }
    var selectedHabits by remember {
        mutableStateOf(
            if (isReopen) {
                emptySet()
            } else {
                setOf(StarterHabitPreset.DRINK_WATER, StarterHabitPreset.REVIEW_SPEND)
            }
        )
    }
    var remindersEnabled by rememberSaveable(isReopen) {
        val grantedOrLegacy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        mutableStateOf(currentPreferences.remindersEnabled && grantedOrLegacy)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        remindersEnabled = granted
    }

    ModalBottomSheet(
        onDismissRequest = onSkip,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KansoAccentIconBadge(
                    icon = Icons.Outlined.Bolt,
                    accent = MaterialTheme.colorScheme.primary,
                    size = 44.dp,
                    iconSize = 22.dp,
                    cornerRadius = 14.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isReopen) "Quick Start Setup" else "Welcome to Kanso",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "30-second setup • 100% offline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onSkip) {
                    Text("Skip")
                }
            }

            if (!isReopen) {
                KansoBentoCard(contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Switching devices?",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Restore your encrypted snapshot before starting fresh.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KansoTonalButton(
                                text = "Google Drive",
                                leadingIcon = Icons.Outlined.CloudDownload,
                                modifier = Modifier.weight(1f),
                                onClick = onRestoreFromDrive
                            )
                            KansoOutlinedButton(
                                text = "Backup File",
                                leadingIcon = Icons.Outlined.Restore,
                                modifier = Modifier.weight(1f),
                                onClick = onImportBackupFile
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name (optional)") },
                    placeholder = { Text("Used for daily greeting") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Primary Currency",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppCurrency.entries) { currency ->
                        val selected = selectedCurrency == currency
                        FilterChip(
                            selected = selected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCurrency = currency
                                budgetInput = CurrencyFormatter.sanitizeInput(budgetInput, currency)
                            },
                            label = { Text("${currency.symbol} ${currency.code}") },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = CurrencyFormatter.sanitizeInput(it, selectedCurrency) },
                    label = { Text("Monthly Spend Cap (optional)") },
                    placeholder = { Text("Powers Safe Today allowance") },
                    prefix = { Text("${selectedCurrency.symbol} ") },
                    trailingIcon = {
                        if (budgetInput.isNotEmpty()) {
                            IconButton(onClick = { budgetInput = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    visualTransformation = remember(selectedCurrency) {
                        CurrencyAmountVisualTransformation(selectedCurrency)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (selectedCurrency.decimalDigits == 0) {
                            KeyboardType.Number
                        } else {
                            KeyboardType.Decimal
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val presets = remember(selectedCurrency) {
                    OnboardingHelper.suggestedMonthlyCaps(selectedCurrency)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { capCents ->
                        AssistChip(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                budgetInput = CurrencyFormatter.formatCompactInput(capCents, selectedCurrency)
                            },
                            label = { Text(CurrencyFormatter.format(capCents, selectedCurrency)) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isReopen) "Add Starter Habits (optional)" else "Starter Habits (optional)",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StarterHabitPreset.entries.forEach { preset ->
                        val selected = preset in selectedHabits
                        FilterChip(
                            selected = selected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedHabits = if (selected) {
                                    selectedHabits - preset
                                } else {
                                    selectedHabits + preset
                                }
                            },
                            label = { Text(preset.habitTitle) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            KansoBentoCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Morning & Evening Briefings",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "8:00 AM & 8:30 PM offline reminders for bills, tasks & habits",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
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
                                remindersEnabled = wantEnabled
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            KansoPrimaryButton(
                text = if (isReopen) "Save Setup" else "Start Kanso",
                size = KansoButtonSize.LARGE,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val budgetCents = CurrencyFormatter.parseInputToCents(budgetInput, selectedCurrency) ?: 0L
                    onComplete(name, selectedCurrency, budgetCents, selectedHabits, remindersEnabled)
                }
            )
        }
    }
}
