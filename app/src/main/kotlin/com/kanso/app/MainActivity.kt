package com.kanso.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kanso.app.navigation.KansoAppRoot
import com.kanso.app.navigation.LaunchCommand
import com.kanso.app.navigation.LaunchCommandParser
import com.kanso.app.navigation.SharedCapturePayload
import com.kanso.app.notifications.KansoReminderScheduler
import com.kanso.app.notifications.PomodoroCompletionWorker
import com.kanso.app.share.ReceiptShareProcessor
import com.kanso.app.sync.DriveSyncScheduler
import com.kanso.app.widget.KansoWidgetUpdater
import com.kanso.core.database.preferences.PomodoroTimerManager
import com.kanso.core.database.preferences.PreferencesRepository
import com.kanso.core.model.AppLockPolicy
import com.kanso.core.security.BiometricAuthHelper
import com.kanso.core.security.BiometricAvailability
import com.kanso.core.ui.components.KansoAccentIconBadge
import com.kanso.core.ui.components.KansoBentoCard
import com.kanso.core.ui.components.KansoButtonSize
import com.kanso.core.ui.components.KansoPrimaryButton
import com.kanso.core.ui.theme.KansoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var pomodoroTimerManager: PomodoroTimerManager

    private val pendingCommandFlow = MutableStateFlow<LaunchCommand?>(null)
    private val sharedCapturePayloadFlow = MutableStateFlow<SharedCapturePayload?>(null)
    private var activeMainViewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            SideEffect {
                activeMainViewModel = mainViewModel
            }

            val prefs by mainViewModel.preferences.collectAsStateWithLifecycle()
            val isPrefsLoaded by mainViewModel.isPreferencesLoaded.collectAsStateWithLifecycle()
            val lockSession by mainViewModel.lockSession.collectAsStateWithLifecycle()
            val driveConfig by mainViewModel.driveSyncConfig.collectAsStateWithLifecycle()
            val pendingCommand by pendingCommandFlow.collectAsStateWithLifecycle()
            val sharedCapturePayload by sharedCapturePayloadFlow.collectAsStateWithLifecycle()
            val pomodoroState by pomodoroTimerManager.state.collectAsStateWithLifecycle()

            var lockAuthError by remember { mutableStateOf<String?>(null) }
            var autoPromptedThisLock by remember { mutableStateOf(false) }

            val isBiometricAvailable = remember(prefs.appLockEnabled, lockSession.isUnlocked) {
                BiometricAuthHelper.checkAvailability(this@MainActivity) == BiometricAvailability.AVAILABLE
            }
            val requireUnlock = isPrefsLoaded && AppLockPolicy.shouldRequireUnlock(
                appLockEnabled = prefs.appLockEnabled,
                isBiometricAvailable = isBiometricAvailable,
                session = lockSession
            )

            val launchUnlockPrompt: () -> Unit = {
                lockAuthError = null
                mainViewModel.setBiometricPromptActive(true)
                BiometricAuthHelper.authenticate(
                    activity = this@MainActivity,
                    title = "Unlock Kanso",
                    subtitle = "Authenticate to access your personal OS",
                    onSuccess = {
                        lockAuthError = null
                        mainViewModel.markSessionUnlocked()
                    },
                    onError = { message ->
                        mainViewModel.setBiometricPromptActive(false)
                        lockAuthError = message
                    },
                    onCancel = {
                        mainViewModel.setBiometricPromptActive(false)
                    }
                )
            }

            LaunchedEffect(requireUnlock) {
                if (requireUnlock && !autoPromptedThisLock) {
                    autoPromptedThisLock = true
                    launchUnlockPrompt()
                } else if (!requireUnlock) {
                    autoPromptedThisLock = false
                    lockAuthError = null
                }
            }

            LaunchedEffect(prefs.hideRecentAppsPreview, requireUnlock) {
                if (prefs.hideRecentAppsPreview || requireUnlock) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            var initialReminderSync by remember { mutableStateOf(true) }
            LaunchedEffect(
                isPrefsLoaded,
                prefs.remindersEnabled,
                prefs.morningReminderEnabled,
                prefs.morningReminderHour,
                prefs.morningReminderMinute,
                prefs.eveningReminderEnabled,
                prefs.eveningReminderHour,
                prefs.eveningReminderMinute
            ) {
                if (!isPrefsLoaded) return@LaunchedEffect
                KansoReminderScheduler.syncSchedule(
                    context = this@MainActivity,
                    prefs = prefs,
                    replaceExisting = !initialReminderSync
                )
                initialReminderSync = false
            }

            LaunchedEffect(driveConfig.isConnected, driveConfig.autoSyncEnabled, driveConfig.hasPassphrase) {
                DriveSyncScheduler.syncPeriodicSchedule(
                    context = this@MainActivity,
                    enabled = driveConfig.isConnected && driveConfig.autoSyncEnabled && driveConfig.hasPassphrase
                )
            }

            LaunchedEffect(pomodoroState.isRunning, pomodoroState.endEpochMillis) {
                PomodoroCompletionWorker.syncScheduledWork(this@MainActivity, pomodoroState)
            }

            LaunchedEffect(Unit) {
                pomodoroTimerManager.completedEvents.collect { completedMode ->
                    PomodoroCompletionWorker.cancelScheduledWork(this@MainActivity)
                    PomodoroCompletionWorker.postCompletionNotification(this@MainActivity, completedMode)
                }
            }

            KansoTheme(
                themeMode = prefs.themeMode,
                useDynamicColor = prefs.useDynamicColor,
                hideBalances = prefs.hideBalances
            ) {
                when {
                    !isPrefsLoaded -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {}
                    }

                    requireUnlock -> {
                        KansoLockScreen(
                            authError = lockAuthError,
                            onRequestUnlock = launchUnlockPrompt
                        )
                    }

                    else -> {
                        KansoAppRoot(
                            mainViewModel = mainViewModel,
                            pendingLaunchCommand = pendingCommand,
                            sharedCapturePayload = sharedCapturePayload,
                            onConsumeLaunchCommand = { pendingCommandFlow.value = null },
                            onUpdateSharedCapture = { sharedCapturePayloadFlow.value = it },
                            onClearSharedCapture = { sharedCapturePayloadFlow.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activeMainViewModel?.onAppForegrounded(SystemClock.elapsedRealtime())
        pomodoroTimerManager.reconcileWithClock()?.let { completedMode ->
            PomodoroCompletionWorker.cancelScheduledWork(this)
            PomodoroCompletionWorker.postCompletionNotification(this, completedMode)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        activeMainViewModel?.onAppBackgrounded(
            isChangingConfigurations = isChangingConfigurations,
            nowElapsedMillis = SystemClock.elapsedRealtime()
        )
        lifecycleScope.launch {
            KansoWidgetUpdater.updateAll(applicationContext)
            val driveConfig = preferencesRepository.getDriveSyncConfig()
            if (driveConfig.isConnected && driveConfig.autoSyncEnabled && driveConfig.hasPassphrase) {
                DriveSyncScheduler.enqueueBackgroundSync(applicationContext)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(EXTRA_KANSO_CONSUMED, false)) return

        if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
            intent.putExtra(EXTRA_KANSO_CONSUMED, true)
            val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
            val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
            val streamUri = extractSharedStreamUri(intent)

            if (streamUri != null) {
                val declaredMime = intent.type
                sharedCapturePayloadFlow.value = SharedCapturePayload(
                    text = sharedText,
                    subject = sharedSubject,
                    isReceiptMedia = true,
                    isScanning = true
                )
                pendingCommandFlow.value = LaunchCommand.OpenSharedCapture
                lifecycleScope.launch {
                    val processed = ReceiptShareProcessor.processUri(
                        context = applicationContext,
                        uri = streamUri,
                        declaredMimeType = declaredMime
                    )
                    val combinedText = listOf(sharedText.trim(), processed.extractedText.trim())
                        .filter { it.isNotEmpty() }
                        .joinToString("\n")
                    sharedCapturePayloadFlow.value = SharedCapturePayload(
                        text = combinedText,
                        subject = sharedSubject,
                        isReceiptMedia = true,
                        isScanning = false,
                        previewBitmap = processed.previewBitmap
                    )
                }
            } else if (sharedText.isNotBlank() || sharedSubject.isNotBlank()) {
                sharedCapturePayloadFlow.value = SharedCapturePayload(
                    text = sharedText,
                    subject = sharedSubject,
                    isReceiptMedia = false,
                    isScanning = false
                )
                pendingCommandFlow.value = LaunchCommand.OpenSharedCapture
            }
            return
        }

        val parsed = LaunchCommandParser.parseUri(intent.dataString)
        if (parsed != null) {
            intent.putExtra(EXTRA_KANSO_CONSUMED, true)
            pendingCommandFlow.value = parsed
            intent.data = null
        }
    }

    private fun extractSharedStreamUri(intent: Intent): Uri? {
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val multiple = IntentCompat.getParcelableArrayListExtra(
                intent,
                Intent.EXTRA_STREAM,
                Uri::class.java
            )
            val first = multiple?.firstOrNull()
            if (first != null) return first
        }
        val single = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        if (single != null) return single
        return intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }

    companion object {
        private const val EXTRA_KANSO_CONSUMED = "com.kanso.app.EXTRA_INTENT_CONSUMED"
    }
}

@Composable
private fun KansoLockScreen(
    authError: String?,
    onRequestUnlock: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            KansoBentoCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KansoAccentIconBadge(
                        icon = Icons.Outlined.Lock,
                        accent = MaterialTheme.colorScheme.primary,
                        size = 56.dp,
                        iconSize = 26.dp,
                        cornerRadius = 18.dp
                    )
                    Text(
                        text = "Kanso is Locked",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Verify with biometrics or device PIN to view your finances, notes, and private records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (!authError.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = authError,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    KansoPrimaryButton(
                        text = "Unlock Kanso",
                        leadingIcon = Icons.Outlined.Fingerprint,
                        size = KansoButtonSize.LARGE,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestUnlock
                    )
                }
            }
        }
    }
}
