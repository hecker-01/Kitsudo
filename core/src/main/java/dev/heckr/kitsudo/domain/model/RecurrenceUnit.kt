package dev.heckr.kitsudo.domain.model

/**
 * The period a recurring task repeats on. Combined with an interval (e.g. unit
 * [WEEK] + interval 2 = "every 2 weeks"). Persisted by [name]; the database keeps
 * a `recurrenceUnit` text column that is null for non-recurring tasks.
 */
enum class RecurrenceUnit {
    DAY,
    WEEK,
    MONTH,
    ;

    companion object {
        /** Tolerant parse from the persisted name; null (no recurrence) on miss. */
        fun fromDb(value: String?): RecurrenceUnit? =
            value?.let { v -> entries.firstOrNull { it.name == v } }
    }
}
