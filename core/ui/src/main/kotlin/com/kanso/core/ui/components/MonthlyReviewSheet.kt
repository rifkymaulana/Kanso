package com.kanso.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.MonthlyReviewCalculations
import com.kanso.core.model.MonthlyReviewReport
import com.kanso.core.ui.theme.kansoColors
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MonthlyReviewSheet(
    report: MonthlyReviewReport,
    currency: AppCurrency,
    hideBalances: Boolean,
    onDismiss: () -> Unit,
    onSaveToNotes: (title: String, content: String) -> Unit,
    onCopySummary: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kansoColors = MaterialTheme.kansoColors
    val incomeColor = incomeSemanticColor()
    val expenseColor = expenseSemanticColor()
    val netPositive = report.netSavingsCents >= 0L

    val noteTitle = "Monthly Review · ${report.monthLabel}"
    val shareableText = remember(report, currency, hideBalances) {
        MonthlyReviewCalculations.formatShareableSummary(
            report = report,
            currency = currency,
            hideBalances = hideBalances
        )
    }
    val plainNoteContent = remember(report, currency) {
        MonthlyReviewCalculations.formatShareableSummary(
            report = report,
            currency = currency,
            hideBalances = false
        )
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = kansoColors.moneyAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Monthly Review",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                KansoPillBadge(
                    text = report.monthLabel,
                    color = kansoColors.moneyAccent
                )
            }

            KansoHeroCard(
                glowColor = if (netPositive) kansoColors.moneyAccent else kansoColors.expense,
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "NET CASHFLOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = kansoColors.heroOnSurfaceMuted
                    )
                    KansoAnimatedCurrencyText(
                        amountCents = report.netSavingsCents,
                        currency = currency,
                        showSign = true,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = kansoColors.heroOnSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = kansoColors.heroOnSurfaceMuted
                                )
                                Text(
                                    text = CurrencyFormatter.formatOrMask(
                                        report.incomeCents,
                                        currency,
                                        hidden = hideBalances
                                    ),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Spent (${report.transactionCount} tx)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = kansoColors.heroOnSurfaceMuted
                                )
                                Text(
                                    text = CurrencyFormatter.formatOrMask(
                                        report.expenseCents,
                                        currency,
                                        hidden = hideBalances
                                    ),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        report.savingsRatePercent?.let { rate ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = (if (rate >= 0) Color(0xFF34D399) else Color(0xFFF87171)).copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = if (rate >= 0) "$rate% saved" else "${-rate}% overspent",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = kansoColors.heroOnSurface
                                )
                            }
                        }
                        report.expenseDeltaPercent?.let { delta ->
                            val sign = if (delta > 0) "+" else ""
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "$sign$delta% vs prev month",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = kansoColors.heroOnSurface
                                )
                            }
                        }
                    }
                }
            }

            if (report.topCategories.isNotEmpty()) {
                KansoBentoCard(contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Top Spending Categories",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        report.topCategories.forEach { highlight ->
                            val catAccent = highlight.category.accentColor()
                            val pct = (highlight.shareRatio * 100f).roundToInt()
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(catAccent.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = highlight.category.icon(),
                                                contentDescription = null,
                                                tint = catAccent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = "${highlight.category.label} · $pct%",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                        )
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatOrMask(
                                            highlight.amountCents,
                                            currency,
                                            hidden = hideBalances
                                        ),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = expenseColor
                                    )
                                }
                                KansoAnimatedProgressBar(
                                    progress = highlight.shareRatio,
                                    height = 6.dp,
                                    color = catAccent
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReviewMetricTile(
                        icon = Icons.Outlined.LocalFireDepartment,
                        accent = kansoColors.habitsAccent,
                        title = "Habits",
                        value = report.habitCompletionPercent?.let { "$it%" } ?: "—",
                        subtitle = report.topHabitName?.let { "$it (${report.topHabitCompletions}x)" }
                            ?: "${report.activeHabitsCount} active",
                        modifier = Modifier.weight(1f)
                    )
                    ReviewMetricTile(
                        icon = Icons.Outlined.WaterDrop,
                        accent = kansoColors.hydrationAccent,
                        title = "Hydration",
                        value = "${report.avgWaterMl} ml",
                        subtitle = "${report.waterGoalHitDays} goal day(s)",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val sleepText = report.avgSleepHours?.let {
                        String.format(Locale.US, "%.1fh", it)
                    } ?: "—"
                    val moodSub = report.avgMood?.let {
                        String.format(Locale.US, "Avg mood %.1f / 5", it)
                    } ?: "No mood logged"
                    ReviewMetricTile(
                        icon = Icons.Outlined.Bedtime,
                        accent = MaterialTheme.colorScheme.primary,
                        title = "Sleep & Mood",
                        value = sleepText,
                        subtitle = moodSub,
                        modifier = Modifier.weight(1f)
                    )
                    ReviewMetricTile(
                        icon = Icons.Outlined.TaskAlt,
                        accent = kansoColors.brainAccent,
                        title = "Tasks Done",
                        value = "${report.completedTasksCount}",
                        subtitle = "Completed this month",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KansoOutlinedButton(
                    text = "Copy",
                    leadingIcon = Icons.Outlined.ContentCopy,
                    size = KansoButtonSize.LARGE,
                    modifier = Modifier.weight(0.42f),
                    onClick = { onCopySummary(shareableText) }
                )
                KansoPrimaryButton(
                    text = "Save to Notes",
                    leadingIcon = Icons.Outlined.Save,
                    size = KansoButtonSize.LARGE,
                    containerColor = incomeColor,
                    contentColor = Color.White,
                    modifier = Modifier.weight(0.58f),
                    onClick = {
                        onSaveToNotes(noteTitle, plainNoteContent)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ReviewMetricTile(
    icon: ImageVector,
    accent: Color,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    KansoBentoCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
