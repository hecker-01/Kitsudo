package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.RecurrenceUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RecurrenceCalculatorTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 9, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            timeZone = TimeZone.getDefault()
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis

    @Test
    fun `daily advances one day when not overdue`() {
        val deadline = millis(2026, Calendar.JUNE, 14)
        val justBefore = deadline - 1
        val next = RecurrenceCalculator.nextDeadline(deadline, RecurrenceUnit.DAY, 1, justBefore)
        assertEquals(millis(2026, Calendar.JUNE, 15), next)
    }

    @Test
    fun `weekly with interval advances multiple weeks`() {
        val deadline = millis(2026, Calendar.JUNE, 1)
        val next = RecurrenceCalculator.nextDeadline(deadline, RecurrenceUnit.WEEK, 2, deadline)
        assertEquals(millis(2026, Calendar.JUNE, 15), next)
    }

    @Test
    fun `monthly clamps to shorter month`() {
        val deadline = millis(2026, Calendar.JANUARY, 31)
        val next = RecurrenceCalculator.nextDeadline(deadline, RecurrenceUnit.MONTH, 1, deadline)
        // Feb 2026 has 28 days; Calendar rolls Jan 31 + 1 month to Feb 28.
        assertEquals(millis(2026, Calendar.FEBRUARY, 28), next)
    }

    @Test
    fun `skips past missed occurrences for an overdue task`() {
        val deadline = millis(2026, Calendar.JUNE, 1)
        // "Now" is 10 days later; a daily task should jump to the next future day.
        val now = millis(2026, Calendar.JUNE, 11)
        val next = RecurrenceCalculator.nextDeadline(deadline, RecurrenceUnit.DAY, 1, now)
        assertTrue("next deadline must be in the future", next > now)
        assertEquals(millis(2026, Calendar.JUNE, 12), next)
    }

    @Test
    fun `always advances at least one period`() {
        val deadline = millis(2026, Calendar.JUNE, 14)
        // after is well before the deadline; result must still move forward by one.
        val next = RecurrenceCalculator.nextDeadline(deadline, RecurrenceUnit.DAY, 1, 0L)
        assertEquals(millis(2026, Calendar.JUNE, 15), next)
    }
}
