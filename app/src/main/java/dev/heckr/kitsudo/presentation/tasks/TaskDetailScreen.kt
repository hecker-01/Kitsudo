package dev.heckr.kitsudo.presentation.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.view.HapticFeedbackConstants
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.presentation.tasks.components.DeadlineChip
import dev.heckr.kitsudo.presentation.tasks.components.DeadlinePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
            CircularProgressIndicator()
        }
        return
    }

    val task = uiState.task ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    TaskDetailContent(
        task = task,
        subtasks = uiState.subtasks,
        onBack = onBack,
        onTitleChange = viewModel::saveTitle,
        onDescriptionChange = viewModel::saveDescription,
        onSetDeadline = viewModel::setDeadline,
        onToggleComplete = viewModel::toggleComplete,
        onAddSubtask = viewModel::addSubtask,
        onToggleSubtask = viewModel::toggleSubtaskComplete,
        onSetSubtaskDeadline = viewModel::setSubtaskDeadline,
        onDeleteSubtask = viewModel::deleteSubtask,
        onDeleteTask = { viewModel.deleteTask(onBack) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailContent(
    task: TaskUi,
    subtasks: List<TaskUi>,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSetDeadline: (Long?) -> Unit,
    onToggleComplete: (Boolean) -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (String, Boolean) -> Unit,
    onSetSubtaskDeadline: (subtaskId: String, deadlineAt: Long?) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onDeleteTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local field state — seeded from the live task, but NOT reset on every recomposition.
    // rememberSaveable(task.id) resets only when a *different* task is opened.
    var titleField by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var descriptionField by rememberSaveable(task.id) { mutableStateOf(task.description) }
    var showDeadlinePicker by rememberSaveable { mutableStateOf(false) }
    var newSubtaskTitle by rememberSaveable { mutableStateOf("") }
    val view = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleField.ifBlank { task.title },
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.task_detail_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        onDeleteTask()
                    }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.task_delete_label),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp)
                .imePadding(),
        ) {
            // ── Completion toggle ──────────────────────────────────────
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { checked ->
                            view.performHapticFeedback(
                                if (checked) HapticFeedbackConstants.TOGGLE_ON
                                else HapticFeedbackConstants.TOGGLE_OFF,
                            )
                            onToggleComplete(checked)
                        },
                    )
                    Text(
                        text = stringResource(
                            if (task.isCompleted) R.string.task_mark_incomplete
                            else R.string.task_mark_complete,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            // ── Title & description ────────────────────────────────────
            SectionCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(12.dp),
                ) {
                    OutlinedTextField(
                        value = titleField,
                        onValueChange = { v ->
                            titleField = v
                            onTitleChange(v)
                        },
                        label = { Text(stringResource(R.string.task_field_title)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = descriptionField,
                        onValueChange = { v ->
                            descriptionField = v
                            onDescriptionChange(v)
                        },
                        label = { Text(stringResource(R.string.task_field_description)) },
                        minLines = 2,
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Deadline ───────────────────────────────────────────────
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle(stringResource(R.string.task_deadline_label))
                    Spacer(Modifier.height(10.dp))

                    // Deadline chip sits on its own line when present
                    if (task.deadlineAt != null) {
                        DeadlineChip(
                            deadlineAt = task.deadlineAt,
                            isOverdue = task.isDeadlineOverdue,
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Action buttons always on their own line — no overflow risk
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { showDeadlinePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                            Text(
                                text = stringResource(
                                    if (task.deadlineAt != null) R.string.task_deadline_change
                                    else R.string.task_deadline_set,
                                ),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                        if (task.deadlineAt != null) {
                            OutlinedButton(onClick = { onSetDeadline(null) }) {
                                Text(stringResource(R.string.task_deadline_clear))
                            }
                        }
                    }
                }
            }

            // ── Subtasks ───────────────────────────────────────────────
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle(
                        text = if (subtasks.isEmpty()) {
                            stringResource(R.string.task_subtasks_label)
                        } else {
                            stringResource(
                                R.string.task_subtasks_label_count,
                                subtasks.count { it.isCompleted },
                                subtasks.size,
                            )
                        },
                    )

                    if (subtasks.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        subtasks.forEachIndexed { index, subtask ->
                            if (index > 0) HorizontalDivider()
                            SubtaskRow(
                                subtask = subtask,
                                onToggle = { onToggleSubtask(subtask.id, it) },
                                onSetDeadline = { onSetSubtaskDeadline(subtask.id, it) },
                                onDelete = { onDeleteSubtask(subtask.id) },
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = newSubtaskTitle,
                            onValueChange = { newSubtaskTitle = it },
                            placeholder = {
                                Text(stringResource(R.string.task_subtask_add_hint))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onAddSubtask(newSubtaskTitle)
                                newSubtaskTitle = ""
                            },
                            enabled = newSubtaskTitle.isNotBlank(),
                        ) {
                            Text(stringResource(R.string.task_subtask_add))
                        }
                    }
                }
            }
        }
    }

    if (showDeadlinePicker) {
        DeadlinePicker(
            initialDeadlineAt = task.deadlineAt,
            onDeadlinePicked = { picked ->
                onSetDeadline(picked)
                showDeadlinePicker = false
            },
            onDismiss = { showDeadlinePicker = false },
        )
    }
}

// ── Shared card wrapper ────────────────────────────────────────────────────

/**
 * All detail cards use the same container/content colors.
 * Explicit values prevent the Catppuccin palette from producing unexpected
 * color inheritance via [CardDefaults.cardColors].
 */
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
        content = { content() },
    )
}

/** Section heading inside a card — always uses [onSurface] so it stands out. */
@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

// ── Subtask row ────────────────────────────────────────────────────────────

@Composable
private fun SubtaskRow(
    subtask: TaskUi,
    onToggle: (Boolean) -> Unit,
    onSetDeadline: (Long?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 2.dp),
        ) {
            Checkbox(
                checked = subtask.isCompleted,
                onCheckedChange = { checked ->
                    view.performHapticFeedback(
                        if (checked) HapticFeedbackConstants.TOGGLE_ON
                        else HapticFeedbackConstants.TOGGLE_OFF,
                    )
                    onToggle(checked)
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            ) {
                Text(
                    text = subtask.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                )
                if (subtask.deadlineAt != null) {
                    DeadlineChip(
                        deadlineAt = subtask.deadlineAt,
                        isOverdue = subtask.isDeadlineOverdue,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            // Calendar icon — tap to set/change deadline
            IconButton(onClick = { showPicker = true }) {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.task_deadline_set),
                    tint = if (subtask.deadlineAt != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.task_delete_label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showPicker) {
        DeadlinePicker(
            initialDeadlineAt = subtask.deadlineAt,
            onDeadlinePicked = { picked ->
                onSetDeadline(picked)
                showPicker = false
            },
            onDismiss = { showPicker = false },
            showClearOption = subtask.deadlineAt != null,
            onClear = {
                onSetDeadline(null)
                showPicker = false
            },
        )
    }
}
