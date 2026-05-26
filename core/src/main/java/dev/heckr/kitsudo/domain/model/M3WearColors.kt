package dev.heckr.kitsudo.domain.model

/**
 * A snapshot of key Material You (dynamic color) ARGB values extracted from
 * the phone's [dynamicDarkColorScheme] and sent to the watch via the Wearable
 * Data Layer.
 *
 * Values are stored as packed-int ARGB (the same format as [android.graphics.Color]
 * and [androidx.compose.ui.graphics.Color.toArgb]).  The watch reconstructs
 * [androidx.compose.ui.graphics.Color] instances from them at theme-build time.
 */
data class M3WearColors(
    val primary: Int,
    val onPrimary: Int,
    val tertiary: Int,            // mapped to "green" semantic slot on the watch
    val error: Int,
    val onSurface: Int,           // mapped to "text"
    val onSurfaceVariant: Int,    // mapped to "subtext"
    val outline: Int,             // mapped to "overlay2"
    val surfaceContainerLow: Int, // surface0
    val surfaceContainer: Int,    // surface1
    val surfaceContainerHigh: Int,// surface2
)
