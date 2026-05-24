package dev.heckr.kitsudo.data.notification

import dev.heckr.kitsudo.domain.model.NotificationPreferences
import java.util.Calendar
import java.util.TimeZone

/**
 * Pure helpers for checking whether `now` falls inside the user's quiet-hours
 * window, and for computing the next time the window ends so a deferred
 * notification can be re-scheduled.
 *
 * Quiet-hours start/end are stored as **minutes from local midnight**. The
 * window may cross midnight (start = 22:00, end = 07:00) — both cases are
 * handled correctly.
 *
 * All helpers are no-ops (or return [nowMillis] verbatim) when the user has
 * disabled quiet hours via [NotificationPreferences.quietHoursEnabled].
 */
object QuietHours {

    /** Returns true if [nowMillis] falls inside the configured quiet window. */
    fun isInside(nowMillis: Long, prefs: NotificationPreferences): Boolean {
        if (!prefs.quietHoursEnabled) return false
        if (prefs.quietStartMinutes == prefs.quietEndMinutes) return false
        val nowMin = minutesOfDay(nowMillis)
        val start = prefs.quietStartMinutes
        val end = prefs.quietEndMinutes
        return if (start < end) {
            nowMin in start until end
        } else {
            // window crosses midnight: e.g. 22:00..07:00 covers 22..24 and 0..7
            nowMin >= start || nowMin < end
        }
    }

    /**
     * Returns the epoch-ms when the current quiet-hours window ends. Caller
     * should only invoke this when [isInside] returned true. Returns
     * [nowMillis] verbatim when quiet hours are disabled.
     */
    fun nextEndAfter(nowMillis: Long, prefs: NotificationPreferences): Long {
        if (!prefs.quietHoursEnabled) return nowMillis
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, prefs.quietEndMinutes / 60)
            set(Calendar.MINUTE, prefs.quietEndMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If we computed an "end" earlier today but it's already passed (which
        // happens when the window crosses midnight and we're inside the post-
        // midnight portion), the end is today's hh:mm; otherwise it's tomorrow.
        return if (cal.timeInMillis > nowMillis) {
            cal.timeInMillis
        } else {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.timeInMillis
        }
    }

    private fun minutesOfDay(epochMillis: Long): Int {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = epochMillis
        }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}
