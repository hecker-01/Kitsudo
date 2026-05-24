package dev.heckr.kitsudo.data.notification

/**
 * Which point in a task's lifecycle a given notification represents. The worker
 * uses this to pick the right title/body strings and to decide whether to
 * chain a follow-up.
 */
enum class NotificationKind {
    /** Pre-deadline reminder ("Due in 1 hour: …"). */
    PRE,

    /** At-the-deadline notification ("Due: …"). */
    MAIN,

    /** Follow-up ping fired after the deadline has passed and task is still incomplete. */
    FOLLOWUP,
    ;

    companion object {
        fun fromName(name: String?): NotificationKind =
            entries.firstOrNull { it.name == name } ?: MAIN
    }
}
