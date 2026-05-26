package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.M3WearColors
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetM3ColorsUseCase @Inject constructor(
    private val repository: ThemeRepository,
) {
    operator fun invoke(): Flow<M3WearColors?> = repository.getM3Colors()
}
