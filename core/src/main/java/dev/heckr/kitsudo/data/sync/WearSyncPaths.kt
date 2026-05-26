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

    /** DataMap key for the active [dev.heckr.kitsudo.domain.model.ThemePalette] name. */
    const val KEY_THEME = "theme_palette"

    /** DataMap key for the active [dev.heckr.kitsudo.domain.model.CatppuccinAccent] name. */
    const val KEY_ACCENT = "accent_color"

    // Material You dynamic color keys — only present in the DataMap when theme = MATERIAL3.
    // Values are packed ARGB ints from the phone's dynamicDarkColorScheme.
    const val KEY_M3_PRIMARY        = "m3_primary"
    const val KEY_M3_ON_PRIMARY     = "m3_on_primary"
    const val KEY_M3_TERTIARY       = "m3_tertiary"
    const val KEY_M3_ERROR          = "m3_error"
    const val KEY_M3_ON_SURFACE     = "m3_on_surface"
    const val KEY_M3_ON_SURF_VAR    = "m3_on_surface_variant"
    const val KEY_M3_OUTLINE        = "m3_outline"
    const val KEY_M3_SURF_LOW       = "m3_surf_low"
    const val KEY_M3_SURF_MID       = "m3_surf_mid"
    const val KEY_M3_SURF_HIGH      = "m3_surf_high"
}
