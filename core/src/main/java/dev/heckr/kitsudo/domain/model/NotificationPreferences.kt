package dev.heckr.kitsudo.domain.model

/**
 * Global, user-tunable notification settings. Per-task overrides are intentionally
 * not supported - every task with a deadline uses these values.
 *
 * @param preReminderLeadMinutes Set of lead times (in minutes before the deadline) at
 *   which to fire an extra reminder. Empty = no pre-reminder. Each value schedules its
 *   own notification, so multiple reminders can fire before one deadline. Allowed values
 *   match the UI chip row: 15, 30, 60, 1440.
 * @param quietHoursEnabled When true, notifications that would fire inside
 *   [quietStartMinutes]..[quietEndMinutes] are deferred until the window ends.
 * @param quietStartMinutes Minutes from midnight (0..1439).
 * @param quietEndMinutes Minutes from midnight (0..1439). May be less than
 *   [quietStartMinutes] - the window crosses midnight in that case.
 * @param snoozeMinutes How long the Snooze notification action defers the
 *   notification before re-firing it.
 */
data class NotificationPreferences(
    val preReminderLeadMinutes: Set<Int> = emptySet(),
    val quietHoursEnabled: Boolean = false,
    val quietStartMinutes: Int = 22 * 60, // 22:00
    val quietEndMinutes: Int = 7 * 60, //  7:00
    val snoozeMinutes: Int = 10,
)
