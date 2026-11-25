package com.kanso.feature.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    initial != null -> "Edit Transaction"
                    type == TransactionType.TRANSFER -> "Transfer Between Wallets"
                    else -> "Add Transaction"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                error = null
                                if (t == TransactionType.INCOME && !category.isIncomeDefault) {
                                    category = ExpenseCategory.SALARY
                                } else if (t == TransactionType.EXPENSE && category.isIncomeDefault) {
                                    category = ExpenseCategory.FOOD
                                } else if (t == TransactionType.TRANSFER && toWallet == wallet) {
                                    toWallet = WalletType.entries.first { it != wallet }
                                }
                            },
                            label = { Text(t.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (${currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (currency.decimalDigits == 0) KeyboardType.Number else KeyboardType.Decimal
                    )
                )
                if (type != TransactionType.TRANSFER) {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    val categories = if (type == TransactionType.INCOME) {
                        ExpenseCategory.incomeCategories
                    } else {
                        ExpenseCategory.expenseCategories
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }
                Text(
                    text = if (type == TransactionType.TRANSFER) "From Wallet" else "Wallet",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WalletType.entries.forEach { w ->
                        FilterChip(
                            selected = wallet == w,
                            onClick = {
                                if (type == TransactionType.TRANSFER && w == toWallet) {
                                    toWallet = wallet
                                }
                                wallet = w
                                error = null
                            },
                            label = { Text(w.label) }
                        )
                    }
                }
                if (type == TransactionType.TRANSFER) {
                    Text("To Wallet", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WalletType.entries.forEach { w ->
                            FilterChip(
                                selected = toWallet == w,
                                onClick = {
                                    if (w == wallet) {
                                        wallet = toWallet
                                    }
                                    toWallet = w
                                    error = null
                                },
                                label = { Text(w.label) }
                            )
                        }
                    }
                    if (initial == null) {
                        OutlinedTextField(
                            value = feeText,
                            onValueChange = {
                                feeText = it
                                error = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Transfer / admin fee (${currency.symbol}, optional)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (currency.decimalDigits == 0) KeyboardType.Number else KeyboardType.Decimal
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
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
                    else -> onSave(
                        type,
                        cents,
                        if (type == TransactionType.TRANSFER) ExpenseCategory.OTHER else category,
                        wallet,
                        if (type == TransactionType.TRANSFER) toWallet else null,
                        parsedFee,
                        note
                    )
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
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun WalletBalanceDialog(
    walletSummary: WalletBalanceSummary,
    currency: AppCurrency,
    onDismiss: () -> Unit,
    onSaveTargetBalance: (Long) -> Unit
) {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${walletSummary.wallet.label} Balance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tracked net flow: ${CurrencyFormatter.format(walletSummary.netDeltaCents, currency, showSign = true)}. Enter your real current balance to reconcile this wallet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Actual current balance (${currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (currency.decimalDigits == 0) KeyboardType.Number else KeyboardType.Decimal
                    )
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isBlank()) {
                    onSaveTargetBalance(0L)
                } else {
                    val cents = CurrencyFormatter.parseInputToCents(text, currency)
                    if (cents == null) {
                        error = "Invalid amount."
                    } else {
                        onSaveTargetBalance(cents)
                    }
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun OverallBudgetDialog(
    currency: AppCurrency,
    currentLimitCents: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var text by remember {
        mutableStateOf(
            if (currentLimitCents > 0L) CurrencyFormatter.formatCompactInput(currentLimitCents, currency) else ""
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Overall Monthly Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Set 0 or leave blank to disable the overall limit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monthly limit (${currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (currency.decimalDigits == 0) KeyboardType.Number else KeyboardType.Decimal
                    )
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isBlank()) {
                    onSave(0L)
                } else {
                    val cents = CurrencyFormatter.parseInputToCents(text, currency)
                    if (cents == null) {
                        error = "Invalid amount."
                    } else {
                        onSave(cents)
                    }
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryBudgetDialog(
    currency: AppCurrency,
    initial: CategoryBudget?,
    onDismiss: () -> Unit,
    onSave: (ExpenseCategory, Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var category by remember { mutableStateOf(initial?.category ?: ExpenseCategory.FOOD) }
    var amountText by remember {
        mutableStateOf(initial?.let { CurrencyFormatter.formatCompactInput(it.limitCents, currency) }.orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Category Budget" else "Edit Budget") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseCategory.expenseCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            enabled = initial == null || initial.category == cat,
                            label = { Text(cat.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monthly limit (${currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (currency.decimalDigits == 0) KeyboardType.Number else KeyboardType.Decimal
                    )
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = CurrencyFormatter.parseInputToCents(amountText, currency)
                if (cents == null || cents <= 0L) {
                    error = "Enter a valid limit greater than 0."
                } else {
                    onSave(category, cents)
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
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillEditorDialog(
    currency: AppCurrency,
    initial: RecurringBill?,
    onDismiss: () -> Unit,
    onSave: (String, Long, ExpenseCategory, WalletType, Int, BillFrequency, Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Recurring Bill" else "Edit Bill") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bill name") },
                    placeholder = { Text("Internet, Rent, Electricity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            error = null
                        },
                        modifier = Modifier.weight(1.4f),
                        label = { Text("Amount (${currency.symbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (currency.decimalDigits == 0) KeyboardType.Number else KeyboardType.Decimal
                        )
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Text("Frequency", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BillFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.label) }
                        )
                    }
                }
                Text("Wallet", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WalletType.entries.forEach { w ->
                        FilterChip(
                            selected = wallet == w,
                            onClick = { wallet = w },
                            label = { Text(w.label) }
                        )
                    }
                }
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        ExpenseCategory.BILLS,
                        ExpenseCategory.HOUSING,
                        ExpenseCategory.ENTERTAINMENT,
                        ExpenseCategory.HEALTH,
                        ExpenseCategory.OTHER
                    ).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active reminder", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = CurrencyFormatter.parseInputToCents(amountText, currency)
                val day = dueDayText.toIntOrNull()
                when {
                    name.isBlank() -> error = "Enter a bill name."
                    cents == null || cents <= 0L -> error = "Enter a valid amount."
                    day == null || day !in 1..31 -> error = "Due day must be 1–31."
                    else -> onSave(name, cents, category, wallet, day, frequency, isActive)
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
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IouEditorDialog(
    currency: AppCurrency,
    initial: IouEntry?,
    onDismiss: () -> Unit,
    onSave: (String, Long, IouDirection, String, Long?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var personName by remember { mutableStateOf(initial?.personName.orEmpty()) }
    var amountText by remember {
        mutableStateOf(initial?.let { CurrencyFormatter.formatCompactInput(it.amountCents, currency) }.orEmpty())
    }
    var direction by remember { mutableStateOf(initial?.direction ?: IouDirection.OWES_ME) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var dueOption by remember { mutableStateOf(if (initial?.dueDateEpochDay != null) 1 else 0) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add IOU" else "Edit IOU") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IouDirection.entries.forEach { dir ->
                        FilterChip(
                            selected = direction == dir,
                            onClick = { direction = dir },
                            label = { Text(dir.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Person") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (${currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (currency.decimalDigits == 0) KeyboardType.Number else KeyboardType.Decimal
                    )
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Reason / note") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Text("Target date", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val labels = listOf("None", "In 7 days", "In 30 days")
                    labels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = dueOption == idx,
                            onClick = { dueOption = idx },
                            label = { Text(label) }
                        )
                    }
                }
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
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
                        onSave(personName, cents, direction, note, dueDay)
                    }
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
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettleIouDialog(
    iou: IouEntry,
    currency: AppCurrency,
    onDismiss: () -> Unit,
    onConfirm: (recordTransaction: Boolean, wallet: WalletType) -> Unit
) {
    var recordTx by remember { mutableStateOf(true) }
    var wallet by remember { mutableStateOf(WalletType.BANK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle IOU") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val actionDesc = if (iou.direction == IouDirection.OWES_ME) {
                    "${iou.personName} paid you ${CurrencyFormatter.format(iou.amountCents, currency)}."
                } else {
                    "You paid ${iou.personName} ${CurrencyFormatter.format(iou.amountCents, currency)}."
                }
                Text(actionDesc, style = MaterialTheme.typography.bodyLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = recordTx,
                        onCheckedChange = { recordTx = it }
                    )
                    Text(
                        text = if (iou.direction == IouDirection.OWES_ME) {
                            "Also log as Income transaction"
                        } else {
                            "Also log as Expense transaction"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (recordTx) {
                    Text("Wallet", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WalletType.entries.forEach { w ->
                            FilterChip(
                                selected = wallet == w,
                                onClick = { wallet = w },
                                label = { Text(w.label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(recordTx, wallet) }) {
                Text("Mark Settled")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
