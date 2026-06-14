package dev.heckr.kitsudo.wear.glance

import android.content.ComponentName
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.tiles.TileService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.heckr.kitsudo.data.local.dao.TaskDao
import dev.heckr.kitsudo.data.mapper.toDomain
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import dev.heckr.kitsudo.ui.theme.accentColor
import dev.heckr.kitsudo.wear.complication.KitsudoComplicationService
import dev.heckr.kitsudo.wear.tile.KitsudoTileService
import kotlinx.coroutines.flow.first

/** Hilt graph access for the (non-@AndroidEntryPoint) Tile and complication services. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlanceEntryPoint {
    fun taskDao(): TaskDao
    fun themeRepository(): ThemeRepository
}

private fun entryPoint(context: Context): GlanceEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, GlanceEntryPoint::class.java)

/** Reads the watch's local task cache and reduces it to a [TaskSummary]. */
suspend fun loadTaskSummary(context: Context): TaskSummary {
    val tasks = entryPoint(context).taskDao().getAllOnce()
        .map { it.toDomain() }
        .filter { it.parentId == null }
    return summarizeTasks(tasks, System.currentTimeMillis())
}

/** The accent ARGB to highlight glanceable surfaces with, matching the app theme. */
suspend fun loadAccentArgb(context: Context): Int {
    val theme = entryPoint(context).themeRepository()
    val palette = theme.getThemePalette().first()
    return if (palette == ThemePalette.MATERIAL3) {
        theme.getM3Colors().first()?.primary ?: FALLBACK_ACCENT_ARGB
    } else {
        accentColor(palette, theme.getAccent().first()).toArgb()
    }
}

/** Mocha mauve, used when no accent can be resolved (e.g. Material You with no synced colors). */
const val FALLBACK_ACCENT_ARGB: Int = 0xFFCBA6F7.toInt()

/** Asks the system to refresh the Tile and all Kitsudo complications. */
object GlanceUpdater {
    fun requestUpdate(context: Context) {
        runCatching {
            TileService.getUpdater(context).requestUpdate(KitsudoTileService::class.java)
        }
        runCatching {
            ComplicationDataSourceUpdateRequester
                .create(context, ComponentName(context, KitsudoComplicationService::class.java))
                .requestUpdateAll()
        }
    }
}
