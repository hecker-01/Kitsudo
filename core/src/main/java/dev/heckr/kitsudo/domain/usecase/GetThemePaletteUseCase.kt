package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThemePaletteUseCase @Inject constructor(
    private val repository: ThemeRepository,
) {
    operator fun invoke(): Flow<ThemePalette> = repository.getThemePalette()
}
