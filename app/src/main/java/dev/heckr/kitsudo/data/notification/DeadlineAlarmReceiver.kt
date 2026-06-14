package dev.heckr.kitsudo.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import dev.heckr.kitsudo.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires the at-deadline (`MAIN`) notification. Triggered by the exact
 * [android.app.AlarmManager] alarm set in [NotificationScheduler.scheduleMainExact],
 * so the banner appears punctually even in Doze (unlike deferrable WorkManager).
 *
 * Mirrors the old worker MAIN path:
 *  - skips if the task was deleted or already completed,
 *  - if inside quiet hours, re-arms the alarm for the window's end instead of firing,
 *  - otherwise shows the notification and (for top-level tasks) starts the follow-up chain.
 *
 * Uses `goAsync` to keep the process alive while the suspend work runs.
 */
@AndroidEntryPoint
class DeadlineAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository

    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject lateinit var preferencesRepository: NotificationPreferencesRepository

    @Inject lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_MAIN) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty()
        val deadlineAt = intent.getLongExtra(EXTRA_DEADLINE_AT, 0L)
        val parentId = intent.getStringExtra(EXTRA_PARENT_TASK_ID)
        val parentTitle = intent.getStringExtra(EXTRA_PARENT_TASK_TITLE)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                fireMain(taskId, taskTitle, deadlineAt, parentId, parentTitle)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun fireMain(
        taskId: String,
        taskTitle: String,
        deadlineAt: Long,
        parentId: String?,
        parentTitle: String?,
    ) {
        val task = taskRepository.getTaskById(taskId) ?: return // deleted
        if (task.isCompleted) return // already done

        val prefs = preferencesRepository.observe().first()
        val now = System.currentTimeMillis()

        // -- Quiet hours: re-arm the alarm for the end of the window ----
        if (QuietHours.isInside(now, prefs)) {
            val deferUntil = QuietHours.nextEndAfter(now, prefs)
            if (deferUntil > now) {
                notificationScheduler.scheduleMainExact(
                    taskId, taskTitle, deferUntil, deadlineAt, parentId, parentTitle,
                )
            }
            return
        }

        // -- Fire, then start the follow-up chain (top-level tasks only) ----
        notificationHelper.showNotification(taskId, taskTitle, NotificationKind.MAIN, parentId, parentTitle)
        if (parentId == null) {
            notificationScheduler.scheduleFollowup(taskId, taskTitle, deadlineAt, attempt = 1)
        }
    }

    companion object {
        const val ACTION_FIRE_MAIN = "dev.heckr.kitsudo.action.FIRE_MAIN_DEADLINE"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_DEADLINE_AT = "deadline_at"
        const val EXTRA_PARENT_TASK_ID = "parent_task_id"
        const val EXTRA_PARENT_TASK_TITLE = "parent_task_title"
    }
}
