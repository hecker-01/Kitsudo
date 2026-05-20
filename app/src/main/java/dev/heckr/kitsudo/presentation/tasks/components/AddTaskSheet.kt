package dev.heckr.kitsudo.presentation.tasks.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.heckr.kitsudo.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onAdd: (title: String, description: String, deadlineAt: Long?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val view = LocalView.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadlineAt by remember { mutableStateOf<Long?>(null) }
    var showDeadlinePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .imePadding(),
        ) {
            Text(
                text = stringResource(R.string.task_add_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.task_field_title)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.task_field_description)) },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = { showDeadlinePicker = true },
                ) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                    )
                    Text(
                        text = if (deadlineAt != null) {
                            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                .format(Date(deadlineAt!!))
                        } else {
                            stringResource(R.string.task_deadline_set)
                        },
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                if (deadlineAt != null) {
                    TextButton(onClick = { deadlineAt = null }) {
                        Text(stringResource(R.string.task_deadline_clear))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.task_add_cancel))
                }
                FilledTonalButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onAdd(title, description, deadlineAt)
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.task_add_confirm))
                }
            }
        }
    }

    if (showDeadlinePicker) {
        DeadlinePicker(
            initialDeadlineAt = deadlineAt,
            onDeadlinePicked = { picked ->
                deadlineAt = picked
                showDeadlinePicker = false
            },
            onDismiss = { showDeadlinePicker = false },
        )
    }
}
