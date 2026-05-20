package dev.heckr.kitsudo.data.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.heckr.kitsudo.domain.repository.TaskRepository

@HiltWorker
class DeadlineNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
    }

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: return Result.failure()

        val task = taskRepository.getTaskById(taskId)
            ?: return Result.success() // task was deleted — nothing to notify

        if (task.isCompleted) return Result.success() // already done

        notificationHelper.showDeadlineNotification(taskId, taskTitle)
        return Result.success()
    }
}
