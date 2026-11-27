package com.kanso.feature.money

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.BillFrequency
import com.kanso.core.model.CategoryBudget
import com.kanso.core.model.CsvExportScope
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.GoalCategory
import com.kanso.core.model.IouDirection
import com.kanso.core.model.IouEntry
import com.kanso.core.model.MoneyCalculations
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.SavingsGoal
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.SnippetItem
import com.kanso.core.model.SplitBillCalculations
import com.kanso.core.model.SplitBillMode
import com.kanso.core.model.SplitExpenseRecording
import com.kanso.core.model.SplitParticipantInput
import com.kanso.core.model.TransactionType
import com.kanso.core.model.WalletBalanceSummary
import com.kanso.core.model.WalletType
import com.kanso.core.ui.components.CurrencyAmountVisualTransformation
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoDatePickerDialog
import com.kanso.core.ui.components.KansoFormBottomSheet
import com.kanso.core.ui.components.KansoMotion
import com.kanso.core.ui.components.KansoOutlinedButton
import com.kanso.core.ui.components.KansoPillBadge
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.core.ui.components.KansoSmallIconButton
import com.kanso.core.ui.components.KansoTonalButton
import com.kanso.core.ui.components.KansoTransactionDateSelector
import com.kanso.core.ui.components.accentColor
import com.kanso.core.ui.components.expenseSemanticColor
import com.kanso.core.ui.components.icon
import com.kanso.core.ui.components.incomeSemanticColor
import com.kanso.core.ui.theme.kansoColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
private fun TransactionType.accentColor(): Color = when (this) {
    TransactionType.EXPENSE -> expenseSemanticColor()
    TransactionType.INCOME -> incomeSemanticColor()
    TransactionType.TRANSFER -> MaterialTheme.kansoColors.utilitiesAccent
}

private fun TransactionType.icon(): ImageVector = when (this) {
    TransactionType.EXPENSE -> Icons.Outlined.ArrowUpward
    TransactionType.INCOME -> Icons.Outlined.ArrowDownward
    TransactionType.TRANSFER -> Icons.Outlined.SwapHoriz
}

