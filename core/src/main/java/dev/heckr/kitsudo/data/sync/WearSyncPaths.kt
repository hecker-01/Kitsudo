package dev.heckr.kitsudo.data.sync

/** Shared path/key constants used by both the phone and watch sync code. */
object WearSyncPaths {
    /** DataClient path: phone pushes full task list snapshot here. */
    const val TASKS_SNAPSHOT = "/tasks/snapshot"

    /** MessageClient path: watch sends a toggle-complete request here. */
    const val TOGGLE_COMPLETE = "/task/toggleComplete"

    /** MessageClient path: watch requests an immediate re-sync from the phone. */
    const val REQUEST_SYNC = "/tasks/requestSync"

    /** DataMap key for the JSON-encoded task list in TASKS_SNAPSHOT items. */
    const val KEY_TASKS_JSON = "tasks_json"

    /** DataMap key for the publish timestamp (prevents stale-data acceptance). */
    const val KEY_TIMESTAMP = "timestamp"
}
