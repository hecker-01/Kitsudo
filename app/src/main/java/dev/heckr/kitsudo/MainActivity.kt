package dev.heckr.kitsudo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.heckr.kitsudo.data.notification.NotificationHelper
import dev.heckr.kitsudo.data.update.AppUpdater
import dev.heckr.kitsudo.presentation.navigation.KitsudoNavHost
import dev.heckr.kitsudo.presentation.onboarding.OnboardingScreen
import dev.heckr.kitsudo.presentation.onboarding.OnboardingState
import dev.heckr.kitsudo.presentation.onboarding.OnboardingViewModel
import dev.heckr.kitsudo.presentation.theme.ThemeViewModel
import dev.heckr.kitsudo.presentation.update.WhatsNewScreen
import dev.heckr.kitsudo.presentation.update.WhatsNewViewModel
import dev.heckr.kitsudo.ui.theme.KitsudoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appUpdater: AppUpdater

    private val themeViewModel: ThemeViewModel by viewModels()
    private val whatsNewViewModel: WhatsNewViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()

    /** A task (and optional subtask to expand) to open from a notification tap. */
    data class PendingOpen(val taskId: String, val expandSubtaskId: String?)

    /**
     * Holds the target from the most recent notification tap. The NavHost
     * collects this and navigates to TaskDetail when it becomes non-null.
     * Reset to null after consumption (handled inside the NavHost).
     */
    private val pendingOpen = MutableStateFlow<PendingOpen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appUpdater.checkForUpdates(this)
        pendingOpen.value = intent?.extractPendingOpen()
        setContent {
            val uiState by themeViewModel.uiState.collectAsStateWithLifecycle()
            val startOpen by pendingOpen.collectAsState()
            val whatsNewState by whatsNewViewModel.state.collectAsStateWithLifecycle()
            val onboardingState by onboardingViewModel.state.collectAsStateWithLifecycle()

            val context = LocalContext.current
            val needsNotificationPermission = {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
            }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }
            // Set when onboarding finishes; the rationale only renders once the
            // overlay is actually gone (state == Completed), so the welcome screen
            // closes first and the reason appears over the task list.
            var pendingNotificationRequest by remember { mutableStateOf(false) }

            KitsudoTheme(palette = uiState.palette, accent = uiState.accent) {
                // Explicit Box so the onboarding overlay and the NavHost share one
                // stacking context: when onboarding leaves the composition the root
                // re-lays-out and redraws instead of leaving stale pixels on screen.
                Box(modifier = Modifier.fillMaxSize()) {
                    KitsudoNavHost(
                        startTaskId = startOpen?.taskId,
                        startExpandSubtaskId = startOpen?.expandSubtaskId,
                        onStartTaskHandled = { pendingOpen.value = null },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (onboardingState == OnboardingState.Required) {
                        // Cover the whole app until the first-install flow is finished.
                        OnboardingScreen(
                            onFinish = {
                                onboardingViewModel.complete()
                                // On a fresh install the welcome flow replaces the
                                // "what's new" sheet, so make sure it doesn't pop up after.
                                whatsNewViewModel.dismiss()
                                if (needsNotificationPermission()) {
                                    pendingNotificationRequest = true
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (onboardingState == OnboardingState.Completed) {
                        if (pendingNotificationRequest) {
                            NotificationRationaleDialog(
                                onConfirm = {
                                    pendingNotificationRequest = false
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                            )
                        } else {
                            WhatsNewScreen(
                                state = whatsNewState,
                                onDismiss = whatsNewViewModel::dismiss,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update the backing intent so further onNewIntent calls see the new extras.
        setIntent(intent)
        intent.extractPendingOpen()?.let { pendingOpen.value = it }
    }

    private fun Intent.extractPendingOpen(): PendingOpen? {
        val taskId = getStringExtra(NotificationHelper.EXTRA_OPEN_TASK_ID) ?: return null
        return PendingOpen(
            taskId = taskId,
            expandSubtaskId = getStringExtra(NotificationHelper.EXTRA_EXPAND_SUBTASK_ID),
        )
    }
}

/**
 * Explains why Kitsudo wants to post notifications, shown over the task list once
 * onboarding has closed. A single confirm button leads straight into the one-shot
 * system permission prompt.
 */
@Composable
private fun NotificationRationaleDialog(onConfirm: () -> Unit) {
    AlertDialog(
        // Any dismissal still leads into the system prompt - there's no decline
        // path here; the OS dialog is where the user actually allows or denies.
        onDismissRequest = onConfirm,
        title = { Text(stringResource(R.string.onboarding_notifications_title)) },
        text = { Text(stringResource(R.string.onboarding_notifications_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.onboarding_notifications_ok))
            }
        },
    )
}
