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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.IouDirection
import com.kanso.core.model.IouEntry
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.TransactionType
import com.kanso.core.model.WalletBalanceSummary
import com.kanso.core.model.WalletType
import com.kanso.core.ui.components.CurrencyAmountVisualTransformation
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoMotion
import com.kanso.core.ui.components.KansoOutlinedButton
import com.kanso.core.ui.components.KansoPillBadge
import com.kanso.core.ui.components.KansoPrimaryButton
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.core.ui.components.accentColor
import com.kanso.core.ui.components.expenseSemanticColor
import com.kanso.core.ui.components.icon
import com.kanso.core.ui.components.incomeSemanticColor
import com.kanso.core.ui.theme.kansoColors
import java.time.LocalDate

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
        note: String
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
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(
                        type,
                        cents,
                        if (type == TransactionType.TRANSFER) ExpenseCategory.OTHER else category,
                        wallet,
                        if (type == TransactionType.TRANSFER) toWallet else null,
                        parsedFee,
                        note
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                KansoPillBadge(
                    text = badgeText,
                    color = badgeColor,
                    leadingIcon = badgeIcon
                )
            }

            content()

            AnimatedVisibility(
                visible = errorText != null,
                enter = expandVertically(KansoMotion.SizeSpring) + fadeIn(tween(180)),
                exit = shrinkVertically() + fadeOut(tween(140))
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = errorText.orEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            if (onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KansoOutlinedButton(
                        text = "Delete",
                        size = KansoButtonSize.LARGE,
                        contentColor = MaterialTheme.colorScheme.error,
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f),
                        onClick = onDelete
                    )
                    KansoPrimaryButton(
                        text = primaryLabel,
                        size = KansoButtonSize.LARGE,
                        containerColor = primaryColor,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1.8f),
                        onClick = onPrimaryClick
                    )
                }
            } else {
                KansoPrimaryButton(
                    text = primaryLabel,
                    size = KansoButtonSize.LARGE,
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onPrimaryClick
                )
            }
        }
    }
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
