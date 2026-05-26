package dev.heckr.kitsudo.domain.repository

import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.M3WearColors
import dev.heckr.kitsudo.domain.model.ThemePalette
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun getThemePalette(): Flow<ThemePalette>
    suspend fun setThemePalette(palette: ThemePalette)

    fun getAccent(): Flow<CatppuccinAccent>
    suspend fun setAccent(accent: CatppuccinAccent)

    /** Null until the phone sends its first Material You snapshot. */
    fun getM3Colors(): Flow<M3WearColors?>
    suspend fun setM3Colors(colors: M3WearColors)
}
