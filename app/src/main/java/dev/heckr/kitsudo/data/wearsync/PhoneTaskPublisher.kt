package dev.heckr.kitsudo.data.wearsync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.heckr.kitsudo.data.sync.TaskDto
import dev.heckr.kitsudo.data.sync.WearSyncPaths
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import dev.heckr.kitsudo.domain.usecase.GetTasksUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes the phone's task list to all connected Wear OS nodes via the
 * Wearable Data Layer.
 *
 * - [start]: long-lived collection — call once from Application scope so the
 *   watch receives a fresh snapshot whenever the phone DB changes.
 * - [publishOnce]: one-shot — collect the current snapshot and push it once;
 *   used by [PhoneSyncService] when the watch requests a sync while the
 *   phone app may not be running in the foreground.
 */
@Singleton
class PhoneTaskPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getTasksUseCase: GetTasksUseCase,
) {
    private val dataClient by lazy { Wearable.getDataClient(context) }

    /** Continuously publishes snapshots whenever the task list changes. */
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            getTasksUseCase().collect { taskList ->
                publishSnapshot(taskList)
            }
        }
    }

    /**
     * Collects the current task list once and publishes it immediately.
     * Safe to call from [runBlocking] inside a [WearableListenerService].
     */
    suspend fun publishOnce() {
        val taskList = getTasksUseCase().first()
        publishSnapshot(taskList)
    }

    private suspend fun publishSnapshot(tasks: List<TaskWithSubtasks>) {
        try {
            val dtoList = tasks.flatMap { tws ->
                listOf(TaskDto.fromDomain(tws.task)) + tws.subtasks.map { TaskDto.fromDomain(it) }
            }
            Log.d(TAG, "publishSnapshot: pushing ${dtoList.size} task DTOs")
            val json = Json.encodeToString(dtoList)
            val request = PutDataMapRequest.create(WearSyncPaths.TASKS_SNAPSHOT).apply {
                dataMap.putString(WearSyncPaths.KEY_TASKS_JSON, json)
                dataMap.putLong(WearSyncPaths.KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()
            Log.d(TAG, "publishSnapshot: DataClient.putDataItem succeeded")
        } catch (e: Exception) {
            Log.e(TAG, "publishSnapshot: failed", e)
        }
    }

    companion object {
        private const val TAG = "PhoneTaskPublisher"
    }
}
