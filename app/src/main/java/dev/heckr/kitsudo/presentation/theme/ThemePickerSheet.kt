package dev.heckr.kitsudo.presentation.theme

import androidx.annotation.StringRes
import dev.heckr.kitsudo.R
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
