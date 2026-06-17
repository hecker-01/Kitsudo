package dev.heckr.kitsudo.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class DeadlineTextParserTest {

    private val zone = ZoneId.of("UTC")

    // Reference "now": Wednesday 2026-06-17, 10:00 UTC.
    private val clock: Clock = Clock.fixed(
        LocalDateTime.of(2026, 6, 17, 10, 0).toInstant(ZoneOffset.UTC),
        zone,
    )

    private fun millis(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone).toInstant().toEpochMilli()

    private fun parse(text: String) = DeadlineTextParser.parse(text, clock)

    @Test
    fun `pm time later today`() {
        val result = parse("buy milk at 5pm")
        assertEquals("buy milk", result.title)
        assertEquals(millis(2026, 6, 17, 17, 0), result.deadlineAt)
    }

    @Test
    fun `am time already past rolls to tomorrow`() {
        // 9am is before the 10am reference, so it lands tomorrow.
        val result = parse("standup at 9am")
        assertEquals("standup", result.title)
        assertEquals(millis(2026, 6, 18, 9, 0), result.deadlineAt)
    }

    @Test
    fun `24 hour clock time`() {
        val result = parse("deploy at 17:30")
        assertEquals("deploy", result.title)
        assertEquals(millis(2026, 6, 17, 17, 30), result.deadlineAt)
    }

    @Test
    fun `meridiem without at prefix`() {
        val result = parse("call dad 6:15pm")
        assertEquals("call dad", result.title)
        assertEquals(millis(2026, 6, 17, 18, 15), result.deadlineAt)
    }

    @Test
    fun `tomorrow with time`() {
        val result = parse("dentist tomorrow at 9am")
        assertEquals("dentist", result.title)
        assertEquals(millis(2026, 6, 18, 9, 0), result.deadlineAt)
    }

    @Test
    fun `tomorrow without time defaults to morning`() {
        val result = parse("water the plants tomorrow")
        assertEquals("water the plants", result.title)
        assertEquals(millis(2026, 6, 18, 9, 0), result.deadlineAt)
    }

    @Test
    fun `tonight defaults to evening`() {
        val result = parse("take out bins tonight")
        assertEquals("take out bins", result.title)
        assertEquals(millis(2026, 6, 17, 18, 0), result.deadlineAt)
    }

    @Test
    fun `noon`() {
        val result = parse("lunch at noon")
        assertEquals("lunch", result.title)
        assertEquals(millis(2026, 6, 17, 12, 0), result.deadlineAt)
    }

    @Test
    fun `ambiguous bare hour picks soonest upcoming`() {
        // "at 5" from 10:00: 05:00 is past, 17:00 is the soonest reading.
        val result = parse("meeting at 5")
        assertEquals("meeting", result.title)
        assertEquals(millis(2026, 6, 17, 17, 0), result.deadlineAt)
    }

    @Test
    fun `weekday resolves to next occurrence`() {
        // Wednesday -> next Friday is two days out.
        val result = parse("submit report on friday")
        assertEquals("submit report", result.title)
        assertEquals(millis(2026, 6, 19, 9, 0), result.deadlineAt)
    }

    @Test
    fun `same weekday as today jumps a full week`() {
        // Reference day is Wednesday.
        val result = parse("review wednesday")
        assertEquals("review", result.title)
        assertEquals(millis(2026, 6, 24, 9, 0), result.deadlineAt)
    }

    @Test
    fun `no time phrase leaves title untouched`() {
        val result = parse("buy 2 apples")
        assertEquals("buy 2 apples", result.title)
        assertNull(result.deadlineAt)
    }

    @Test
    fun `at without a time is not treated as a deadline`() {
        val result = parse("call mum at work")
        assertEquals("call mum at work", result.title)
        assertNull(result.deadlineAt)
    }

    @Test
    fun `empty input`() {
        val result = parse("")
        assertEquals("", result.title)
        assertNull(result.deadlineAt)
    }

    @Test
    fun `midnight`() {
        val result = parse("backup at midnight")
        assertEquals("backup", result.title)
        // Midnight today is past 10:00, so it rolls to tomorrow.
        assertEquals(millis(2026, 6, 18, 0, 0), result.deadlineAt)
    }
}
