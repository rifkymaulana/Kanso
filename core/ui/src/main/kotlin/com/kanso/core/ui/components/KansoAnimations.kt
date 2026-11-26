package com.kanso.core.ui.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kanso.core.model.AppCurrency
import com.kanso.core.model.CurrencyFormatter
import com.kanso.core.ui.theme.LocalBalancesHidden
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

object KansoMotion {
    val SnappySpring = spring<Float>(
        dampingRatio = 0.74f,
        stiffness = Spring.StiffnessMedium
    )

    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val GentleSpring = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessLow
    )

    val SizeSpring = spring<IntSize>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMedium
    )

    val DpSpring = spring<Dp>(
        dampingRatio = 0.76f,
        stiffness = Spring.StiffnessMedium
    )

    fun directionalTabTransition(
        targetIndex: Int,
        initialIndex: Int
    ): ContentTransform {
        val forward = targetIndex >= initialIndex
        return ContentTransform(
            targetContentEnter = slideInHorizontally(
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                initialOffsetX = { fullWidth -> if (forward) fullWidth / 7 else -fullWidth / 7 }
            ) + fadeIn(animationSpec = tween(durationMillis = 220)),
            initialContentExit = slideOutHorizontally(
                animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing),
                targetOffsetX = { fullWidth -> if (forward) -fullWidth / 10 else fullWidth / 10 }
            ) + fadeOut(animationSpec = tween(durationMillis = 150)),
            sizeTransform = SizeTransform(clip = false)
        )
    }
}

@Composable
fun Modifier.kansoPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = KansoMotion.SnappySpring,
        label = "kansoPressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun KansoStaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    delayStepMs: Int = 38,
    maxIndex: Int = 7,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val clampedIndex = index.coerceIn(0, maxIndex)

    LaunchedEffect(Unit) {
        if (clampedIndex > 0) {
            delay((clampedIndex * delayStepMs).toLong())
        }
        visible = true
    }

    val translationPx = with(LocalDensity.current) { 16.dp.toPx() }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "staggerAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else translationPx,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
        label = "staggerOffset"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.975f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
        label = "staggerScale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = offsetY
            this.scaleX = scale
            this.scaleY = scale
        }
    ) {
        content()
    }
}

@Composable
fun KansoAnimatedCurrencyText(
    amountCents: Long,
    currency: AppCurrency,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    showSign: Boolean = false,
    prefix: String = "",
    maxLines: Int = 1
) {
    val hidden = LocalBalancesHidden.current
    if (hidden) {
        Text(
            text = "$prefix${CurrencyFormatter.mask(currency)}",
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
        return
    }

    var displayedCents by remember { mutableLongStateOf(amountCents) }
    var previousTarget by remember { mutableLongStateOf(amountCents) }
    var isFirstComposition by remember { mutableStateOf(true) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(amountCents) {
        if (isFirstComposition) {
            isFirstComposition = false
            previousTarget = amountCents
            displayedCents = amountCents
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        val start = displayedCents
        previousTarget = amountCents
        if (start == amountCents) {
            displayedCents = amountCents
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
        ) {
            displayedCents = if (value >= 0.995f) {
                amountCents
            } else {
                start + ((amountCents - start).toDouble() * value.toDouble()).roundToLong()
            }
        }
        displayedCents = amountCents
    }

    val shownValue = if (previousTarget == amountCents) displayedCents else displayedCents
    Text(
        text = "$prefix${CurrencyFormatter.format(shownValue, currency, showSign = showSign)}",
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun KansoAnimatedIntText(
    value: Int,
    format: (Int) -> String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "kansoIntRoll"
    )
    Text(
        text = format(animatedValue),
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun KansoBounceOnChange(
    trigger: Any?,
    modifier: Modifier = Modifier,
    scalePeak: Float = 1.20f,
    content: @Composable () -> Unit
) {
    var firstRender by remember { mutableStateOf(true) }
    val bounce = remember { Animatable(1f) }

    LaunchedEffect(trigger) {
        if (firstRender) {
            firstRender = false
            return@LaunchedEffect
        }
        bounce.snapTo(1f)
        bounce.animateTo(
            targetValue = scalePeak,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessHigh)
        )
        bounce.animateTo(
            targetValue = 1f,
            animationSpec = KansoMotion.BouncySpring
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = bounce.value
            scaleY = bounce.value
        }
    ) {
        content()
    }
}
