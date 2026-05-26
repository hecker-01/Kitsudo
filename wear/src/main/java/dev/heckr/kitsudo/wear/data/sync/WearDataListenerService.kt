package dev.heckr.kitsudo.wear.data.sync

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.heckr.kitsudo.data.local.dao.TaskDao
import dev.heckr.kitsudo.data.mapper.toEntity
import dev.heckr.kitsudo.data.sync.TaskDto
import dev.heckr.kitsudo.data.sync.WearSyncPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Receives the task-list snapshot pushed by the phone's PhoneTaskPublisher
 * and writes it into the watch's local Room database atomically.
 *
 * NOTE: [WearableListenerService] conflicts with Hilt's [dagger.hilt.android.AndroidEntryPoint]
 * binder instrumentation. [EntryPointAccessors] is used instead.
 *
 * [runBlocking] is safe here: [onDataChanged] is called on a background
 * thread by Google Play Services, and a full table-replace completes quickly.
 */
class WearDataListenerService : WearableListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WearSyncEntryPoint {
        fun taskDao(): TaskDao
    }

    private val taskDao by lazy {
        EntryPointAccessors.fromApplication<WearSyncEntryPoint>(applicationContext).taskDao()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: received ${dataEvents.count} events")
        dataEvents.forEach { event ->
            Log.d(TAG, "onDataChanged: event type=${event.type} path=${event.dataItem.uri.path}")
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == WearSyncPaths.TASKS_SNAPSHOT
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val json = dataMap.getString(WearSyncPaths.KEY_TASKS_JSON) ?: run {
                    Log.e(TAG, "onDataChanged: tasks_json key missing from DataMap")
                    return@forEach
                }
                Log.d(TAG, "onDataChanged: applying snapshot, json length=${json.length}")
                runBlocking(Dispatchers.IO) {
                    applySnapshot(json)
                }
            }
        }
    }

    private suspend fun applySnapshot(json: String) {
        try {
            val dtoList: List<TaskDto> = Json.decodeFromString(json)
            Log.d(TAG, "applySnapshot: decoded ${dtoList.size} DTOs, writing to DB")
            val entities = dtoList.map { it.toDomain().toEntity() }
            taskDao.deleteAll()
            taskDao.insertAll(entities)
            Log.d(TAG, "applySnapshot: DB write complete")
        } catch (e: Exception) {
            Log.e(TAG, "applySnapshot: failed", e)
        }
    }

    companion object {
        private const val TAG = "WearDataListener"
    }
}
