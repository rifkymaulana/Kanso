package com.kanso.core.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.kanso.core.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = KansoTealLight,
    onPrimary = KansoOnTealLight,
    primaryContainer = KansoTealContainerLight,
    onPrimaryContainer = KansoOnTealContainerLight,
    secondary = KansoAmberLight,
    onSecondary = KansoOnAmberLight,
    secondaryContainer = KansoAmberContainerLight,
    onSecondaryContainer = KansoOnAmberContainerLight,
    background = KansoBackgroundLight,
    onBackground = KansoOnSurfaceLight,
    surface = KansoSurfaceLight,
    onSurface = KansoOnSurfaceLight,
    surfaceVariant = KansoSurfaceVariantLight,
    onSurfaceVariant = KansoOnSurfaceVariantLight,
    surfaceContainerLowest = KansoSurfaceLight,
    surfaceContainerLow = KansoBackgroundLight,
    surfaceContainer = KansoSurfaceLight,
    surfaceContainerHigh = KansoSurfaceVariantLight,
    surfaceContainerHighest = Color(0xFFDDE2E8),
    outline = KansoOutlineLight,
    outlineVariant = KansoOutlineVariantLight,
    error = ExpenseCoral,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

private val DarkColorScheme = darkColorScheme(
    primary = KansoTealDark,
    onPrimary = KansoOnTealDark,
    primaryContainer = KansoTealContainerDark,
    onPrimaryContainer = KansoOnTealContainerDark,
    secondary = KansoAmberDark,
    onSecondary = KansoOnAmberDark,
    secondaryContainer = KansoAmberContainerDark,
    onSecondaryContainer = KansoOnAmberContainerDark,
    background = KansoBackgroundDark,
    onBackground = KansoOnSurfaceDark,
    surface = KansoSurfaceDark,
    onSurface = KansoOnSurfaceDark,
    surfaceVariant = KansoSurfaceVariantDark,
    onSurfaceVariant = KansoOnSurfaceVariantDark,
    surfaceContainerLowest = KansoBackgroundDark,
    surfaceContainerLow = Color(0xFF10151E),
    surfaceContainer = KansoSurfaceDark,
    surfaceContainerHigh = KansoSurfaceVariantDark,
    surfaceContainerHighest = Color(0xFF283144),
    outline = KansoOutlineDark,
    outlineVariant = KansoOutlineVariantDark,
    error = ExpenseCoralDark,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF4C1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

val LocalBalancesHidden = compositionLocalOf { false }

val MaterialTheme.kansoColors: KansoExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalKansoColors.current

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun KansoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = false,
    hideBalances: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkKansoExtendedColors else LightKansoExtendedColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalKansoColors provides extendedColors,
        LocalBalancesHidden provides hideBalances
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KansoTypography,
            content = content
        )
    }
}
