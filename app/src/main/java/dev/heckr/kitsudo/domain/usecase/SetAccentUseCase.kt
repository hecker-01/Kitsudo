package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import javax.inject.Inject

class SetAccentUseCase @Inject constructor(
    private val repository: ThemeRepository,
) {
    suspend operator fun invoke(accent: CatppuccinAccent) = repository.setAccent(accent)
}
