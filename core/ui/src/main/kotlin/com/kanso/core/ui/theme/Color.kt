package com.kanso.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light palette — Warm-cool Alabaster canvas + pure white crisp Bento cards
val KansoTealLight = Color(0xFF0F766E)
val KansoOnTealLight = Color(0xFFFFFFFF)
val KansoTealContainerLight = Color(0xFFCCFBF1)
val KansoOnTealContainerLight = Color(0xFF0B4F4A)

val KansoAmberLight = Color(0xFFD97706)
val KansoOnAmberLight = Color(0xFFFFFFFF)
val KansoAmberContainerLight = Color(0xFFFEF3C7)
val KansoOnAmberContainerLight = Color(0xFF78350F)

val KansoBackgroundLight = Color(0xFFF3F4F6)
val KansoSurfaceLight = Color(0xFFFFFFFF)
val KansoSurfaceVariantLight = Color(0xFFEAECEF)
val KansoOnSurfaceLight = Color(0xFF0F1419)
val KansoOnSurfaceVariantLight = Color(0xFF5B6573)
val KansoOutlineLight = Color(0xFFCBD2DC)
val KansoOutlineVariantLight = Color(0xFFE2E6EC)

// Dark palette — Deep Obsidian canvas + Graphite Bento cards
val KansoTealDark = Color(0xFF2DD4BF)
val KansoOnTealDark = Color(0xFF042F2E)
val KansoTealContainerDark = Color(0xFF113B38)
val KansoOnTealContainerDark = Color(0xFF99F6E4)

val KansoAmberDark = Color(0xFFFBBF24)
val KansoOnAmberDark = Color(0xFF451A03)
val KansoAmberContainerDark = Color(0xFF3A280E)
val KansoOnAmberContainerDark = Color(0xFFFDE68A)

val KansoBackgroundDark = Color(0xFF0B0E14)
val KansoSurfaceDark = Color(0xFF141923)
val KansoSurfaceVariantDark = Color(0xFF1E2533)
val KansoOnSurfaceDark = Color(0xFFF1F5F9)
val KansoOnSurfaceVariantDark = Color(0xFF94A3B8)
val KansoOutlineDark = Color(0xFF334155)
val KansoOutlineVariantDark = Color(0xFF232B3A)

// Semantic tokens
val IncomeGreen = Color(0xFF10B981)
val IncomeGreenDark = Color(0xFF34D399)
val ExpenseCoral = Color(0xFFEF4444)
val ExpenseCoralDark = Color(0xFFF87171)
val WarningAmber = Color(0xFFF59E0B)
val WarningAmberDark = Color(0xFFFBBF24)

@Immutable
data class KansoExtendedColors(
    val isDark: Boolean,
    val income: Color,
    val expense: Color,
    val warning: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroGlow: Color,
    val heroOnSurface: Color,
    val heroOnSurfaceMuted: Color,
    val heroBorder: Color,
    val moneyAccent: Color,
    val brainAccent: Color,
    val habitsAccent: Color,
    val hydrationAccent: Color,
    val lifeAdminAccent: Color,
    val utilitiesAccent: Color,
    val settingsAccent: Color
)

val LightKansoExtendedColors = KansoExtendedColors(
    isDark = false,
    income = IncomeGreen,
    expense = ExpenseCoral,
    warning = WarningAmber,
    heroStart = Color(0xFF0F172A),
    heroEnd = Color(0xFF132A2E),
    heroGlow = Color(0xFF14B8A6),
    heroOnSurface = Color(0xFFF8FAFC),
    heroOnSurfaceMuted = Color(0xFFCBD5E1),
    heroBorder = Color(0xFF1E293B),
    moneyAccent = Color(0xFF059669),
    brainAccent = Color(0xFF6366F1),
    habitsAccent = Color(0xFFF43F5E),
    hydrationAccent = Color(0xFF0EA5E9),
    lifeAdminAccent = Color(0xFFD97706),
    utilitiesAccent = Color(0xFF06B6D4),
    settingsAccent = Color(0xFF64748B)
)

val DarkKansoExtendedColors = KansoExtendedColors(
    isDark = true,
    income = IncomeGreenDark,
    expense = ExpenseCoralDark,
    warning = WarningAmberDark,
    heroStart = Color(0xFF111827),
    heroEnd = Color(0xFF0D2426),
    heroGlow = Color(0xFF2DD4BF),
    heroOnSurface = Color(0xFFF8FAFC),
    heroOnSurfaceMuted = Color(0xFF94A3B8),
    heroBorder = Color(0xFF233240),
    moneyAccent = Color(0xFF34D399),
    brainAccent = Color(0xFF818CF8),
    habitsAccent = Color(0xFFFB7185),
    hydrationAccent = Color(0xFF38BDF8),
    lifeAdminAccent = Color(0xFFFBBF24),
    utilitiesAccent = Color(0xFF22D3EE),
    settingsAccent = Color(0xFF94A3B8)
)

val LocalKansoColors = staticCompositionLocalOf { LightKansoExtendedColors }
