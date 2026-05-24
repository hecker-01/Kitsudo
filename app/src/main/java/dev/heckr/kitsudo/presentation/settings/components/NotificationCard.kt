package dev.heckr.kitsudo.presentation.settings.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.domain.model.NotificationPreferences

/**
 * Settings section card for notification preferences. Visual style matches
 * `AppearanceCard` in SettingsScreen — same surfaceVariant container, same
 * chip-style selectable options.
 *
 * All three controls are reactive: the displayed values come from [prefs];
 * user actions call back through the provided lambdas.
 */
@Composable
fun NotificationCard(
    prefs: NotificationPreferences,
    onSetLeadMinutes: (Int) -> Unit,
    onSetQuietEnabled: (Boolean) -> Unit,
    onSetQuietStart: (Int) -> Unit,
    onSetQuietEnd: (Int) -> Unit,
    onSetSnoozeMinutes: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Pre-reminder lead time ─────────────────────────────────
            SubSectionLabel(stringResource(R.string.settings_notif_lead_title))
            Subtitle(stringResource(R.string.settings_notif_lead_subtitle))
            Spacer(Modifier.height(8.dp))
            ChipRow(
                options = LEAD_OPTIONS,
                selected = prefs.preReminderLeadMinutes,
                onSelect = onSetLeadMinutes,
            )

            Spacer(Modifier.height(20.dp))

            // ── Quiet hours ────────────────────────────────────────────
            QuietHoursSection(
                enabled = prefs.quietHoursEnabled,
                startMinutes = prefs.quietStartMinutes,
                endMinutes = prefs.quietEndMinutes,
                onEnabledChange = onSetQuietEnabled,
                onStartChange = onSetQuietStart,
                onEndChange = onSetQuietEnd,
            )

            Spacer(Modifier.height(20.dp))

            // ── Snooze duration ────────────────────────────────────────
            SubSectionLabel(stringResource(R.string.settings_notif_snooze_title))
            Subtitle(stringResource(R.string.settings_notif_snooze_subtitle))
            Spacer(Modifier.height(8.dp))
            ChipRow(
                options = SNOOZE_OPTIONS,
                selected = prefs.snoozeMinutes,
                onSelect = onSetSnoozeMinutes,
            )
        }
    }
}

// ── Lead-time / snooze chip-row options ────────────────────────────────────

private data class ChipOption(val value: Int, val labelRes: Int)

private val LEAD_OPTIONS = listOf(
    ChipOption(0, R.string.settings_notif_lead_none),
    ChipOption(15, R.string.settings_notif_lead_15m),
    ChipOption(30, R.string.settings_notif_lead_30m),
    ChipOption(60, R.string.settings_notif_lead_1h),
    ChipOption(1440, R.string.settings_notif_lead_1d),
)

private val SNOOZE_OPTIONS = listOf(
    ChipOption(5, R.string.settings_notif_snooze_5m),
    ChipOption(10, R.string.settings_notif_snooze_10m),
    ChipOption(15, R.string.settings_notif_snooze_15m),
    ChipOption(30, R.string.settings_notif_snooze_30m),
    ChipOption(60, R.string.settings_notif_snooze_1h),
)

// ── Pieces ─────────────────────────────────────────────────────────────────

@Composable
private fun SubSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun Subtitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun ChipRow(
    options: List<ChipOption>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    // Same pattern as ThemeOptionCard rows: equal weight so chips always fill
    // the full card width, regardless of label length.
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { opt ->
            ChipOptionCard(
                label = stringResource(opt.labelRes),
                selected = opt.value == selected,
                onClick = { onSelect(opt.value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Same visual treatment as `ThemeOptionCard` in SettingsScreen.kt — inline
 * check + label, primaryContainer fill when selected. Defined inline here to
 * keep the file self-contained.
 */
@Composable
private fun ChipOptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Card(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
        modifier = modifier,
    ) {
        // Single-select: the primaryContainer fill already makes selection obvious,
        // so no check icon — giving the label the full width without truncation.
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
        )
    }
}

// ── Quiet hours section ────────────────────────────────────────────────────

@Composable
private fun QuietHoursSection(
    enabled: Boolean,
    startMinutes: Int,
    endMinutes: Int,
    onEnabledChange: (Boolean) -> Unit,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
) {
    val view = LocalView.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SubSectionLabel(stringResource(R.string.settings_notif_quiet_title))
                Subtitle(stringResource(R.string.settings_notif_quiet_subtitle))
            }
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    view.performHapticFeedback(
                        if (checked) HapticFeedbackConstants.TOGGLE_ON
                        else HapticFeedbackConstants.TOGGLE_OFF,
                    )
                    onEnabledChange(checked)
                },
                thumbContent = if (enabled) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }

        AnimatedVisibility(visible = enabled) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                TimeChip(
                    label = stringResource(R.string.settings_notif_quiet_start),
                    minutes = startMinutes,
                    onChange = onStartChange,
                    modifier = Modifier.weight(1f),
                )
                TimeChip(
                    label = stringResource(R.string.settings_notif_quiet_end),
                    minutes = endMinutes,
                    onChange = onEndChange,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Surface card showing a label + HH:MM that opens a TimePicker dialog on tap. */
@Composable
private fun TimeChip(
    label: String,
    minutes: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier.clickable {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            showPicker = true
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.settings_notif_quiet_time_format,
                    minutes / 60,
                    minutes % 60,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }

    if (showPicker) {
        TimePickerDialog(
            initialMinutes = minutes,
            onDismiss = { showPicker = false },
            onConfirm = { newMinutes ->
                onChange(newMinutes)
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.deadline_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.deadline_picker_cancel))
            }
        },
    )
}
