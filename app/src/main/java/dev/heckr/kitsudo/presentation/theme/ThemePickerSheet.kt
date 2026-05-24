package dev.heckr.kitsudo.presentation.theme

import androidx.annotation.StringRes
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.CatppuccinFlavor
import dev.heckr.kitsudo.domain.model.ThemePalette

@StringRes
fun CatppuccinFlavor.labelRes(): Int = when (this) {
    CatppuccinFlavor.LATTE -> R.string.theme_flavor_latte
    CatppuccinFlavor.FRAPPE -> R.string.theme_flavor_frappe
    CatppuccinFlavor.MACCHIATO -> R.string.theme_flavor_macchiato
    CatppuccinFlavor.MOCHA -> R.string.theme_flavor_mocha
}

@StringRes
fun ThemePalette.labelRes(): Int = when (this) {
    ThemePalette.MATERIAL3 -> R.string.theme_palette_material3
    ThemePalette.LATTE -> R.string.theme_flavor_latte
    ThemePalette.FRAPPE -> R.string.theme_flavor_frappe
    ThemePalette.MACCHIATO -> R.string.theme_flavor_macchiato
    ThemePalette.MOCHA -> R.string.theme_flavor_mocha
}

@StringRes
fun CatppuccinAccent.labelRes(): Int = when (this) {
    CatppuccinAccent.ROSEWATER -> R.string.accent_rosewater
    CatppuccinAccent.FLAMINGO  -> R.string.accent_flamingo
    CatppuccinAccent.PINK      -> R.string.accent_pink
    CatppuccinAccent.MAUVE     -> R.string.accent_mauve
    CatppuccinAccent.RED       -> R.string.accent_red
    CatppuccinAccent.MAROON    -> R.string.accent_maroon
    CatppuccinAccent.PEACH     -> R.string.accent_peach
    CatppuccinAccent.YELLOW    -> R.string.accent_yellow
    CatppuccinAccent.GREEN     -> R.string.accent_green
    CatppuccinAccent.TEAL      -> R.string.accent_teal
    CatppuccinAccent.SKY       -> R.string.accent_sky
    CatppuccinAccent.SAPPHIRE  -> R.string.accent_sapphire
    CatppuccinAccent.BLUE      -> R.string.accent_blue
    CatppuccinAccent.LAVENDER  -> R.string.accent_lavender
}
