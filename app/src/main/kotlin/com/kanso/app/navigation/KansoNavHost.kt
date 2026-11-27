package com.kanso.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kanso.app.MainViewModel
import com.kanso.app.ui.hub.HubRoute
import com.kanso.app.ui.search.GlobalSearchRoute
import com.kanso.app.ui.settings.SettingsRoute
import com.kanso.app.update.AppUpdateState
import com.kanso.app.update.KansoUpdateBanner
import com.kanso.app.update.launchApkInstall
import com.kanso.app.widget.KansoWidgetUpdater
import com.kanso.core.ui.components.KansoMotion
import com.kanso.core.ui.components.QuickCaptureMode
import com.kanso.core.ui.components.QuickCaptureSheet
import com.kanso.feature.brain.BrainRoute
import com.kanso.feature.habits.HabitsRoute
import com.kanso.feature.lifeadmin.LifeAdminRoute
import com.kanso.feature.money.MoneyRoute
import com.kanso.feature.today.TodayRoute
import com.kanso.feature.utilities.UtilitiesRoute
import kotlinx.coroutines.launch

private object Routes {
    const val TODAY = "today"
    const val MONEY = "money"
    const val BRAIN_PATTERN = "brain?tab={tab}"
    const val HUB = "hub"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val HABITS = "habits"
    const val LIFEADMIN_PATTERN = "lifeadmin?tab={tab}"
    const val UTILITIES_PATTERN = "utilities?tab={tab}"

    fun brain(tab: Int = 0): String = "brain?tab=$tab"
    fun lifeAdmin(tab: Int = 0): String = "lifeadmin?tab=$tab"
    fun utilities(tab: Int = 0): String = "utilities?tab=$tab"

    fun isTopLevel(route: String?): Boolean {
        return route == TODAY || route == MONEY || route == BRAIN_PATTERN || route == HUB
    }
}

