package dev.heckr.kitsudo.domain.repository

import dev.heckr.kitsudo.domain.model.ThemePalette
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun getThemePalette(): Flow<ThemePalette>
    suspend fun setThemePalette(palette: ThemePalette)
}
