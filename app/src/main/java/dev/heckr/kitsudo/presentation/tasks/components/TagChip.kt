package dev.heckr.kitsudo.presentation.tasks.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.Tag
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.ui.theme.accentColor

/**
 * Resolves a [CatppuccinAccent] to a concrete color for the current theme. Picks
 * the Latte (light) or Mocha (dark) shade based on the active surface luminance,
 * so tag colors stay vivid and legible under any palette - including Material You.
 */
@Composable
fun tagColor(accent: CatppuccinAccent): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return accentColor(if (dark) ThemePalette.MOCHA else ThemePalette.LATTE, accent)
}

/** A small color-coded label chip. */
@Composable
fun TagChip(tag: Tag, modifier: Modifier = Modifier) {
    val color = tagColor(tag.color)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.22f),
        contentColor = color,
        modifier = modifier,
    ) {
        Text(
            text = tag.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
