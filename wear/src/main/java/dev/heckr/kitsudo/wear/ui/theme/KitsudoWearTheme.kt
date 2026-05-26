package dev.heckr.kitsudo.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/**
 * Wear theme wrapper. Uses the default Wear Material3 dark color scheme
 * (suitable for AMOLED watch screens). Individual screens use Catppuccin
 * Mocha color constants from :core directly for branded colors.
 */
@Composable
fun KitsudoWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
