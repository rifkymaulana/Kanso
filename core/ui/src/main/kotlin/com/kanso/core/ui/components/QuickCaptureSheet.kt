package com.kanso.core.ui.components

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableLongStateOf
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
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.ExpiryCategory
import com.kanso.core.model.SnippetCategory
import com.kanso.core.model.TaskPriority
import com.kanso.core.model.TransactionType
import com.kanso.core.model.WalletType
import com.kanso.core.ui.theme.kansoColors
import java.time.LocalDate

enum class QuickCaptureMode(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer"),
    NOTE("Note"),
    TASK("Task"),
    SNIPPET("Snippet"),
    EXPIRY("Expiry")
}

@Composable
private fun QuickCaptureMode.accentColor(): Color = when (this) {
    QuickCaptureMode.EXPENSE -> MaterialTheme.kansoColors.expense
    QuickCaptureMode.INCOME -> MaterialTheme.kansoColors.income
    QuickCaptureMode.TRANSFER -> MaterialTheme.kansoColors.utilitiesAccent
    QuickCaptureMode.NOTE -> MaterialTheme.kansoColors.brainAccent
    QuickCaptureMode.TASK -> Color(0xFF8B5CF6)
    QuickCaptureMode.SNIPPET -> MaterialTheme.colorScheme.primary
    QuickCaptureMode.EXPIRY -> MaterialTheme.kansoColors.lifeAdminAccent
}

