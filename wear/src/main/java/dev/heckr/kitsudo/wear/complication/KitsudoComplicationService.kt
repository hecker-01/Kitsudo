package dev.heckr.kitsudo.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dev.heckr.kitsudo.wear.MainActivity
import dev.heckr.kitsudo.wear.R
import dev.heckr.kitsudo.wear.glance.TaskSummary
import dev.heckr.kitsudo.wear.glance.loadTaskSummary

/**
 * Complication data source exposing the "due today" count (SHORT_TEXT), a
 * completed/total progress ring (RANGED_VALUE), and a tappable icon
 * (MONOCHROMATIC_ICON). All variants open the watch app when tapped.
 */
class KitsudoComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        complicationData(
            type,
            TaskSummary(dueCount = 3, totalActive = 5, completedCount = 2, totalCount = 7, nextTitle = "Pay rent", nextDeadlineAt = null),
        )

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val summary = loadTaskSummary(this)
        return complicationData(request.complicationType, summary)
    }

    private fun complicationData(type: ComplicationType, summary: TaskSummary): ComplicationData? {
        val tap = openAppIntent()
        val description = PlainComplicationText.Builder(
            getString(R.string.complication_content_description, summary.dueCount),
        ).build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(summary.dueCount.toString()).build(),
                contentDescription = description,
            )
                .setTitle(PlainComplicationText.Builder(getString(R.string.complication_due_label)).build())
                .setMonochromaticImage(appIcon())
                .setTapAction(tap)
                .build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = summary.completedCount.toFloat(),
                min = 0f,
                max = summary.totalCount.coerceAtLeast(1).toFloat(),
                contentDescription = description,
            )
                .setText(
                    PlainComplicationText.Builder("${summary.completedCount}/${summary.totalCount}").build(),
                )
                .setMonochromaticImage(appIcon())
                .setTapAction(tap)
                .build()

            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
                monochromaticImage = appIcon(),
                contentDescription = description,
            )
                .setTapAction(tap)
                .build()

            else -> null
        }
    }

    private fun appIcon(): MonochromaticImage =
        MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_complication)).build()

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
