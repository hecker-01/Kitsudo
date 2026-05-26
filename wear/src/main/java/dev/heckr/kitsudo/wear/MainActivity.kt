package dev.heckr.kitsudo.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.usecase.GetAccentUseCase
import dev.heckr.kitsudo.domain.usecase.GetM3ColorsUseCase
import dev.heckr.kitsudo.domain.usecase.GetThemeFlavorUseCase
import dev.heckr.kitsudo.wear.presentation.navigation.WearNavHost
import dev.heckr.kitsudo.wear.ui.theme.KitsudoWearTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var getThemeFlavorUseCase: GetThemeFlavorUseCase
    @Inject lateinit var getAccentUseCase: GetAccentUseCase
    @Inject lateinit var getM3ColorsUseCase: GetM3ColorsUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val palette by getThemeFlavorUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemePalette.MOCHA)
            val accent by getAccentUseCase()
                .collectAsStateWithLifecycle(initialValue = CatppuccinAccent.MAUVE)
            val m3Colors by getM3ColorsUseCase()
                .collectAsStateWithLifecycle(initialValue = null)

            KitsudoWearTheme(palette = palette, accent = accent, m3Colors = m3Colors) {
                WearNavHost()
            }
        }
    }
}