@Composable
fun TransactionEditorDialog(
    currency: AppCurrency,
    initial: MoneyTransaction?,
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onSave: (
        type: TransactionType,
        amountCents: Long,
        category: ExpenseCategory,
        wallet: WalletType,
        toWallet: WalletType?,
        feeCents: Long,
        note: String,
        timestampMillis: Long
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var type by remember { mutableStateOf(initial?.type ?: initialType) }
    var amountText by remember {
        mutableStateOf(
            initial?.let { CurrencyFormatter.formatCompactInput(it.amountCents, currency) }.orEmpty()
        )
    }
    var category by remember { mutableStateOf(initial?.category ?: ExpenseCategory.FOOD) }
    var wallet by remember {
        mutableStateOf(
            initial?.wallet ?: if (initialType == TransactionType.TRANSFER) WalletType.BANK else WalletType.EWALLET
        )
    }
    var toWallet by remember {
        mutableStateOf(
            initial?.toWallet ?: if (wallet == WalletType.BANK) WalletType.EWALLET else WalletType.BANK
        )
    }
    var feeText by remember { mutableStateOf("") }
    var selectedEpochDay by remember {
        mutableLongStateOf(
            initial?.let {
                Instant.ofEpochMilli(it.timestampMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toEpochDay()
            } ?: LocalDate.now().toEpochDay()
        )
    }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    val activeAccent by animateColorAsState(
        targetValue = type.accentColor(),
        animationSpec = tween(220),
        label = "txSheetAccent"
    )
    val sheetTitle = when {
        initial != null -> "Edit ${type.label}"
        type == TransactionType.TRANSFER -> "Transfer"
        else -> "New Transaction"
    }

    MoneySheetScaffold(
        title = sheetTitle,
        badgeText = type.label,
        badgeColor = activeAccent,
        badgeIcon = type.icon(),
        primaryLabel = if (initial == null) "Save ${type.label}" else "Update ${type.label}",
        primaryColor = activeAccent,
        errorText = error,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onPrimaryClick = {
            val cents = CurrencyFormatter.parseInputToCents(amountText, currency)
            val parsedFee = if (type == TransactionType.TRANSFER && initial == null && feeText.isNotBlank()) {
                CurrencyFormatter.parseInputToCents(feeText, currency)
            } else {
                0L
            }
            when {
                cents == null || cents <= 0L -> error = "Enter an amount greater than 0."
                type == TransactionType.TRANSFER && wallet == toWallet ->
                    error = "Choose two different wallets."
                parsedFee == null -> error = "Enter a valid transfer fee."
                else -> {
                    val resolvedTimestampMillis = MoneyCalculations.resolveTransactionTimestampMillis(
                        selectedEpochDay = selectedEpochDay,
                        existingTimestampMillis = initial?.timestampMillis,
                        nowMillis = System.currentTimeMillis()
                    )
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(
                        type,
                        cents,
                        if (type == TransactionType.TRANSFER) ExpenseCategory.OTHER else category,
                        wallet,
                        if (type == TransactionType.TRANSFER) toWallet else null,
                        parsedFee,
                        note,
                        resolvedTimestampMillis
                    )
                }
            }
        }
    ) {
        KansoSegmentedRow(
            options = TransactionType.entries.map { it.label },
            selectedIndex = TransactionType.entries.indexOf(type),
            onSelect = { idx ->
                val nextType = TransactionType.entries[idx]
                type = nextType
                error = null
                if (nextType == TransactionType.INCOME && !category.isIncomeDefault) {
                    category = ExpenseCategory.SALARY
                } else if (nextType == TransactionType.EXPENSE && category.isIncomeDefault) {
                    category = ExpenseCategory.FOOD
                } else if (nextType == TransactionType.TRANSFER && toWallet == wallet) {
                    toWallet = WalletType.entries.first { it != wallet }
                }
            }
        )

        MoneyAmountHeroInput(
            label = "${type.label.uppercase()} AMOUNT",
            value = amountText,
            onValueChange = {
                amountText = it
                error = null
            },
            currency = currency,
            accentColor = activeAccent,
            presets = currency.quickPresets
        )

        AnimatedContent(
            targetState = type,
            transitionSpec = {
                KansoMotion.directionalTabTransition(targetState.ordinal, initialState.ordinal)
            },
            label = "txEditorModeContent"
        ) { activeType ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (activeType == TransactionType.TRANSFER) {
                    Text("From Wallet", style = MaterialTheme.typography.labelLarge)
                    WalletTileRow(
                        selected = wallet,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        onSelect = { chosen ->
                            if (chosen == toWallet) {
                                toWallet = wallet
                            }
                            wallet = chosen
                            error = null
                        }
                    )

                    Text("To Wallet", style = MaterialTheme.typography.labelLarge)
                    WalletTileRow(
                        selected = toWallet,
                        selectedColor = activeAccent,
                        onSelect = { chosen ->
                            if (chosen == wallet) {
                                wallet = toWallet
                            }
                            toWallet = chosen
                            error = null
                        }
                    )

                    if (initial == null) {
                        val feeVisualTransformation = remember(currency) { CurrencyAmountVisualTransformation(currency) }
                        OutlinedTextField(
                            value = feeText,
                            onValueChange = {
                                feeText = CurrencyFormatter.sanitizeInput(it, currency)
                                error = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Admin / top-up fee (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = feeVisualTransformation,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (currency.decimalDigits == 0) {
                                    KeyboardType.Number
                                } else {
                                    KeyboardType.Decimal
                                }
                            )
                        )
                    }
                } else {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    val categories = if (activeType == TransactionType.INCOME) {
                        ExpenseCategory.incomeCategories
                    } else {
                        ExpenseCategory.expenseCategories
                    }
                    CategoryChipRow(
                        categories = categories,
                        selected = category,
                        onSelect = { category = it }
                    )

                    Text("Wallet", style = MaterialTheme.typography.labelLarge)
                    WalletTileRow(
                        selected = wallet,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        onSelect = { wallet = it }
                    )
                }
            }
        }

        KansoTransactionDateSelector(
            selectedEpochDay = selectedEpochDay,
            onSelectEpochDay = { selectedEpochDay = it },
            accentColor = activeAccent
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
    }
}

@Composable
fun WalletBalanceDialog(
    walletSummary: WalletBalanceSummary,
    currency: AppCurrency,
    onDismiss: () -> Unit,
    onSaveTargetBalance: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var text by remember(walletSummary.wallet) {
        mutableStateOf(
            if (walletSummary.currentBalanceCents > 0L) {
                CurrencyFormatter.formatCompactInput(walletSummary.currentBalanceCents, currency)
            } else {
                ""
            }
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    val accent = MaterialTheme.colorScheme.primary

    MoneySheetScaffold(
        title = "${walletSummary.wallet.label} Balance",
        badgeText = walletSummary.wallet.label,
        badgeColor = accent,
        badgeIcon = walletSummary.wallet.icon(),
        primaryLabel = "Save Balance",
        primaryColor = accent,
        errorText = error,
        onDismiss = onDismiss,
        onPrimaryClick = {
            if (text.isBlank()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSaveTargetBalance(0L)
            } else {
                val cents = CurrencyFormatter.parseInputToCents(text, currency)
                if (cents == null) {
                    error = "Invalid amount."
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSaveTargetBalance(cents)
                }
            }
        }
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Tracked net flow: ${
                        CurrencyFormatter.format(walletSummary.netDeltaCents, currency, showSign = true)
                    }",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enter your real current balance to reconcile this wallet automatically.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        MoneyAmountHeroInput(
            label = "ACTUAL CURRENT BALANCE",
            value = text,
            onValueChange = {
                text = it
                error = null
            },
            currency = currency,
            accentColor = accent,
            presets = currency.quickPresets.map { it * 10L },
            zeroChipLabel = "Reset to 0"
        )
    }
}

@Composable
fun OverallBudgetDialog(
    currency: AppCurrency,
    currentLimitCents: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var text by remember {
        mutableStateOf(
            if (currentLimitCents > 0L) CurrencyFormatter.formatCompactInput(currentLimitCents, currency) else ""
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    val moneyAccent = MaterialTheme.kansoColors.moneyAccent

    MoneySheetScaffold(
        title = "Monthly Budget",
        badgeText = if (currentLimitCents > 0L) "Active" else "Optional",
        badgeColor = moneyAccent,
        badgeIcon = Icons.Outlined.AccountBalanceWallet,
        primaryLabel = "Save Monthly Budget",
        primaryColor = moneyAccent,
        errorText = error,
        onDismiss = onDismiss,
        onPrimaryClick = {
            if (text.isBlank()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSave(0L)
            } else {
                val cents = CurrencyFormatter.parseInputToCents(text, currency)
                if (cents == null) {
                    error = "Invalid amount."
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(cents)
                }
            }
        }
    ) {
        MoneyAmountHeroInput(
            label = "MONTHLY SPENDING CAP",
            value = text,
            onValueChange = {
                text = it
                error = null
            },
            currency = currency,
            accentColor = moneyAccent,
            presets = currency.quickPresets.map { it * 50L },
            zeroChipLabel = "No Limit (0)"
        )

        Text(
            text = "Set 0 or leave blank to disable your overall monthly cap.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryBudgetDialog(
    currency: AppCurrency,
    initial: CategoryBudget?,
    onDismiss: () -> Unit,
    onSave: (ExpenseCategory, Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var category by remember { mutableStateOf(initial?.category ?: ExpenseCategory.FOOD) }
    var amountText by remember {
        mutableStateOf(initial?.let { CurrencyFormatter.formatCompactInput(it.limitCents, currency) }.orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }
    val catAccent = category.accentColor()

    MoneySheetScaffold(
        title = if (initial == null) "Category Budget" else "Edit ${category.label} Cap",
        badgeText = category.label,
        badgeColor = catAccent,
        badgeIcon = category.icon(),
        primaryLabel = "Save Category Cap",
        primaryColor = catAccent,
        errorText = error,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onPrimaryClick = {
            val cents = CurrencyFormatter.parseInputToCents(amountText, currency)
            if (cents == null || cents <= 0L) {
                error = "Enter a valid limit greater than 0."
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSave(category, cents)
            }
        }
    ) {
        Text("Category", style = MaterialTheme.typography.labelLarge)
        CategoryChipRow(
            categories = ExpenseCategory.expenseCategories,
            selected = category,
            enabled = initial == null,
            onSelect = { category = it }
        )

        MoneyAmountHeroInput(
            label = "MONTHLY CATEGORY CAP",
            value = amountText,
            onValueChange = {
                amountText = it
                error = null
            },
            currency = currency,
            accentColor = catAccent,
            presets = currency.quickPresets.map { it * 15L }
        )
    }
}

@Composable
fun BillEditorDialog(
    currency: AppCurrency,
    initial: RecurringBill?,
    onDismiss: () -> Unit,
    onSave: (String, Long, ExpenseCategory, WalletType, Int, BillFrequency, Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var amountText by remember {
        mutableStateOf(initial?.let { CurrencyFormatter.formatCompactInput(it.amountCents, currency) }.orEmpty())
    }
    var dueDayText by remember { mutableStateOf((initial?.dueDayOfMonth ?: 25).toString()) }
    var category by remember { mutableStateOf(initial?.category ?: ExpenseCategory.BILLS) }
    var wallet by remember { mutableStateOf(initial?.wallet ?: WalletType.BANK) }
    var frequency by remember { mutableStateOf(initial?.frequency ?: BillFrequency.MONTHLY) }
    var isActive by remember { mutableStateOf(initial?.isActive ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    val billAccent = category.accentColor()
    val billCategories = remember {
        listOf(
            ExpenseCategory.BILLS,
            ExpenseCategory.HOUSING,
            ExpenseCategory.ENTERTAINMENT,
            ExpenseCategory.HEALTH,
            ExpenseCategory.OTHER
        )
    }

    MoneySheetScaffold(
        title = if (initial == null) "New Recurring Bill" else "Edit Bill",
        badgeText = frequency.label,
        badgeColor = billAccent,
        badgeIcon = category.icon(),
        primaryLabel = "Save Bill",
        primaryColor = billAccent,
        errorText = error,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onPrimaryClick = {
            val cents = CurrencyFormatter.parseInputToCents(amountText, currency)
            val day = dueDayText.toIntOrNull()
            when {
                name.isBlank() -> error = "Enter a bill name."
                cents == null || cents <= 0L -> error = "Enter a valid amount."
                day == null || day !in 1..31 -> error = "Due day must be 1–31."
                else -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(name, cents, category, wallet, day, frequency, isActive)
                }
            }
        }
    ) {
        MoneyAmountHeroInput(
            label = "BILL AMOUNT",
            value = amountText,
            onValueChange = {
                amountText = it
                error = null
            },
            currency = currency,
            accentColor = billAccent,
            presets = currency.quickPresets.map { it * 10L }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                modifier = Modifier.weight(1.6f),
                label = { Text("Bill name") },
                placeholder = { Text("Internet, Rent") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            OutlinedTextField(
                value = dueDayText,
                onValueChange = {
                    dueDayText = it.filter { ch -> ch.isDigit() }.take(2)
                    error = null
                },
                modifier = Modifier.weight(0.8f),
                label = { Text("Due day") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Text("Frequency", style = MaterialTheme.typography.labelLarge)
        KansoSegmentedRow(
            options = BillFrequency.entries.map { it.label },
            selectedIndex = BillFrequency.entries.indexOf(frequency),
            onSelect = { idx -> frequency = BillFrequency.entries[idx] }
        )

        Text("Category", style = MaterialTheme.typography.labelLarge)
        CategoryChipRow(
            categories = billCategories,
            selected = category,
            onSelect = { category = it }
        )

        Text("Pay From Wallet", style = MaterialTheme.typography.labelLarge)
        WalletTileRow(
            selected = wallet,
            selectedColor = MaterialTheme.colorScheme.primary,
            onSelect = { wallet = it }
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Active reminder", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }
        }
    }
}

@Composable
fun IouEditorDialog(
    currency: AppCurrency,
    initial: IouEntry?,
    onDismiss: () -> Unit,
    onSave: (String, Long, IouDirection, String, Long?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var personName by remember { mutableStateOf(initial?.personName.orEmpty()) }
    var amountText by remember {
        mutableStateOf(initial?.let { CurrencyFormatter.formatCompactInput(it.amountCents, currency) }.orEmpty())
    }
    var direction by remember { mutableStateOf(initial?.direction ?: IouDirection.OWES_ME) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var dueOption by remember { mutableIntStateOf(if (initial?.dueDateEpochDay != null) 1 else 0) }
    var error by remember { mutableStateOf<String?>(null) }

    val directionColor = if (direction == IouDirection.OWES_ME) {
        incomeSemanticColor()
    } else {
        expenseSemanticColor()
    }

    MoneySheetScaffold(
        title = if (initial == null) "New IOU" else "Edit IOU",
        badgeText = direction.label,
        badgeColor = directionColor,
        badgeIcon = Icons.Outlined.Handshake,
        primaryLabel = "Save IOU",
        primaryColor = directionColor,
        errorText = error,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onPrimaryClick = {
            val cents = CurrencyFormatter.parseInputToCents(amountText, currency)
            when {
                personName.isBlank() -> error = "Enter a name."
                cents == null || cents <= 0L -> error = "Enter a valid amount."
                else -> {
                    val today = LocalDate.now()
                    val dueDay = when (dueOption) {
                        1 -> today.plusDays(7).toEpochDay()
                        2 -> today.plusDays(30).toEpochDay()
                        else -> initial?.dueDateEpochDay
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(personName, cents, direction, note, dueDay)
                }
            }
        }
    ) {
        KansoSegmentedRow(
            options = IouDirection.entries.map { it.label },
            selectedIndex = IouDirection.entries.indexOf(direction),
            onSelect = { idx -> direction = IouDirection.entries[idx] }
        )

        MoneyAmountHeroInput(
            label = if (direction == IouDirection.OWES_ME) "OWED TO YOU" else "YOU OWE",
            value = amountText,
            onValueChange = {
                amountText = it
                error = null
            },
            currency = currency,
            accentColor = directionColor,
            presets = currency.quickPresets
        )

        OutlinedTextField(
            value = personName,
            onValueChange = {
                personName = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Person") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Reason / note (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Text("Target Date", style = MaterialTheme.typography.labelLarge)
        KansoSegmentedRow(
            options = listOf("None", "In 7d", "In 30d"),
            selectedIndex = dueOption,
            onSelect = { dueOption = it }
        )
    }
}

@Composable
fun SettleIouDialog(
    iou: IouEntry,
    currency: AppCurrency,
    onDismiss: () -> Unit,
    onConfirm: (recordTransaction: Boolean, wallet: WalletType) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var recordTx by remember { mutableStateOf(true) }
    var wallet by remember { mutableStateOf(WalletType.BANK) }

    val directionColor = if (iou.direction == IouDirection.OWES_ME) {
        incomeSemanticColor()
    } else {
        expenseSemanticColor()
    }
    val actionDesc = if (iou.direction == IouDirection.OWES_ME) {
        "${iou.personName} paid you back"
    } else {
        "You paid ${iou.personName}"
    }

    MoneySheetScaffold(
        title = "Settle IOU",
        badgeText = iou.direction.label,
        badgeColor = directionColor,
        badgeIcon = Icons.Outlined.Handshake,
        primaryLabel = "Mark Settled",
        primaryColor = directionColor,
        errorText = null,
        onDismiss = onDismiss,
        onPrimaryClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onConfirm(recordTx, wallet)
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = directionColor.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, directionColor.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = actionDesc,
                    style = MaterialTheme.typography.labelLarge,
                    color = directionColor
                )
                Text(
                    text = CurrencyFormatter.format(iou.amountCents, currency),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (iou.note.isNotBlank()) {
                    Text(
                        text = iou.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (iou.direction == IouDirection.OWES_ME) {
                        "Also log as Income transaction"
                    } else {
                        "Also log as Expense transaction"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = recordTx, onCheckedChange = { recordTx = it })
            }
        }

        AnimatedVisibility(
            visible = recordTx,
            enter = expandVertically(KansoMotion.SizeSpring) + fadeIn(tween(180)),
            exit = shrinkVertically() + fadeOut(tween(140))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wallet", style = MaterialTheme.typography.labelLarge)
                WalletTileRow(
                    selected = wallet,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onSelect = { wallet = it }
                )
            }
        }
    }
}

@Composable
private fun MoneySheetScaffold(
    title: String,
    badgeText: String,
    badgeColor: Color,
    badgeIcon: ImageVector,
    primaryLabel: String,
    primaryColor: Color,
    errorText: String?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onPrimaryClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    KansoFormBottomSheet(
        title = title,
        onDismiss = onDismiss,
        primaryLabel = primaryLabel,
        onPrimaryClick = onPrimaryClick,
        primaryColor = primaryColor,
        primaryContentColor = Color.White,
        badgeText = badgeText,
        badgeColor = badgeColor,
        badgeIcon = badgeIcon,
        secondaryLabel = if (onDelete != null) "Delete" else null,
        onSecondaryClick = onDelete,
        secondaryIsDestructive = true,
        errorText = errorText,
        content = content
    )
}

@Composable
private fun MoneyAmountHeroInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    currency: AppCurrency,
    accentColor: Color,
    presets: List<Long> = emptyList(),
    zeroChipLabel: String? = null
) {
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val placeholder = if (currency.decimalDigits == 0) "0" else "0.00"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { focusRequester.requestFocus() },
        shape = RoundedCornerShape(20.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currency.symbol,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = accentColor,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }
                    val amountVisualTransformation = remember(currency) {
                        CurrencyAmountVisualTransformation(currency)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = { raw ->
                            onValueChange(CurrencyFormatter.sanitizeInput(raw, currency))
                        },
                        modifier = Modifier.focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        ),
                        cursorBrush = SolidColor(accentColor),
                        visualTransformation = amountVisualTransformation,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (currency.decimalDigits == 0) {
                                KeyboardType.Number
                            } else {
                                KeyboardType.Decimal
                            }
                        )
                    )
                }
            }
        }
    }

    if (presets.isNotEmpty() || zeroChipLabel != null) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (zeroChipLabel != null) {
                item(key = "zero_preset") {
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onValueChange("0")
                        },
                        label = { Text(zeroChipLabel) },
                        shape = RoundedCornerShape(50)
                    )
                }
            }
            items(presets, key = { it }) { presetCents ->
                AssistChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onValueChange(CurrencyFormatter.formatCompactInput(presetCents, currency))
                    },
                    label = { Text(CurrencyFormatter.format(presetCents, currency)) },
                    shape = RoundedCornerShape(50)
                )
            }
        }
    }
}

@Composable
private fun WalletTileRow(
    selected: WalletType,
    selectedColor: Color,
    onSelect: (WalletType) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WalletType.entries.forEach { wallet ->
            val isSelected = selected == wallet
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(wallet)
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) {
                    selectedColor.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected) selectedColor else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = wallet.icon(),
                        contentDescription = null,
                        tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = wallet.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChipRow(
    categories: List<ExpenseCategory>,
    selected: ExpenseCategory,
    onSelect: (ExpenseCategory) -> Unit,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories, key = { it.name }) { category ->
            val isSelected = selected == category
            val catAccent = category.accentColor()
            val clickableModifier = if (enabled) {
                Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(category)
                }
            } else {
                Modifier
            }
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .then(clickableModifier),
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) {
                    catAccent.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.5f else 0.25f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected) catAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 1f else 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = category.icon(),
                        contentDescription = null,
                        tint = when {
                            isSelected -> catAccent
                            enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                color = when {
                            isSelected -> catAccent
                            enabled -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsGoalEditorDialog(
    currency: AppCurrency,
    initial: SavingsGoal?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: GoalCategory,
        targetAmountCents: Long,
        savedAmountCents: Long,
        linkedWallet: WalletType?,
        targetEpochDay: Long?,
        note: String,
        isArchived: Boolean
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: GoalCategory.EMERGENCY) }
    var targetText by remember {
        mutableStateOf(initial?.let { CurrencyFormatter.formatCompactInput(it.targetAmountCents, currency) }.orEmpty())
    }
    var savedText by remember {
        mutableStateOf(
            initial?.takeIf { it.savedAmountCents > 0L }
                ?.let { CurrencyFormatter.formatCompactInput(it.savedAmountCents, currency) }
                .orEmpty()
        )
    }
    var linkedWallet by remember { mutableStateOf<WalletType?>(initial?.linkedWallet ?: WalletType.BANK) }
    var targetEpochDay by remember { mutableStateOf<Long?>(initial?.targetEpochDay) }
    var showDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var isArchived by remember { mutableStateOf(initial?.isArchived ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    val goalAccent = category.accentColor()

    MoneySheetScaffold(
        title = if (initial == null) "New Savings Goal" else "Edit Goal",
        badgeText = category.label,
        badgeColor = goalAccent,
        badgeIcon = category.icon(),
        primaryLabel = if (initial == null) "Create Goal" else "Save Goal",
        primaryColor = goalAccent,
        errorText = error,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onPrimaryClick = {
            val targetCents = CurrencyFormatter.parseInputToCents(targetText, currency)
            val savedCents = if (savedText.isBlank()) 0L else CurrencyFormatter.parseInputToCents(savedText, currency)
            when {
                name.isBlank() -> error = "Enter a goal name."
                targetCents == null || targetCents <= 0L -> error = "Enter a target amount greater than 0."
                savedCents == null -> error = "Enter a valid saved amount."
                else -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(name, category, targetCents, savedCents, linkedWallet, targetEpochDay, note, isArchived)
                }
            }
        }
    ) {
        MoneyAmountHeroInput(
            label = "TARGET AMOUNT",
            value = targetText,
            onValueChange = {
                targetText = it
                error = null
            },
            currency = currency,
            accentColor = goalAccent,
            presets = currency.quickPresets.map { it * 20L }
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Goal name") },
            placeholder = { Text("Emergency Fund, Japan Trip...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        val savedVisualTransformation = remember(currency) { CurrencyAmountVisualTransformation(currency) }
        OutlinedTextField(
            value = savedText,
            onValueChange = {
                savedText = CurrencyFormatter.sanitizeInput(it, currency)
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (initial == null) "Starting saved amount (optional)" else "Current saved amount") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            visualTransformation = savedVisualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (currency.decimalDigits == 0) KeyboardType.Number else KeyboardType.Decimal
            )
        )

        Text("Category", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GoalCategory.entries, key = { it.name }) { item ->
                val isSelected = category == item
                val accent = item.accentColor()
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            category = item
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = item.icon(),
                            contentDescription = null,
                            tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reserve in Wallet", style = MaterialTheme.typography.labelLarge)
            FilterChip(
                selected = linkedWallet == null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    linkedWallet = if (linkedWallet == null) WalletType.BANK else null
                },
                label = { Text("Track separately") }
            )
        }

        AnimatedVisibility(
            visible = linkedWallet != null,
            enter = expandVertically(KansoMotion.SizeSpring) + fadeIn(tween(180)),
            exit = shrinkVertically() + fadeOut(tween(140))
        ) {
            WalletTileRow(
                selected = linkedWallet ?: WalletType.BANK,
                selectedColor = goalAccent,
                onSelect = { linkedWallet = it }
            )
        }

        Text("Target Date", style = MaterialTheme.typography.labelLarge)
        val today = remember { LocalDate.now() }
        val in3Months = remember { today.plusMonths(3).toEpochDay() }
        val in6Months = remember { today.plusMonths(6).toEpochDay() }
        val in1Year = remember { today.plusYears(1).toEpochDay() }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "goal_date_none") {
                FilterChip(
                    selected = targetEpochDay == null,
                    onClick = { targetEpochDay = null },
                    label = { Text("Flexible") }
                )
            }
            item(key = "goal_date_3m") {
                FilterChip(
                    selected = targetEpochDay == in3Months,
                    onClick = { targetEpochDay = in3Months },
                    label = { Text("3 Months") }
                )
            }
            item(key = "goal_date_6m") {
                FilterChip(
                    selected = targetEpochDay == in6Months,
                    onClick = { targetEpochDay = in6Months },
                    label = { Text("6 Months") }
                )
            }
            item(key = "goal_date_1y") {
                FilterChip(
                    selected = targetEpochDay == in1Year,
                    onClick = { targetEpochDay = in1Year },
                    label = { Text("1 Year") }
                )
            }
            item(key = "goal_date_pick") {
                val customSelected = targetEpochDay != null &&
                    targetEpochDay != in3Months &&
                    targetEpochDay != in6Months &&
                    targetEpochDay != in1Year
                FilterChip(
                    selected = customSelected,
                    onClick = { showDatePicker = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.EventAvailable,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            if (customSelected && targetEpochDay != null) {
                                KansoDateFormatters.formatFullDate(targetEpochDay!!)
                            } else {
                                "Pick Date"
                            }
                        )
                    }
                )
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        if (initial != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Archive goal", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isArchived, onCheckedChange = { isArchived = it })
                }
            }
        }
    }

    if (showDatePicker) {
        KansoDatePickerDialog(
            initialEpochDay = targetEpochDay ?: LocalDate.now().plusMonths(3).toEpochDay(),
            onSelect = { targetEpochDay = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun GoalAllocationDialog(
    goalStatus: MoneyCalculations.GoalStatus,
    initialWithdraw: Boolean,
    currency: AppCurrency,
    onDismiss: () -> Unit,
    onConfirm: (
        deltaCents: Long,
        recordExpense: Boolean,
        expenseCategory: ExpenseCategory,
        expenseWallet: WalletType,
        note: String
    ) -> Unit
) {
    val goal = goalStatus.goal
    val haptic = LocalHapticFeedback.current
    var isWithdraw by remember { mutableStateOf(initialWithdraw) }
    var amountText by remember { mutableStateOf("") }
    var recordExpense by remember { mutableStateOf(false) }
    var expenseCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var expenseWallet by remember { mutableStateOf(goal.linkedWallet ?: WalletType.BANK) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val activeAccent = if (isWithdraw) expenseSemanticColor() else goal.category.accentColor()
    val parsedCents = CurrencyFormatter.parseInputToCents(amountText, currency) ?: 0L
    val previewSavedCents = if (isWithdraw) {
        (goal.savedAmountCents - parsedCents).coerceAtLeast(0L)
    } else {
        goal.savedAmountCents + parsedCents
    }
    val previewPct = if (goal.targetAmountCents > 0L) {
        ((previewSavedCents.toFloat() / goal.targetAmountCents.toFloat()) * 100f).toInt().coerceIn(0, 100)
    } else {
        0
    }

    MoneySheetScaffold(
        title = goal.name,
        badgeText = "$previewPct% funded",
        badgeColor = activeAccent,
        badgeIcon = Icons.Outlined.Savings,
        primaryLabel = if (isWithdraw) "Withdraw from Goal" else "Allocate to Goal",
        primaryColor = activeAccent,
        errorText = error,
        onDismiss = onDismiss,
        onPrimaryClick = {
            val cents = CurrencyFormatter.parseInputToCents(amountText, currency)
            when {
                cents == null || cents <= 0L -> error = "Enter an amount greater than 0."
                isWithdraw && cents > goal.savedAmountCents ->
                    error = "Cannot withdraw more than saved (${CurrencyFormatter.format(goal.savedAmountCents, currency)})."
                else -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm(
                        if (isWithdraw) -cents else cents,
                        isWithdraw && recordExpense,
                        expenseCategory,
                        expenseWallet,
                        note
                    )
                }
            }
        }
    ) {
        KansoSegmentedRow(
            options = listOf("Allocate", "Withdraw"),
            selectedIndex = if (isWithdraw) 1 else 0,
            onSelect = { idx ->
                isWithdraw = idx == 1
                error = null
            }
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Saved ${CurrencyFormatter.format(goal.savedAmountCents, currency)} of ${
                        CurrencyFormatter.format(goal.targetAmountCents, currency)
                    }",
                    style = MaterialTheme.typography.titleSmall
                )
                if (parsedCents > 0L) {
                    Text(
                        text = "After update: ${CurrencyFormatter.format(previewSavedCents, currency)} ($previewPct%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = activeAccent
                    )
                }
            }
        }

        val quickPresets = remember(currency, isWithdraw, goalStatus.remainingCents, goal.savedAmountCents) {
            val extra = if (isWithdraw) goal.savedAmountCents else goalStatus.remainingCents
            buildList {
                if (extra > 0L) add(extra)
                currency.quickPresets.forEach { add(it) }
            }.distinct()
        }

        MoneyAmountHeroInput(
            label = if (isWithdraw) "WITHDRAW AMOUNT" else "ALLOCATE AMOUNT",
            value = amountText,
            onValueChange = {
                amountText = it
                error = null
            },
            currency = currency,
            accentColor = activeAccent,
            presets = quickPresets
        )

        AnimatedVisibility(
            visible = isWithdraw,
            enter = expandVertically(KansoMotion.SizeSpring) + fadeIn(tween(180)),
            exit = shrinkVertically() + fadeOut(tween(140))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Also log as Expense transaction",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = recordExpense, onCheckedChange = { recordExpense = it })
                    }
                }

                AnimatedVisibility(
                    visible = recordExpense,
                    enter = expandVertically(KansoMotion.SizeSpring) + fadeIn(tween(180)),
                    exit = shrinkVertically() + fadeOut(tween(140))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Expense Category", style = MaterialTheme.typography.labelLarge)
                        CategoryChipRow(
                            categories = ExpenseCategory.expenseCategories,
                            selected = expenseCategory,
                            onSelect = { expenseCategory = it }
                        )

                        Text("Pay From Wallet", style = MaterialTheme.typography.labelLarge)
                        WalletTileRow(
                            selected = expenseWallet,
                            selectedColor = activeAccent,
                            onSelect = { expenseWallet = it }
                        )

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Note (optional)") },
                            placeholder = { Text("Goal spent: ${goal.name}") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CsvExportDialog(
    selectedMonthLabel: String,
    monthCount: Int,
    allCount: Int,
    hasActiveFilter: Boolean,
    onDismiss: () -> Unit,
    onShareCsv: (CsvExportScope) -> Unit,
    onSaveCsvToDevice: (CsvExportScope) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var scope by remember {
        mutableStateOf(
            if (monthCount > 0) CsvExportScope.SELECTED_MONTH else CsvExportScope.ALL_HISTORY
        )
    }
    val chosenCount = if (scope == CsvExportScope.SELECTED_MONTH) monthCount else allCount
    val moneyAccent = MaterialTheme.kansoColors.moneyAccent

    KansoFormBottomSheet(
        title = "Export Transactions CSV",
        onDismiss = onDismiss,
        primaryLabel = "Share CSV",
        onPrimaryClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onShareCsv(scope)
        },
        primaryColor = moneyAccent,
        primaryEnabled = chosenCount > 0,
        badgeText = "$chosenCount rows",
        badgeColor = moneyAccent,
        badgeIcon = Icons.Outlined.Share,
        secondaryLabel = "Save File",
        onSecondaryClick = {
            if (chosenCount > 0) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSaveCsvToDevice(scope)
            }
        },
        secondaryIsDestructive = false
    ) {
        KansoSegmentedRow(
            options = listOf("$selectedMonthLabel ($monthCount)", "All History ($allCount)"),
            selectedIndex = if (scope == CsvExportScope.SELECTED_MONTH) 0 else 1,
            onSelect = { idx ->
                scope = if (idx == 0) CsvExportScope.SELECTED_MONTH else CsvExportScope.ALL_HISTORY
            }
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = null,
                    tint = moneyAccent,
                    modifier = Modifier.size(20.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (chosenCount == 0) {
                            "No transactions in the selected scope."
                        } else {
                            "UTF-8 CSV formatted for Excel, Google Sheets, and accounting apps."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (scope == CsvExportScope.SELECTED_MONTH && hasActiveFilter) {
                        Text(
                            text = "Includes only transactions matching your active filters.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private data class SplitFriendDraft(
    val id: Long,
    val name: String = "",
    val itemAmountText: String = ""
)

private fun sanitizePercentInput(raw: String): String {
    val sb = StringBuilder()
    var hasDot = false
    for (ch in raw) {
        when {
            ch.isDigit() -> sb.append(ch)
            (ch == '.' || ch == ',') && !hasDot -> {
                if (sb.isEmpty()) sb.append('0')
                sb.append('.')
                hasDot = true
            }
        }
    }
    return sb.toString().take(5)
}

@Composable
fun SplitBillDialog(
    currency: AppCurrency,
    recentPeople: List<String>,
    paymentSnippets: List<SnippetItem>,
    onDismiss: () -> Unit,
    onCopySummary: (String) -> Unit,
    onShareSummary: (String) -> Unit,
    onSave: (
        title: String,
        expenseAmountCents: Long,
        category: ExpenseCategory,
        wallet: WalletType,
        friendShares: List<Pair<String, Long>>,
        dueDateEpochDay: Long?,
        timestampMillis: Long
    ) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val moneyAccent = MaterialTheme.kansoColors.moneyAccent

    var mode by remember { mutableStateOf(SplitBillMode.EQUAL) }
    var recordingMode by remember { mutableStateOf(SplitExpenseRecording.FULL_BILL) }
    var title by remember { mutableStateOf("") }
    var subtotalText by remember { mutableStateOf("") }
    var serviceText by remember { mutableStateOf("") }
    var taxText by remember { mutableStateOf("") }
    var discountText by remember { mutableStateOf("") }
    var payerItemText by remember { mutableStateOf("") }
    var nextFriendId by remember { mutableLongStateOf(2L) }
    val friends = remember { mutableStateListOf(SplitFriendDraft(id = 1L)) }

    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var selectedWallet by remember { mutableStateOf(WalletType.EWALLET) }
    var selectedEpochDay by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var dueDateEpochDay by remember { mutableStateOf<Long?>(null) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var selectedSnippetId by remember(paymentSnippets) {
        mutableStateOf(
            paymentSnippets.firstOrNull {
                it.category == SnippetCategory.FINANCE && it.isPinned && !it.isSensitive
            }?.id
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    val amountVisualTransformation = remember(currency) { CurrencyAmountVisualTransformation(currency) }
    val numberKeyboard = remember(currency) {
        KeyboardOptions(
            keyboardType = if (currency.decimalDigits == 0) KeyboardType.Number else KeyboardType.Decimal
        )
    }

    val manualSubtotalCents = CurrencyFormatter.parseInputToCents(subtotalText, currency) ?: 0L
    val servicePct = serviceText.toDoubleOrNull() ?: 0.0
    val taxPct = taxText.toDoubleOrNull() ?: 0.0
    val discountCents = CurrencyFormatter.parseInputToCents(discountText, currency) ?: 0L
    val payerItemCents = CurrencyFormatter.parseInputToCents(payerItemText, currency) ?: 0L

    val participantInputs = remember(mode, payerItemCents, friends.toList(), currency) {
        buildList {
            add(SplitParticipantInput(id = 0L, name = "Me", isPayer = true, itemAmountCents = payerItemCents))
            friends.forEachIndexed { idx, draft ->
                val itemCents = CurrencyFormatter.parseInputToCents(draft.itemAmountText, currency) ?: 0L
                add(
                    SplitParticipantInput(
                        id = draft.id,
                        name = draft.name.trim().ifBlank { "Friend ${idx + 1}" },
                        isPayer = false,
                        itemAmountCents = itemCents
                    )
                )
            }
        }
    }

    val splitResult = remember(
        mode,
        manualSubtotalCents,
        servicePct,
        taxPct,
        discountCents,
        participantInputs,
        currency
    ) {
        SplitBillCalculations.calculate(
            mode = mode,
            manualSubtotalCents = manualSubtotalCents,
            servicePercent = servicePct,
            taxPercent = taxPct,
            discountCents = discountCents,
            participants = participantInputs,
            currency = currency
        )
    }

    val friendSharePairs = remember(splitResult, friends.toList()) {
        splitResult.shares
            .filter { !it.isPayer }
            .mapIndexed { idx, share ->
                val rawName = friends.getOrNull(idx)?.name?.trim().orEmpty()
                rawName to share.finalShareCents
            }
    }
    val selectedSnippet = remember(paymentSnippets, selectedSnippetId) {
        paymentSnippets.firstOrNull { it.id == selectedSnippetId }
    }
    val dateLabel = remember(selectedEpochDay) { KansoDateFormatters.formatFullDate(selectedEpochDay) }
    val shareSummaryText = remember(title, dateLabel, splitResult, currency, selectedSnippet) {
        SplitBillCalculations.formatShareSummary(
            title = title,
            dateLabel = dateLabel,
            result = splitResult,
            currency = currency,
            paymentSnippet = selectedSnippet
        )
    }

    val expenseToRecordCents = if (recordingMode == SplitExpenseRecording.FULL_BILL) {
        splitResult.grandTotalCents
    } else {
        splitResult.payerShareCents
    }
    val activeFriendCount = friendSharePairs.count { it.first.isNotBlank() && it.second > 0L }

    MoneySheetScaffold(
        title = "Split Bill",
        badgeText = "${participantInputs.size} people",
        badgeColor = moneyAccent,
        badgeIcon = Icons.Outlined.Handshake,
        primaryLabel = "Save Split & ${activeFriendCount.coerceAtLeast(friends.size)} IOU(s)",
        primaryColor = moneyAccent,
        errorText = error,
        onDismiss = onDismiss,
        onPrimaryClick = {
            val missingNameIndex = friends.indexOfFirst { it.name.isBlank() }
            val positiveFriendShares = friendSharePairs.filter { it.second > 0L }
            when {
                splitResult.grandTotalCents <= 0L ->
                    error = if (mode == SplitBillMode.EQUAL) {
                        "Enter a bill subtotal greater than 0."
                    } else {
                        "Enter at least one item amount greater than 0."
                    }
                missingNameIndex >= 0 ->
                    error = "Enter a name for Friend ${missingNameIndex + 1}."
                positiveFriendShares.isEmpty() ->
                    error = "At least one friend must owe an amount greater than 0."
                else -> {
                    val timestampMillis = MoneyCalculations.resolveTransactionTimestampMillis(
                        selectedEpochDay = selectedEpochDay,
                        existingTimestampMillis = null,
                        nowMillis = System.currentTimeMillis()
                    )
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(
                        title,
                        expenseToRecordCents,
                        selectedCategory,
                        selectedWallet,
                        positiveFriendShares,
                        dueDateEpochDay,
                        timestampMillis
                    )
                }
            }
        }
    ) {
        KansoSegmentedRow(
            options = SplitBillMode.entries.map { it.label },
            selectedIndex = SplitBillMode.entries.indexOf(mode),
            onSelect = { idx ->
                mode = SplitBillMode.entries[idx]
                error = null
            }
        )

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Place or occasion") },
            placeholder = { Text("e.g. Ramen dinner, Coffee run") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        AnimatedVisibility(
            visible = mode == SplitBillMode.EQUAL,
            enter = expandVertically(KansoMotion.SizeSpring) + fadeIn(tween(180)),
            exit = shrinkVertically() + fadeOut(tween(140))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyAmountHeroInput(
                    label = "BILL SUBTOTAL",
                    value = subtotalText,
                    onValueChange = {
                        subtotalText = it
                        error = null
                    },
                    currency = currency,
                    accentColor = moneyAccent,
                    presets = currency.quickPresets
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Service, Tax & Discount (optional)", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val hasService5 = serviceText == "5"
                val hasTax10 = taxText == "10"
                val hasTax11 = taxText == "11"
                item(key = "svc5") {
                    FilterChip(
                        selected = hasService5,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            serviceText = if (hasService5) "" else "5"
                        },
                        label = { Text("+5% Service") }
                    )
                }
                item(key = "tax10") {
                    FilterChip(
                        selected = hasTax10,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            taxText = if (hasTax10) "" else "10"
                        },
                        label = { Text("+10% Tax") }
                    )
                }
                item(key = "tax11") {
                    FilterChip(
                        selected = hasTax11,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            taxText = if (hasTax11) "" else "11"
                        },
                        label = { Text("+11% Tax") }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = serviceText,
                    onValueChange = { serviceText = sanitizePercentInput(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Service %") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = taxText,
                    onValueChange = { taxText = sanitizePercentInput(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Tax %") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            OutlinedTextField(
                value = discountText,
                onValueChange = { discountText = CurrencyFormatter.sanitizeInput(it, currency) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Discount / Promo (${currency.symbol})") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                visualTransformation = amountVisualTransformation,
                keyboardOptions = numberKeyboard
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("People (${participantInputs.size})", style = MaterialTheme.typography.labelLarge)
                KansoTonalButton(
                    text = "Add Friend",
                    leadingIcon = Icons.Outlined.PersonAdd,
                    size = KansoButtonSize.COMPACT,
                    onClick = {
                        friends.add(SplitFriendDraft(id = nextFriendId))
                        nextFriendId += 1L
                        error = null
                    }
                )
            }

            val existingNamesLower = friends.mapNotNull {
                it.name.trim().lowercase().takeIf(String::isNotEmpty)
            }.toSet()
            val suggestedPeople = recentPeople.filter { !existingNamesLower.contains(it.lowercase()) }
            if (suggestedPeople.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestedPeople, key = { it }) { person ->
                        AssistChip(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val emptyIdx = friends.indexOfFirst { it.name.isBlank() }
                                if (emptyIdx >= 0) {
                                    friends[emptyIdx] = friends[emptyIdx].copy(name = person)
                                } else {
                                    friends.add(SplitFriendDraft(id = nextFriendId, name = person))
                                    nextFriendId += 1L
                                }
                                error = null
                            },
                            label = { Text("+ $person") },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = moneyAccent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, moneyAccent.copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Me (Paid)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        KansoPillBadge(
                            text = CurrencyFormatter.format(splitResult.payerShareCents, currency),
                            color = moneyAccent
                        )
                    }
                    if (mode == SplitBillMode.BY_ITEM) {
                        OutlinedTextField(
                            value = payerItemText,
                            onValueChange = {
                                payerItemText = CurrencyFormatter.sanitizeInput(it, currency)
                                error = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("My items subtotal (${currency.symbol})") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = amountVisualTransformation,
                            keyboardOptions = numberKeyboard
                        )
                    }
                }
            }

            friends.forEachIndexed { index, friend ->
                val computedShare = splitResult.shares.firstOrNull { it.id == friend.id }?.finalShareCents ?: 0L
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = friend.name,
                                onValueChange = { updated ->
                                    friends[index] = friend.copy(name = updated)
                                    error = null
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Friend ${index + 1}") },
                                placeholder = { Text("Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                            )
                            KansoPillBadge(
                                text = CurrencyFormatter.format(computedShare, currency),
                                color = incomeSemanticColor()
                            )
                            if (friends.size > 1) {
                                KansoSmallIconButton(
                                    icon = Icons.Outlined.Close,
                                    contentDescription = "Remove friend",
                                    onClick = { friends.removeAt(index) }
                                )
                            }
                        }
                        if (mode == SplitBillMode.BY_ITEM) {
                            OutlinedTextField(
                                value = friend.itemAmountText,
                                onValueChange = { raw ->
                                    friends[index] = friend.copy(
                                        itemAmountText = CurrencyFormatter.sanitizeInput(raw, currency)
                                    )
                                    error = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Items subtotal (${currency.symbol})") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = amountVisualTransformation,
                                keyboardOptions = numberKeyboard
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Log Wallet Expense As", style = MaterialTheme.typography.labelLarge)
            KansoSegmentedRow(
                options = listOf(
                    "Full (${CurrencyFormatter.format(splitResult.grandTotalCents, currency)})",
                    "My Share (${CurrencyFormatter.format(splitResult.payerShareCents, currency)})"
                ),
                selectedIndex = SplitExpenseRecording.entries.indexOf(recordingMode),
                onSelect = { idx -> recordingMode = SplitExpenseRecording.entries[idx] }
            )
            Text(
                text = if (recordingMode == SplitExpenseRecording.FULL_BILL) {
                    "Deducts the full receipt from your wallet now; settling IOUs later adds friend repayments back."
                } else {
                    "Logs only your personal share to your wallet while still creating IOUs for friends."
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text("Category", style = MaterialTheme.typography.labelLarge)
        CategoryChipRow(
            categories = ExpenseCategory.expenseCategories,
            selected = selectedCategory,
            onSelect = { selectedCategory = it }
        )

        Text("Paid Using Wallet", style = MaterialTheme.typography.labelLarge)
        WalletTileRow(
            selected = selectedWallet,
            selectedColor = moneyAccent,
            onSelect = { selectedWallet = it }
        )

        KansoTransactionDateSelector(
            selectedEpochDay = selectedEpochDay,
            onSelectEpochDay = { selectedEpochDay = it },
            accentColor = moneyAccent
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("IOU Due Date (optional)", style = MaterialTheme.typography.labelLarge)
            val todayEpoch = remember { LocalDate.now().toEpochDay() }
            val plus3 = todayEpoch + 3L
            val plus7 = todayEpoch + 7L
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "due_none") {
                    FilterChip(
                        selected = dueDateEpochDay == null,
                        onClick = { dueDateEpochDay = null },
                        label = { Text("No due date") }
                    )
                }
                item(key = "due_3d") {
                    FilterChip(
                        selected = dueDateEpochDay == plus3,
                        onClick = { dueDateEpochDay = plus3 },
                        label = { Text("+3 days") }
                    )
                }
                item(key = "due_7d") {
                    FilterChip(
                        selected = dueDateEpochDay == plus7,
                        onClick = { dueDateEpochDay = plus7 },
                        label = { Text("+7 days") }
                    )
                }
                item(key = "due_pick") {
                    val isCustom = dueDateEpochDay != null && dueDateEpochDay != plus3 && dueDateEpochDay != plus7
                    FilterChip(
                        selected = isCustom,
                        onClick = { showDueDatePicker = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.EventAvailable,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                if (isCustom) KansoDateFormatters.formatFullDate(dueDateEpochDay!!) else "Pick Date"
                            )
                        }
                    )
                }
            }
        }

        KansoBentoCard(
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SplitSummaryLine(
                    label = "Total Bill",
                    value = CurrencyFormatter.format(splitResult.grandTotalCents, currency),
                    bold = true
                )
                if (splitResult.serviceCents > 0L || splitResult.taxCents > 0L || splitResult.discountCents > 0L) {
                    val details = buildList {
                        add("Sub ${CurrencyFormatter.format(splitResult.subtotalCents, currency)}")
                        if (splitResult.serviceCents > 0L) {
                            add("Svc ${CurrencyFormatter.format(splitResult.serviceCents, currency)}")
                        }
                        if (splitResult.taxCents > 0L) {
                            add("Tax ${CurrencyFormatter.format(splitResult.taxCents, currency)}")
                        }
                        if (splitResult.discountCents > 0L) {
                            add("Disc -${CurrencyFormatter.format(splitResult.discountCents, currency)}")
                        }
                    }.joinToString(" · ")
                    Text(
                        text = details,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SplitSummaryLine(
                    label = "Your Share",
                    value = CurrencyFormatter.format(splitResult.payerShareCents, currency)
                )
                SplitSummaryLine(
                    label = "Friends Owe You",
                    value = CurrencyFormatter.format(splitResult.friendsTotalCents, currency),
                    valueColor = incomeSemanticColor(),
                    bold = true
                )

                if (paymentSnippets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Include Payment Snippet in Summary",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item(key = "snip_none") {
                            FilterChip(
                                selected = selectedSnippetId == null,
                                onClick = { selectedSnippetId = null },
                                label = { Text("None") }
                            )
                        }
                        items(paymentSnippets, key = { it.id }) { snippet ->
                            FilterChip(
                                selected = selectedSnippetId == snippet.id,
                                onClick = { selectedSnippetId = snippet.id },
                                label = { Text(snippet.label) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KansoOutlinedButton(
                        text = "Copy Summary",
                        leadingIcon = Icons.Outlined.ContentCopy,
                        size = KansoButtonSize.COMPACT,
                        enabled = splitResult.grandTotalCents > 0L,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCopySummary(shareSummaryText)
                        }
                    )
                    KansoTonalButton(
                        text = "Share",
                        leadingIcon = Icons.Outlined.Share,
                        size = KansoButtonSize.COMPACT,
                        enabled = splitResult.grandTotalCents > 0L,
                        modifier = Modifier.weight(0.85f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShareSummary(shareSummaryText)
                        }
                    )
                }
            }
        }
    }

    if (showDueDatePicker) {
        KansoDatePickerDialog(
            initialEpochDay = dueDateEpochDay ?: LocalDate.now().plusDays(7).toEpochDay(),
            onSelect = { dueDateEpochDay = it },
            onDismiss = { showDueDatePicker = false }
        )
    }
}

@Composable
private fun SplitSummaryLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            ),
            color = valueColor
        )
    }
}
