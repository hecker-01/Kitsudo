package dev.heckr.kitsudo.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import dev.heckr.kitsudo.domain.usecase.CascadeCompleteUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the inline Complete / Snooze action buttons on deadline notifications.
 *
 * Registered in the manifest as a non-exported receiver. Both actions are
 * triggered by [PendingIntent]s built in [NotificationHelper.buildActionIntent].
 *
 * Because completing a task / scheduling work is suspend-y, the receiver uses
 * `goAsync` to keep the process alive while the coroutine runs.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var cascadeComplete: CascadeCompleteUseCase

    @Inject lateinit var notificationScheduler: NotificationScheduler

    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject lateinit var preferencesRepository: NotificationPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty()
        val parentId = intent.getStringExtra(EXTRA_PARENT_TASK_ID)
        val parentTitle = intent.getStringExtra(EXTRA_PARENT_TASK_TITLE)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE -> handleComplete(taskId)
                    ACTION_SNOOZE -> handleSnooze(taskId, taskTitle, parentId, parentTitle)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleComplete(taskId: String) {
        cascadeComplete(taskId, isCompleted = true)
        notificationScheduler.cancel(taskId)
        notificationHelper.cancelDisplayed(taskId)
    }

    private suspend fun handleSnooze(
        taskId: String,
        taskTitle: String,
        parentId: String?,
        parentTitle: String?,
    ) {
        val prefs = preferencesRepository.observe().first()
        notificationHelper.cancelDisplayed(taskId)
        notificationScheduler.snoozeMain(taskId, taskTitle, prefs.snoozeMinutes, parentId, parentTitle)
    }

    companion object {
        const val ACTION_COMPLETE = "dev.heckr.kitsudo.action.NOTIF_COMPLETE"
        const val ACTION_SNOOZE = "dev.heckr.kitsudo.action.NOTIF_SNOOZE"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_PARENT_TASK_ID = "parent_task_id"
        const val EXTRA_PARENT_TASK_TITLE = "parent_task_title"
    }
}
