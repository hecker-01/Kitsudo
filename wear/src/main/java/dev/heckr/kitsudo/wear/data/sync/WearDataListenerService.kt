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
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.M3WearColors
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import dev.heckr.kitsudo.wear.glance.GlanceUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Receives the task-list snapshot pushed by the phone's PhoneTaskPublisher
 * and writes it into the watch's local Room database atomically.
 * Also reads the theme/accent keys and persists them to DataStore so
 * [KitsudoWearTheme] stays in sync with the phone.
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
        fun themeRepository(): ThemeRepository
    }

    private val ep by lazy {
        EntryPointAccessors.fromApplication<WearSyncEntryPoint>(applicationContext)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: received ${dataEvents.count} events")
        dataEvents.forEach { event ->
            Log.d(TAG, "onDataChanged: type=${event.type} path=${event.dataItem.uri.path}")
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == WearSyncPaths.TASKS_SNAPSHOT
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val json = dataMap.getString(WearSyncPaths.KEY_TASKS_JSON) ?: run {
                    Log.e(TAG, "tasks_json key missing")
                    return@forEach
                }
                val themeName  = dataMap.getString(WearSyncPaths.KEY_THEME)
                val accentName = dataMap.getString(WearSyncPaths.KEY_ACCENT)
                Log.d(TAG, "applying snapshot: json=${json.length}ch, theme=$themeName, accent=$accentName")
                val m3Colors = readM3Colors(dataMap)
                runBlocking(Dispatchers.IO) {
                    applySnapshot(json)
                    applyTheme(themeName, accentName)
                    m3Colors?.let { ep.themeRepository().setM3Colors(it) }
                }
                // Refresh the Tile and complications with the new data.
                GlanceUpdater.requestUpdate(applicationContext)
            }
        }
    }

    private suspend fun applySnapshot(json: String) {
        try {
            val dtoList: List<TaskDto> = Json.decodeFromString(json)
            Log.d(TAG, "applySnapshot: decoded ${dtoList.size} DTOs")
            val entities = dtoList.map { it.toDomain().toEntity() }
            // Atomic swap: a kill between clear and insert can't empty the watch DB.
            ep.taskDao().replaceAll(entities)
        } catch (e: Exception) {
            Log.e(TAG, "applySnapshot failed", e)
        }
    }

    private suspend fun applyTheme(themeName: String?, accentName: String?) {
        try {
            themeName?.let { name ->
                val palette = ThemePalette.entries.firstOrNull { it.name == name } ?: return@let
                ep.themeRepository().setThemePalette(palette)
            }
            accentName?.let { name ->
                val accent = CatppuccinAccent.fromName(name)
                ep.themeRepository().setAccent(accent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyTheme failed", e)
        }
    }

    /**
     * Reads M3 dynamic color ARGB values from the DataMap, or returns null if
     * the phone didn't include them (theme wasn't MATERIAL3, or older phone build).
     */
    private fun readM3Colors(dataMap: com.google.android.gms.wearable.DataMap): M3WearColors? {
        if (!dataMap.containsKey(WearSyncPaths.KEY_M3_PRIMARY)) return null
        return try {
            M3WearColors(
                primary             = dataMap.getInt(WearSyncPaths.KEY_M3_PRIMARY),
                onPrimary           = dataMap.getInt(WearSyncPaths.KEY_M3_ON_PRIMARY),
                tertiary            = dataMap.getInt(WearSyncPaths.KEY_M3_TERTIARY),
                error               = dataMap.getInt(WearSyncPaths.KEY_M3_ERROR),
                onSurface           = dataMap.getInt(WearSyncPaths.KEY_M3_ON_SURFACE),
                onSurfaceVariant    = dataMap.getInt(WearSyncPaths.KEY_M3_ON_SURF_VAR),
                outline             = dataMap.getInt(WearSyncPaths.KEY_M3_OUTLINE),
                surfaceContainerLow  = dataMap.getInt(WearSyncPaths.KEY_M3_SURF_LOW),
                surfaceContainer     = dataMap.getInt(WearSyncPaths.KEY_M3_SURF_MID),
                surfaceContainerHigh = dataMap.getInt(WearSyncPaths.KEY_M3_SURF_HIGH),
            )
        } catch (e: Exception) {
            Log.e(TAG, "readM3Colors failed", e)
            null
        }
    }

    companion object {
        private const val TAG = "WearDataListener"
    }
}
