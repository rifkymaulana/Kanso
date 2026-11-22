package com.kanso.feature.money

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CategoryBudget
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.IouDirection
import com.kanso.core.model.IouEntry
import com.kanso.core.model.MoneyCalculations
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.TransactionType
import com.kanso.core.model.WalletType
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoEmptyState
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.core.ui.components.MoneyAmountBadge
import com.kanso.core.ui.components.SectionHeader
import com.kanso.core.ui.components.expenseSemanticColor
import com.kanso.core.ui.components.icon
import com.kanso.core.ui.components.incomeSemanticColor
import com.kanso.core.ui.theme.KansoTheme
import com.kanso.core.ui.theme.WarningAmber
import com.kanso.core.ui.theme.WarningAmberDark
import java.time.YearMonth

@Composable
fun MoneyRoute(
    onShowMessage: (String) -> Unit,
    viewModel: MoneyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var editingTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }
    var showNewTransactionDialog by remember { mutableStateOf(false) }

    var showOverallBudgetDialog by remember { mutableStateOf(false) }
    var editingCategoryBudget by remember { mutableStateOf<CategoryBudget?>(null) }
    var showNewCategoryBudgetDialog by remember { mutableStateOf(false) }

    var editingBill by remember { mutableStateOf<RecurringBill?>(null) }
    var showNewBillDialog by remember { mutableStateOf(false) }

    var editingIou by remember { mutableStateOf<IouEntry?>(null) }
    var showNewIouDialog by remember { mutableStateOf(false) }
    var settlingIou by remember { mutableStateOf<IouEntry?>(null) }

    MoneyScreenContent(
        uiState = uiState,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onAddTransaction = { showNewTransactionDialog = true },
        onEditTransaction = { editingTransaction = it },
        onEditOverallBudget = { showOverallBudgetDialog = true },
        onAddCategoryBudget = { showNewCategoryBudgetDialog = true },
        onEditCategoryBudget = { category ->
            editingCategoryBudget = uiState.budgets.firstOrNull { it.category == category }
        },
        onAddBill = { showNewBillDialog = true },
        onEditBill = { editingBill = it },
        onPayBill = { bill ->
            viewModel.payRecurringBill(bill)
            onShowMessage("Paid ${bill.name} and logged expense")
        },
        onAddIou = { showNewIouDialog = true },
        onEditIou = { editingIou = it },
        onRequestSettleIou = { settlingIou = it }
    )

    if (showNewTransactionDialog || editingTransaction != null) {
        val current = editingTransaction
        TransactionEditorDialog(
            currency = uiState.currency,
            initial = current,
            onDismiss = {
                showNewTransactionDialog = false
                editingTransaction = null
            },
            onSave = { type, amountCents, category, wallet, note ->
                viewModel.saveTransaction(
                    existingId = current?.id ?: 0L,
                    type = type,
                    amountCents = amountCents,
                    category = category,
                    wallet = wallet,
                    note = note,
                    timestampMillis = current?.timestampMillis ?: System.currentTimeMillis()
                )
                showNewTransactionDialog = false
                editingTransaction = null
            },
            onDelete = current?.let { tx ->
                {
                    viewModel.deleteTransaction(tx.id)
                    editingTransaction = null
                    onShowMessage("Transaction deleted")
                }
            }
        )
    }

    if (showOverallBudgetDialog) {
        OverallBudgetDialog(
            currency = uiState.currency,
            currentLimitCents = uiState.overallMonthlyBudgetCents,
            onDismiss = { showOverallBudgetDialog = false },
            onSave = { limit ->
                viewModel.setOverallMonthlyBudget(limit)
                showOverallBudgetDialog = false
                onShowMessage("Monthly budget updated")
            }
        )
    }

    if (showNewCategoryBudgetDialog || editingCategoryBudget != null) {
        val current = editingCategoryBudget
        CategoryBudgetDialog(
            currency = uiState.currency,
            initial = current,
            onDismiss = {
                showNewCategoryBudgetDialog = false
                editingCategoryBudget = null
            },
            onSave = { cat, limit ->
                viewModel.saveCategoryBudget(current?.id ?: 0L, cat, limit)
                showNewCategoryBudgetDialog = false
                editingCategoryBudget = null
            },
            onDelete = current?.let { budget ->
                {
                    viewModel.deleteCategoryBudget(budget.id)
                    editingCategoryBudget = null
                    onShowMessage("Budget removed")
                }
            }
        )
    }

    if (showNewBillDialog || editingBill != null) {
        val current = editingBill
        BillEditorDialog(
            currency = uiState.currency,
            initial = current,
            onDismiss = {
                showNewBillDialog = false
                editingBill = null
            },
            onSave = { name, amountCents, category, wallet, dueDay, frequency, isActive ->
                viewModel.saveRecurringBill(
                    existing = current,
                    name = name,
                    amountCents = amountCents,
                    category = category,
                    wallet = wallet,
                    dueDayOfMonth = dueDay,
                    frequency = frequency,
                    isActive = isActive
                )
                showNewBillDialog = false
                editingBill = null
            },
            onDelete = current?.let { bill ->
                {
                    viewModel.deleteRecurringBill(bill.id)
                    editingBill = null
                    onShowMessage("Bill deleted")
                }
            }
        )
    }

    if (showNewIouDialog || editingIou != null) {
        val current = editingIou
        IouEditorDialog(
            currency = uiState.currency,
            initial = current,
            onDismiss = {
                showNewIouDialog = false
                editingIou = null
            },
            onSave = { person, amountCents, dir, note, dueDay ->
                viewModel.saveIou(
                    existing = current,
                    personName = person,
                    amountCents = amountCents,
                    direction = dir,
                    note = note,
                    dueDateEpochDay = dueDay
                )
                showNewIouDialog = false
                editingIou = null
            },
            onDelete = current?.let { iou ->
                {
                    viewModel.deleteIou(iou.id)
                    editingIou = null
                    onShowMessage("IOU deleted")
                }
            }
        )
    }

    settlingIou?.let { target ->
        SettleIouDialog(
            iou = target,
            currency = uiState.currency,
            onDismiss = { settlingIou = null },
            onConfirm = { recordTx, wallet ->
                viewModel.settleIou(target, recordTx, wallet)
                settlingIou = null
                onShowMessage("Settled IOU with ${target.personName}")
            }
        )
    }
}

