package com.kanso.feature.lifeadmin

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ExpiryCategory
import com.kanso.core.model.ExpiryItem
import com.kanso.core.model.MaintenanceCategory
import com.kanso.core.model.MaintenanceLog
import com.kanso.core.model.VaultCategory
import com.kanso.core.model.VaultItem
import com.kanso.core.ui.components.CurrencyAmountVisualTransformation
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoDatePickerDialog
import com.kanso.core.ui.components.KansoOutlinedButton
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpiryEditorDialog(
    initial: ExpiryItem?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        category: ExpiryCategory,
        referenceNumber: String,
        expiryEpochDay: Long,
        remindDaysBefore: Int,
        note: String
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: ExpiryCategory.IDENTITY) }
    var referenceNumber by remember { mutableStateOf(initial?.referenceNumber.orEmpty()) }
    var expiryEpochDay by remember {
        mutableLongStateOf(initial?.expiryEpochDay ?: LocalDate.now().plusDays(30).toEpochDay())
    }
    var remindDaysBefore by remember { mutableIntStateOf(initial?.remindDaysBefore ?: 30) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Expiry Alert" else "Edit Expiry") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Item / document") },
                    placeholder = { Text("Passport, SIM A, STNK, Laptop Warranty") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpiryCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Reference / ID number (optional)") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Expires on", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = KansoDateFormatters.formatFullDate(expiryEpochDay),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    KansoOutlinedButton(
                        text = "Change Date",
                        size = KansoButtonSize.COMPACT,
                        onClick = { showDatePicker = true }
                    )
                }
                Text("Remind days before", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7, 14, 30, 60, 90).forEach { days ->
                        FilterChip(
                            selected = remindDaysBefore == days,
                            onClick = { remindDaysBefore = days },
                            label = { Text("${days}d") }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    maxLines = 4,
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
                    error = "Enter a title."
                } else {
                    onSave(title, category, referenceNumber, expiryEpochDay, remindDaysBefore, note)
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

    if (showDatePicker) {
        KansoDatePickerDialog(
            initialEpochDay = expiryEpochDay,
            onSelect = { expiryEpochDay = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaintenanceEditorDialog(
    currency: AppCurrency,
    initial: MaintenanceLog?,
    onDismiss: () -> Unit,
    onSave: (
        assetName: String,
        category: MaintenanceCategory,
        serviceDescription: String,
        serviceEpochDay: Long,
        odometerKm: Int?,
        costCents: Long?,
        nextDueEpochDay: Long?,
        nextDueOdometerKm: Int?,
        note: String
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var assetName by remember { mutableStateOf(initial?.assetName.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: MaintenanceCategory.CAR) }
    var serviceDescription by remember { mutableStateOf(initial?.serviceDescription.orEmpty()) }
    var serviceEpochDay by remember {
        mutableLongStateOf(initial?.serviceEpochDay ?: LocalDate.now().toEpochDay())
    }
    var odometerText by remember { mutableStateOf(initial?.odometerKm?.toString().orEmpty()) }
    var costText by remember {
        val existingCost = initial?.costCents
        mutableStateOf(if (existingCost != null) CurrencyFormatter.formatCompactInput(existingCost, currency) else "")
    }
    var nextDueEpochDay by remember { mutableStateOf(initial?.nextDueEpochDay) }
    var nextDueOdoText by remember { mutableStateOf(initial?.nextDueOdometerKm?.toString().orEmpty()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }

    var showServiceDatePicker by remember { mutableStateOf(false) }
    var showNextDueDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Service Log" else "Edit Service Log") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = assetName,
                    onValueChange = {
                        assetName = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Asset name") },
                    placeholder = { Text("Civic, Vespa, Living Room AC") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MaintenanceCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = serviceDescription,
                    onValueChange = {
                        serviceDescription = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Service performed") },
                    placeholder = { Text("Oil change, filter clean, tire swap") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Service date", style = MaterialTheme.typography.labelLarge)
                        Text(KansoDateFormatters.formatFullDate(serviceEpochDay))
                    }
                    KansoOutlinedButton(
                        text = "Pick",
                        size = KansoButtonSize.COMPACT,
                        onClick = { showServiceDatePicker = true }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = odometerText,
                        onValueChange = { odometerText = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier.weight(1f),
                        label = { Text("Odometer (km)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    val costVisualTransformation = remember(currency) { CurrencyAmountVisualTransformation(currency) }
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = CurrencyFormatter.sanitizeInput(it, currency) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Cost (${currency.symbol})") },
                        singleLine = true,
                        visualTransformation = costVisualTransformation,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (currency.decimalDigits == 0) KeyboardType.Number else KeyboardType.Decimal
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Next due date", style = MaterialTheme.typography.labelLarge)
                        val nextDay = nextDueEpochDay
                        Text(if (nextDay != null) KansoDateFormatters.formatFullDate(nextDay) else "Not set")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (nextDueEpochDay != null) {
                            TextButton(onClick = { nextDueEpochDay = null }) { Text("Clear") }
                        }
                        KansoOutlinedButton(
                            text = "Set",
                            size = KansoButtonSize.COMPACT,
                            onClick = { showNextDueDatePicker = true }
                        )
                    }
                }
                OutlinedTextField(
                    value = nextDueOdoText,
                    onValueChange = { nextDueOdoText = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Next due odometer km (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    assetName.isBlank() -> error = "Enter an asset name."
                    serviceDescription.isBlank() -> error = "Describe the service performed."
                    else -> {
                        val costCents = if (costText.isBlank()) {
                            null
                        } else {
                            CurrencyFormatter.parseInputToCents(costText, currency)
                        }
                        onSave(
                            assetName,
                            category,
                            serviceDescription,
                            serviceEpochDay,
                            odometerText.toIntOrNull(),
                            costCents,
                            nextDueEpochDay,
                            nextDueOdoText.toIntOrNull(),
                            note
                        )
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

    if (showServiceDatePicker) {
        KansoDatePickerDialog(
            initialEpochDay = serviceEpochDay,
            onSelect = { serviceEpochDay = it },
            onDismiss = { showServiceDatePicker = false }
        )
    }

    if (showNextDueDatePicker) {
        KansoDatePickerDialog(
            initialEpochDay = nextDueEpochDay ?: LocalDate.ofEpochDay(serviceEpochDay).plusMonths(6).toEpochDay(),
            onSelect = { nextDueEpochDay = it },
            onDismiss = { showNextDueDatePicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VaultEditorDialog(
    initial: VaultItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, category: VaultCategory, secretContent: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: VaultCategory.CREDENTIAL) }
    var secretContent by remember { mutableStateOf(initial?.secretContent.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Vault Secret" else "Edit Vault Secret") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    placeholder = { Text("GitHub 2FA Backup Codes, Home Wi-Fi") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VaultCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = secretContent,
                    onValueChange = {
                        secretContent = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Encrypted secret content") },
                    minLines = 3,
                    maxLines = 8
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    title.isBlank() -> error = "Enter a title."
                    secretContent.isBlank() -> error = "Enter the secret content."
                    else -> onSave(title, category, secretContent)
                }
            }) {
                Text("Save Encrypted")
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
