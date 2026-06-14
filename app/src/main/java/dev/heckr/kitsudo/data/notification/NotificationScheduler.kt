package dev.heckr.kitsudo.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules deadline-related notifications for tasks.
 *
 * - **Main (at-deadline)** notifications use an **exact** [AlarmManager] alarm so
 *   they fire punctually even in Doze. The alarm targets [DeadlineAlarmReceiver],
 *   which shows the notification and chains the first follow-up. When the OS hasn't
 *   granted exact-alarm permission we fall back to an inexact, Doze-allowed alarm.
 * - **Pre-reminders** and the **follow-up chain** stay on WorkManager: they are
 *   not time-critical, and WorkManager survives process death and reboot for free.
 *
 * Work/alarms for a task all share a common cancel tag / request code so a single
 * [cancel] call tears the whole chain down.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val preferencesRepository: NotificationPreferencesRepository,
) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    /**
     * Schedules the pre-reminder(s) (if any) via WorkManager and the main
     * at-deadline notification via an exact alarm. Existing schedules are replaced.
     *
     * Pass [parentId] and [parentTitle] for subtasks so the rendered notification
     * references the parent task (lower priority, grouped, no follow-up).
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

        // -- Main (at-deadline) notification: exact alarm ---------------
        if (deadlineAt > now) {
            scheduleMainExact(taskId, taskTitle, deadlineAt, deadlineAt, parentId, parentTitle)
        }

        // -- Pre-reminders (one per configured lead time): WorkManager --
        prefs.preReminderLeadMinutes.filter { it > 0 }.forEach { leadMin ->
            val preDelay = deadlineAt - leadMin * 60_000L - now
            if (preDelay > 0) {
                workManager.enqueueUniqueWork(
                    preWorkName(taskId, leadMin),
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                        .setInitialDelay(preDelay, TimeUnit.MILLISECONDS)
                        .setInputData(
                            buildInputData(taskId, taskTitle, NotificationKind.PRE, deadlineAt, preLeadMinutes = leadMin, parentId = parentId, parentTitle = parentTitle),
                        )
                        .addTag(taskNotificationsTag(taskId))
                        .build(),
                )
            }
        }
    }

    /**
     * Re-arms the main notification [snoozeMinutes] from now. Used by
     * [NotificationActionReceiver] when the user taps Snooze.
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
        val wakeAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
        scheduleMainExact(taskId, taskTitle, wakeAt, wakeAt, parentId, parentTitle)
    }

    /**
     * Sets (or replaces) the exact alarm for the at-deadline notification, firing
     * at [fireAt]. [deadlineAt] is carried through to the receiver for follow-up
     * bookkeeping (it differs from [fireAt] for snoozes / quiet-hours deferrals).
     *
     * Falls back to an inexact, Doze-allowed alarm when exact alarms aren't
     * permitted (API 33+ requires the user to grant SCHEDULE_EXACT_ALARM).
     */
    fun scheduleMainExact(
        taskId: String,
        taskTitle: String,
        fireAt: Long,
        deadlineAt: Long,
        parentId: String?,
        parentTitle: String?,
    ) {
        val pendingIntent = mainAlarmPendingIntent(
            taskId, taskTitle, deadlineAt, parentId, parentTitle,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        val am = alarmManager
        try {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Exact-alarm permission revoked between the check and the call.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent)
        }
    }

    /**
     * Enqueues the next follow-up ping [FOLLOWUP_INTERVAL] later. Called by
     * [DeadlineAlarmReceiver] after the main fire (attempt 1) and by
     * [DeadlineNotificationWorker] to chain subsequent follow-ups.
     */
    fun scheduleFollowup(taskId: String, taskTitle: String, deadlineAt: Long, attempt: Int) {
        workManager.enqueueUniqueWork(
            followupWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                .setInitialDelay(DeadlineNotificationWorker.FOLLOWUP_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setInputData(
                    buildInputData(taskId, taskTitle, NotificationKind.FOLLOWUP, deadlineAt, followupAttempt = attempt),
                )
                .addTag(taskNotificationsTag(taskId))
                .build(),
        )
    }

    /** Cancels every pending notification (pre/main alarm/followup) for [taskId]. */
    fun cancel(taskId: String) {
        workManager.cancelAllWorkByTag(taskNotificationsTag(taskId))
        mainAlarmPendingIntent(
            taskId, "", 0L, null, null,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let { pi ->
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    /**
     * Builds the [PendingIntent] for the main alarm. [PendingIntent] equality
     * ignores extras, so [FLAG_NO_CREATE] reliably retrieves the existing alarm
     * for cancellation regardless of the placeholder values passed here.
     */
    private fun mainAlarmPendingIntent(
        taskId: String,
        taskTitle: String,
        deadlineAt: Long,
        parentId: String?,
        parentTitle: String?,
        flags: Int,
    ): PendingIntent? {
        val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            action = DeadlineAlarmReceiver.ACTION_FIRE_MAIN
            putExtra(DeadlineAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(DeadlineAlarmReceiver.EXTRA_TASK_TITLE, taskTitle)
            putExtra(DeadlineAlarmReceiver.EXTRA_DEADLINE_AT, deadlineAt)
            putExtra(DeadlineAlarmReceiver.EXTRA_PARENT_TASK_ID, parentId)
            putExtra(DeadlineAlarmReceiver.EXTRA_PARENT_TASK_TITLE, parentTitle)
        }
        return PendingIntent.getBroadcast(context, mainAlarmRequestCode(taskId), intent, flags)
    }

    private fun buildInputData(
        taskId: String,
        taskTitle: String,
        kind: NotificationKind,
        deadlineAt: Long,
        followupAttempt: Int = 0,
        preLeadMinutes: Int = 0,
        parentId: String? = null,
        parentTitle: String? = null,
    ) = workDataOf(
        DeadlineNotificationWorker.KEY_TASK_ID to taskId,
        DeadlineNotificationWorker.KEY_TASK_TITLE to taskTitle,
        DeadlineNotificationWorker.KEY_KIND to kind.name,
        DeadlineNotificationWorker.KEY_DEADLINE_AT to deadlineAt,
        DeadlineNotificationWorker.KEY_FOLLOWUP_ATTEMPT to followupAttempt,
        DeadlineNotificationWorker.KEY_PRE_LEAD_MINUTES to preLeadMinutes,
        DeadlineNotificationWorker.KEY_PARENT_TASK_ID to parentId,
        DeadlineNotificationWorker.KEY_PARENT_TASK_TITLE to parentTitle,
    )

    companion object {
        fun taskNotificationsTag(taskId: String) = "task_notifications_$taskId"
        /** Per-lead-time unique name so multiple pre-reminders don't overwrite each other. */
        fun preWorkName(taskId: String, leadMinutes: Int) = "deadline_${taskId}_pre_$leadMinutes"
        fun followupWorkName(taskId: String) = "deadline_${taskId}_followup"
        /** Stable per-task request code for the main alarm PendingIntent. */
        private fun mainAlarmRequestCode(taskId: String) = taskId.hashCode() xor 0x4
    }
}
