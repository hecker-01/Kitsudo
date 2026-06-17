package dev.heckr.kitsudo.presentation.tasks.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.RecurrenceUnit
import dev.heckr.kitsudo.domain.model.Tag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskSheet(
    onAdd: (
        title: String,
        description: String,
        deadlineAt: Long?,
        recurrenceUnit: RecurrenceUnit?,
        recurrenceInterval: Int,
        tagIds: List<String>,
    ) -> Unit,
    onDismiss: () -> Unit,
    availableTags: List<Tag>,
    onCreateTag: (String, CatppuccinAccent, (Tag) -> Unit) -> Unit,
    onUpdateTag: (Tag) -> Unit,
    onDeleteTag: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialTitle: String = "",
    initialDescription: String = "",
    initialDeadlineAt: Long? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val view = LocalView.current
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var deadlineAt by remember { mutableStateOf(initialDeadlineAt) }
    var showDeadlinePicker by remember { mutableStateOf(false) }
    var recurrenceUnit by remember { mutableStateOf<RecurrenceUnit?>(null) }
    var recurrenceInterval by remember { mutableIntStateOf(1) }
    var showRecurrencePicker by remember { mutableStateOf(false) }
    val selectedTagIds = remember { mutableListOf<String>().toMutableStateList() }
    var showTagPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            // Provide the theme content color so the drag handle's press/hover
            // ripple follows the selected theme instead of an inherited tint.
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
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
                color = MaterialTheme.colorScheme.primary,
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
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
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
                    TextButton(onClick = {
                        deadlineAt = null
                        // Recurrence is anchored to a deadline - drop it when cleared.
                        recurrenceUnit = null
                        recurrenceInterval = 1
                    }) {
                        Text(stringResource(R.string.task_deadline_clear))
                    }
                }
            }

            // -- Repeat (only available once a deadline is set) ----------
            if (deadlineAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = { showRecurrencePicker = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Text(
                            text = recurrenceUnit?.let { recurrenceSummary(it, recurrenceInterval) }
                                ?: stringResource(R.string.recurrence_set),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    if (recurrenceUnit != null) {
                        TextButton(onClick = {
                            recurrenceUnit = null
                            recurrenceInterval = 1
                        }) {
                            Text(stringResource(R.string.recurrence_clear))
                        }
                    }
                }
            }

            // -- Tags ----------------------------------------------------
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = { showTagPicker = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Text(text = stringResource(R.string.tags_set))
                }
            }
            val selectedTags = availableTags.filter { it.id in selectedTagIds }
            if (selectedTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    selectedTags.forEach { tag -> TagChip(tag = tag) }
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
                        onAdd(
                            title,
                            description,
                            deadlineAt,
                            recurrenceUnit,
                            recurrenceInterval,
                            selectedTagIds.toList(),
                        )
                    },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
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

    if (showRecurrencePicker) {
        RecurrencePicker(
            initialUnit = recurrenceUnit,
            initialInterval = recurrenceInterval,
            onConfirm = { unit, interval ->
                recurrenceUnit = unit
                recurrenceInterval = interval
                showRecurrencePicker = false
            },
            onDismiss = { showRecurrencePicker = false },
        )
    }

    if (showTagPicker) {
        TagPickerSheet(
            allTags = availableTags,
            selectedTagIds = selectedTagIds.toSet(),
            onToggle = { id ->
                if (id in selectedTagIds) selectedTagIds.remove(id) else selectedTagIds.add(id)
            },
            onCreate = { name, color ->
                // New tags are auto-selected for the task being added.
                onCreateTag(name, color) { tag -> selectedTagIds.add(tag.id) }
            },
            onUpdate = onUpdateTag,
            onDelete = { id ->
                selectedTagIds.remove(id)
                onDeleteTag(id)
            },
            onDismiss = { showTagPicker = false },
        )
    }
}
