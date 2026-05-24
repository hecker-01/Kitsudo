package dev.heckr.kitsudo.data.notification

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules deadline-related notifications for tasks via WorkManager.
 *
 * Each scheduled task produces up to two upfront work requests, both sharing a
 * common cancel tag so a single [cancel] call kills the whole chain:
 *
 * - `deadline_<id>_pre`  — fires `deadlineAt − leadTime` (skipped if lead time is 0)
 * - `deadline_<id>_main` — fires at `deadlineAt`
 *
 * The optional **follow-up** chain (`deadline_<id>_followup`) is enqueued by the
 * worker itself after the main notification fires and the task is still
 * incomplete; cancellation via the shared tag still cleans it up.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val preferencesRepository: NotificationPreferencesRepository,
) {

    /**
     * Schedules the pre-reminder (if any) and the main deadline notification
     * for [taskId]. Existing work for either name is replaced.
     *
     * Pass [parentId] and [parentTitle] for subtasks so the worker can render
     * a distinct, lower-priority notification that references the parent task.
     */
    suspend fun schedule(
        taskId: String,
        taskTitle: String,
        deadlineAt: Long,
        parentId: String? = null,
        parentTitle: String? = null,
    ) {
        val now = System.currentTimeMillis()
        val prefs = preferencesRepository.observe().first()
        val tag = taskNotificationsTag(taskId)

        // ── Main (at-deadline) notification ────────────────────────────
        val mainDelay = deadlineAt - now
        if (mainDelay > 0) {
            workManager.enqueueUniqueWork(
                mainWorkName(taskId),
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                    .setInitialDelay(mainDelay, TimeUnit.MILLISECONDS)
                    .setInputData(
                        buildInputData(taskId, taskTitle, NotificationKind.MAIN, deadlineAt, parentId = parentId, parentTitle = parentTitle),
                    )
                    .addTag(tag)
                    .build(),
            )
        }

        // ── Pre-reminder ───────────────────────────────────────────────
        val leadMin = prefs.preReminderLeadMinutes
        if (leadMin > 0) {
            val preDelay = deadlineAt - leadMin * 60_000L - now
            if (preDelay > 0) {
                workManager.enqueueUniqueWork(
                    preWorkName(taskId),
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                        .setInitialDelay(preDelay, TimeUnit.MILLISECONDS)
                        .setInputData(
                            buildInputData(taskId, taskTitle, NotificationKind.PRE, deadlineAt, parentId = parentId, parentTitle = parentTitle),
                        )
                        .addTag(tag)
                        .build(),
                )
            }
        }
    }

    /**
     * Reschedules the main worker [snoozeMinutes] from now. Used by
     * [NotificationActionReceiver] when the user taps the Snooze action.
     *
     * [parentId] and [parentTitle] must be forwarded from the original action intent
     * so a snoozed subtask notification is still rendered as a subtask notification.
     */
    fun snoozeMain(
        taskId: String,
        taskTitle: String,
        snoozeMinutes: Int,
        parentId: String? = null,
        parentTitle: String? = null,
    ) {
        val tag = taskNotificationsTag(taskId)
        val wakeAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
        workManager.enqueueUniqueWork(
            mainWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                .setInitialDelay(snoozeMinutes * 60_000L, TimeUnit.MILLISECONDS)
                .setInputData(
                    buildInputData(taskId, taskTitle, NotificationKind.MAIN, wakeAt, parentId = parentId, parentTitle = parentTitle),
                )
                .addTag(tag)
                .build(),
        )
    }

    /** Cancels every pending notification work (pre, main, followup) for [taskId]. */
    fun cancel(taskId: String) {
        workManager.cancelAllWorkByTag(taskNotificationsTag(taskId))
    }

    private fun buildInputData(
        taskId: String,
        taskTitle: String,
        kind: NotificationKind,
        deadlineAt: Long,
        followupAttempt: Int = 0,
        parentId: String? = null,
        parentTitle: String? = null,
    ) = workDataOf(
        DeadlineNotificationWorker.KEY_TASK_ID to taskId,
        DeadlineNotificationWorker.KEY_TASK_TITLE to taskTitle,
        DeadlineNotificationWorker.KEY_KIND to kind.name,
        DeadlineNotificationWorker.KEY_DEADLINE_AT to deadlineAt,
        DeadlineNotificationWorker.KEY_FOLLOWUP_ATTEMPT to followupAttempt,
        DeadlineNotificationWorker.KEY_PARENT_TASK_ID to parentId,
        DeadlineNotificationWorker.KEY_PARENT_TASK_TITLE to parentTitle,
    )

    companion object {
        fun taskNotificationsTag(taskId: String) = "task_notifications_$taskId"
        fun preWorkName(taskId: String) = "deadline_${taskId}_pre"
        fun mainWorkName(taskId: String) = "deadline_${taskId}_main"
        fun followupWorkName(taskId: String) = "deadline_${taskId}_followup"
    }
}