private fun QuickCaptureMode.icon(): ImageVector = when (this) {
    QuickCaptureMode.EXPENSE -> Icons.Outlined.ArrowUpward
    QuickCaptureMode.INCOME -> Icons.Outlined.ArrowDownward
    QuickCaptureMode.TRANSFER -> Icons.Outlined.SwapHoriz
    QuickCaptureMode.NOTE -> Icons.Outlined.Description
    QuickCaptureMode.TASK -> Icons.Outlined.CheckCircle
    QuickCaptureMode.SNIPPET -> Icons.Outlined.ContentPaste
    QuickCaptureMode.EXPIRY -> Icons.Outlined.EventAvailable
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
    onSaveTransfer: (
        fromWallet: WalletType,
        toWallet: WalletType,
        amountCents: Long,
        feeCents: Long,
        note: String
    ) -> Unit = { _, _, _, _, _ -> },
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
    val haptic = LocalHapticFeedback.current
    var mode by remember(initialMode) { mutableStateOf(initialMode) }

    // Transaction state
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember(initialMode) {
        mutableStateOf(
            if (initialMode == QuickCaptureMode.INCOME) ExpenseCategory.SALARY else ExpenseCategory.FOOD
        )
    }
    var selectedWallet by remember { mutableStateOf(WalletType.EWALLET) }
    var transferFromWallet by remember { mutableStateOf(WalletType.BANK) }
    var transferToWallet by remember { mutableStateOf(WalletType.EWALLET) }
    var transferFeeText by remember { mutableStateOf("") }
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
            val badgeColor by animateColorAsState(
                targetValue = mode.accentColor(),
                animationSpec = tween(220),
                label = "qcHeaderBadge"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Capture",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                KansoPillBadge(
                    text = mode.label,
                    color = badgeColor,
                    leadingIcon = mode.icon()
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QuickCaptureMode.entries, key = { it.name }) { item ->
                    val selected = mode == item
                    val accent = item.accentColor()
                    val chipContainer by animateColorAsState(
                        targetValue = if (selected) {
                            accent.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        animationSpec = tween(200),
                        label = "qcChipBg"
                    )
                    val chipBorder by animateColorAsState(
                        targetValue = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = tween(200),
                        label = "qcChipBorder"
                    )
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                mode = item
                                errorText = null
                                if (item == QuickCaptureMode.INCOME && !selectedCategory.isIncomeDefault) {
                                    selectedCategory = ExpenseCategory.SALARY
                                } else if (item == QuickCaptureMode.EXPENSE && selectedCategory.isIncomeDefault) {
                                    selectedCategory = ExpenseCategory.FOOD
                                }
                            },
                        shape = RoundedCornerShape(50),
                        color = chipContainer,
                        border = BorderStroke(1.dp, chipBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = item.icon(),
                                contentDescription = null,
                                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    KansoMotion.directionalTabTransition(targetState.ordinal, initialState.ordinal)
                },
                label = "quickCaptureModeContent"
            ) { activeMode ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (activeMode) {
                QuickCaptureMode.EXPENSE, QuickCaptureMode.INCOME -> {
                    val amountAccent = activeMode.accentColor()
                    val amountFocus = remember { FocusRequester() }
                    val placeholder = if (currency.decimalDigits == 0) "0" else "0.00"

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { amountFocus.requestFocus() },
                        shape = RoundedCornerShape(20.dp),
                        color = amountAccent.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, amountAccent.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (activeMode == QuickCaptureMode.INCOME) "INCOME AMOUNT" else "EXPENSE AMOUNT",
                                style = MaterialTheme.typography.labelSmall,
                                color = amountAccent
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currency.symbol,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = amountAccent,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (amountText.isEmpty()) {
                                        Text(
                                            text = placeholder,
                                            style = MaterialTheme.typography.displaySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        )
                                    }
                                    BasicTextField(
                                        value = amountText,
                                        onValueChange = {
                                            amountText = CurrencyFormatter.sanitizeInput(it, currency)
                                            errorText = null
                                        },
                                        modifier = Modifier.focusRequester(amountFocus),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.displaySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Start
                                        ),
                                        cursorBrush = SolidColor(amountAccent),
                                        visualTransformation = remember(currency) {
                                            CurrencyAmountVisualTransformation(currency)
                                        },
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

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(currency.quickPresets, key = { it }) { presetCents ->
                            AssistChip(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    amountText = CurrencyFormatter.formatCompactInput(presetCents, currency)
                                    errorText = null
                                },
                                label = { Text(CurrencyFormatter.format(presetCents, currency)) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }

                    val categoryPool = if (activeMode == QuickCaptureMode.INCOME) {
                        ExpenseCategory.incomeCategories
                    } else {
                        ExpenseCategory.expenseCategories
                    }
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoryPool, key = { it.name }) { category ->
                            val selected = selectedCategory == category
                            val catAccent = category.accentColor()
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedCategory = category
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) catAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) catAccent else MaterialTheme.colorScheme.outlineVariant
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
                                        tint = if (selected) catAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = category.label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (selected) catAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Text("Wallet", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WalletType.entries.forEach { wallet ->
                            val selected = selectedWallet == wallet
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedWallet = wallet
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
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
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = wallet.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = txNote,
                        onValueChange = { txNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }

                QuickCaptureMode.TRANSFER -> {
                    val amountAccent = activeMode.accentColor()
                    val amountFocus = remember { FocusRequester() }
                    val placeholder = if (currency.decimalDigits == 0) "0" else "0.00"

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { amountFocus.requestFocus() },
                        shape = RoundedCornerShape(20.dp),
                        color = amountAccent.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, amountAccent.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "TRANSFER AMOUNT",
                                style = MaterialTheme.typography.labelSmall,
                                color = amountAccent
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currency.symbol,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = amountAccent,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (amountText.isEmpty()) {
                                        Text(
                                            text = placeholder,
                                            style = MaterialTheme.typography.displaySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        )
                                    }
                                    BasicTextField(
                                        value = amountText,
                                        onValueChange = {
                                            amountText = CurrencyFormatter.sanitizeInput(it, currency)
                                            errorText = null
                                        },
                                        modifier = Modifier.focusRequester(amountFocus),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.displaySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Start
                                        ),
                                        cursorBrush = SolidColor(amountAccent),
                                        visualTransformation = remember(currency) {
                                            CurrencyAmountVisualTransformation(currency)
                                        },
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

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(currency.quickPresets, key = { it }) { presetCents ->
                            AssistChip(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    amountText = CurrencyFormatter.formatCompactInput(presetCents, currency)
                                    errorText = null
                                },
                                label = { Text(CurrencyFormatter.format(presetCents, currency)) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }

                    Text("From Wallet", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WalletType.entries.forEach { wallet ->
                            val selected = transferFromWallet == wallet
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (wallet == transferToWallet) {
                                            transferToWallet = transferFromWallet
                                        }
                                        transferFromWallet = wallet
                                        errorText = null
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
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
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = wallet.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Text("To Wallet", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WalletType.entries.forEach { wallet ->
                            val selected = transferToWallet == wallet
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (wallet == transferFromWallet) {
                                            transferFromWallet = transferToWallet
                                        }
                                        transferToWallet = wallet
                                        errorText = null
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    amountAccent.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) amountAccent else MaterialTheme.colorScheme.outlineVariant
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
                                        tint = if (selected) amountAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = wallet.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) amountAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    val transferFeeVisualTransformation = remember(currency) {
                        CurrencyAmountVisualTransformation(currency)
                    }
                    OutlinedTextField(
                        value = transferFeeText,
                        onValueChange = {
                            transferFeeText = CurrencyFormatter.sanitizeInput(it, currency)
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Transfer / top-up fee (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = transferFeeVisualTransformation,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (currency.decimalDigits == 0) {
                                KeyboardType.Number
                            } else {
                                KeyboardType.Decimal
                            }
                        )
                    )

                    OutlinedTextField(
                        value = txNote,
                        onValueChange = { txNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    OutlinedTextField(
                        value = noteTagsText,
                        onValueChange = { noteTagsText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tags (comma separated)") },
                        placeholder = { Text("ideas, work, shopping") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
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
                        shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(14.dp),
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
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp)
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
                        shape = RoundedCornerShape(14.dp),
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
                    Text(
                        "Expires on: ${KansoDateFormatters.formatFullDate(expiryEpochDay)}",
                        style = MaterialTheme.typography.labelLarge
                    )
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
                        KansoOutlinedButton(
                            text = "Pick Date",
                            size = KansoButtonSize.COMPACT,
                            onClick = { showExpiryDatePicker = true }
                        )
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
                }
            }

            AnimatedVisibility(
                visible = errorText != null,
                enter = expandVertically(KansoMotion.SizeSpring) + fadeIn(tween(180)),
                exit = shrinkVertically() + fadeOut(tween(140))
            ) {
                Text(
                    text = errorText.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            val buttonColor = when (mode) {
                QuickCaptureMode.EXPENSE, QuickCaptureMode.INCOME, QuickCaptureMode.TRANSFER -> mode.accentColor()
                else -> MaterialTheme.colorScheme.primary
            }
            KansoPrimaryButton(
                text = "Save ${mode.label}",
                size = KansoButtonSize.LARGE,
                containerColor = buttonColor,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSaveTransaction(type, cents, selectedCategory, selectedWallet, txNote.trim())
                                onDismiss()
                            }
                        }

                        QuickCaptureMode.TRANSFER -> {
                            val cents = CurrencyFormatter.parseInputToCents(amountText, currency)
                            val parsedFee = if (transferFeeText.isNotBlank()) {
                                CurrencyFormatter.parseInputToCents(transferFeeText, currency)
                            } else {
                                0L
                            }
                            when {
                                cents == null || cents <= 0L ->
                                    errorText = "Enter a valid amount greater than 0."
                                transferFromWallet == transferToWallet ->
                                    errorText = "Choose two different wallets."
                                parsedFee == null ->
                                    errorText = "Enter a valid transfer fee."
                                else -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSaveTransfer(
                                        transferFromWallet,
                                        transferToWallet,
                                        cents,
                                        parsedFee,
                                        txNote.trim()
                                    )
                                    onDismiss()
                                }
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSaveTask(taskTitle.trim(), taskPriority, dueDay)
                                onDismiss()
                            }
                        }

                        QuickCaptureMode.SNIPPET -> {
                            if (snippetLabel.isBlank() || snippetValue.isBlank()) {
                                errorText = "Enter both a label and value."
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                }
            )
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
