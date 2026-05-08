package dev.heckr.kitsudo.domain.repository

import dev.heckr.kitsudo.domain.model.CatppuccinFlavor
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun getThemeFlavor(): Flow<CatppuccinFlavor>
    suspend fun setThemeFlavor(flavor: CatppuccinFlavor)
}
