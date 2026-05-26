package dev.heckr.kitsudo.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import dev.heckr.kitsudo.wear.presentation.navigation.WearNavHost
import dev.heckr.kitsudo.wear.ui.theme.KitsudoWearTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KitsudoWearTheme {
                WearNavHost()
            }
        }
    }
}
