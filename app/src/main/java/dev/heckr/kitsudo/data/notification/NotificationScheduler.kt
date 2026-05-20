package dev.heckr.kitsudo.data.notification

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    /** Schedule a deadline notification for [taskId] at [deadlineAt] epoch-ms. */
    fun schedule(taskId: String, taskTitle: String, deadlineAt: Long) {
        val delayMs = deadlineAt - System.currentTimeMillis()
        if (delayMs <= 0) return // deadline is in the past

        val work = OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    DeadlineNotificationWorker.KEY_TASK_ID to taskId,
                    DeadlineNotificationWorker.KEY_TASK_TITLE to taskTitle,
                ),
            )
            .addTag(workTag(taskId))
            .build()

        workManager.enqueueUniqueWork(
            workTag(taskId),
            ExistingWorkPolicy.REPLACE,
            work,
        )
    }

    /** Cancel any pending notification for [taskId]. */
    fun cancel(taskId: String) {
        workManager.cancelUniqueWork(workTag(taskId))
    }

    private fun workTag(taskId: String) = "deadline_$taskId"
}
