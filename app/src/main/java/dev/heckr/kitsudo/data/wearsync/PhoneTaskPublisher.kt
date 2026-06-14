package dev.heckr.kitsudo.data.wearsync

import android.content.Context
import android.util.Log
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.heckr.kitsudo.data.sync.TaskDto
import dev.heckr.kitsudo.data.sync.WearSyncPaths
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.usecase.GetAccentUseCase
import dev.heckr.kitsudo.domain.usecase.GetTasksUseCase
import dev.heckr.kitsudo.domain.usecase.GetThemePaletteUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes the phone's task list **and** active theme to all connected Wear OS
 * nodes via the Wearable Data Layer.
 *
 * - [start]: long-lived collection — call once from Application scope so the
 *   watch receives a fresh snapshot whenever the phone DB or theme changes.
 * - [publishOnce]: one-shot — used by [PhoneSyncService] when the watch requests
 *   a sync while the phone app may not be running in the foreground.
 */
@Singleton
class PhoneTaskPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getTasksUseCase: GetTasksUseCase,
    private val getThemePaletteUseCase: GetThemePaletteUseCase,
    private val getAccentUseCase: GetAccentUseCase,
) {
    private val dataClient by lazy { Wearable.getDataClient(context) }

    /** Continuously publishes snapshots whenever tasks, theme, or accent changes. */
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            combine(
                getTasksUseCase(),
                getThemePaletteUseCase(),
                getAccentUseCase(),
            ) { tasks, theme, accent -> Triple(tasks, theme, accent) }
                .collect { (tasks, theme, accent) ->
                    publishSnapshot(tasks, theme, accent)
                }
        }
    }

    /**
     * Collects the current state once and publishes it immediately.
     * Safe to call from [runBlocking] inside a [WearableListenerService].
     */
    suspend fun publishOnce() {
        val tasks  = getTasksUseCase().first()
        val theme  = getThemePaletteUseCase().first()
        val accent = getAccentUseCase().first()
        publishSnapshot(tasks, theme, accent)
    }

    private suspend fun publishSnapshot(
        tasks: List<TaskWithSubtasks>,
        theme: ThemePalette,
        accent: CatppuccinAccent,
    ) {
        try {
            val dtoList = tasks.flatMap { tws ->
                listOf(TaskDto.fromDomain(tws.task)) + tws.subtasks.map { TaskDto.fromDomain(it) }
            }
            Log.d(TAG, "publishSnapshot: ${dtoList.size} tasks, theme=$theme, accent=$accent")
            val json = Json.encodeToString(dtoList)
            val request = PutDataMapRequest.create(WearSyncPaths.TASKS_SNAPSHOT).apply {
                dataMap.putString(WearSyncPaths.KEY_TASKS_JSON, json)
                dataMap.putLong(WearSyncPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                dataMap.putString(WearSyncPaths.KEY_THEME, theme.name)
                dataMap.putString(WearSyncPaths.KEY_ACCENT, accent.name)
                // When Material You is active, push the phone's resolved dynamic colors so
                // the watch (which has no wallpaper) can mirror the same palette.
                if (theme == ThemePalette.MATERIAL3) {
                    val m3 = dynamicDarkColorScheme(context)
                    dataMap.putInt(WearSyncPaths.KEY_M3_PRIMARY,     m3.primary.toArgb())
                    dataMap.putInt(WearSyncPaths.KEY_M3_ON_PRIMARY,   m3.onPrimary.toArgb())
                    dataMap.putInt(WearSyncPaths.KEY_M3_TERTIARY,     m3.tertiary.toArgb())
                    dataMap.putInt(WearSyncPaths.KEY_M3_ERROR,        m3.error.toArgb())
                    dataMap.putInt(WearSyncPaths.KEY_M3_ON_SURFACE,   m3.onSurface.toArgb())
                    dataMap.putInt(WearSyncPaths.KEY_M3_ON_SURF_VAR,  m3.onSurfaceVariant.toArgb())
                    dataMap.putInt(WearSyncPaths.KEY_M3_OUTLINE,      m3.outline.toArgb())
                    dataMap.putInt(WearSyncPaths.KEY_M3_SURF_LOW,     m3.surfaceContainerLow.toArgb())
                    dataMap.putInt(WearSyncPaths.KEY_M3_SURF_MID,     m3.surfaceContainer.toArgb())
                    dataMap.putInt(WearSyncPaths.KEY_M3_SURF_HIGH,    m3.surfaceContainerHigh.toArgb())
                }
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()
            Log.d(TAG, "publishSnapshot: succeeded")
        } catch (e: Exception) {
            Log.e(TAG, "publishSnapshot: failed", e)
        }
    }

    companion object {
        private const val TAG = "PhoneTaskPublisher"
    }
}
