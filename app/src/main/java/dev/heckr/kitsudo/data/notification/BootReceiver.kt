package dev.heckr.kitsudo.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.heckr.kitsudo.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-arms at-deadline alarms after a reboot.
 *
 * Exact [android.app.AlarmManager] alarms are cleared when the device restarts
 * (unlike WorkManager, which persists). On boot we walk the task tree and
 * re-schedule the main alarm for every incomplete task whose deadline is still
 * in the future. Pre-reminders and follow-ups are WorkManager-backed and restore
 * themselves, so they need no handling here.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository

    @Inject lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val now = System.currentTimeMillis()
                taskRepository.getTopLevelTasksWithSubtasks().first().forEach { tws ->
                    val parent = tws.task
                    if (shouldArm(parent.isCompleted, parent.deadlineAt, now)) {
                        notificationScheduler.scheduleMainExact(
                            taskId = parent.id,
                            taskTitle = parent.title,
                            fireAt = parent.deadlineAt!!,
                            deadlineAt = parent.deadlineAt!!,
                            parentId = null,
                            parentTitle = null,
                        )
                    }
                    tws.subtasks.forEach { sub ->
                        if (shouldArm(sub.isCompleted, sub.deadlineAt, now)) {
                            notificationScheduler.scheduleMainExact(
                                taskId = sub.id,
                                taskTitle = sub.title,
                                fireAt = sub.deadlineAt!!,
                                deadlineAt = sub.deadlineAt!!,
                                parentId = parent.id,
                                parentTitle = parent.title,
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun shouldArm(isCompleted: Boolean, deadlineAt: Long?, now: Long): Boolean =
        !isCompleted && deadlineAt != null && deadlineAt > now
}
