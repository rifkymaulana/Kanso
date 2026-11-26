package com.kanso.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

data class GroupedBarItem(
    val label: String,
    val firstValue: Float,
    val secondValue: Float,
    val isCurrent: Boolean = false
)

data class SingleBarItem(
    val label: String,
    val value: Float,
    val metGoal: Boolean = false
)

data class SparklineItem(
    val label: String,
    val value: Float?
)

@Composable
fun KansoGroupedBarChart(
    items: List<GroupedBarItem>,
    firstColor: Color,
    secondColor: Color,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 152.dp
) {
    if (items.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val selectedBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    var targetReveal by remember(items) { mutableStateOf(0f) }
    LaunchedEffect(items) {
        targetReveal = 1f
    }
    val reveal by animateFloatAsState(
        targetValue = targetReveal,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "groupedBarReveal"
    )

    val maxValue = remember(items) {
        items.maxOfOrNull { maxOf(it.firstValue, it.secondValue, 0f) }?.coerceAtLeast(1f) ?: 1f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(items) {
                    detectTapGestures { offset ->
                        if (size.width <= 0) return@detectTapGestures
                        val idx = ((offset.x / size.width.toFloat()) * items.size)
                            .toInt()
                            .coerceIn(0, items.lastIndex)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectIndex(idx)
                    }
                }
        ) {
            val topPad = 8.dp.toPx()
            val bottomPad = 4.dp.toPx()
            val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))

            for (step in 0..2) {
                val y = topPad + (chartHeight * step / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (step < 2) dashEffect else null
                )
            }

            val groupWidth = size.width / items.size.toFloat()
            val barWidth = (groupWidth * 0.24f).coerceIn(6.dp.toPx(), 16.dp.toPx())
            val barGap = (groupWidth * 0.07f).coerceAtLeast(3.dp.toPx())
            val cornerPx = CornerRadius(4.dp.toPx(), 4.dp.toPx())

            items.forEachIndexed { index, item ->
                val groupLeft = index * groupWidth
                val centerX = groupLeft + groupWidth / 2f
                val isSelected = selectedIndex == index

                if (isSelected) {
                    drawRoundRect(
                        color = selectedBg,
                        topLeft = Offset(groupLeft + 3.dp.toPx(), 0f),
                        size = Size((groupWidth - 6.dp.toPx()).coerceAtLeast(0f), size.height),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                }

                val alphaScale = if (selectedIndex == null || isSelected) 1f else 0.55f
                val firstRatio = (item.firstValue.coerceAtLeast(0f) / maxValue).coerceIn(0f, 1f) * reveal
                val secondRatio = (item.secondValue.coerceAtLeast(0f) / maxValue).coerceIn(0f, 1f) * reveal

                val firstH = (chartHeight * firstRatio).let {
                    if (item.firstValue > 0f && reveal > 0.05f) maxOf(it, 3.dp.toPx()) else it
                }
                val secondH = (chartHeight * secondRatio).let {
                    if (item.secondValue > 0f && reveal > 0.05f) maxOf(it, 3.dp.toPx()) else it
                }

                val baseLineY = topPad + chartHeight
                if (firstH > 0f) {
                    drawRoundRect(
                        color = firstColor.copy(alpha = alphaScale),
                        topLeft = Offset(centerX - barGap / 2f - barWidth, baseLineY - firstH),
                        size = Size(barWidth, firstH),
                        cornerRadius = cornerPx
                    )
                }
                if (secondH > 0f) {
                    drawRoundRect(
                        color = secondColor.copy(alpha = alphaScale),
                        topLeft = Offset(centerX + barGap / 2f, baseLineY - secondH),
                        size = Size(barWidth, secondH),
                        cornerRadius = cornerPx
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEachIndexed { index, item ->
                val active = selectedIndex == index || (selectedIndex == null && item.isCurrent)
                Text(
                    text = item.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (active) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
fun KansoSingleBarChart(
    items: List<SingleBarItem>,
    barColor: Color,
    goalMetColor: Color,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    goalValue: Float? = null,
    height: Dp = 138.dp
) {
    if (items.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val goalLineColor = goalMetColor.copy(alpha = 0.72f)
    val selectedOutline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)

    var targetReveal by remember(items) { mutableStateOf(0f) }
    LaunchedEffect(items) {
        targetReveal = 1f
    }
    val reveal by animateFloatAsState(
        targetValue = targetReveal,
        animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
        label = "singleBarReveal"
    )

    val maxValue = remember(items, goalValue) {
        val dataMax = items.maxOfOrNull { it.value.coerceAtLeast(0f) } ?: 0f
        maxOf(dataMax, goalValue ?: 0f, 1f)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(items) {
                    detectTapGestures { offset ->
                        if (size.width <= 0) return@detectTapGestures
                        val idx = ((offset.x / size.width.toFloat()) * items.size)
                            .toInt()
                            .coerceIn(0, items.lastIndex)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectIndex(idx)
                    }
                }
        ) {
            val topPad = 8.dp.toPx()
            val bottomPad = 4.dp.toPx()
            val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val baseLineY = topPad + chartHeight
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))

            drawLine(
                color = gridColor,
                start = Offset(0f, baseLineY),
                end = Offset(size.width, baseLineY),
                strokeWidth = 1.dp.toPx()
            )

            if (goalValue != null && goalValue > 0f) {
                val goalRatio = (goalValue / maxValue).coerceIn(0f, 1f)
                val goalY = baseLineY - chartHeight * goalRatio
                drawLine(
                    color = goalLineColor,
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 1.4.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            val slotWidth = size.width / items.size.toFloat()
            val barWidth = (slotWidth * 0.62f).coerceIn(4.dp.toPx(), 18.dp.toPx())
            val cornerPx = CornerRadius((barWidth / 2f).coerceAtMost(5.dp.toPx()))

            items.forEachIndexed { index, item ->
                val ratio = (item.value.coerceAtLeast(0f) / maxValue).coerceIn(0f, 1f) * reveal
                val barH = if (item.value > 0f && reveal > 0.05f) {
                    maxOf(chartHeight * ratio, 3.dp.toPx())
                } else {
                    0f
                }
                val isSelected = selectedIndex == index
                val alphaScale = if (selectedIndex == null || isSelected) 1f else 0.48f
                val fill = if (item.metGoal) goalMetColor else barColor
                val left = index * slotWidth + (slotWidth - barWidth) / 2f

                if (barH > 0f) {
                    drawRoundRect(
                        color = fill.copy(alpha = alphaScale),
                        topLeft = Offset(left, baseLineY - barH),
                        size = Size(barWidth, barH),
                        cornerRadius = cornerPx
                    )
                    if (isSelected) {
                        drawRoundRect(
                            color = selectedOutline,
                            topLeft = Offset(left, baseLineY - barH),
                            size = Size(barWidth, barH),
                            cornerRadius = cornerPx,
                            style = Stroke(width = 1.4.dp.toPx())
                        )
                    }
                } else if (isSelected) {
                    drawCircle(
                        color = barColor,
                        radius = 3.dp.toPx(),
                        center = Offset(index * slotWidth + slotWidth / 2f, baseLineY - 4.dp.toPx())
                    )
                }
            }
        }

        SparseAxisLabelsRow(items = items.map { it.label }, selectedIndex = selectedIndex)
    }
}

@Composable
fun KansoAreaSparklineChart(
    items: List<SparklineItem>,
    lineColor: Color,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    zeroAnchored: Boolean = false,
    referenceValue: Float? = null,
    height: Dp = 128.dp
) {
    if (items.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val refColor = lineColor.copy(alpha = 0.45f)
    val dotCenterColor = MaterialTheme.colorScheme.surface

    val nonNullValues = remember(items) { items.mapNotNull { it.value } }
    if (nonNullValues.isEmpty()) return

    var targetReveal by remember(items) { mutableStateOf(0f) }
    LaunchedEffect(items) {
        targetReveal = 1f
    }
    val reveal by animateFloatAsState(
        targetValue = targetReveal,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "sparklineReveal"
    )

    val (minVal, maxVal) = remember(nonNullValues, zeroAnchored, referenceValue) {
        val rawMin = if (zeroAnchored) 0f else nonNullValues.min()
        val rawMax = maxOf(
            nonNullValues.max(),
            referenceValue ?: nonNullValues.max()
        )
        if (abs(rawMax - rawMin) < 0.0001f) {
            if (zeroAnchored) 0f to maxOf(rawMax, 1f)
            else (rawMin - 1f) to (rawMax + 1f)
        } else {
            val pad = (rawMax - rawMin) * if (zeroAnchored) 0.10f else 0.14f
            val low = if (zeroAnchored) 0f else (rawMin - pad)
            low to (rawMax + pad)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(items) {
                    detectTapGestures { offset ->
                        val validIndices = items.mapIndexedNotNull { i, pt -> if (pt.value != null) i else null }
                        if (validIndices.isEmpty() || size.width <= 0) return@detectTapGestures
                        val inset = 10.dp.toPx()
                        val usableWidth = (size.width - inset * 2f).coerceAtLeast(1f)
                        val nearestIdx = validIndices.minBy { idx ->
                            val x = if (items.size <= 1) {
                                size.width / 2f
                            } else {
                                inset + (idx.toFloat() / (items.size - 1).toFloat()) * usableWidth
                            }
                            abs(x - offset.x)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectIndex(nearestIdx)
                    }
                }
        ) {
            val inset = 10.dp.toPx()
            val topPad = 10.dp.toPx()
            val bottomPad = 8.dp.toPx()
            val usableWidth = (size.width - inset * 2f).coerceAtLeast(1f)
            val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val bottomY = topPad + chartHeight
            val range = (maxVal - minVal).coerceAtLeast(0.0001f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))

            drawLine(
                color = gridColor,
                start = Offset(0f, bottomY),
                end = Offset(size.width, bottomY),
                strokeWidth = 1.dp.toPx()
            )

            if (referenceValue != null && referenceValue in minVal..maxVal) {
                val refY = bottomY - ((referenceValue - minVal) / range) * chartHeight
                drawLine(
                    color = refColor,
                    start = Offset(0f, refY),
                    end = Offset(size.width, refY),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            val coords = items.mapIndexedNotNull { idx, pt ->
                val value = pt.value ?: return@mapIndexedNotNull null
                val x = if (items.size <= 1) {
                    size.width / 2f
                } else {
                    inset + (idx.toFloat() / (items.size - 1).toFloat()) * usableWidth
                }
                val normalizedY = ((value - minVal) / range).coerceIn(0f, 1f) * reveal
                val y = bottomY - normalizedY * chartHeight
                idx to Offset(x, y)
            }

            if (coords.size >= 2) {
                val linePath = Path().apply {
                    moveTo(coords.first().second.x, coords.first().second.y)
                    for (i in 1 until coords.size) {
                        val p0 = coords[i - 1].second
                        val p1 = coords[i].second
                        val midX = (p0.x + p1.x) / 2f
                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(coords.last().second.x, bottomY)
                    lineTo(coords.first().second.x, bottomY)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.24f * reveal),
                            Color.Transparent
                        ),
                        startY = topPad,
                        endY = bottomY
                    )
                )
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            val dotRadius = if (items.size <= 16) 3.4.dp.toPx() else 2.6.dp.toPx()
            coords.forEach { (idx, pt) ->
                val isSelected = selectedIndex == idx
                if (isSelected) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.35f),
                        start = Offset(pt.x, topPad),
                        end = Offset(pt.x, bottomY),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawCircle(
                        color = lineColor.copy(alpha = 0.22f),
                        radius = 7.dp.toPx(),
                        center = pt
                    )
                }
                drawCircle(
                    color = lineColor,
                    radius = if (isSelected) 4.8.dp.toPx() else dotRadius,
                    center = pt
                )
                drawCircle(
                    color = dotCenterColor,
                    radius = if (isSelected) 2.2.dp.toPx() else dotRadius * 0.45f,
                    center = pt
                )
            }
        }

        SparseAxisLabelsRow(items = items.map { it.label }, selectedIndex = selectedIndex)
    }
}

@Composable
private fun SparseAxisLabelsRow(
    items: List<String>,
    selectedIndex: Int?
) {
    if (items.isEmpty()) return
    val firstLabel = items.first()
    val midIdx = items.size / 2
    val midLabel = items[midIdx]
    val lastLabel = items.last()
    val selectedLabel = selectedIndex?.takeIf { it in items.indices }?.let { items[it] }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = firstLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (items.size >= 5) {
                Text(
                    text = selectedLabel ?: midLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selectedLabel != null) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selectedLabel != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (items.size >= 2) {
                Text(
                    text = lastLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