@Composable
fun MoneyScreenContent(
    uiState: MoneyUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (MoneyTransaction) -> Unit,
    onEditOverallBudget: () -> Unit,
    onAddCategoryBudget: () -> Unit,
    onEditCategoryBudget: (ExpenseCategory) -> Unit,
    onAddBill: () -> Unit,
    onEditBill: (RecurringBill) -> Unit,
    onPayBill: (RecurringBill) -> Unit,
    onAddIou: () -> Unit,
    onEditIou: (IouEntry) -> Unit,
    onRequestSettleIou: (IouEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var iouFilterSettled by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Money",
            style = MaterialTheme.typography.headlineMedium
        )

        KansoSegmentedRow(
            options = listOf("Log", "Budgets", "Bills", "IOUs"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )

        when (selectedTab) {
            0 -> LogTabContent(
                uiState = uiState,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onAddTransaction = onAddTransaction,
                onEditTransaction = onEditTransaction
            )

            1 -> BudgetsTabContent(
                uiState = uiState,
                onEditOverallBudget = onEditOverallBudget,
                onAddCategoryBudget = onAddCategoryBudget,
                onEditCategoryBudget = onEditCategoryBudget
            )

            2 -> BillsTabContent(
                uiState = uiState,
                onAddBill = onAddBill,
                onEditBill = onEditBill,
                onPayBill = onPayBill
            )

            else -> IousTabContent(
                uiState = uiState,
                showSettled = iouFilterSettled,
                onToggleShowSettled = { iouFilterSettled = it },
                onAddIou = onAddIou,
                onEditIou = onEditIou,
                onRequestSettleIou = onRequestSettleIou
            )
        }
    }
}

