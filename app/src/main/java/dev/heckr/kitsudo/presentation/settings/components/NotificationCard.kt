package dev.heckr.kitsudo.presentation.settings.components

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.domain.model.NotificationPreferences
import dev.heckr.kitsudo.presentation.tasks.components.TimeWheelPicker

/**
 * Settings section card for notification preferences. Visual style matches
 * `AppearanceCard` in SettingsScreen - same surfaceVariant container, same
 * chip-style selectable options.
 *
 * All three controls are reactive: the displayed values come from [prefs];
 * user actions call back through the provided lambdas.
 */
@Composable
fun NotificationCard(
    prefs: NotificationPreferences,
    onSetLeadMinutes: (Set<Int>) -> Unit,
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
            // -- Permission prompts (each only shows when its grant is missing) --
            NotificationsEnabledPrompt()
            ExactAlarmPrompt()

            // -- Pre-reminder lead time (multi-select) ------------------
            SubSectionLabel(stringResource(R.string.settings_notif_lead_title))
            Subtitle(stringResource(R.string.settings_notif_lead_subtitle))
            Spacer(Modifier.height(8.dp))
            LeadChipRow(
                options = LEAD_OPTIONS,
                selected = prefs.preReminderLeadMinutes,
                onChange = onSetLeadMinutes,
            )

            Spacer(Modifier.height(20.dp))

            // -- Quiet hours --------------------------------------------
            QuietHoursSection(
                enabled = prefs.quietHoursEnabled,
                startMinutes = prefs.quietStartMinutes,
                endMinutes = prefs.quietEndMinutes,
                onEnabledChange = onSetQuietEnabled,
                onStartChange = onSetQuietStart,
                onEndChange = onSetQuietEnd,
            )

            Spacer(Modifier.height(20.dp))

            // -- Snooze duration ----------------------------------------
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

// -- Permission prompts -----------------------------------------------------

/**
 * Shown only when notifications are disabled for the app (permission denied or
 * turned off at the app/channel level). Tapping "Enable" opens the app's system
 * notification settings; the state re-checks on resume so the prompt disappears
 * once the user returns having enabled them.
 */
@Composable
private fun NotificationsEnabledPrompt() {
    val context = LocalContext.current
    val enabled = rememberPermissionState { context.areNotificationsEnabled() }

    PermissionPrompt(
        visible = !enabled,
        title = stringResource(R.string.settings_notif_perm_title),
        subtitle = stringResource(R.string.settings_notif_perm_subtitle),
        actionLabel = stringResource(R.string.settings_notif_perm_action),
        onAction = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        },
    )
}

/**
 * Shown only when the OS hasn't granted exact-alarm permission. Tapping "Allow"
 * opens the system screen for it; the state re-checks on resume so the prompt
 * disappears once the user returns having granted it.
 */
@Composable
private fun ExactAlarmPrompt() {
    val context = LocalContext.current
    val canSchedule = rememberPermissionState { context.canScheduleExactAlarms() }

    PermissionPrompt(
        visible = !canSchedule,
        title = stringResource(R.string.settings_notif_exact_title),
        subtitle = stringResource(R.string.settings_notif_exact_subtitle),
        actionLabel = stringResource(R.string.settings_notif_exact_action),
        onAction = {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        },
    )
}

/** Label + subtitle on the left, an accent-colored action button on the right. */
@Composable
private fun PermissionPrompt(
    visible: Boolean,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    AnimatedVisibility(visible = visible) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SubSectionLabel(title)
                    Subtitle(subtitle)
                }
                Spacer(Modifier.width(12.dp))
                FilledTonalButton(
                    onClick = onAction,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        // Label tracks the user's accent color (drives colorScheme.primary).
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(actionLabel)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Holds a boolean grant state, re-evaluating [check] on every ON_RESUME. */
@Composable
private fun rememberPermissionState(check: () -> Boolean): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(check()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = check()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}

private fun Context.canScheduleExactAlarms(): Boolean =
    getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

private fun Context.areNotificationsEnabled(): Boolean =
    NotificationManagerCompat.from(this).areNotificationsEnabled()

// -- Lead-time / snooze chip-row options ------------------------------------

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

// -- Pieces -----------------------------------------------------------------

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
 * Multi-select variant for pre-reminder lead times. Several lead times can be
 * active at once - each fires its own reminder before the deadline. The
 * `value == 0` ("None") chip clears the whole selection and shows as selected
 * when no lead time is active.
 */
@Composable
private fun LeadChipRow(
    options: List<ChipOption>,
    selected: Set<Int>,
    onChange: (Set<Int>) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { opt ->
            val isSelected = if (opt.value == 0) selected.isEmpty() else opt.value in selected
            ChipOptionCard(
                label = stringResource(opt.labelRes),
                selected = isSelected,
                onClick = {
                    val newSet = when {
                        opt.value == 0 -> emptySet()
                        opt.value in selected -> selected - opt.value
                        else -> selected + opt.value
                    }
                    onChange(newSet)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Same visual treatment as `ThemeOptionCard` in SettingsScreen.kt - inline
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
        // so no check icon - giving the label the full width without truncation.
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

// -- Quiet hours section ----------------------------------------------------

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
        TimeWheelPicker(
            title = label,
            initialMinutes = minutes,
            onTimePicked = { newMinutes ->
                onChange(newMinutes)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}
