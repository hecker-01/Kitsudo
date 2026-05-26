package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import javax.inject.Inject

class SetThemeFlavorUseCase @Inject constructor(
    private val repository: ThemeRepository,
) {
    suspend operator fun invoke(palette: ThemePalette) = repository.setThemePalette(palette)
}
