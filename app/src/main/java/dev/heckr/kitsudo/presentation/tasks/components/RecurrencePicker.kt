package dev.heckr.kitsudo.presentation.tasks.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.domain.model.RecurrenceUnit
import kotlinx.coroutines.launch

/**
 * Bottom-sheet repeat picker. Presets (Daily/Weekly/Monthly) plus a "Custom"
 * mode exposing an "every N unit" stepper. A null result means "does not repeat".
 *
 * Only shown for tasks that already have a deadline (recurrence is anchored to it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrencePicker(
    initialUnit: RecurrenceUnit?,
    initialInterval: Int,
    onConfirm: (unit: RecurrenceUnit?, interval: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // A preset is the (unit, 1) shape; anything else is treated as Custom.
    val startsCustom = initialUnit != null && initialInterval > 1
    var selection by remember {
        mutableStateOf(
            when {
                initialUnit == null -> RecurrenceChoice.NONE
                startsCustom -> RecurrenceChoice.CUSTOM
                initialUnit == RecurrenceUnit.DAY -> RecurrenceChoice.DAILY
                initialUnit == RecurrenceUnit.WEEK -> RecurrenceChoice.WEEKLY
                else -> RecurrenceChoice.MONTHLY
            },
        )
    }
    var customInterval by remember { mutableIntStateOf(initialInterval.coerceAtLeast(2)) }
    var customUnit by remember {
        mutableStateOf(if (startsCustom) initialUnit!! else RecurrenceUnit.DAY)
    }

    fun dismissThen(after: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) after()
        }
    }

    fun confirm() {
        val (unit, interval) = when (selection) {
            RecurrenceChoice.NONE -> null to 1
            RecurrenceChoice.DAILY -> RecurrenceUnit.DAY to 1
            RecurrenceChoice.WEEKLY -> RecurrenceUnit.WEEK to 1
            RecurrenceChoice.MONTHLY -> RecurrenceUnit.MONTH to 1
            RecurrenceChoice.CUSTOM -> customUnit to customInterval.coerceAtLeast(1)
        }
        dismissThen { onConfirm(unit, interval) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // Force press/hover ripples to the accent instead of the inherited content
        // tint (which reads as a muddy red under the Catppuccin palette).
        CompositionLocalProvider(
            LocalRippleConfiguration provides RippleConfiguration(
                color = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text(
                text = stringResource(R.string.recurrence_label),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            )

            RecurrenceChoice.entries.forEach { choice ->
                ChoiceRow(
                    selected = selection == choice,
                    label = stringResource(choice.labelRes),
                    onSelect = { selection = choice },
                )
            }

            if (selection == RecurrenceChoice.CUSTOM) {
                Spacer(Modifier.height(8.dp))
                CustomRecurrenceRow(
                    interval = customInterval,
                    unit = customUnit,
                    onIntervalChange = { customInterval = it.coerceIn(2, 365) },
                    onUnitChange = { customUnit = it },
                )
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                TextButton(onClick = { dismissThen(onDismiss) }) {
                    Text(stringResource(R.string.deadline_picker_cancel))
                }
                TextButton(onClick = { confirm() }) {
                    Text(stringResource(R.string.deadline_picker_confirm))
                }
            }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    selected: Boolean,
    label: String,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun CustomRecurrenceRow(
    interval: Int,
    unit: RecurrenceUnit,
    onIntervalChange: (Int) -> Unit,
    onUnitChange: (RecurrenceUnit) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.recurrence_every_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = { onIntervalChange(interval - 1) }) {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = interval.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.size(width = 28.dp, height = 24.dp),
            )
            IconButton(onClick = { onIntervalChange(interval + 1) }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.recurrence_interval_increase),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        ) {
            RecurrenceUnit.entries.forEach { u ->
                FilterChip(
                    selected = unit == u,
                    onClick = { onUnitChange(u) },
                    label = { Text(stringResource(u.unitNameRes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

private enum class RecurrenceChoice(val labelRes: Int) {
    NONE(R.string.recurrence_none),
    DAILY(R.string.recurrence_daily),
    WEEKLY(R.string.recurrence_weekly),
    MONTHLY(R.string.recurrence_monthly),
    CUSTOM(R.string.recurrence_custom),
}

/** Plural resource describing this unit's "every N x" phrasing. */
private val RecurrenceUnit.unitPluralRes: Int
    get() = when (this) {
        RecurrenceUnit.DAY -> R.plurals.recurrence_every_days
        RecurrenceUnit.WEEK -> R.plurals.recurrence_every_weeks
        RecurrenceUnit.MONTH -> R.plurals.recurrence_every_months
    }

/** Plain unit name for the custom-unit chips ("Days" / "Weeks" / "Months"). */
private val RecurrenceUnit.unitNameRes: Int
    get() = when (this) {
        RecurrenceUnit.DAY -> R.string.recurrence_unit_days
        RecurrenceUnit.WEEK -> R.string.recurrence_unit_weeks
        RecurrenceUnit.MONTH -> R.string.recurrence_unit_months
    }

/**
 * Short user-facing summary of a recurrence rule, e.g. "Daily" / "Every 2 weeks".
 * interval 1 collapses to the preset name.
 */
@Composable
fun recurrenceSummary(unit: RecurrenceUnit, interval: Int): String =
    if (interval <= 1) {
        stringResource(
            when (unit) {
                RecurrenceUnit.DAY -> R.string.recurrence_daily
                RecurrenceUnit.WEEK -> R.string.recurrence_weekly
                RecurrenceUnit.MONTH -> R.string.recurrence_monthly
            },
        )
    } else {
        pluralStringResource(unit.unitPluralRes, interval, interval)
    }
