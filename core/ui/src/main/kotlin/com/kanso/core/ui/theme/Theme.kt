package com.kanso.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
    outline = KansoOutlineLight
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
    outline = KansoOutlineDark
)

@Composable
fun KansoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KansoTypography,
        content = content
    )
}
