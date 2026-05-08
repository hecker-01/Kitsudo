package dev.heckr.kitsudo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.heckr.kitsudo.presentation.navigation.KitsudoNavHost
import dev.heckr.kitsudo.presentation.theme.ThemeViewModel
import dev.heckr.kitsudo.ui.theme.KitsudoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by themeViewModel.uiState.collectAsStateWithLifecycle()
            KitsudoTheme(flavor = uiState.flavor) {
                KitsudoNavHost(
                    currentFlavor = uiState.flavor,
                    onFlavorChange = themeViewModel::setFlavor,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
