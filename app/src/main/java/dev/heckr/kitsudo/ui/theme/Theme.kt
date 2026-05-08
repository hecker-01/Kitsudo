package dev.heckr.kitsudo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.heckr.kitsudo.domain.model.CatppuccinFlavor

private fun latteColorScheme(): ColorScheme = lightColorScheme(
    primary = Latte.Mauve,
    onPrimary = Latte.Base,
    primaryContainer = Latte.Surface1,
    onPrimaryContainer = Latte.Mauve,
    secondary = Latte.Blue,
    onSecondary = Latte.Base,
    secondaryContainer = Latte.Surface1,
    onSecondaryContainer = Latte.Blue,
    tertiary = Latte.Pink,
    onTertiary = Latte.Base,
    tertiaryContainer = Latte.Surface1,
    onTertiaryContainer = Latte.Pink,
    error = Latte.Red,
    onError = Latte.Base,
    errorContainer = Latte.Surface0,
    onErrorContainer = Latte.Red,
    background = Latte.Base,
    onBackground = Latte.Text,
    surface = Latte.Base,
    onSurface = Latte.Text,
    surfaceVariant = Latte.Mantle,
    onSurfaceVariant = Latte.Subtext0,
    outline = Latte.Overlay1,
    outlineVariant = Latte.Surface2,
    inverseSurface = Latte.Text,
    inverseOnSurface = Latte.Base,
    inversePrimary = Latte.Lavender,
    scrim = Latte.Crust,
    surfaceTint = Latte.Mauve,
)

private fun frappeColorScheme(): ColorScheme = darkColorScheme(
    primary = Frappe.Mauve,
    onPrimary = Frappe.Crust,
    primaryContainer = Frappe.Surface1,
    onPrimaryContainer = Frappe.Mauve,
    secondary = Frappe.Blue,
    onSecondary = Frappe.Crust,
    secondaryContainer = Frappe.Surface1,
    onSecondaryContainer = Frappe.Blue,
    tertiary = Frappe.Pink,
    onTertiary = Frappe.Crust,
    tertiaryContainer = Frappe.Surface1,
    onTertiaryContainer = Frappe.Pink,
    error = Frappe.Red,
    onError = Frappe.Crust,
    errorContainer = Frappe.Surface0,
    onErrorContainer = Frappe.Red,
    background = Frappe.Base,
    onBackground = Frappe.Text,
    surface = Frappe.Base,
    onSurface = Frappe.Text,
    surfaceVariant = Frappe.Surface0,
    onSurfaceVariant = Frappe.Subtext0,
    outline = Frappe.Overlay1,
    outlineVariant = Frappe.Surface2,
    inverseSurface = Frappe.Text,
    inverseOnSurface = Frappe.Base,
    inversePrimary = Frappe.Lavender,
    scrim = Frappe.Crust,
    surfaceTint = Frappe.Mauve,
)

private fun macchiatoColorScheme(): ColorScheme = darkColorScheme(
    primary = Macchiato.Mauve,
    onPrimary = Macchiato.Crust,
    primaryContainer = Macchiato.Surface1,
    onPrimaryContainer = Macchiato.Mauve,
    secondary = Macchiato.Blue,
    onSecondary = Macchiato.Crust,
    secondaryContainer = Macchiato.Surface1,
    onSecondaryContainer = Macchiato.Blue,
    tertiary = Macchiato.Pink,
    onTertiary = Macchiato.Crust,
    tertiaryContainer = Macchiato.Surface1,
    onTertiaryContainer = Macchiato.Pink,
    error = Macchiato.Red,
    onError = Macchiato.Crust,
    errorContainer = Macchiato.Surface0,
    onErrorContainer = Macchiato.Red,
    background = Macchiato.Base,
    onBackground = Macchiato.Text,
    surface = Macchiato.Base,
    onSurface = Macchiato.Text,
    surfaceVariant = Macchiato.Surface0,
    onSurfaceVariant = Macchiato.Subtext0,
    outline = Macchiato.Overlay1,
    outlineVariant = Macchiato.Surface2,
    inverseSurface = Macchiato.Text,
    inverseOnSurface = Macchiato.Base,
    inversePrimary = Macchiato.Lavender,
    scrim = Macchiato.Crust,
    surfaceTint = Macchiato.Mauve,
)

private fun mochaColorScheme(): ColorScheme = darkColorScheme(
    primary = Mocha.Mauve,
    onPrimary = Mocha.Crust,
    primaryContainer = Mocha.Surface1,
    onPrimaryContainer = Mocha.Mauve,
    secondary = Mocha.Blue,
    onSecondary = Mocha.Crust,
    secondaryContainer = Mocha.Surface1,
    onSecondaryContainer = Mocha.Blue,
    tertiary = Mocha.Pink,
    onTertiary = Mocha.Crust,
    tertiaryContainer = Mocha.Surface1,
    onTertiaryContainer = Mocha.Pink,
    error = Mocha.Red,
    onError = Mocha.Crust,
    errorContainer = Mocha.Surface0,
    onErrorContainer = Mocha.Red,
    background = Mocha.Base,
    onBackground = Mocha.Text,
    surface = Mocha.Base,
    onSurface = Mocha.Text,
    surfaceVariant = Mocha.Surface0,
    onSurfaceVariant = Mocha.Subtext0,
    outline = Mocha.Overlay1,
    outlineVariant = Mocha.Surface2,
    inverseSurface = Mocha.Text,
    inverseOnSurface = Mocha.Base,
    inversePrimary = Mocha.Lavender,
    scrim = Mocha.Crust,
    surfaceTint = Mocha.Mauve,
)

fun colorSchemeFor(flavor: CatppuccinFlavor): ColorScheme = when (flavor) {
    CatppuccinFlavor.LATTE -> latteColorScheme()
    CatppuccinFlavor.FRAPPE -> frappeColorScheme()
    CatppuccinFlavor.MACCHIATO -> macchiatoColorScheme()
    CatppuccinFlavor.MOCHA -> mochaColorScheme()
}

fun flavorPreviewColors(flavor: CatppuccinFlavor): List<Color> = when (flavor) {
    CatppuccinFlavor.LATTE -> listOf(Latte.Mauve, Latte.Blue, Latte.Pink, Latte.Green, Latte.Peach, Latte.Teal)
    CatppuccinFlavor.FRAPPE -> listOf(Frappe.Mauve, Frappe.Blue, Frappe.Pink, Frappe.Green, Frappe.Peach, Frappe.Teal)
    CatppuccinFlavor.MACCHIATO -> listOf(Macchiato.Mauve, Macchiato.Blue, Macchiato.Pink, Macchiato.Green, Macchiato.Peach, Macchiato.Teal)
    CatppuccinFlavor.MOCHA -> listOf(Mocha.Mauve, Mocha.Blue, Mocha.Pink, Mocha.Green, Mocha.Peach, Mocha.Teal)
}

@Composable
fun KitsudoTheme(
    flavor: CatppuccinFlavor = CatppuccinFlavor.MOCHA,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorSchemeFor(flavor),
        typography = Typography,
        content = content,
    )
}
