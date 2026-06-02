package dev.heckr.kitsudo.data.notification

import android.R as AndroidR
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.heckr.kitsudo.MainActivity
import dev.heckr.kitsudo.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the notification channel and builds the notifications themselves.
 *
 * Each posted notification:
 *  - Taps deep-link into the task detail screen (via [MainActivity] extras)
 *  - Has a "Complete" action that fires [NotificationActionReceiver]
 *  - Has a "Snooze" action that fires [NotificationActionReceiver]
 */
@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "task_deadlines"
        const val EXTRA_OPEN_TASK_ID = "open_task_id"

        /**
         * Set alongside [EXTRA_OPEN_TASK_ID] when the notification is for a subtask.
         * [EXTRA_OPEN_TASK_ID] then carries the *parent* id, and this carries the
         * subtask id to pre-expand - mirroring a subtask tap on the list screen.
         */
        const val EXTRA_EXPAND_SUBTASK_ID = "expand_subtask_id"

        /** Pre + main share an id so main visually replaces the pre-reminder. */
        private fun notificationId(taskId: String, kind: NotificationKind): Int = when (kind) {
            NotificationKind.PRE, NotificationKind.MAIN -> taskId.hashCode()
            // xor 1 so followup banners don't overwrite the main one if it's
            // still in the shade.
            NotificationKind.FOLLOWUP -> taskId.hashCode() xor 1
        }

        /** Stable per-task request codes so PendingIntents don't collide. */
        private fun contentRequestCode(taskId: String) = taskId.hashCode()
        private fun completeRequestCode(taskId: String) = taskId.hashCode() xor 0x1
        private fun snoozeRequestCode(taskId: String) = taskId.hashCode() xor 0x2
    }

    /** Call once from Application.onCreate. */
    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * Posts a notification of the given [kind].
     *
     * Pre and Main share a notification id so the at-deadline one replaces the pre-reminder.
     *
     * When [parentId] is non-null (i.e. this is a subtask):
     *  - Body shows "Part of: [parentTitle]" instead of the generic copy.
     *  - Priority is DEFAULT rather than HIGH - no heads-up banner for secondary items.
     *  - Notification is placed in a group keyed to [parentId] so multiple subtask
     *    notifications collapse together in the shade.
     *  - No follow-up chain is scheduled (handled in the worker).
     */
    fun showNotification(
        taskId: String,
        taskTitle: String,
        kind: NotificationKind,
        parentId: String? = null,
        parentTitle: String? = null,
    ) {
        val isSubtask = parentId != null

        val title = if (isSubtask) {
            when (kind) {
                // Pre-reminder keeps the same title; the body distinguishes it.
                NotificationKind.PRE -> context.getString(R.string.notification_pre_title, taskTitle)
                NotificationKind.MAIN -> context.getString(R.string.notification_subtask_deadline_title, taskTitle)
                NotificationKind.FOLLOWUP -> context.getString(R.string.notification_subtask_overdue_title, taskTitle)
            }
        } else {
            when (kind) {
                NotificationKind.PRE -> context.getString(R.string.notification_pre_title, taskTitle)
                NotificationKind.MAIN -> context.getString(R.string.notification_deadline_title, taskTitle)
                NotificationKind.FOLLOWUP -> context.getString(R.string.notification_overdue_title, taskTitle)
            }
        }

        val body = if (isSubtask && parentTitle != null) {
            context.getString(R.string.notification_subtask_body, parentTitle)
        } else {
            when (kind) {
                NotificationKind.PRE -> context.getString(R.string.notification_pre_body)
                NotificationKind.MAIN -> context.getString(R.string.notification_deadline_body)
                NotificationKind.FOLLOWUP -> context.getString(R.string.notification_overdue_body)
            }
        }

        val priority = if (isSubtask) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(buildContentIntent(taskId, parentId))
            .addAction(
                AndroidR.drawable.checkbox_on_background,
                context.getString(R.string.notification_action_complete),
                buildActionIntent(taskId, taskTitle, NotificationActionReceiver.ACTION_COMPLETE, parentId, parentTitle),
            )
            .addAction(
                AndroidR.drawable.ic_lock_idle_alarm,
                context.getString(R.string.notification_action_snooze),
                buildActionIntent(taskId, taskTitle, NotificationActionReceiver.ACTION_SNOOZE, parentId, parentTitle),
            )

        if (parentId != null) {
            builder.setGroup("task_group_$parentId")
        }

        NotificationManagerCompat.from(context)
            .notify(notificationId(taskId, kind), builder.build())
    }

    /** Cancels any currently-displayed notification(s) for this task. */
    fun cancelDisplayed(taskId: String) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(notificationId(taskId, NotificationKind.MAIN))
        manager.cancel(notificationId(taskId, NotificationKind.FOLLOWUP))
    }

    private fun buildContentIntent(taskId: String, parentId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (parentId != null) {
                // Subtask: open the parent's detail with this subtask pre-expanded,
                // exactly like tapping the subtask on the list screen.
                putExtra(EXTRA_OPEN_TASK_ID, parentId)
                putExtra(EXTRA_EXPAND_SUBTASK_ID, taskId)
            } else {
                putExtra(EXTRA_OPEN_TASK_ID, taskId)
            }
        }
        return PendingIntent.getActivity(
            context,
            contentRequestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildActionIntent(
        taskId: String,
        taskTitle: String,
        action: String,
        parentId: String? = null,
        parentTitle: String? = null,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_TITLE, taskTitle)
            // Carry parent info so snooze re-enqueues with the right subtask context.
            if (parentId != null) putExtra(NotificationActionReceiver.EXTRA_PARENT_TASK_ID, parentId)
            if (parentTitle != null) putExtra(NotificationActionReceiver.EXTRA_PARENT_TASK_TITLE, parentTitle)
        }
        val requestCode = when (action) {
            NotificationActionReceiver.ACTION_COMPLETE -> completeRequestCode(taskId)
            else -> snoozeRequestCode(taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
