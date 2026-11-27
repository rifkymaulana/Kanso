package com.kanso.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kanso.app.MainActivity
import com.kanso.app.R
import com.kanso.app.navigation.KansoUris
import com.kanso.app.navigation.createLaunchIntent
import com.kanso.core.database.dao.BrainDao
import com.kanso.core.database.dao.HabitsDao
import com.kanso.core.database.dao.LifeAdminDao
import com.kanso.core.database.dao.MoneyDao
import com.kanso.core.database.entity.toDomain
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.database.repository.HabitsRepository
import com.kanso.core.model.LifeAdminCalculations
import com.kanso.core.model.MoneyCalculations
import com.kanso.core.model.RecurringBill
import com.kanso.core.model.SmartCoachCalculations
import com.kanso.core.model.TransactionType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface KansoWidgetEntryPoint {
    fun moneyDao(): MoneyDao
    fun brainDao(): BrainDao
    fun habitsDao(): HabitsDao
    fun lifeAdminDao(): LifeAdminDao
    fun habitsRepository(): HabitsRepository
    fun preferencesRepository(): PreferencesRepository
}

class KansoTodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            KansoWidgetEntryPoint::class.java
        )

        val initialData = withContext(Dispatchers.IO) {
            loadInitialSnapshot(entryPoint)
        }
        val dataFlow = observeWidgetData(entryPoint)

        provideContent {
            GlanceTheme {
                val data by dataFlow.collectAsState(initial = initialData)
                KansoWidgetContent(context = context, data = data)
            }
        }
    }

    private suspend fun loadInitialSnapshot(entryPoint: KansoWidgetEntryPoint): WidgetDashboardData {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayEpoch = today.toEpochDay()
        val currentMonth = YearMonth.from(today)
        val monthStartMillis = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEndMillis = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val todayStartMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEndMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val prefs = entryPoint.preferencesRepository().getCurrentPreferences()
        val monthTxs = entryPoint.moneyDao().getAllTransactions()
            .filter { it.timestampMillis in monthStartMillis until monthEndMillis }
        val monthIncomeCents = monthTxs
            .filter { it.type == TransactionType.INCOME.name }
            .sumOf { it.amountCents }
        val monthExpenseCents = monthTxs
            .filter { it.type == TransactionType.EXPENSE.name }
            .sumOf { it.amountCents }
        val todayExpenseCents = monthTxs
            .filter {
                it.type == TransactionType.EXPENSE.name &&
                    it.timestampMillis in todayStartMillis until todayEndMillis
            }
            .sumOf { it.amountCents }

        val bills = entryPoint.moneyDao().getAllBills().map { it.toDomain() }
        val dueBillsCount = bills.count { MoneyCalculations.isBillDueSoon(it, today, withinDays = 5) }
        val dueSoonIousCount = entryPoint.moneyDao().getAllIous()
            .map { it.toDomain() }
            .count { MoneyCalculations.isIouDueSoon(it, todayEpoch, withinDays = 3) }

        val openTasksCount = entryPoint.brainDao().getAllTasks().count { !it.isCompleted }
        val activeHabits = entryPoint.habitsDao().getAllHabits().filter { !it.isArchived }
        val completedHabitIds = entryPoint.habitsDao().getAllCompletions()
            .filter { it.epochDay == todayEpoch }
            .map { it.habitId }
            .toSet()
        val healthLog = entryPoint.habitsDao().getHealthLog(todayEpoch)
        val expiringCount = entryPoint.lifeAdminDao().getAllExpiries()
            .map { it.toDomain() }
            .count { LifeAdminCalculations.needsAttention(it, todayEpoch) }
        val dueMaintenanceCount = LifeAdminCalculations.activeDueMaintenanceLogs(
            logs = entryPoint.lifeAdminDao().getAllMaintenanceLogs().map { it.toDomain() },
            todayEpochDay = todayEpoch,
            withinDays = 14
        ).size

        val safeToSpend = SmartCoachCalculations.computeSafeToSpend(
            targetMonth = currentMonth,
            today = today,
            monthlyBudgetCents = prefs.overallMonthlyBudgetCents,
            monthIncomeCents = monthIncomeCents,
            monthExpenseCents = monthExpenseCents,
            todayExpenseCents = todayExpenseCents,
            bills = bills
        )
        val hideAmounts = prefs.hideBalances || prefs.appLockEnabled
        val moneySummary = WidgetSummaryFormatter.formatMoneySection(
            report = safeToSpend,
            currency = prefs.currency,
            hideAmounts = hideAmounts
        )

        return WidgetDashboardData(
            dateLabel = today.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())),
            moneyLabel = moneySummary.label,
            moneyValue = moneySummary.value,
            moneySubtitle = moneySummary.subtitle,
            openTasksCount = openTasksCount,
            habitsDone = activeHabits.count { completedHabitIds.contains(it.id) },
            habitsTotal = activeHabits.size,
            waterMl = healthLog?.waterMl ?: 0,
            waterGoalMl = prefs.waterGoalMl,
            dueBillsCount = dueBillsCount,
            expiringCount = expiringCount,
            dueMaintenanceCount = dueMaintenanceCount,
            dueSoonIousCount = dueSoonIousCount
        )
    }

    private fun observeWidgetData(entryPoint: KansoWidgetEntryPoint): Flow<WidgetDashboardData> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayEpoch = today.toEpochDay()
        val currentMonth = YearMonth.from(today)
        val monthStartMillis = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEndMillis = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val todayStartMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEndMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val moneyFlow = combine(
            entryPoint.moneyDao().observeTransactionsInRange(monthStartMillis, monthEndMillis),
            entryPoint.moneyDao().observeBills(),
            entryPoint.moneyDao().observeIous(),
            entryPoint.brainDao().observeOpenTaskCount()
        ) { txs, bills, ious, openTasks ->
            val monthIncomeCents = txs
                .filter { it.type == TransactionType.INCOME.name }
                .sumOf { it.amountCents }
            val monthExpenseCents = txs
                .filter { it.type == TransactionType.EXPENSE.name }
                .sumOf { it.amountCents }
            val todayExpenseCents = txs
                .filter {
                    it.type == TransactionType.EXPENSE.name &&
                        it.timestampMillis in todayStartMillis until todayEndMillis
                }
                .sumOf { it.amountCents }
            val domainBills = bills.map { it.toDomain() }
            val dueBills = domainBills.count { MoneyCalculations.isBillDueSoon(it, today, withinDays = 5) }
            val dueSoonIous = ious
                .map { it.toDomain() }
                .count { MoneyCalculations.isIouDueSoon(it, todayEpoch, withinDays = 3) }
            MoneyTasksSnapshot(
                monthIncomeCents = monthIncomeCents,
                monthExpenseCents = monthExpenseCents,
                todayExpenseCents = todayExpenseCents,
                bills = domainBills,
                dueBillsCount = dueBills,
                dueSoonIousCount = dueSoonIous,
                openTasksCount = openTasks
            )
        }

        val habitsLifeFlow = combine(
            entryPoint.habitsDao().observeActiveHabits(),
            entryPoint.habitsDao().observeCompletionsSince(todayEpoch),
            entryPoint.habitsDao().observeHealthLog(todayEpoch),
            entryPoint.lifeAdminDao().observeExpiries(),
            entryPoint.lifeAdminDao().observeMaintenanceLogs()
        ) { habits, completions, healthLog, expiries, maintenanceLogs ->
            val completedIds = completions.filter { it.epochDay == todayEpoch }.map { it.habitId }.toSet()
            val expiringCount = expiries
                .map { it.toDomain() }
                .count { LifeAdminCalculations.needsAttention(it, todayEpoch) }
            val dueMaintenanceCount = LifeAdminCalculations.activeDueMaintenanceLogs(
                logs = maintenanceLogs.map { it.toDomain() },
                todayEpochDay = todayEpoch,
                withinDays = 14
            ).size
            HabitsLifeSnapshot(
                habitsDone = habits.count { completedIds.contains(it.id) },
                habitsTotal = habits.size,
                waterMl = healthLog?.waterMl ?: 0,
                expiringCount = expiringCount,
                dueMaintenanceCount = dueMaintenanceCount
            )
        }

        return combine(
            entryPoint.preferencesRepository().preferences,
            moneyFlow,
            habitsLifeFlow
        ) { prefs, money, hl ->
            val safeToSpend = SmartCoachCalculations.computeSafeToSpend(
                targetMonth = currentMonth,
                today = today,
                monthlyBudgetCents = prefs.overallMonthlyBudgetCents,
                monthIncomeCents = money.monthIncomeCents,
                monthExpenseCents = money.monthExpenseCents,
                todayExpenseCents = money.todayExpenseCents,
                bills = money.bills
            )
            val hideAmounts = prefs.hideBalances || prefs.appLockEnabled
            val moneySummary = WidgetSummaryFormatter.formatMoneySection(
                report = safeToSpend,
                currency = prefs.currency,
                hideAmounts = hideAmounts
            )
            WidgetDashboardData(
                dateLabel = today.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())),
                moneyLabel = moneySummary.label,
                moneyValue = moneySummary.value,
                moneySubtitle = moneySummary.subtitle,
                openTasksCount = money.openTasksCount,
                habitsDone = hl.habitsDone,
                habitsTotal = hl.habitsTotal,
                waterMl = hl.waterMl,
                waterGoalMl = prefs.waterGoalMl,
                dueBillsCount = money.dueBillsCount,
                expiringCount = hl.expiringCount,
                dueMaintenanceCount = hl.dueMaintenanceCount,
                dueSoonIousCount = money.dueSoonIousCount
            )
        }
    }

    private data class MoneyTasksSnapshot(
        val monthIncomeCents: Long,
        val monthExpenseCents: Long,
        val todayExpenseCents: Long,
        val bills: List<RecurringBill>,
        val dueBillsCount: Int,
        val dueSoonIousCount: Int,
        val openTasksCount: Int
    )

    private data class HabitsLifeSnapshot(
        val habitsDone: Int,
        val habitsTotal: Int,
        val waterMl: Int,
        val expiringCount: Int,
        val dueMaintenanceCount: Int
    )
}

