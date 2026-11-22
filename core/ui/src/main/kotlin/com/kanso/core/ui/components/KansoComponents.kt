package com.kanso.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.model.ExpenseCategory
import com.kanso.core.model.TransactionType
import com.kanso.core.model.WalletType
import com.kanso.core.ui.theme.kansoColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun incomeSemanticColor(): Color = MaterialTheme.kansoColors.income

@Composable
fun expenseSemanticColor(): Color = MaterialTheme.kansoColors.expense

@Composable
fun warningSemanticColor(): Color = MaterialTheme.kansoColors.warning

fun ExpenseCategory.accentColor(): Color = when (this) {
    ExpenseCategory.FOOD -> Color(0xFFF97316)
    ExpenseCategory.TRANSPORT -> Color(0xFF0EA5E9)
    ExpenseCategory.GROCERIES -> Color(0xFF10B981)
    ExpenseCategory.BILLS -> Color(0xFFF59E0B)
    ExpenseCategory.SHOPPING -> Color(0xFFEC4899)
    ExpenseCategory.HEALTH -> Color(0xFFEF4444)
    ExpenseCategory.ENTERTAINMENT -> Color(0xFF8B5CF6)
    ExpenseCategory.HOUSING -> Color(0xFF6366F1)
    ExpenseCategory.EDUCATION -> Color(0xFF14B8A6)
    ExpenseCategory.SALARY -> Color(0xFF059669)
    ExpenseCategory.FREELANCE -> Color(0xFF06B6D4)
    ExpenseCategory.GIFT -> Color(0xFFD946EF)
    ExpenseCategory.OTHER -> Color(0xFF64748B)
}

@Composable
fun KansoBentoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    cornerRadius: Dp = 22.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bentoScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(containerColor, shape)
            .border(1.dp, borderColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun KansoHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glowColor: Color = MaterialTheme.kansoColors.heroGlow,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val kansoColors = MaterialTheme.kansoColors
    val shape = RoundedCornerShape(26.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && pressed) 0.988f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "heroScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(kansoColors.heroStart, kansoColors.heroEnd)
                ),
                shape = shape
            )
            .border(1.dp, kansoColors.heroBorder, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.88f, size.height * 0.16f)
            val radius = size.maxDimension * 0.62f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.24f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun KansoProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 7.dp,
    trackColor: Color = color.copy(alpha = 0.16f),
    centerContent: @Composable BoxScope.() -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "ringProgress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
        }
        centerContent()
    }
}

data class DonutSlice(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun KansoCategoryDonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 124.dp,
    strokeWidth: Dp = 15.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    centerContent: @Composable BoxScope.() -> Unit = {}
) {
    val validSlices = remember(slices) { slices.filter { it.value > 0f } }
    val total = remember(validSlices) { validSlices.sumOf { it.value.toDouble() }.toFloat() }
    var targetReveal by remember(validSlices) { mutableStateOf(0f) }
    LaunchedEffect(validSlices) {
        targetReveal = 1f
    }
    val reveal by animateFloatAsState(
        targetValue = targetReveal,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "donutReveal"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            if (total > 0f && validSlices.isNotEmpty()) {
                if (validSlices.size == 1) {
                    drawArc(
                        color = validSlices.first().color,
                        startAngle = -90f,
                        sweepAngle = 360f * reveal,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                } else {
                    val gapDegrees = 5f
                    val totalGap = gapDegrees * validSlices.size
                    val usableDegrees = (360f - totalGap).coerceAtLeast(180f)
                    var startAngle = -90f
                    validSlices.forEach { slice ->
                        val rawSweep = (slice.value / total) * usableDegrees
                        val sweep = (rawSweep * reveal).coerceAtLeast(0.5f)
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                        startAngle += rawSweep + gapDegrees
                    }
                }
            }
        }
        centerContent()
    }
}

@Composable
fun KansoAnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "barProgress"
    )
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor, shape)
    ) {
        if (animated > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .background(color, shape)
            )
        }
    }
}