private data class TopLevelTab(
    val route: String,
    val baseRoute: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TopLevelTabs = listOf(
    TopLevelTab(Routes.TODAY, Routes.TODAY, "Today", Icons.Filled.Today, Icons.Outlined.Today),
    TopLevelTab(Routes.MONEY, Routes.MONEY, "Money", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    TopLevelTab(Routes.brain(0), "brain", "Brain", Icons.Filled.Psychology, Icons.Outlined.Psychology),
    TopLevelTab(Routes.HUB, Routes.HUB, "Hub", Icons.Filled.GridView, Icons.Outlined.GridView)
)

@Composable
fun KansoAppRoot(
    mainViewModel: MainViewModel,
    pendingLaunchCommand: LaunchCommand? = null,
    onConsumeLaunchCommand: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute == Routes.TODAY ||
        currentRoute == Routes.MONEY ||
        currentRoute == Routes.BRAIN_PATTERN ||
        currentRoute == Routes.HUB

    val prefs by mainViewModel.preferences.collectAsStateWithLifecycle()
    val updateState by mainViewModel.updateState.collectAsStateWithLifecycle()
    val dismissedUpdateTag by mainViewModel.dismissedUpdateTag.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showQuickCapture by remember { mutableStateOf(false) }
    var quickCaptureInitialMode by remember { mutableStateOf(QuickCaptureMode.EXPENSE) }
    var autoLaunchInstallOnReady by remember { mutableStateOf(false) }

    val showMessage: (String) -> Unit = remember(scope, snackbarHostState) {
        { message ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    val showUndoMessage: (String, () -> Unit) -> Unit = remember(scope, snackbarHostState, context) {
        { message, onUndo ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onUndo()
                    KansoWidgetUpdater.updateAll(context)
                }
            }
        }
    }

    val refreshWidgets: () -> Unit = remember(scope, context) {
        {
            scope.launch {
                KansoWidgetUpdater.updateAll(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.triggerAutoUpdateCheck()
        mainViewModel.triggerAutoDriveSync()
    }

    LaunchedEffect(
        prefs.hideBalances,
        prefs.appLockEnabled,
        prefs.overallMonthlyBudgetCents,
        prefs.currency,
        prefs.waterGoalMl
    ) {
        KansoWidgetUpdater.updateAll(context)
    }

    LaunchedEffect(updateState) {
        val ready = updateState as? AppUpdateState.ReadyToInstall
        if (ready != null && autoLaunchInstallOnReady) {
            autoLaunchInstallOnReady = false
            launchApkInstall(
                context = context,
                apkFile = ready.apkFile,
                canInstallPackages = mainViewModel::canInstallPackages,
                createInstallIntent = mainViewModel::createInstallIntent,
                createUnknownSourcesIntent = mainViewModel::createUnknownSourcesIntent,
                onMessage = showMessage
            )
        }
    }

    val navigateTopTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openBrainTab: (Int) -> Unit = { tab ->
        navController.navigate(Routes.brain(tab)) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    val openDefaultQuickCapture: () -> Unit = {
        quickCaptureInitialMode = when (currentRoute) {
            Routes.BRAIN_PATTERN -> QuickCaptureMode.NOTE
            else -> QuickCaptureMode.EXPENSE
        }
        showQuickCapture = true
    }

    LaunchedEffect(pendingLaunchCommand) {
        val cmd = pendingLaunchCommand ?: return@LaunchedEffect
        when (cmd) {
            is LaunchCommand.OpenQuickCapture -> {
                quickCaptureInitialMode = cmd.mode
                showQuickCapture = true
            }
            is LaunchCommand.OpenBrainTab -> openBrainTab(cmd.tab)
            is LaunchCommand.OpenUtilitiesTab -> {
                navController.navigate(Routes.utilities(cmd.tab)) { launchSingleTop = true }
            }
            LaunchCommand.OpenHabits -> {
                navController.navigate(Routes.HABITS) { launchSingleTop = true }
            }
            is LaunchCommand.OpenLifeAdminTab -> {
                navController.navigate(Routes.lifeAdmin(cmd.tab)) { launchSingleTop = true }
            }
            LaunchCommand.OpenMoney -> navigateTopTab(Routes.MONEY)
        }
        onConsumeLaunchCommand()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (isTopLevel) {
                KansoBottomDock(
                    hierarchy = backStackEntry?.destination?.hierarchy,
                    isQuickCaptureOpen = showQuickCapture,
                    onSelectTab = navigateTopTab,
                    onQuickCapture = openDefaultQuickCapture
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTopLevel) {
                KansoUpdateBanner(
                    updateState = updateState,
                    dismissedTag = dismissedUpdateTag,
                    onDownload = { release ->
                        autoLaunchInstallOnReady = true
                        mainViewModel.downloadUpdate(release)
                    },
                    onInstall = { apkFile ->
                        launchApkInstall(
                            context = context,
                            apkFile = apkFile,
                            canInstallPackages = mainViewModel::canInstallPackages,
                            createInstallIntent = mainViewModel::createInstallIntent,
                            createUnknownSourcesIntent = mainViewModel::createUnknownSourcesIntent,
                            onMessage = showMessage
                        )
                    },
                    onDismiss = mainViewModel::dismissUpdateBanner
                )
            }

            NavHost(
                navController = navController,
                startDestination = Routes.TODAY,
                enterTransition = {
                    if (Routes.isTopLevel(targetState.destination.route) &&
                        Routes.isTopLevel(initialState.destination.route)
                    ) {
                        fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220))
                    } else {
                        slideInHorizontally(tween(240)) { it / 5 } + fadeIn(tween(220))
                    }
                },
                exitTransition = {
                    if (Routes.isTopLevel(targetState.destination.route) &&
                        Routes.isTopLevel(initialState.destination.route)
                    ) {
                        fadeOut(tween(150))
                    } else {
                        slideOutHorizontally(tween(200)) { -it / 10 } + fadeOut(tween(160))
                    }
                },
                popEnterTransition = {
                    if (Routes.isTopLevel(targetState.destination.route) &&
                        Routes.isTopLevel(initialState.destination.route)
                    ) {
                        fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220))
                    } else {
                        slideInHorizontally(tween(220)) { -it / 10 } + fadeIn(tween(200))
                    }
                },
                popExitTransition = {
                    if (Routes.isTopLevel(targetState.destination.route) &&
                        Routes.isTopLevel(initialState.destination.route)
                    ) {
                        fadeOut(tween(150))
                    } else {
                        slideOutHorizontally(tween(220)) { it / 5 } + fadeOut(tween(180))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                composable(Routes.TODAY) {
                    TodayRoute(
                        onOpenQuickCapture = openDefaultQuickCapture,
                        onOpenSearch = { navController.navigate(Routes.SEARCH) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenMoney = { navigateTopTab(Routes.MONEY) },
                        onOpenBrainTab = openBrainTab,
                        onOpenHabits = { navController.navigate(Routes.HABITS) },
                        onOpenExpiries = { navController.navigate(Routes.lifeAdmin(0)) },
                        onOpenMaintenance = { navController.navigate(Routes.lifeAdmin(1)) },
                        onShowMessage = showMessage,
                        onShowUndoMessage = showUndoMessage,
                        onBalancesVisibilityChanged = refreshWidgets
                    )
                }

                composable(Routes.MONEY) {
                    MoneyRoute(
                        onShowMessage = showMessage,
                        onShowUndoMessage = showUndoMessage,
                        onBalancesVisibilityChanged = refreshWidgets
                    )
                }

                composable(
                    route = Routes.BRAIN_PATTERN,
                    arguments = listOf(
                        navArgument("tab") {
                            type = NavType.IntType
                            defaultValue = 0
                        }
                    )
                ) { entry ->
                    val initialTab = entry.arguments?.getInt("tab") ?: 0
                    BrainRoute(
                        initialTab = initialTab,
                        onShowMessage = showMessage,
                        onShowUndoMessage = showUndoMessage
                    )
                }

                composable(Routes.HUB) {
                    HubRoute(
                        onOpenMoney = { navigateTopTab(Routes.MONEY) },
                        onOpenBrain = { navigateTopTab(Routes.brain(0)) },
                        onOpenSearch = { navController.navigate(Routes.SEARCH) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenHabits = { navController.navigate(Routes.HABITS) },
                        onOpenLifeAdmin = { navController.navigate(Routes.lifeAdmin(0)) },
                        onOpenUtilities = { navController.navigate(Routes.utilities(0)) }
                    )
                }

                composable(Routes.HABITS) {
                    HabitsRoute(
                        onBack = { navController.popBackStack() },
                        onShowMessage = showMessage,
                        onShowUndoMessage = showUndoMessage
                    )
                }

                composable(
                    route = Routes.LIFEADMIN_PATTERN,
                    arguments = listOf(
                        navArgument("tab") {
                            type = NavType.IntType
                            defaultValue = 0
                        }
                    )
                ) { entry ->
                    val initialTab = entry.arguments?.getInt("tab") ?: 0
                    LifeAdminRoute(
                        initialTab = initialTab,
                        onBack = { navController.popBackStack() },
                        onShowMessage = showMessage,
                        onShowUndoMessage = showUndoMessage
                    )
                }

                composable(
                    route = Routes.UTILITIES_PATTERN,
                    arguments = listOf(
                        navArgument("tab") {
                            type = NavType.IntType
                            defaultValue = 0
                        }
                    )
                ) { entry ->
                    val initialTab = entry.arguments?.getInt("tab") ?: 0
                    UtilitiesRoute(
                        initialTab = initialTab,
                        onBack = { navController.popBackStack() },
                        onShowMessage = showMessage,
                        onShowUndoMessage = showUndoMessage
                    )
                }

                composable(Routes.SEARCH) {
                    GlobalSearchRoute(
                        onBack = { navController.popBackStack() },
                        onOpenMoney = { navigateTopTab(Routes.MONEY) },
                        onOpenBrainTab = openBrainTab,
                        onOpenLifeAdminTab = { tab -> navController.navigate(Routes.lifeAdmin(tab)) },
                        onShowMessage = showMessage
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsRoute(
                        onBack = { navController.popBackStack() },
                        onShowMessage = showMessage,
                        onAppLockActivated = mainViewModel::markSessionUnlocked,
                        onBiometricPromptActive = mainViewModel::setBiometricPromptActive
                    )
                }
            }
        }
    }

    if (showQuickCapture) {
        QuickCaptureSheet(
            currency = prefs.currency,
            initialMode = quickCaptureInitialMode,
            onDismiss = { showQuickCapture = false },
            onSaveTransaction = { type, amountCents, category, wallet, note, timestampMillis ->
                mainViewModel.saveQuickTransaction(type, amountCents, category, wallet, note, timestampMillis)
                showMessage("Saved ${if (type.name == "INCOME") "income" else "expense"}")
                scope.launch { KansoWidgetUpdater.updateAll(context) }
            },
            onSaveTransfer = { fromWallet, toWallet, amountCents, feeCents, note, timestampMillis ->
                mainViewModel.saveQuickTransfer(fromWallet, toWallet, amountCents, feeCents, note, timestampMillis)
                showMessage("Saved transfer")
                scope.launch { KansoWidgetUpdater.updateAll(context) }
            },
            onSaveNote = { title, content, tags, isPinned ->
                mainViewModel.saveQuickNote(title, content, tags, isPinned)
                showMessage("Saved note")
                scope.launch { KansoWidgetUpdater.updateAll(context) }
            },
            onSaveTask = { title, priority, dueDateEpochDay, recurrence ->
                mainViewModel.saveQuickTask(title, priority, dueDateEpochDay, recurrence)
                showMessage("Saved task")
                scope.launch { KansoWidgetUpdater.updateAll(context) }
            },
            onSaveSnippet = { label, value, category, isSensitive, isPinned ->
                mainViewModel.saveQuickSnippet(label, value, category, isSensitive, isPinned)
                showMessage("Saved snippet")
                scope.launch { KansoWidgetUpdater.updateAll(context) }
            },
            onSaveExpiry = { title, category, expiryEpochDay, remindDaysBefore ->
                mainViewModel.saveQuickExpiry(title, category, expiryEpochDay, remindDaysBefore)
                showMessage("Saved expiry alert")
                scope.launch { KansoWidgetUpdater.updateAll(context) }
            }
        )
    }
}

@Composable
private fun KansoBottomDock(
    hierarchy: Sequence<NavDestination>?,
    isQuickCaptureOpen: Boolean,
    onSelectTab: (String) -> Unit,
    onQuickCapture: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val fabInteraction = remember { MutableInteractionSource() }
    val fabPressed by fabInteraction.collectIsPressedAsState()
    val fabScale by animateFloatAsState(
        targetValue = if (fabPressed) 0.90f else 1f,
        animationSpec = KansoMotion.BouncySpring,
        label = "fabScale"
    )
    val fabRotation by animateFloatAsState(
        targetValue = if (isQuickCaptureOpen) 45f else 0f,
        animationSpec = KansoMotion.BouncySpring,
        label = "fabRotation"
    )
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(66.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TopLevelTabs.take(2).forEach { tab ->
                    DockTabItem(
                        tab = tab,
                        selected = hierarchy?.any { dest ->
                            dest.route == tab.route || (dest.route?.startsWith(tab.baseRoute) == true)
                        } == true,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTab(tab.route)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = fabScale
                                scaleY = fabScale
                            }
                            .width(52.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = fabInteraction,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onQuickCapture()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Quick Capture",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(26.dp)
                                    .graphicsLayer { rotationZ = fabRotation }
                            )
                        }
                    }
                }

                TopLevelTabs.drop(2).forEach { tab ->
                    DockTabItem(
                        tab = tab,
                        selected = hierarchy?.any { dest ->
                            dest.route == tab.route || (dest.route?.startsWith(tab.baseRoute) == true)
                        } == true,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTab(tab.route)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DockTabItem(
    tab: TopLevelTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "dockPill"
    )
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 50.dp else 28.dp,
        animationSpec = KansoMotion.DpSpring,
        label = "dockPillWidth"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1.0f,
        animationSpec = KansoMotion.BouncySpring,
        label = "dockIconScale"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(180),
        label = "dockContent"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .width(pillWidth)
                .height(28.dp)
                .clip(RoundedCornerShape(50))
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.label,
                tint = contentColor,
                modifier = Modifier
                    .size(21.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}
