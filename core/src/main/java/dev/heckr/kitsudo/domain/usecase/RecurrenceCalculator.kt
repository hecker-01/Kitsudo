package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.RecurrenceUnit
import java.util.Calendar

/**
 * Pure date math for recurring deadlines. Kept Android-free so it is unit-testable
 * on the JVM.
 */
object RecurrenceCalculator {

    /**
     * The next deadline strictly **after** [after] for a task whose current
     * deadline is [current], repeating every [interval] [unit]s.
     *
     * Advancing in calendar units (via [Calendar.add]) keeps the local time-of-day
     * stable across DST and handles month-length differences (e.g. Jan 31 + 1 month
     * → Feb 28). When a task is overdue we keep advancing until the result is in the
     * future, so completing it lands on the next upcoming occurrence rather than a
     * past one.
     */
    fun nextDeadline(
        current: Long,
        unit: RecurrenceUnit,
        interval: Int,
        after: Long,
    ): Long {
        val step = interval.coerceAtLeast(1)
        val field = when (unit) {
            RecurrenceUnit.DAY -> Calendar.DAY_OF_MONTH
            RecurrenceUnit.WEEK -> Calendar.WEEK_OF_YEAR
            RecurrenceUnit.MONTH -> Calendar.MONTH
        }
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        // Always advance at least one period; keep going while not yet past `after`.
        do {
            cal.add(field, step)
        } while (cal.timeInMillis <= after)
        return cal.timeInMillis
    }
}
