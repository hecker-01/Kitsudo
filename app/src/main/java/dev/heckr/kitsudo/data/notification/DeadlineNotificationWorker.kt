package dev.heckr.kitsudo.data.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import dev.heckr.kitsudo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Fires (or defers, or chains) a single notification of a given [NotificationKind]
 * for one task.
 *
 *  - Honors quiet hours: if `now` is inside the window the worker reschedules
 *    itself for the window's end instead of firing.
 *  - After firing a `MAIN`, if the task is still incomplete, enqueues a
 *    `FOLLOWUP` worker [FOLLOWUP_INTERVAL_MINUTES] later. The follow-up
 *    re-checks, fires (if still incomplete), and chains the next one up to
 *    [MAX_FOLLOWUP_ATTEMPTS] total.
 */
@HiltWorker
class DeadlineNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val notificationHelper: NotificationHelper,
    private val preferencesRepository: NotificationPreferencesRepository,
    private val workManager: WorkManager,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_KIND = "kind"
        const val KEY_DEADLINE_AT = "deadline_at"
        const val KEY_FOLLOWUP_ATTEMPT = "followup_attempt"
        /** Null in the data bundle for top-level tasks; set for subtasks. */
        const val KEY_PARENT_TASK_ID = "parent_task_id"
        const val KEY_PARENT_TASK_TITLE = "parent_task_title"

        /** Minutes between follow-up pings after the deadline. */
        const val FOLLOWUP_INTERVAL_MINUTES = 4 * 60L

        /** Hard cap on the follow-up chain (≈ 24 hours total at 4-hour spacing). */
        const val MAX_FOLLOWUP_ATTEMPTS = 6
    }

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: return Result.failure()
        val kind = NotificationKind.fromName(inputData.getString(KEY_KIND))
        val deadlineAt = inputData.getLong(KEY_DEADLINE_AT, 0L)
        val followupAttempt = inputData.getInt(KEY_FOLLOWUP_ATTEMPT, 0)
        val parentId = inputData.getString(KEY_PARENT_TASK_ID)
        val parentTitle = inputData.getString(KEY_PARENT_TASK_TITLE)

        val task = taskRepository.getTaskById(taskId)
            ?: return Result.success() // task was deleted - nothing to notify
        if (task.isCompleted) return Result.success() // already done

        val prefs = preferencesRepository.observe().first()
        val now = System.currentTimeMillis()

        // -- Quiet hours: re-schedule self for the end of the window ----
        if (QuietHours.isInside(now, prefs)) {
            val deferUntil = QuietHours.nextEndAfter(now, prefs)
            val deferDelay = deferUntil - now
            if (deferDelay > 0) {
                workManager.enqueueUniqueWork(
                    uniqueNameForKind(taskId, kind),
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                        .setInitialDelay(deferDelay, TimeUnit.MILLISECONDS)
                        .setInputData(
                            workDataOf(
                                KEY_TASK_ID to taskId,
                                KEY_TASK_TITLE to taskTitle,
                                KEY_KIND to kind.name,
                                KEY_DEADLINE_AT to deadlineAt,
                                KEY_FOLLOWUP_ATTEMPT to followupAttempt,
                                KEY_PARENT_TASK_ID to parentId,
                                KEY_PARENT_TASK_TITLE to parentTitle,
                            ),
                        )
                        .addTag(NotificationScheduler.taskNotificationsTag(taskId))
                        .build(),
                )
            }
            return Result.success()
        }

        // -- Fire the actual notification -------------------------------
        notificationHelper.showNotification(taskId, taskTitle, kind, parentId, parentTitle)

        // -- Chain a follow-up after MAIN / FOLLOWUP (not after PRE, not for subtasks) ----
        // Subtasks only get a single MAIN ping - no recurring follow-up spam for secondary items.
        if (parentId == null && (kind == NotificationKind.MAIN || kind == NotificationKind.FOLLOWUP)) {
            val nextAttempt = if (kind == NotificationKind.MAIN) 1 else followupAttempt + 1
            if (nextAttempt <= MAX_FOLLOWUP_ATTEMPTS) {
                workManager.enqueueUniqueWork(
                    NotificationScheduler.followupWorkName(taskId),
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                        .setInitialDelay(FOLLOWUP_INTERVAL_MINUTES, TimeUnit.MINUTES)
                        .setInputData(
                            workDataOf(
                                KEY_TASK_ID to taskId,
                                KEY_TASK_TITLE to taskTitle,
                                KEY_KIND to NotificationKind.FOLLOWUP.name,
                                KEY_DEADLINE_AT to deadlineAt,
                                KEY_FOLLOWUP_ATTEMPT to nextAttempt,
                            ),
                        )
                        .addTag(NotificationScheduler.taskNotificationsTag(taskId))
                        .build(),
                )
            }
        }

        return Result.success()
    }

    private fun uniqueNameForKind(taskId: String, kind: NotificationKind): String = when (kind) {
        NotificationKind.PRE -> NotificationScheduler.preWorkName(taskId)
        NotificationKind.MAIN -> NotificationScheduler.mainWorkName(taskId)
        NotificationKind.FOLLOWUP -> NotificationScheduler.followupWorkName(taskId)
    }
}
