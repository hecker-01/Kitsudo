package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.CatppuccinFlavor
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThemeFlavorUseCase @Inject constructor(
    private val repository: ThemeRepository,
) {
    operator fun invoke(): Flow<CatppuccinFlavor> = repository.getThemeFlavor()
}
