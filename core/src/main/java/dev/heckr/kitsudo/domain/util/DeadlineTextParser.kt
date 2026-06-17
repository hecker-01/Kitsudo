package dev.heckr.kitsudo.domain.util

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Pulls a natural-language deadline out of a free-text task title - typically an
 * utterance captured by Google Assistant / Gemini such as "buy milk at 5pm" or
 * "call the dentist tomorrow at 9".
 *
 * [parse] returns the title with any recognised date/time words stripped, plus
 * the resolved epoch-millisecond deadline (or null when nothing time-like is
 * found). It is deliberately conservative: a bare number is only treated as a
 * time when introduced by "at"/"@", written with a colon (17:00), or carrying an
 * am/pm suffix, so "buy 2 apples" or "call mum at work" keep their full title.
 */
object DeadlineTextParser {

    data class Result(val title: String, val deadlineAt: Long?)

    /** A time-of-day that may still be am/pm ambiguous (a bare "at 5"). */
    private sealed interface TimeSpec {
        data class Exact(val time: LocalTime) : TimeSpec
        data class Ambiguous(val hour: Int, val minute: Int) : TimeSpec
    }

    private const val DEFAULT_HOUR = 9
    private const val EVENING_HOUR = 18

    // Day-of-week names and common abbreviations, longest-first so "thurs"
    // wins over "thu". The optional "next"/"on" prefix is consumed too.
    private val DAY_REGEX = Regex(
        "\\b(?:on\\s+|next\\s+)?(today|tonight|tomorrow|" +
            "monday|mon|tuesday|tues|tue|wednesday|weds|wed|" +
            "thursday|thurs|thur|thu|friday|fri|saturday|sat|sunday|sun)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val NAMED_TIME = Regex("\\b(noon|midday|midnight)\\b", RegexOption.IGNORE_CASE)
    // "at 5", "at 5:30", "at 5pm", "@5", "at 17:00".
    private val AT_TIME = Regex(
        "\\b(?:at|@)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*([ap])\\.?m\\.?\\b|" +
            "\\b(?:at|@)\\s*(\\d{1,2})(?::(\\d{2}))?\\b",
        RegexOption.IGNORE_CASE,
    )
    // "5pm", "5:30 pm" - a meridiem makes the time unambiguous without "at".
    private val MERIDIEM_TIME = Regex(
        "\\b(\\d{1,2})(?::(\\d{2}))?\\s*([ap])\\.?m\\.?\\b",
        RegexOption.IGNORE_CASE,
    )
    // "17:00" - a colon marks an explicit 24-hour clock time.
    private val COLON_TIME = Regex("\\b(\\d{1,2}):(\\d{2})\\b")

    // Trailing connector left dangling after a date/time phrase is removed.
    private val TRAILING_CONNECTOR = Regex("[\\s,]+(?:at|on|by|@)$", RegexOption.IGNORE_CASE)

    fun parse(raw: String, clock: Clock = Clock.systemDefaultZone()): Result {
        val text = raw.trim()
        if (text.isEmpty()) return Result(text, null)

        val now = ZonedDateTime.now(clock)
        val removals = mutableListOf<IntRange>()

        // -- Day -------------------------------------------------------------
        var date: LocalDate? = null
        var eveningDefault = false
        DAY_REGEX.find(text)?.let { match ->
            val word = match.groupValues[1].lowercase()
            date = resolveDay(word, now.toLocalDate())
            eveningDefault = word == "tonight"
            removals += match.range
        }

        // -- Time ------------------------------------------------------------
        val time = extractTime(text, removals)

        val deadlineAt = resolve(date, eveningDefault, time, now)
        if (deadlineAt == null) return Result(text, null)

        return Result(stripRanges(text, removals), deadlineAt.toInstant().toEpochMilli())
    }

    /** Finds the first qualifying time phrase, recording its range for removal. */
    private fun extractTime(text: String, removals: MutableList<IntRange>): TimeSpec? {
        NAMED_TIME.find(text)?.let { match ->
            val spec = when (match.groupValues[1].lowercase()) {
                "midnight" -> TimeSpec.Exact(LocalTime.MIDNIGHT)
                else -> TimeSpec.Exact(LocalTime.NOON) // noon / midday
            }
            removals += match.range
            return spec
        }
        AT_TIME.find(text)?.let { match ->
            // First alternative has the meridiem (groups 1-3); second is bare (4-5).
            val withMeridiem = match.groupValues[1].isNotEmpty()
            val hour = (if (withMeridiem) match.groupValues[1] else match.groupValues[4]).toInt()
            val minute = (if (withMeridiem) match.groupValues[2] else match.groupValues[5])
                .toIntOrNull() ?: 0
            val meridiem = match.groupValues[3].lowercase().ifEmpty { null }
            toTimeSpec(hour, minute, meridiem)?.let { removals += match.range; return it }
        }
        MERIDIEM_TIME.find(text)?.let { match ->
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            toTimeSpec(hour, minute, match.groupValues[3].lowercase())?.let {
                removals += match.range
                return it
            }
        }
        COLON_TIME.find(text)?.let { match ->
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            if (hour in 0..23 && minute in 0..59) {
                removals += match.range
                return TimeSpec.Exact(LocalTime.of(hour, minute))
            }
        }
        return null
    }

    private fun toTimeSpec(hour: Int, minute: Int, meridiem: String?): TimeSpec? {
        if (minute !in 0..59) return null
        return when {
            meridiem == "a" -> if (hour in 1..12) {
                TimeSpec.Exact(LocalTime.of(hour % 12, minute))
            } else {
                null
            }
            meridiem == "p" -> if (hour in 1..12) {
                TimeSpec.Exact(LocalTime.of((hour % 12) + 12, minute))
            } else {
                null
            }
            hour in 13..23 -> TimeSpec.Exact(LocalTime.of(hour, minute))
            hour == 0 -> TimeSpec.Exact(LocalTime.MIDNIGHT)
            hour in 1..12 -> TimeSpec.Ambiguous(hour, minute)
            else -> null
        }
    }

    private fun resolveDay(word: String, today: LocalDate): LocalDate = when (word) {
        "today", "tonight" -> today
        "tomorrow" -> today.plusDays(1)
        else -> dayOfWeek(word)?.let { nextWeekday(today, it) } ?: today
    }

    private fun dayOfWeek(word: String): DayOfWeek? = when {
        word.startsWith("mon") -> DayOfWeek.MONDAY
        word.startsWith("tue") -> DayOfWeek.TUESDAY
        word.startsWith("wed") -> DayOfWeek.WEDNESDAY
        word.startsWith("thu") -> DayOfWeek.THURSDAY
        word.startsWith("fri") -> DayOfWeek.FRIDAY
        word.startsWith("sat") -> DayOfWeek.SATURDAY
        word.startsWith("sun") -> DayOfWeek.SUNDAY
        else -> null
    }

    /** The next date strictly after [today] that falls on [target]. */
    private fun nextWeekday(today: LocalDate, target: DayOfWeek): LocalDate {
        val delta = (target.value - today.dayOfWeek.value + 7) % 7
        return today.plusDays(if (delta == 0) 7L else delta.toLong())
    }

    private fun resolve(
        date: LocalDate?,
        eveningDefault: Boolean,
        time: TimeSpec?,
        now: ZonedDateTime,
    ): ZonedDateTime? {
        val zone = now.zone
        if (time == null) {
            // A day with no time: default to morning (or evening for "tonight").
            val day = date ?: return null
            val hour = if (eveningDefault) EVENING_HOUR else DEFAULT_HOUR
            return day.atTime(LocalTime.of(hour, 0)).atZone(zone)
        }
        return when (time) {
            is TimeSpec.Exact -> {
                var dt = (date ?: now.toLocalDate()).atTime(time.time).atZone(zone)
                // A time without a day rolls to tomorrow once it is already past.
                if (date == null && !dt.isAfter(now)) dt = dt.plusDays(1)
                dt
            }
            is TimeSpec.Ambiguous -> resolveAmbiguous(date, time, now, zone)
        }
    }

    /** Picks the soonest upcoming am/pm reading of a bare hour like "at 5". */
    private fun resolveAmbiguous(
        date: LocalDate?,
        time: TimeSpec.Ambiguous,
        now: ZonedDateTime,
        zone: java.time.ZoneId,
    ): ZonedDateTime {
        val amHour = if (time.hour == 12) 0 else time.hour
        val pmHour = if (time.hour == 12) 12 else time.hour + 12
        val base = date ?: now.toLocalDate()
        val candidates = buildList {
            add(base.atTime(amHour, time.minute).atZone(zone))
            add(base.atTime(pmHour, time.minute).atZone(zone))
            if (date == null) {
                add(base.plusDays(1).atTime(amHour, time.minute).atZone(zone))
                add(base.plusDays(1).atTime(pmHour, time.minute).atZone(zone))
            }
        }
        return candidates.filter { it.isAfter(now) }.minOrNull()
            ?: candidates.min()
    }

    /** Removes the recorded ranges from [text] and tidies leftover whitespace. */
    private fun stripRanges(text: String, ranges: List<IntRange>): String {
        var result = text
        ranges.sortedByDescending { it.first }.forEach { range ->
            result = result.removeRange(range)
        }
        return result
            .replace(Regex("\\s+"), " ")
            .trim()
            .replace(TRAILING_CONNECTOR, "")
            .trim()
            .trim(',', ' ')
    }
}
