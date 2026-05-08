package dev.heckr.kitsudo.presentation.theme

import dev.heckr.kitsudo.domain.model.CatppuccinFlavor

data class ThemeUiState(
    val flavor: CatppuccinFlavor = CatppuccinFlavor.MOCHA,
)
