package com.kanso.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.ExpiryCategory
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.TaskPriority
import com.kanso.core.model.TransactionType
import com.kanso.core.model.WalletType
import java.time.LocalDate

enum class QuickCaptureMode(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    NOTE("Note"),
    TASK("Task"),
    SNIPPET("Snippet"),
    EXPIRY("Expiry")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickCaptureSheet(
    currency: AppCurrency,
    initialMode: QuickCaptureMode = QuickCaptureMode.EXPENSE,
    onDismiss: () -> Unit,
    onSaveTransaction: (
        type: TransactionType,
        amountCents: Long,
        category: ExpenseCategory,
        wallet: WalletType,
        note: String
    ) -> Unit,
    onSaveNote: (title: String, content: String, tags: List<String>, isPinned: Boolean) -> Unit,
    onSaveTask: (title: String, priority: TaskPriority, dueDateEpochDay: Long?) -> Unit,
    onSaveSnippet: (
        label: String,
        value: String,
        category: SnippetCategory,
        isSensitive: Boolean,
        isPinned: Boolean
    ) -> Unit,
    onSaveExpiry: (
        title: String,
        category: ExpiryCategory,
        expiryEpochDay: Long,
        remindDaysBefore: Int
    ) -> Unit = { _, _, _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember(initialMode) { mutableStateOf(initialMode) }

    // Transaction state
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember(initialMode) {
        mutableStateOf(
            if (initialMode == QuickCaptureMode.INCOME) ExpenseCategory.SALARY else ExpenseCategory.FOOD
        )
    }
    var selectedWallet by remember { mutableStateOf(WalletType.EWALLET) }
    var txNote by remember { mutableStateOf("") }

    // Note state
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var noteTagsText by remember { mutableStateOf("") }
    var notePinned by remember { mutableStateOf(false) }

    // Task state
    var taskTitle by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var taskDueOption by remember { mutableStateOf(0) } // 0=Today, 1=Tomorrow, 2=Next Week, 3=None

    // Snippet state
    var snippetLabel by remember { mutableStateOf("") }
    var snippetValue by remember { mutableStateOf("") }
    var snippetCategory by remember { mutableStateOf(SnippetCategory.OTHER) }
    var snippetSensitive by remember { mutableStateOf(false) }
    var snippetPinned by remember { mutableStateOf(true) }

    // Expiry state
    var expiryTitle by remember { mutableStateOf("") }
    var expiryCategory by remember { mutableStateOf(ExpiryCategory.IDENTITY) }
    var expiryEpochDay by remember { mutableLongStateOf(LocalDate.now().plusDays(30).toEpochDay()) }
    var expiryRemindDays by remember { mutableIntStateOf(30) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }

    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Capture",
                style = MaterialTheme.typography.titleLarge
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QuickCaptureMode.entries.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = {
                            mode = item
                            errorText = null
                            if (item == QuickCaptureMode.INCOME && !selectedCategory.isIncomeDefault) {
                                selectedCategory = ExpenseCategory.SALARY
                            } else if (item == QuickCaptureMode.EXPENSE && selectedCategory.isIncomeDefault) {
                                selectedCategory = ExpenseCategory.FOOD
                            }
                        },
                        label = { Text(item.label) }
                    )
                }
            }

            when (mode) {
                QuickCaptureMode.EXPENSE, QuickCaptureMode.INCOME -> {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Amount (${currency.symbol})") },
                        placeholder = { Text(if (currency.decimalDigits == 0) "25000" else "12.50") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (currency.decimalDigits == 0) {
                                KeyboardType.Number
                            } else {
                                KeyboardType.Decimal
                            }
                        )
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        currency.quickPresets.forEach { presetCents ->
                            AssistChip(
                                onClick = {
                                    amountText = CurrencyFormatter.formatCompactInput(presetCents, currency)
                                    errorText = null
                                },
                                label = { Text(CurrencyFormatter.format(presetCents, currency)) }
                            )
                        }
                    }

                    val categoryPool = if (mode == QuickCaptureMode.INCOME) {
                        ExpenseCategory.incomeCategories
                    } else {
                        ExpenseCategory.expenseCategories
                    }
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categoryPool.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category.label) }
                            )
                        }
                    }

                    Text("Wallet", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WalletType.entries.forEach { wallet ->
                            FilterChip(
                                selected = selectedWallet == wallet,
                                onClick = { selectedWallet = wallet },
                                label = { Text(wallet.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = txNote,
                        onValueChange = { txNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }

                QuickCaptureMode.NOTE -> {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = {
                            noteTitle = it
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = {
                            noteContent = it
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Thought or note") },
                        minLines = 3,
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    OutlinedTextField(
                        value = noteTagsText,
                        onValueChange = { noteTagsText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tags (comma separated)") },
                        placeholder = { Text("ideas, work, shopping") },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pin to Today & top of Notes", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = notePinned, onCheckedChange = { notePinned = it })
                    }
                }

                QuickCaptureMode.TASK -> {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = {
                            taskTitle = it
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("What needs to get done?") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    Text("Priority", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskPriority.entries.forEach { priority ->
                            FilterChip(
                                selected = taskPriority == priority,
                                onClick = { taskPriority = priority },
                                label = { Text(priority.label) }
                            )
                        }
                    }
                    Text("Due", style = MaterialTheme.typography.labelLarge)
                    val dueLabels = listOf("Today", "Tomorrow", "Next Week", "No Due Date")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dueLabels.forEachIndexed { index, label ->
                            FilterChip(
                                selected = taskDueOption == index,
                                onClick = { taskDueOption = index },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                QuickCaptureMode.SNIPPET -> {
                    OutlinedTextField(
                        value = snippetLabel,
                        onValueChange = {
                            snippetLabel = it
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Label") },
                        placeholder = { Text("e.g., Home Address, Bank Account") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    OutlinedTextField(
                        value = snippetValue,
                        onValueChange = {
                            snippetValue = it
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Value (copied in 1 tap)") },
                        minLines = 2,
                        maxLines = 4
                    )
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SnippetCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = snippetCategory == cat,
                                onClick = { snippetCategory = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mask value until tapped", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = snippetSensitive, onCheckedChange = { snippetSensitive = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pin to Today screen", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = snippetPinned, onCheckedChange = { snippetPinned = it })
                    }
                }

                QuickCaptureMode.EXPIRY -> {
                    OutlinedTextField(
                        value = expiryTitle,
                        onValueChange = {
                            expiryTitle = it
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Document or item") },
                        placeholder = { Text("e.g., Passport, Driver's License, STNK") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpiryCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = expiryCategory == cat,
                                onClick = { expiryCategory = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                    Text("Expires on: ${KansoDateFormatters.formatFullDate(expiryEpochDay)}", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { expiryEpochDay = LocalDate.now().plusDays(30).toEpochDay() },
                            label = { Text("+30d") }
                        )
                        AssistChip(
                            onClick = { expiryEpochDay = LocalDate.now().plusDays(90).toEpochDay() },
                            label = { Text("+90d") }
                        )
                        AssistChip(
                            onClick = { expiryEpochDay = LocalDate.now().plusYears(1).toEpochDay() },
                            label = { Text("+1 yr") }
                        )
                        OutlinedButton(onClick = { showExpiryDatePicker = true }) {
                            Text("Pick Date")
                        }
                    }
                    Text("Remind before", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 14, 30, 60).forEach { days ->
                            FilterChip(
                                selected = expiryRemindDays == days,
                                onClick = { expiryRemindDays = days },
                                label = { Text("${days}d") }
                            )
                        }
                    }
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    when (mode) {
                        QuickCaptureMode.EXPENSE, QuickCaptureMode.INCOME -> {
                            val cents = CurrencyFormatter.parseInputToCents(amountText, currency)
                            if (cents == null || cents <= 0L) {
                                errorText = "Enter a valid amount greater than 0."
                            } else {
                                val type = if (mode == QuickCaptureMode.INCOME) {
                                    TransactionType.INCOME
                                } else {
                                    TransactionType.EXPENSE
                                }
                                onSaveTransaction(type, cents, selectedCategory, selectedWallet, txNote.trim())
                                onDismiss()
                            }
                        }

                        QuickCaptureMode.NOTE -> {
                            if (noteTitle.isBlank() && noteContent.isBlank()) {
                                errorText = "Add a title or some note text."
                            } else {
                                val resolvedTitle = noteTitle.trim().ifEmpty {
                                    noteContent.trim().lineSequence().first().take(40)
                                }
                                val tags = noteTagsText.split(',')
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                onSaveNote(resolvedTitle, noteContent.trim(), tags, notePinned)
                                onDismiss()
                            }
                        }

                        QuickCaptureMode.TASK -> {
                            if (taskTitle.isBlank()) {
                                errorText = "Enter a task title."
                            } else {
                                val today = LocalDate.now()
                                val dueDay = when (taskDueOption) {
                                    0 -> today.toEpochDay()
                                    1 -> today.plusDays(1).toEpochDay()
                                    2 -> today.plusDays(7).toEpochDay()
                                    else -> null
                                }
                                onSaveTask(taskTitle.trim(), taskPriority, dueDay)
                                onDismiss()
                            }
                        }

                        QuickCaptureMode.SNIPPET -> {
                            if (snippetLabel.isBlank() || snippetValue.isBlank()) {
                                errorText = "Enter both a label and value."
                            } else {
                                onSaveSnippet(
                                    snippetLabel.trim(),
                                    snippetValue.trim(),
                                    snippetCategory,
                                    snippetSensitive,
                                    snippetPinned
                                )
                                onDismiss()
                            }
                        }

                        QuickCaptureMode.EXPIRY -> {
                            if (expiryTitle.isBlank()) {
                                errorText = "Enter a document or item name."
                            } else {
                                onSaveExpiry(
                                    expiryTitle.trim(),
                                    expiryCategory,
                                    expiryEpochDay,
                                    expiryRemindDays
                                )
                                onDismiss()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save ${mode.label}")
            }
        }
    }

    if (showExpiryDatePicker) {
        KansoDatePickerDialog(
            initialEpochDay = expiryEpochDay,
            onSelect = { expiryEpochDay = it },
            onDismiss = { showExpiryDatePicker = false }
        )
    }
}