@Composable
private fun LogTabContent(
    uiState: MoneyUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (MoneyTransaction) -> Unit
) {
    val topCategories = remember(uiState.summary.byCategory) {
        uiState.summary.byCategory.entries
            .sortedByDescending { it.value }
            .take(5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous Month")
                }
                Text(
                    text = uiState.selectedMonthLabel,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Next Month")
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetric(
                        label = "Expense",
                        value = CurrencyFormatter.format(uiState.summary.expenseCents, uiState.currency),
                        color = expenseSemanticColor()
                    )
                    SummaryMetric(
                        label = "Income",
                        value = CurrencyFormatter.format(uiState.summary.incomeCents, uiState.currency),
                        color = incomeSemanticColor()
                    )
                    SummaryMetric(
                        label = "Net",
                        value = CurrencyFormatter.format(uiState.summary.netCents, uiState.currency, showSign = true),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (topCategories.isNotEmpty() && uiState.summary.expenseCents > 0L) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Top Categories",
                            style = MaterialTheme.typography.titleMedium
                        )
                        topCategories.forEach { (category, amount) ->
                            val ratio = (amount.toFloat() / uiState.summary.expenseCents.toFloat()).coerceIn(0f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = category.label,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${CurrencyFormatter.format(amount, uiState.currency)} (${(ratio * 100).toInt()}%)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Transactions",
                actionLabel = "Add",
                onAction = onAddTransaction
            )
        }

        if (uiState.transactions.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "No entries in ${uiState.selectedMonthLabel}",
                    subtitle = "Tap + or Add to record an expense or income in seconds.",
                    actionLabel = "Add Transaction",
                    onAction = onAddTransaction
                )
            }
        } else {
            items(uiState.transactions, key = { it.id }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    currency = uiState.currency,
                    onClick = { onEditTransaction(tx) }
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

@Composable
private fun TransactionRowCard(
    transaction: MoneyTransaction,
    currency: AppCurrency,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transaction.category.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifBlank { transaction.category.label },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transaction.category.label} • ${transaction.wallet.label} • ${
                        KansoDateFormatters.formatShortDate(transaction.timestampMillis)
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MoneyAmountBadge(
                amountCents = transaction.amountCents,
                currency = currency,
                type = transaction.type
            )
        }
    }
}

@Composable
private fun BudgetsTabContent(
    uiState: MoneyUiState,
    onEditOverallBudget: () -> Unit,
    onAddCategoryBudget: () -> Unit,
    onEditCategoryBudget: (ExpenseCategory) -> Unit
) {
    val warningColor = if (isSystemInDarkTheme()) WarningAmberDark else WarningAmber

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Overall Monthly Budget", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = onEditOverallBudget) {
                            Text(if (uiState.overallMonthlyBudgetCents > 0L) "Edit" else "Set")
                        }
                    }
                    if (uiState.overallMonthlyBudgetCents > 0L) {
                        val ratio = uiState.summary.expenseCents.toFloat() / uiState.overallMonthlyBudgetCents.toFloat()
                        val barColor = when {
                            ratio > 1f -> expenseSemanticColor()
                            ratio >= 0.8f -> warningColor
                            else -> MaterialTheme.colorScheme.primary
                        }
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${CurrencyFormatter.format(uiState.summary.expenseCents, uiState.currency)} of ${
                                CurrencyFormatter.format(uiState.overallMonthlyBudgetCents, uiState.currency)
                            } (${(ratio * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No monthly cap set yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Category Limits",
                actionLabel = "Add",
                onAction = onAddCategoryBudget
            )
        }

        if (uiState.budgetStatuses.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.PieChart,
                    title = "No category budgets",
                    subtitle = "Set monthly caps on Food, Transport, or Shopping to spot overspending early.",
                    actionLabel = "Add Category Budget",
                    onAction = onAddCategoryBudget
                )
            }
        } else {
            items(uiState.budgetStatuses, key = { it.category.name }) { status ->
                val color = when {
                    status.isOverBudget -> expenseSemanticColor()
                    status.isWarning -> warningColor
                    else -> MaterialTheme.colorScheme.primary
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onEditCategoryBudget(status.category) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(status.category.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${(status.ratio * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = color
                            )
                        }
                        LinearProgressIndicator(
                            progress = { status.ratio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${CurrencyFormatter.format(status.spentCents, uiState.currency)} / ${
                                CurrencyFormatter.format(status.limitCents, uiState.currency)
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillsTabContent(
    uiState: MoneyUiState,
    onAddBill: () -> Unit,
    onEditBill: (RecurringBill) -> Unit,
    onPayBill: (RecurringBill) -> Unit
) {
    val currentMonth = YearMonth.now()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurring Bills", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onAddBill) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add Bill")
                }
            }
        }

        if (uiState.bills.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "No recurring bills yet",
                    subtitle = "Track rent, internet, phone, or subscriptions and mark them paid in 1 tap.",
                    actionLabel = "Add Bill",
                    onAction = onAddBill
                )
            }
        } else {
            items(uiState.bills, key = { it.id }) { bill ->
                val isPaid = MoneyCalculations.isBillPaidForMonth(bill, currentMonth)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onEditBill(bill) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = if (!bill.isActive) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bill.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Day ${bill.dueDayOfMonth} • ${bill.frequency.label} • ${bill.wallet.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.format(bill.amountCents, uiState.currency),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        if (!bill.isActive) {
                            Text(
                                text = "Paused",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (isPaid) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = incomeSemanticColor().copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "Paid",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = incomeSemanticColor()
                                )
                            }
                        } else {
                            FilledTonalButton(onClick = { onPayBill(bill) }) {
                                Text("Mark Paid")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IousTabContent(
    uiState: MoneyUiState,
    showSettled: Boolean,
    onToggleShowSettled: (Boolean) -> Unit,
    onAddIou: () -> Unit,
    onEditIou: (IouEntry) -> Unit,
    onRequestSettleIou: (IouEntry) -> Unit
) {
    val displayed = if (showSettled) uiState.settledIous else uiState.openIous

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetric(
                        label = "Owes Me",
                        value = CurrencyFormatter.format(uiState.owesMeTotalCents, uiState.currency),
                        color = incomeSemanticColor()
                    )
                    SummaryMetric(
                        label = "I Owe",
                        value = CurrencyFormatter.format(uiState.iOweTotalCents, uiState.currency),
                        color = expenseSemanticColor()
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showSettled,
                        onClick = { onToggleShowSettled(false) },
                        label = { Text("Open (${uiState.openIous.size})") }
                    )
                    FilterChip(
                        selected = showSettled,
                        onClick = { onToggleShowSettled(true) },
                        label = { Text("Settled (${uiState.settledIous.size})") }
                    )
                }
                Button(onClick = onAddIou) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add")
                }
            }
        }

        if (displayed.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.Handshake,
                    title = if (showSettled) "No settled IOUs" else "No open IOUs",
                    subtitle = "Keep track of split dinners, loans, or money owed either way.",
                    actionLabel = if (!showSettled) "Add IOU" else null,
                    onAction = if (!showSettled) onAddIou else null
                )
            }
        } else {
            items(displayed, key = { it.id }) { iou ->
                val dueLabel = KansoDateFormatters.formatEpochDayLabel(iou.dueDateEpochDay)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onEditIou(iou) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = iou.personName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            val meta = buildString {
                                append(iou.direction.label)
                                if (iou.note.isNotBlank()) append(" • ${iou.note}")
                                if (dueLabel != null) append(" • Due $dueLabel")
                            }
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.format(iou.amountCents, uiState.currency),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = if (iou.direction == IouDirection.OWES_ME) {
                                    incomeSemanticColor()
                                } else {
                                    expenseSemanticColor()
                                }
                            )
                        }

                        if (!iou.isSettled) {
                            FilledTonalButton(onClick = { onRequestSettleIou(iou) }) {
                                Text("Settle")
                            }
                        } else {
                            Text(
                                text = "Settled",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun MoneyScreenPreview() {
    KansoTheme {
        MoneyScreenContent(
            uiState = MoneyUiState(
                selectedMonthLabel = "November 2026",
                currency = AppCurrency.IDR,
                summary = MoneyCalculations.MonthlySummary(
                    incomeCents = 8_000_000_00L,
                    expenseCents = 1_450_000_00L,
                    netCents = 6_550_000_00L,
                    byCategory = mapOf(
                        ExpenseCategory.FOOD to 850_000_00L,
                        ExpenseCategory.TRANSPORT to 600_000_00L
                    )
                ),
                transactions = listOf(
                    MoneyTransaction(
                        id = 1L,
                        type = TransactionType.EXPENSE,
                        amountCents = 45_000_00L,
                        category = ExpenseCategory.FOOD,
                        wallet = WalletType.EWALLET,
                        note = "Coffee & lunch",
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onAddTransaction = {},
            onEditTransaction = {},
            onEditOverallBudget = {},
            onAddCategoryBudget = {},
            onEditCategoryBudget = {},
            onAddBill = {},
            onEditBill = {},
            onPayBill = {},
            onAddIou = {},
            onEditIou = {},
            onRequestSettleIou = {}
        )
    }
}