@androidx.compose.runtime.Composable
private fun KansoWidgetContent(
    context: Context,
    data: WidgetDashboardData
) {
    val homeIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val attentionLine = WidgetSummaryFormatter.formatAttentionLine(
        dueBillsCount = data.dueBillsCount,
        expiringCount = data.expiringCount,
        dueMaintenanceCount = data.dueMaintenanceCount,
        dueSoonIousCount = data.dueSoonIousCount
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(14.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(homeIntent))
            ) {
                Text(
                    text = "Kanso",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = data.dateLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(12.dp)
                    .clickable(actionRunCallback<RefreshWidgetAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_refresh),
                    contentDescription = "Refresh widget",
                    modifier = GlanceModifier.size(18.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
                )
            }
        }

        if (attentionLine != null) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            val attentionUri = WidgetSummaryFormatter.resolveAttentionUri(
                dueBillsCount = data.dueBillsCount,
                expiringCount = data.expiringCount,
                dueMaintenanceCount = data.dueMaintenanceCount,
                dueSoonIousCount = data.dueSoonIousCount
            )
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(10.dp)
                    .clickable(actionStartActivity(createLaunchIntent(context, attentionUri)))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = attentionLine,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(createLaunchIntent(context, KansoUris.MONEY)))
            ) {
                Text(
                    text = data.moneyLabel,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
                )
                Text(
                    text = data.moneyValue,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = data.moneySubtitle,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
                )
            }

            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.End
            ) {
                Column(
                    modifier = GlanceModifier
                        .clickable(actionStartActivity(createLaunchIntent(context, KansoUris.HABITS))),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = WidgetSummaryFormatter.formatHabitsBadge(data.habitsDone, data.habitsTotal),
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = WidgetSummaryFormatter.formatWaterBadge(data.waterMl, data.waterGoalMl),
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
                Box(
                    modifier = GlanceModifier
                        .clickable(actionStartActivity(createLaunchIntent(context, KansoUris.BRAIN_TASKS)))
                ) {
                    Text(
                        text = WidgetSummaryFormatter.formatTasksBadge(data.openTasksCount),
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.defaultWeight())
        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetActionButton(
                text = "+ Expense",
                onClick = actionStartActivity(createLaunchIntent(context, KansoUris.QUICK_CAPTURE_EXPENSE)),
                backgroundColor = GlanceTheme.colors.primary,
                contentColor = GlanceTheme.colors.onPrimary,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            WidgetActionButton(
                text = "+ Note",
                onClick = actionStartActivity(createLaunchIntent(context, KansoUris.QUICK_CAPTURE_NOTE)),
                backgroundColor = GlanceTheme.colors.primaryContainer,
                contentColor = GlanceTheme.colors.onPrimaryContainer,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            WidgetActionButton(
                text = "+250 ml",
                onClick = actionRunCallback<AddWaterWidgetAction>(),
                backgroundColor = GlanceTheme.colors.primaryContainer,
                contentColor = GlanceTheme.colors.onPrimaryContainer,
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetActionButton(
    text: String,
    onClick: Action,
    backgroundColor: ColorProvider,
    contentColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .background(backgroundColor)
            .cornerRadius(12.dp)
            .clickable(onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
    }
}

class AddWaterWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            KansoWidgetEntryPoint::class.java
        )
        entryPoint.habitsRepository().adjustWater(LocalDate.now().toEpochDay(), 250)
        KansoTodayWidget().updateAll(context)
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        KansoTodayWidget().updateAll(context)
    }
}

class KansoTodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KansoTodayWidget()
}

object KansoWidgetUpdater {
    suspend fun updateAll(context: Context) {
        runCatching {
            KansoTodayWidget().updateAll(context)
        }
    }
}