@Composable
fun KansoAccentIconBadge(
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    cornerRadius: Dp = 14.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(accent.copy(alpha = 0.14f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun KansoPillBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = color
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
fun KansoCheckCircle(
    checked: Boolean,
    accentColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val fill by animateColorAsState(
        targetValue = if (checked) accentColor else Color.Transparent,
        animationSpec = tween(200),
        label = "checkFill"
    )
    val border by animateColorAsState(
        targetValue = if (checked) accentColor else MaterialTheme.colorScheme.outline,
        animationSpec = tween(200),
        label = "checkBorder"
    )
    val bounce by animateFloatAsState(
        targetValue = if (checked) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkBounce"
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = bounce
                    scaleY = bounce
                }
                .clip(CircleShape)
                .background(fill, CircleShape)
                .border(1.75.dp, border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Done",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun KansoCompactEmptyHint(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    KansoBentoCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KansoAccentIconBadge(
                icon = icon,
                accent = accentColor,
                size = 38.dp,
                iconSize = 19.dp,
                cornerRadius = 12.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (actionLabel != null && onAction != null) {
                FilledTonalButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionLabel != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun KansoEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    KansoBentoCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Delete"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
fun KansoSegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val trackShape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = trackShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val pillColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = tween(180),
                    label = "segPillColor"
                )
                val pillTextColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(180),
                    label = "segTextColor"
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(index)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = pillColor,
                    shadowElevation = if (selected) 1.dp else 0.dp
                ) {
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp, horizontal = 6.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = pillTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun MoneyAmountBadge(
    amountCents: Long,
    currency: AppCurrency,
    type: TransactionType? = null,
    modifier: Modifier = Modifier
) {
    val color = when (type) {
        TransactionType.INCOME -> incomeSemanticColor()
        TransactionType.EXPENSE -> expenseSemanticColor()
        null -> MaterialTheme.colorScheme.onSurface
    }
    val prefix = when (type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        null -> ""
    }
    Text(
        text = "$prefix${CurrencyFormatter.format(amountCents, currency)}",
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum"
        ),
        color = color
    )
}

fun ExpenseCategory.icon(): ImageVector = when (this) {
    ExpenseCategory.FOOD -> Icons.Outlined.Restaurant
    ExpenseCategory.TRANSPORT -> Icons.Outlined.DirectionsCar
    ExpenseCategory.GROCERIES -> Icons.Outlined.ShoppingCart
    ExpenseCategory.BILLS -> Icons.Outlined.Bolt
    ExpenseCategory.SHOPPING -> Icons.Outlined.ShoppingBag
    ExpenseCategory.HEALTH -> Icons.Outlined.FavoriteBorder
    ExpenseCategory.ENTERTAINMENT -> Icons.Outlined.Movie
    ExpenseCategory.HOUSING -> Icons.Outlined.Home
    ExpenseCategory.EDUCATION -> Icons.Outlined.School
    ExpenseCategory.SALARY -> Icons.Outlined.AttachMoney
    ExpenseCategory.FREELANCE -> Icons.Outlined.LaptopMac
    ExpenseCategory.GIFT -> Icons.Outlined.CardGiftcard
    ExpenseCategory.OTHER -> Icons.Outlined.Category
}

fun WalletType.icon(): ImageVector = when (this) {
    WalletType.CASH -> Icons.Outlined.Payments
    WalletType.BANK -> Icons.Outlined.AccountBalance
    WalletType.EWALLET -> Icons.Outlined.Smartphone
    WalletType.CARD -> Icons.Outlined.CreditCard
}

object KansoDateFormatters {
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())

    fun formatShortDate(epochMillis: Long): String {
        val date = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(shortDateFormatter)
        }
    }

    fun formatEpochDayLabel(epochDay: Long?): String? {
        if (epochDay == null) return null
        val date = LocalDate.ofEpochDay(epochDay)
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(shortDateFormatter)
        }
    }

    fun formatFullDate(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).format(fullDateFormatter)
    }

    fun formatFullDateTime(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(dateTimeFormatter)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KansoDatePickerDialog(
    initialEpochDay: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialEpochDay) {
        LocalDate.ofEpochDay(initialEpochDay)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selected = state.selectedDateMillis
                if (selected != null) {
                    val epochDay = Instant.ofEpochMilli(selected)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                        .toEpochDay()
                    onSelect(epochDay)
                }
                onDismiss()
            }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = state)
    }
}
