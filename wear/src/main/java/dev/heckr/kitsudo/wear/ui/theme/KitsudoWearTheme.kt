package dev.heckr.kitsudo.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.M3WearColors
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.ui.theme.Frappe
import dev.heckr.kitsudo.ui.theme.Latte
import dev.heckr.kitsudo.ui.theme.Macchiato
import dev.heckr.kitsudo.ui.theme.Mocha
import dev.heckr.kitsudo.ui.theme.accentColor

/**
 * Flat bag of every Catppuccin color the wear screens reference.
 * All dark flavors map naturally; Latte (light) is remapped to Frappe surfaces
 * so the watch display (always AMOLED black) stays readable.
 */
data class WearPaletteColors(
    val text: Color,
    val subtext0: Color,
    val subtext1: Color,
    val overlay2: Color,
    val surface0: Color,
    val surface1: Color,
    val surface2: Color,
    val base: Color,
    val green: Color,
    val red: Color,
    val accent: Color,
)

/** Ambient palette; screens read this instead of hardcoding Mocha.* */
val LocalWearPaletteColors = staticCompositionLocalOf { mochaColors(CatppuccinAccent.MAUVE) }

// ── Palette builders ─────────────────────────────────────────────────────────

private fun mochaColors(accent: CatppuccinAccent) = WearPaletteColors(
    text      = Mocha.Text,
    subtext0  = Mocha.Subtext0,
    subtext1  = Mocha.Subtext1,
    overlay2  = Mocha.Overlay2,
    surface0  = Mocha.Surface0,
    surface1  = Mocha.Surface1,
    surface2  = Mocha.Surface2,
    base      = Mocha.Base,
    green     = Mocha.Green,
    red       = Mocha.Red,
    accent    = accentColor(ThemePalette.MOCHA, accent),
)

private fun macchiatoColors(accent: CatppuccinAccent) = WearPaletteColors(
    text      = Macchiato.Text,
    subtext0  = Macchiato.Subtext0,
    subtext1  = Macchiato.Subtext1,
    overlay2  = Macchiato.Overlay2,
    surface0  = Macchiato.Surface0,
    surface1  = Macchiato.Surface1,
    surface2  = Macchiato.Surface2,
    base      = Macchiato.Base,
    green     = Macchiato.Green,
    red       = Macchiato.Red,
    accent    = accentColor(ThemePalette.MACCHIATO, accent),
)

private fun frappeColors(accent: CatppuccinAccent) = WearPaletteColors(
    text      = Frappe.Text,
    subtext0  = Frappe.Subtext0,
    subtext1  = Frappe.Subtext1,
    overlay2  = Frappe.Overlay2,
    surface0  = Frappe.Surface0,
    surface1  = Frappe.Surface1,
    surface2  = Frappe.Surface2,
    base      = Frappe.Base,
    green     = Frappe.Green,
    red       = Frappe.Red,
    accent    = accentColor(ThemePalette.FRAPPE, accent),
)

/**
 * Latte is a light flavor — on an AMOLED watch face it would produce dark text
 * on a black background (unreadable). We use Latte accent/green/red values (so
 * the colour character of the selection is preserved) but borrow Frappe's dark
 * surfaces for the neutral tones.
 */
private fun latteColors(accent: CatppuccinAccent) = WearPaletteColors(
    text      = Latte.Base,        // off-white — readable on AMOLED black
    subtext0  = Latte.Mantle,
    subtext1  = Latte.Crust,
    overlay2  = Latte.Surface2,
    surface0  = Frappe.Surface0,   // borrow dark surfaces from Frappe
    surface1  = Frappe.Surface1,
    surface2  = Frappe.Surface2,
    base      = Frappe.Base,
    green     = Latte.Green,
    red       = Latte.Red,
    accent    = accentColor(ThemePalette.LATTE, accent),
)

/**
 * Builds a [WearPaletteColors] from the phone's Material You dynamic color snapshot.
 * Background is always [Color.Black] (AMOLED). Text/surface roles are derived from
 * the phone's dark-scheme [M3WearColors].
 */
private fun m3Colors(m3: M3WearColors): WearPaletteColors {
    val text      = Color(m3.onSurface)
    val surfVar   = Color(m3.onSurfaceVariant)
    return WearPaletteColors(
        text      = text,
        subtext1  = lerp(surfVar, text, 0.4f),   // slightly brighter than surfaceVariant
        subtext0  = surfVar,
        overlay2  = Color(m3.outline),
        surface0  = Color(m3.surfaceContainerLow),
        surface1  = Color(m3.surfaceContainer),
        surface2  = Color(m3.surfaceContainerHigh),
        base      = Color.Black,
        green     = Color(m3.tertiary),
        red       = Color(m3.error),
        accent    = Color(m3.primary),
    )
}

fun buildWearPaletteColors(
    palette: ThemePalette,
    accent: CatppuccinAccent,
    m3Colors: M3WearColors? = null,
): WearPaletteColors =
    when (palette) {
        ThemePalette.MATERIAL3 -> if (m3Colors != null) m3Colors(m3Colors) else mochaColors(accent)
        ThemePalette.MOCHA     -> mochaColors(accent)
        ThemePalette.MACCHIATO -> macchiatoColors(accent)
        ThemePalette.FRAPPE    -> frappeColors(accent)
        ThemePalette.LATTE     -> latteColors(accent)
    }

// ── Theme composable ──────────────────────────────────────────────────────────

@Composable
fun KitsudoWearTheme(
    palette: ThemePalette = ThemePalette.MOCHA,
    accent: CatppuccinAccent = CatppuccinAccent.MAUVE,
    m3Colors: M3WearColors? = null,
    content: @Composable () -> Unit,
) {
    val colors = buildWearPaletteColors(palette, accent, m3Colors)

    val colorScheme = ColorScheme(
        primary              = colors.accent,
        primaryDim           = colors.accent.copy(alpha = 0.7f),
        primaryContainer     = colors.surface1,
        onPrimary            = colors.base,
        onPrimaryContainer   = colors.text,
        secondary            = colors.subtext0,
        secondaryDim         = colors.subtext1,
        secondaryContainer   = colors.surface0,
        onSecondary          = colors.base,
        onSecondaryContainer = colors.text,
        tertiary             = colors.green,
        tertiaryDim          = colors.green.copy(alpha = 0.7f),
        tertiaryContainer    = colors.surface0,
        onTertiary           = colors.base,
        onTertiaryContainer  = colors.text,
        background           = Color.Black,
        onBackground         = colors.text,
        surfaceContainerLow  = colors.surface0,
        surfaceContainer     = colors.surface1,
        surfaceContainerHigh = colors.surface2,
        onSurface            = colors.text,
        onSurfaceVariant     = colors.subtext0,
        outline              = colors.overlay2,
        outlineVariant       = colors.surface2,
        error                = colors.red,
        onError              = colors.base,
        errorContainer       = colors.surface0,
        onErrorContainer     = colors.red,
    )

    CompositionLocalProvider(LocalWearPaletteColors provides colors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
