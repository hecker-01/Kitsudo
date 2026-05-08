package dev.heckr.kitsudo.presentation.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.domain.model.CatppuccinFlavor
import dev.heckr.kitsudo.ui.theme.flavorPreviewColors

@StringRes
fun CatppuccinFlavor.labelRes(): Int = when (this) {
    CatppuccinFlavor.LATTE -> R.string.theme_flavor_latte
    CatppuccinFlavor.FRAPPE -> R.string.theme_flavor_frappe
    CatppuccinFlavor.MACCHIATO -> R.string.theme_flavor_macchiato
    CatppuccinFlavor.MOCHA -> R.string.theme_flavor_mocha
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(
    currentFlavor: CatppuccinFlavor,
    onFlavorSelected: (CatppuccinFlavor) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.theme_picker_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            CatppuccinFlavor.entries.forEach { flavor ->
                FlavorCard(
                    flavor = flavor,
                    isSelected = flavor == currentFlavor,
                    onClick = { onFlavorSelected(flavor) },
                )
            }
        }
    }
}

@Composable
private fun FlavorCard(
    flavor: CatppuccinFlavor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
    }
    val label = stringResource(flavor.labelRes())
    val selectedDesc = stringResource(R.string.theme_flavor_selected_description, label)

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .semantics {
                if (isSelected) contentDescription = selectedDesc
            },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlavorSwatch(flavor = flavor)
        }
    }
}

@Composable
private fun FlavorSwatch(
    flavor: CatppuccinFlavor,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        flavorPreviewColors(flavor).forEach { color ->
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
