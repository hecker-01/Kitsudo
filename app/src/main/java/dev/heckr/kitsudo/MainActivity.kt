package dev.heckr.kitsudo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.heckr.kitsudo.data.notification.NotificationHelper
import dev.heckr.kitsudo.data.update.UpdateChecker
import dev.heckr.kitsudo.presentation.navigation.KitsudoNavHost
import dev.heckr.kitsudo.presentation.theme.ThemeViewModel
import dev.heckr.kitsudo.ui.theme.KitsudoTheme
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    /**
     * Holds the task id from the most recent notification tap. The NavHost
     * collects this and navigates to TaskDetail when it becomes non-null.
     * Reset to null after consumption (handled inside the NavHost).
     */
    private val pendingTaskId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UpdateChecker.check(this)
        pendingTaskId.value = intent?.extractOpenTaskId()
        setContent {
            val uiState by themeViewModel.uiState.collectAsStateWithLifecycle()
            val startTaskId by pendingTaskId.collectAsState()
            KitsudoTheme(palette = uiState.palette, accent = uiState.accent) {
                KitsudoNavHost(
                    startTaskId = startTaskId,
                    onStartTaskHandled = { pendingTaskId.value = null },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update the backing intent so further onNewIntent calls see the new extras.
        setIntent(intent)
        intent.extractOpenTaskId()?.let { pendingTaskId.value = it }
    }

    private fun Intent.extractOpenTaskId(): String? =
        getStringExtra(NotificationHelper.EXTRA_OPEN_TASK_ID)
}
