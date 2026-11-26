package com.kanso.feature.money

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.BudgetAnchorSource
import com.kanso.core.model.CategoryBudget
import com.kanso.core.model.CoachActionTarget
import com.kanso.core.model.CoachInsight
import com.kanso.core.model.CoachInsightTone
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.IouDirection
import com.kanso.core.model.IouEntry
import com.kanso.core.model.MoneyCalculations
import com.kanso.core.model.MoneyTransaction
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.SafeToSpendReport
import com.kanso.core.model.SpendPaceStatus
import com.kanso.core.model.TransactionType
import com.kanso.core.model.WalletBalanceSummary
import com.kanso.core.model.WalletType
import com.kanso.core.ui.components.DonutSlice
import com.kanso.core.ui.components.KansoAccentIconBadge
import com.kanso.core.ui.components.KansoAnimatedCurrencyText
import com.kanso.core.ui.components.KansoAnimatedIntText
import com.kanso.core.ui.components.KansoAnimatedProgressBar
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoCategoryDonutChart
import com.kanso.core.ui.components.KansoDateFormatters
import com.kanso.core.ui.components.KansoEmptyState
import com.kanso.core.ui.components.KansoHeroCard
import com.kanso.core.ui.components.KansoMotion
import com.kanso.core.ui.components.KansoPillBadge
import com.kanso.core.ui.components.KansoPrivacyEyeButton
import com.kanso.core.ui.components.KansoProgressRing
import com.kanso.core.ui.components.KansoSegmentedRow
import com.kanso.core.ui.components.KansoTonalButton
import com.kanso.core.ui.components.MoneyAmountBadge
import com.kanso.core.ui.components.SectionHeader
import com.kanso.core.ui.components.accentColor
import com.kanso.core.ui.components.expenseSemanticColor
import com.kanso.core.ui.components.icon
import com.kanso.core.ui.components.incomeSemanticColor
import com.kanso.core.ui.components.warningSemanticColor
import com.kanso.core.ui.theme.KansoTheme
import com.kanso.core.ui.theme.kansoColors
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MoneyRoute(
    onShowMessage: (String) -> Unit,
    onShowUndoMessage: (String, () -> Unit) -> Unit = { msg, _ -> onShowMessage(msg) },
    onBalancesVisibilityChanged: () -> Unit = {},
    viewModel: MoneyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var editingTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }
    var showNewTransactionDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var adjustingWallet by remember { mutableStateOf<WalletBalanceSummary?>(null) }

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
        onStartTransfer = { showTransferDialog = true },
        onAdjustWallet = { adjustingWallet = it },
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
        onRequestSettleIou = { settlingIou = it },
        onToggleTypeFilter = viewModel::toggleTypeFilter,
        onToggleWalletFilter = viewModel::toggleWalletFilter,
        onToggleCategoryFilter = viewModel::toggleCategoryFilter,
        onClearFilters = viewModel::clearTransactionFilters,
        onToggleHideBalances = {
            viewModel.toggleHideBalances()
            onBalancesVisibilityChanged()
        }
    )

    if (showNewTransactionDialog || showTransferDialog || editingTransaction != null) {
        val current = editingTransaction
        TransactionEditorDialog(
            currency = uiState.currency,
            initial = current,
            initialType = if (showTransferDialog) TransactionType.TRANSFER else TransactionType.EXPENSE,
            onDismiss = {
                showNewTransactionDialog = false
                showTransferDialog = false
                editingTransaction = null
            },
            onSave = { type, amountCents, category, wallet, toWallet, feeCents, note, timestampMillis ->
                viewModel.saveTransaction(
                    existingId = current?.id ?: 0L,
                    type = type,
                    amountCents = amountCents,
                    category = category,
                    wallet = wallet,
                    toWallet = toWallet,
                    transferFeeCents = feeCents,
                    note = note,
                    timestampMillis = timestampMillis
                )
                val savedMonth = YearMonth.from(
                    Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                )
                if (savedMonth != uiState.selectedMonth) {
                    val monthLabel = savedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                    onShowMessage("Saved to $monthLabel")
                } else if (current == null && type == TransactionType.TRANSFER) {
                    onShowMessage("Saved transfer")
                }
                showNewTransactionDialog = false
                showTransferDialog = false
                editingTransaction = null
            },
            onDelete = current?.let { tx ->
                {
                    viewModel.deleteTransaction(tx.id)
                    editingTransaction = null
                    onShowUndoMessage("Transaction deleted") {
                        viewModel.restoreTransaction(tx)
                    }
                }
            }
        )
    }

    adjustingWallet?.let { targetWallet ->
        WalletBalanceDialog(
            walletSummary = targetWallet,
            currency = uiState.currency,
            onDismiss = { adjustingWallet = null },
            onSaveTargetBalance = { desiredCents ->
                viewModel.adjustWalletCurrentBalance(targetWallet.wallet, desiredCents)
                adjustingWallet = null
                onShowMessage("${targetWallet.wallet.label} balance updated")
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
                    onShowUndoMessage("Budget removed") {
                        viewModel.restoreCategoryBudget(budget)
                    }
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
                    onShowUndoMessage("Bill deleted") {
                        viewModel.restoreRecurringBill(bill)
                    }
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
                    onShowUndoMessage("IOU deleted") {
                        viewModel.restoreIou(iou)
                    }
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
    onStartTransfer: () -> Unit = {},
    onAdjustWallet: (WalletBalanceSummary) -> Unit = {},
    onEditOverallBudget: () -> Unit,
    onAddCategoryBudget: () -> Unit,
    onEditCategoryBudget: (ExpenseCategory) -> Unit,
    onAddBill: () -> Unit,
    onEditBill: (RecurringBill) -> Unit,
    onPayBill: (RecurringBill) -> Unit,
    onAddIou: () -> Unit,
    onEditIou: (IouEntry) -> Unit,
    onRequestSettleIou: (IouEntry) -> Unit,
    onToggleTypeFilter: (TransactionType) -> Unit = {},
    onToggleWalletFilter: (WalletType) -> Unit = {},
    onToggleCategoryFilter: (ExpenseCategory) -> Unit = {},
    onClearFilters: () -> Unit = {},
    onToggleHideBalances: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var iouFilterSettled by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Money",
                style = MaterialTheme.typography.headlineMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KansoPrivacyEyeButton(
                    hidden = uiState.hideBalances,
                    onToggle = onToggleHideBalances,
                    size = 38.dp
                )
                MonthPillSelector(
                    label = uiState.selectedMonthLabel,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth
                )
            }
        }

        KansoSegmentedRow(
            options = listOf("Log", "Budgets", "Bills", "IOUs"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { KansoMotion.directionalTabTransition(targetState, initialState) },
            label = "moneyTab"
        ) { tab ->
            when (tab) {
                0 -> LogTabContent(
                    uiState = uiState,
                    onStartTransfer = onStartTransfer,
                    onAdjustWallet = onAdjustWallet,
                    onEditOverallBudget = onEditOverallBudget,
                    onAddTransaction = onAddTransaction,
                    onEditTransaction = onEditTransaction,
                    onToggleTypeFilter = onToggleTypeFilter,
                    onToggleWalletFilter = onToggleWalletFilter,
                    onToggleCategoryFilter = onToggleCategoryFilter,
                    onClearFilters = onClearFilters
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
}

@Composable
private fun MonthPillSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPrevious),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Previous Month",
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedContent(
                targetState = label,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "moneyMonthLabel"
            ) { currentLabel ->
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Next Month",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LogTabContent(
    uiState: MoneyUiState,
    onStartTransfer: () -> Unit,
    onAdjustWallet: (WalletBalanceSummary) -> Unit,
    onEditOverallBudget: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (MoneyTransaction) -> Unit,
    onToggleTypeFilter: (TransactionType) -> Unit,
    onToggleWalletFilter: (WalletType) -> Unit,
    onToggleCategoryFilter: (ExpenseCategory) -> Unit,
    onClearFilters: () -> Unit
) {
    val kansoColors = MaterialTheme.kansoColors
    val topCategories = remember(uiState.summary.byCategory) {
        uiState.summary.byCategory.entries
            .sortedByDescending { it.value }
            .take(5)
    }
    val slices = remember(topCategories) {
        topCategories.map { (cat, amount) ->
            DonutSlice(
                label = cat.label,
                value = amount.toFloat(),
                color = cat.accentColor()
            )
        }
    }
    val effectiveDailyGroups = remember(
        uiState.transactions,
        uiState.dailyGroups,
        uiState.transactionFilter
    ) {
        if (!uiState.transactionFilter.isActive && uiState.dailyGroups.isEmpty() && uiState.transactions.isNotEmpty()) {
            MoneyCalculations.groupTransactionsByDay(uiState.transactions)
        } else {
            uiState.dailyGroups
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val isPositive = uiState.summary.netCents >= 0L
            val statusBg by animateColorAsState(
                targetValue = (if (isPositive) Color(0xFF34D399) else Color(0xFFF87171)).copy(alpha = 0.18f),
                animationSpec = tween(250),
                label = "netBadgeBg"
            )
            val statusFg by animateColorAsState(
                targetValue = if (isPositive) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                animationSpec = tween(250),
                label = "netBadgeFg"
            )
            KansoHeroCard(glowColor = if (isPositive) kansoColors.moneyAccent else kansoColors.expense) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NET CASHFLOW",
                            style = MaterialTheme.typography.labelSmall,
                            color = kansoColors.heroOnSurfaceMuted
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusBg
                        ) {
                            Text(
                                text = if (isPositive) "Positive" else "Deficit",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = statusFg
                            )
                        }
                    }

                    KansoAnimatedCurrencyText(
                        amountCents = uiState.summary.netCents,
                        currency = uiState.currency,
                        showSign = true,
                        style = MaterialTheme.typography.displaySmall,
                        color = kansoColors.heroOnSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HeroCashPill(
                            label = "Spent",
                            amountCents = uiState.summary.expenseCents,
                            currency = uiState.currency,
                            dotColor = Color(0xFFF87171),
                            modifier = Modifier.weight(1f)
                        )
                        HeroCashPill(
                            label = "Income",
                            amountCents = uiState.summary.incomeCents,
                            currency = uiState.currency,
                            dotColor = Color(0xFF34D399),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (uiState.walletBalances.isNotEmpty()) {
            item {
                WalletsBentoCard(
                    wallets = uiState.walletBalances,
                    totalBalanceCents = uiState.totalWalletBalanceCents,
                    currency = uiState.currency,
                    hideBalances = uiState.hideBalances,
                    selectedWalletFilter = uiState.transactionFilter.wallet,
                    onToggleWalletFilter = onToggleWalletFilter,
                    onStartTransfer = onStartTransfer,
                    onAdjustWallet = onAdjustWallet
                )
            }
        }

        uiState.safeToSpend?.let { report ->
            item {
                MoneySmartCoachCard(
                    report = report,
                    currency = uiState.currency,
                    hideBalances = uiState.hideBalances,
                    insights = uiState.coachInsights,
                    onEditOverallBudget = onEditOverallBudget
                )
            }
        }

        if (topCategories.isNotEmpty() && uiState.summary.expenseCents > 0L) {
            item {
                KansoBentoCard(contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Spending Breakdown",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KansoCategoryDonutChart(
                                slices = slices,
                                size = 122.dp,
                                strokeWidth = 15.dp
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    KansoAnimatedIntText(
                                        value = uiState.summary.byCategory.size,
                                        format = { "$it" },
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "categories",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                topCategories.forEach { (category, amount) ->
                                    val pct = ((amount.toFloat() / uiState.summary.expenseCents.toFloat()) * 100f).toInt()
                                    val isCatSelected = uiState.transactionFilter.category == category
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isCatSelected) {
                                                    category.accentColor().copy(alpha = 0.14f)
                                                } else {
                                                    Color.Transparent
                                                }
                                            )
                                            .clickable { onToggleCategoryFilter(category) }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(category.accentColor())
                                            )
                                            Text(
                                                text = category.label,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isCatSelected) FontWeight.SemiBold else FontWeight.Normal
                                                ),
                                                color = if (isCatSelected) {
                                                    category.accentColor()
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "$pct% · ${
                                                CurrencyFormatter.formatOrMask(amount, uiState.currency, hidden = uiState.hideBalances)
                                            }",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isCatSelected) {
                                                category.accentColor()
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
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

        if (uiState.transactions.isNotEmpty() || uiState.transactionFilter.isActive) {
            item {
                TransactionFilterStrip(
                    transactions = uiState.transactions,
                    filter = uiState.transactionFilter,
                    filteredCount = uiState.filteredTransactions.size,
                    onToggleTypeFilter = onToggleTypeFilter,
                    onToggleWalletFilter = onToggleWalletFilter,
                    onToggleCategoryFilter = onToggleCategoryFilter,
                    onClearFilters = onClearFilters
                )
            }
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
        } else if (effectiveDailyGroups.isEmpty()) {
            item {
                KansoEmptyState(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "No matching transactions",
                    subtitle = "Clear or adjust the active type, wallet, or category filters.",
                    actionLabel = "Clear Filters",
                    onAction = onClearFilters
                )
            }
        } else {
            effectiveDailyGroups.forEach { group ->
                item(key = "day_${group.epochDay}") {
                    DailyTransactionGroupHeader(
                        group = group,
                        currency = uiState.currency,
                        hideBalances = uiState.hideBalances
                    )
                }
                items(group.transactions, key = { it.id }) { tx ->
                    TransactionRowCard(
                        transaction = tx,
                        currency = uiState.currency,
                        modifier = Modifier.animateItem(),
                        onClick = { onEditTransaction(tx) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionFilterStrip(
    transactions: List<MoneyTransaction>,
    filter: MoneyCalculations.TransactionFilter,
    filteredCount: Int,
    onToggleTypeFilter: (TransactionType) -> Unit,
    onToggleWalletFilter: (WalletType) -> Unit,
    onToggleCategoryFilter: (ExpenseCategory) -> Unit,
    onClearFilters: () -> Unit
) {
    val activeCategories = remember(transactions, filter.category) {
        (transactions.filter { it.type != TransactionType.TRANSFER }
            .map { it.category }
            .distinct() + listOfNotNull(filter.category))
            .distinct()
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (filter.isActive) {
            item(key = "filter_clear") {
                FilterChip(
                    selected = true,
                    onClick = onClearFilters,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear filters",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Clear ($filteredCount)") }
                )
            }
        }
        items(TransactionType.entries, key = { "type_${it.name}" }) { type ->
            FilterChip(
                selected = filter.type == type,
                onClick = { onToggleTypeFilter(type) },
                label = { Text(type.label) }
            )
        }
        items(WalletType.entries, key = { "wallet_${it.name}" }) { wallet ->
            FilterChip(
                selected = filter.wallet == wallet,
                onClick = { onToggleWalletFilter(wallet) },
                label = { Text(wallet.label) }
            )
        }
        items(activeCategories, key = { "cat_${it.name}" }) { category ->
            FilterChip(
                selected = filter.category == category,
                onClick = { onToggleCategoryFilter(category) },
                label = { Text(category.label) }
            )
        }
    }
}

@Composable
private fun DailyTransactionGroupHeader(
    group: MoneyCalculations.DailyTransactionGroup,
    currency: AppCurrency,
    hideBalances: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = KansoDateFormatters.formatDayGroupHeader(group.epochDay),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val (summaryText, summaryColor) = when {
            group.dayIncomeCents > 0L && group.dayExpenseCents == 0L ->
                "+${CurrencyFormatter.formatOrMask(group.dayIncomeCents, currency, hidden = hideBalances)}" to incomeSemanticColor()
            group.dayExpenseCents > 0L && group.dayIncomeCents == 0L ->
                "-${CurrencyFormatter.formatOrMask(group.dayExpenseCents, currency, hidden = hideBalances)}" to expenseSemanticColor()
            group.dayIncomeCents > 0L && group.dayExpenseCents > 0L ->
                CurrencyFormatter.formatOrMask(group.dayNetCents, currency, hidden = hideBalances, showSign = true) to
                    if (group.dayNetCents >= 0L) incomeSemanticColor() else expenseSemanticColor()
            else -> "Transfer" to MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = summaryText,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            ),
            color = summaryColor
        )
    }
}

@Composable
private fun WalletsBentoCard(
    wallets: List<WalletBalanceSummary>,
    totalBalanceCents: Long,
    currency: AppCurrency,
    hideBalances: Boolean,
    selectedWalletFilter: WalletType?,
    onToggleWalletFilter: (WalletType) -> Unit,
    onStartTransfer: () -> Unit,
    onAdjustWallet: (WalletBalanceSummary) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    KansoBentoCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Wallets",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total ${CurrencyFormatter.formatOrMask(totalBalanceCents, currency, hidden = hideBalances)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (totalBalanceCents < 0L) {
                            expenseSemanticColor()
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                KansoTonalButton(
                    text = "Transfer",
                    leadingIcon = Icons.Outlined.SwapHoriz,
                    size = KansoButtonSize.COMPACT,
                    onClick = onStartTransfer
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                wallets.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { summary ->
                            val isNegative = summary.currentBalanceCents < 0L
                            val isFiltered = selectedWalletFilter == summary.wallet
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onToggleWalletFilter(summary.wallet)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isFiltered) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                                border = if (isFiltered) {
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = summary.wallet.icon(),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = summary.wallet.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isFiltered) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .clickable { onAdjustWallet(summary) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = "Adjust ${summary.wallet.label} balance",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    if (isNegative) {
                                        Text(
                                            text = CurrencyFormatter.formatOrMask(summary.currentBalanceCents, currency, hidden = hideBalances),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = expenseSemanticColor(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        KansoAnimatedCurrencyText(
                                            amountCents = summary.currentBalanceCents,
                                            currency = currency,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoneySmartCoachCard(
    report: SafeToSpendReport,
    currency: AppCurrency,
    hideBalances: Boolean,
    insights: List<CoachInsight>,
    onEditOverallBudget: () -> Unit
) {
    val statusColor = when (report.paceStatus) {
        SpendPaceStatus.UNDER_PACE -> incomeSemanticColor()
        SpendPaceStatus.ON_TRACK -> MaterialTheme.kansoColors.moneyAccent
        SpendPaceStatus.WATCH_PACE -> warningSemanticColor()
        SpendPaceStatus.OVER_BUDGET -> expenseSemanticColor()
        else -> MaterialTheme.colorScheme.primary
    }

    KansoBentoCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KansoAccentIconBadge(
                        icon = Icons.Outlined.AutoAwesome,
                        accent = statusColor,
                        size = 34.dp,
                        iconSize = 18.dp,
                        cornerRadius = 10.dp
                    )
                    Text(
                        text = "Smart Coach",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                KansoPillBadge(
                    text = report.paceStatus.badgeLabel,
                    color = statusColor
                )
            }

            if (report.isCurrentMonth && report.hasAnchor) {
                val isExceeded = report.todayRemainingAllowanceCents < 0L
                val forecastColor = if (report.projectedMonthEndCents > report.anchorAmountCents) {
                    warningSemanticColor()
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                val anchorLabel = if (report.anchorSource == BudgetAnchorSource.MONTHLY_INCOME) {
                    "Income ${CurrencyFormatter.formatOrMask(report.anchorAmountCents, currency, hidden = hideBalances)}"
                } else {
                    "Cap ${CurrencyFormatter.formatOrMask(report.anchorAmountCents, currency, hidden = hideBalances)}"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "SAFE TODAY",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isExceeded) expenseSemanticColor() else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isExceeded) {
                                Text(
                                    text = "-${CurrencyFormatter.formatOrMask(-report.todayRemainingAllowanceCents, currency, hidden = hideBalances)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = expenseSemanticColor(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                KansoAnimatedCurrencyText(
                                    amountCents = report.todayRemainingAllowanceCents.coerceAtLeast(0L),
                                    currency = currency,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${CurrencyFormatter.formatOrMask(report.dailySafeAllowanceCents, currency, hidden = hideBalances)}/day · ${report.daysRemainingIncludingToday}d left",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "MONTH FORECAST",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            KansoAnimatedCurrencyText(
                                amountCents = report.projectedMonthEndCents,
                                currency = currency,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = forecastColor
                            )
                            Text(
                                text = anchorLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else if (report.isPastMonth) {
                Text(
                    text = "Daily avg ${CurrencyFormatter.formatOrMask(report.averageDailySpendCents, currency, hidden = hideBalances)}/day across ${report.daysElapsed} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (insights.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    insights.forEach { insight ->
                        val toneColor = when (insight.tone) {
                            CoachInsightTone.POSITIVE -> incomeSemanticColor()
                            CoachInsightTone.NEUTRAL -> MaterialTheme.colorScheme.primary
                            CoachInsightTone.WARNING -> warningSemanticColor()
                            CoachInsightTone.CRITICAL -> expenseSemanticColor()
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(toneColor)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = insight.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = insight.body,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (insight.actionTarget == CoachActionTarget.OPEN_MONEY_BUDGETS ||
                                (!report.hasAnchor && report.isCurrentMonth && insight.actionLabel != null)
                            ) {
                                KansoTonalButton(
                                    text = insight.actionLabel ?: "Set Cap",
                                    size = KansoButtonSize.COMPACT,
                                    onClick = onEditOverallBudget
                                )
                            }
                        }
                    }
                }
            } else if (!report.hasAnchor && report.isCurrentMonth) {
                KansoTonalButton(
                    text = "Set Monthly Budget",
                    size = KansoButtonSize.COMPACT,
                    onClick = onEditOverallBudget
                )
            }
        }
    }
}

@Composable
private fun HeroCashPill(
    label: String,
    amountCents: Long,
    currency: AppCurrency,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.kansoColors.heroOnSurfaceMuted
                )
            }
            KansoAnimatedCurrencyText(
                amountCents = amountCents,
                currency = currency,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.kansoColors.heroOnSurface
            )
        }
    }
}

@Composable
private fun TransactionRowCard(
    transaction: MoneyTransaction,
    currency: AppCurrency,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    KansoBentoCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isTransfer = transaction.type == TransactionType.TRANSFER
            val transferRoute = "${transaction.wallet.label} → ${(transaction.toWallet ?: transaction.wallet).label}"
            KansoAccentIconBadge(
                icon = if (isTransfer) Icons.Outlined.SwapHoriz else transaction.category.icon(),
                accent = if (isTransfer) MaterialTheme.colorScheme.primary else transaction.category.accentColor()
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = transaction.note.ifBlank { if (isTransfer) transferRoute else transaction.category.label },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isTransfer) {
                        "Transfer · $transferRoute · ${
                            KansoDateFormatters.formatTime(transaction.timestampMillis)
                        }"
                    } else {
                        "${transaction.category.label} · ${transaction.wallet.label} · ${
                            KansoDateFormatters.formatTime(transaction.timestampMillis)
                        }"
                    },
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
    val warningColor = warningSemanticColor()
    val moneyAccent = MaterialTheme.kansoColors.moneyAccent

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            KansoBentoCard(contentPadding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (uiState.overallMonthlyBudgetCents > 0L) {
                        val ratio = uiState.summary.expenseCents.toFloat() / uiState.overallMonthlyBudgetCents.toFloat()
                        val ringColor = when {
                            ratio > 1f -> expenseSemanticColor()
                            ratio >= 0.8f -> warningColor
                            else -> moneyAccent
                        }
                        KansoProgressRing(
                            progress = ratio,
                            color = ringColor,
                            size = 72.dp,
                            strokeWidth = 7.dp
                        ) {
                            KansoAnimatedIntText(
                                value = (ratio * 100).toInt(),
                                format = { "$it%" },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = ringColor
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Overall Monthly Budget", style = MaterialTheme.typography.titleMedium)
                        if (uiState.overallMonthlyBudgetCents > 0L) {
                            Text(
                                text = "${CurrencyFormatter.formatOrMask(uiState.summary.expenseCents, uiState.currency, hidden = uiState.hideBalances)} spent",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Limit ${CurrencyFormatter.formatOrMask(uiState.overallMonthlyBudgetCents, uiState.currency, hidden = uiState.hideBalances)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val coachReport = uiState.safeToSpend
                            if (coachReport != null && coachReport.isCurrentMonth) {
                                Text(
                                    text = "Safe today: ${
                                        CurrencyFormatter.formatOrMask(
                                            coachReport.todayRemainingAllowanceCents.coerceAtLeast(0L),
                                            uiState.currency,
                                            hidden = uiState.hideBalances
                                        )
                                    } · Forecast ${
                                        CurrencyFormatter.formatOrMask(
                                            coachReport.projectedMonthEndCents,
                                            uiState.currency,
                                            hidden = uiState.hideBalances
                                        )
                                    }",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Text(
                                text = "Set a monthly cap to track pace.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    KansoTonalButton(
                        text = if (uiState.overallMonthlyBudgetCents > 0L) "Edit" else "Set",
                        size = KansoButtonSize.COMPACT,
                        onClick = onEditOverallBudget
                    )
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
                val statusColor = when {
                    status.isOverBudget -> expenseSemanticColor()
                    status.isWarning -> warningColor
                    else -> status.category.accentColor()
                }
                KansoBentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    onClick = { onEditCategoryBudget(status.category) },
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                KansoAccentIconBadge(
                                    icon = status.category.icon(),
                                    accent = status.category.accentColor(),
                                    size = 36.dp,
                                    iconSize = 18.dp,
                                    cornerRadius = 11.dp
                                )
                                Column {
                                    Text(status.category.label, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${CurrencyFormatter.formatOrMask(status.spentCents, uiState.currency, hidden = uiState.hideBalances)} / ${
                                            CurrencyFormatter.formatOrMask(status.limitCents, uiState.currency, hidden = uiState.hideBalances)
                                        }",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            KansoPillBadge(
                                text = "${(status.ratio * 100).toInt()}%",
                                color = statusColor
                            )
                        }
                        KansoAnimatedProgressBar(
                            progress = status.ratio,
                            height = 7.dp,
                            color = statusColor
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
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurring Bills", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                KansoTonalButton(
                    text = "Add Bill",
                    leadingIcon = Icons.Outlined.Add,
                    size = KansoButtonSize.COMPACT,
                    onClick = onAddBill
                )
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
                KansoBentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    onClick = { onEditBill(bill) },
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KansoAccentIconBadge(
                            icon = bill.category.icon(),
                            accent = bill.category.accentColor()
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bill.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Day ${bill.dueDayOfMonth} · ${bill.frequency.label} · ${bill.wallet.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = CurrencyFormatter.formatOrMask(bill.amountCents, uiState.currency, hidden = uiState.hideBalances),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }

                        if (!bill.isActive) {
                            KansoPillBadge(text = "Paused", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (isPaid) {
                            KansoPillBadge(text = "Paid", color = incomeSemanticColor())
                        } else {
                            KansoTonalButton(
                                text = "Mark Paid",
                                size = KansoButtonSize.COMPACT,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPayBill(bill)
                                }
                            )
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
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KansoBentoCard(
                    modifier = Modifier.weight(1f),
                    containerColor = incomeSemanticColor().copy(alpha = 0.08f),
                    borderColor = incomeSemanticColor().copy(alpha = 0.25f),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text(
                        text = "OWES ME",
                        style = MaterialTheme.typography.labelSmall,
                        color = incomeSemanticColor()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    KansoAnimatedCurrencyText(
                        amountCents = uiState.owesMeTotalCents,
                        currency = uiState.currency,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = incomeSemanticColor()
                    )
                }
                KansoBentoCard(
                    modifier = Modifier.weight(1f),
                    containerColor = expenseSemanticColor().copy(alpha = 0.08f),
                    borderColor = expenseSemanticColor().copy(alpha = 0.25f),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text(
                        text = "I OWE",
                        style = MaterialTheme.typography.labelSmall,
                        color = expenseSemanticColor()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    KansoAnimatedCurrencyText(
                        amountCents = uiState.iOweTotalCents,
                        currency = uiState.currency,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                KansoTonalButton(
                    text = "Add",
                    leadingIcon = Icons.Outlined.Add,
                    size = KansoButtonSize.COMPACT,
                    onClick = onAddIou
                )
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
                val accent = if (iou.direction == IouDirection.OWES_ME) {
                    incomeSemanticColor()
                } else {
                    expenseSemanticColor()
                }
                KansoBentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    onClick = { onEditIou(iou) },
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                if (iou.note.isNotBlank()) append(" · ${iou.note}")
                                if (dueLabel != null) append(" · Due $dueLabel")
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
                                text = CurrencyFormatter.formatOrMask(iou.amountCents, uiState.currency, hidden = uiState.hideBalances),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = accent
                            )
                        }

                        if (!iou.isSettled) {
                            KansoTonalButton(
                                text = "Settle",
                                size = KansoButtonSize.COMPACT,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onRequestSettleIou(iou)
                                }
                            )
                        } else {
                            KansoPillBadge(text = "Settled", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
