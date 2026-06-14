package dev.heckr.kitsudo.wear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import dev.heckr.kitsudo.wear.MainActivity
import dev.heckr.kitsudo.wear.R
import dev.heckr.kitsudo.wear.glance.TaskSummary
import dev.heckr.kitsudo.wear.glance.loadAccentArgb
import dev.heckr.kitsudo.wear.glance.loadTaskSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * A glanceable Tile showing the next upcoming deadline plus a count of tasks due
 * today (overdue + later today). Tapping anywhere opens the watch app.
 */
class KitsudoTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<Tile> = scope.future {
        val summary = loadTaskSummary(this@KitsudoTileService)
        val accent = loadAccentArgb(this@KitsudoTileService)
        Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(TimeUnit.MINUTES.toMillis(30))
            .setTileTimeline(
                Timeline.fromLayoutElement(tileLayout(this@KitsudoTileService, summary, accent)),
            )
            .build()
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<Resources> = scope.future {
        Resources.Builder().setVersion(RESOURCES_VERSION).build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val RESOURCES_VERSION = "1"
    }
}

private fun tileLayout(context: Context, summary: TaskSummary, accentArgb: Int): LayoutElement {
    val onSurface = 0xFFE6E1E5.toInt()
    val muted = 0xFF9A9A9A.toInt()

    val title = if (summary.hasNext) summary.nextTitle!! else context.getString(R.string.tile_all_clear)
    val subtitle = if (summary.hasNext) {
        relativeDeadline(context, summary.nextDeadlineAt!!)
    } else {
        context.getString(R.string.tile_no_deadlines)
    }
    val footer = if (summary.dueCount > 0) {
        context.getString(R.string.tile_due_format, summary.dueCount)
    } else {
        context.getString(R.string.tile_active_format, summary.totalActive)
    }

    val column = Column.Builder()
        .setWidth(expand())
        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
        .addContent(
            Text.Builder(context, title)
                .setTypography(Typography.TYPOGRAPHY_TITLE3)
                .setColor(argb(onSurface))
                .setMaxLines(2)
                .build(),
        )
        .addContent(Spacer.Builder().setHeight(dp(4f)).build())
        .addContent(
            Text.Builder(context, subtitle)
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setColor(argb(muted))
                .setMaxLines(1)
                .build(),
        )
        .addContent(Spacer.Builder().setHeight(dp(10f)).build())
        .addContent(
            Text.Builder(context, footer)
                .setTypography(Typography.TYPOGRAPHY_BUTTON)
                .setColor(argb(if (summary.dueCount > 0) accentArgb else muted))
                .setMaxLines(1)
                .build(),
        )
        .build()

    return Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setModifiers(
            Modifiers.Builder().setClickable(openAppClickable(context)).build(),
        )
        .addContent(column)
        .build()
}

private fun openAppClickable(context: Context): Clickable =
    Clickable.Builder()
        .setId("open_kitsudo")
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(context.packageName)
                        .setClassName(MainActivity::class.java.name)
                        .build(),
                )
                .build(),
        )
        .build()

/** "Overdue", "Today HH:mm", "Tomorrow", or "MMM d" for the next deadline. */
private fun relativeDeadline(context: Context, deadlineAt: Long): String {
    val now = System.currentTimeMillis()
    if (deadlineAt < now) return context.getString(R.string.tile_overdue)
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_YEAR)
    val year = cal.get(Calendar.YEAR)
    cal.timeInMillis = deadlineAt
    val sameDay = cal.get(Calendar.YEAR) == year && cal.get(Calendar.DAY_OF_YEAR) == today
    val tomorrow = cal.get(Calendar.YEAR) == year && cal.get(Calendar.DAY_OF_YEAR) == today + 1
    return when {
        sameDay -> context.getString(
            R.string.tile_today_format,
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(deadlineAt)),
        )
        tomorrow -> context.getString(R.string.tile_tomorrow)
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(deadlineAt))
    }
}
