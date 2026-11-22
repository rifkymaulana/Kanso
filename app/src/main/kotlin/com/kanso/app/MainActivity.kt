package com.kanso.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kanso.app.navigation.KansoAppRoot
import com.kanso.app.navigation.LaunchCommand
import com.kanso.app.navigation.LaunchCommandParser
import com.kanso.app.notifications.KansoReminderScheduler
import com.kanso.app.sync.DriveSyncScheduler
import com.kanso.app.widget.KansoWidgetUpdater
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.ui.theme.KansoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val pendingCommandFlow = MutableStateFlow<LaunchCommand?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val prefs by mainViewModel.preferences.collectAsStateWithLifecycle()
            val driveConfig by mainViewModel.driveSyncConfig.collectAsStateWithLifecycle()
            val pendingCommand by pendingCommandFlow.collectAsStateWithLifecycle()

            LaunchedEffect(prefs.remindersEnabled) {
                KansoReminderScheduler.syncSchedule(this@MainActivity, prefs.remindersEnabled)
            }

            LaunchedEffect(driveConfig.isConnected, driveConfig.autoSyncEnabled, driveConfig.hasPassphrase) {
                DriveSyncScheduler.syncPeriodicSchedule(
                    context = this@MainActivity,
                    enabled = driveConfig.isConnected && driveConfig.autoSyncEnabled && driveConfig.hasPassphrase
                )
            }

            KansoTheme(
                themeMode = prefs.themeMode,
                useDynamicColor = prefs.useDynamicColor
            ) {
                KansoAppRoot(
                    mainViewModel = mainViewModel,
                    pendingLaunchCommand = pendingCommand,
                    onConsumeLaunchCommand = { pendingCommandFlow.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            KansoWidgetUpdater.updateAll(applicationContext)
            val driveConfig = preferencesRepository.getDriveSyncConfig()
            if (driveConfig.isConnected && driveConfig.autoSyncEnabled && driveConfig.hasPassphrase) {
                DriveSyncScheduler.enqueueBackgroundSync(applicationContext)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        val parsed = LaunchCommandParser.parseUri(intent?.dataString)
        if (parsed != null) {
            pendingCommandFlow.value = parsed
        }
    }
}
