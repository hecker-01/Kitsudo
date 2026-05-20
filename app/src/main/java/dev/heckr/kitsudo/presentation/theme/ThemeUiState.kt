package dev.heckr.kitsudo.presentation.theme

import dev.heckr.kitsudo.domain.model.ThemePalette

data class ThemeUiState(
    val palette: ThemePalette = ThemePalette.MOCHA,
)
