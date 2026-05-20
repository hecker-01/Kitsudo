package dev.heckr.kitsudo.presentation.tasks.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.heckr.kitsudo.R
import java.util.Calendar

/**
 * Two-step date → time picker.
 *
 * - [showClearOption] shows a "Clear" button to remove the deadline.
 * - [onClear] is called when that button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlinePicker(
    initialDeadlineAt: Long? = null,
    onDeadlinePicked: (Long) -> Unit,
    onDismiss: () -> Unit,
    showClearOption: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    val initialCal = remember(initialDeadlineAt) {
        Calendar.getInstance().also { cal ->
            if (initialDeadlineAt != null) cal.timeInMillis = initialDeadlineAt
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMs by remember { mutableStateOf<Long?>(null) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initialDeadlineAt,
    )

    if (!showTimePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (showClearOption && onClear != null) {
                        TextButton(onClick = onClear) {
                            Text(stringResource(R.string.task_deadline_clear))
                        }
                    }
                    TextButton(
                        onClick = {
                            selectedDateMs = dateState.selectedDateMillis
                            showTimePicker = true
                        },
                        enabled = dateState.selectedDateMillis != null,
                    ) {
                        Text(stringResource(R.string.deadline_picker_next))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.deadline_picker_cancel))
                }
            },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = initialCal.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.deadline_picker_time_title)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val dateCal = Calendar.getInstance().also { cal ->
                            cal.timeInMillis = selectedDateMs ?: System.currentTimeMillis()
                            cal.set(Calendar.HOUR_OF_DAY, timeState.hour)
                            cal.set(Calendar.MINUTE, timeState.minute)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                        }
                        onDeadlinePicked(dateCal.timeInMillis)
                    },
                ) {
                    Text(stringResource(R.string.deadline_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.deadline_picker_back))
                }
            },
        )
    }
}
