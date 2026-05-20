package dev.heckr.kitsudo.presentation.tasks

import android.Manifest
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.domain.model.SyncStatus
import dev.heckr.kitsudo.presentation.tasks.components.AddTaskSheet
import dev.heckr.kitsudo.presentation.tasks.components.DeadlineChip
import dev.heckr.kitsudo.presentation.tasks.components.SwipeActionBox
import dev.heckr.kitsudo.presentation.tasks.components.SwipeDirection
import dev.heckr.kitsudo.ui.theme.KitsudoTheme

@Composable
fun TaskListScreen(
    onOpenSettings: () -> Unit,
    onOpenTask: (String) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    TaskListContent(
        uiState = uiState,
        onOpenSettings = onOpenSettings,
        onOpenTask = onOpenTask,
        onShowAddSheet = viewModel::showAddSheet,
        onHideAddSheet = viewModel::hideAddSheet,
        onAddTask = viewModel::addTask,
        onToggleComplete = viewModel::toggleComplete,
        onToggleSubtaskComplete = viewModel::toggleSubtaskComplete,
        onDeleteTask = viewModel::deleteTask,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListContent(
    uiState: TaskListUiState,
    onOpenSettings: () -> Unit,
    onOpenTask: (String) -> Unit,
    onShowAddSheet: () -> Unit,
    onHideAddSheet: () -> Unit,
    onAddTask: (title: String, description: String, deadlineAt: Long?) -> Unit,
    onToggleComplete: (taskId: String, Boolean) -> Unit,
    onToggleSubtaskComplete: (subtaskId: String, Boolean) -> Unit,
    onDeleteTask: (taskId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_list_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onShowAddSheet) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.task_add_label))
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(innerPadding))
            uiState.error != null -> ErrorMessage(uiState.error, Modifier.padding(innerPadding))
            uiState.tasks.isEmpty() -> EmptyState(Modifier.padding(innerPadding))
            else -> TaskList(
                tasks = uiState.tasks,
                onOpenTask = onOpenTask,
                onToggleComplete = onToggleComplete,
                onToggleSubtaskComplete = onToggleSubtaskComplete,
                onDeleteTask = onDeleteTask,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (uiState.showAddSheet) {
        AddTaskSheet(onAdd = onAddTask, onDismiss = onHideAddSheet)
    }
}

// ── Task list ──────────────────────────────────────────────────────────────

@Composable
private fun TaskList(
    tasks: List<TaskWithSubtasksUi>,
    onOpenTask: (String) -> Unit,
    onToggleComplete: (String, Boolean) -> Unit,
    onToggleSubtaskComplete: (String, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onTap = { onOpenTask(task.id) },
                onToggleComplete = { onToggleComplete(task.id, it) },
                onToggleSubtaskComplete = onToggleSubtaskComplete,
                onDelete = { onDeleteTask(task.id) },
            )
        }
    }
}

// ── Task card ──────────────────────────────────────────────────────────────

@Composable
private fun TaskCard(
    task: TaskWithSubtasksUi,
    onTap: () -> Unit,
    onToggleComplete: (Boolean) -> Unit,
    onToggleSubtaskComplete: (String, Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(task.id) { mutableStateOf(false) }
    val arrowAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrow",
    )
    val view = LocalView.current

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Swipeable header (action fires on finger lift only) ────────
        SwipeActionBox(
            onSwipeLeft = onDelete,
            onSwipeRight = { onToggleComplete(!task.isCompleted) },
            swipeLeftHaptic = HapticFeedbackConstants.REJECT,
            swipeRightHaptic = if (task.isCompleted) {
                HapticFeedbackConstants.TOGGLE_OFF
            } else {
                HapticFeedbackConstants.CONFIRM
            },
            backgroundContent = { direction ->
                TaskSwipeBackground(direction = direction, isCompleted = task.isCompleted)
            },
        ) {
            Surface(
                onClick = onTap,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough
                            else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (task.description.isNotBlank()) {
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            if (task.deadlineAt != null) {
                                DeadlineChip(
                                    deadlineAt = task.deadlineAt,
                                    isOverdue = task.isDeadlineOverdue,
                                )
                            }
                            if (task.subtaskCount > 0) {
                                Text(
                                    text = stringResource(
                                        R.string.task_subtask_count,
                                        task.subtaskCompletedCount,
                                        task.subtaskCount,
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (task.subtaskCount > 0) {
                        IconButton(
                            onClick = {
                                expanded = !expanded
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            },
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = stringResource(
                                    if (expanded) R.string.task_subtasks_collapse
                                    else R.string.task_subtasks_expand,
                                ),
                                modifier = Modifier.rotate(arrowAngle),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // ── Expanded subtask list (not part of the swipe area) ─────────
        AnimatedVisibility(visible = expanded && task.subtaskCount > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(start = 56.dp, end = 8.dp, bottom = 4.dp, top = 2.dp),
            ) {
                task.subtasks.forEachIndexed { index, subtask ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        )
                    }
                    SubtaskListRow(
                        subtask = subtask,
                        onToggle = { checked ->
                            view.performHapticFeedback(
                                if (checked) HapticFeedbackConstants.TOGGLE_ON
                                else HapticFeedbackConstants.TOGGLE_OFF,
                            )
                            onToggleSubtaskComplete(subtask.id, checked)
                        },
                    )
                }
            }
        }

        HorizontalDivider()
    }
}

// ── Swipe reveal backgrounds ───────────────────────────────────────────────

@Composable
private fun TaskSwipeBackground(
    direction: SwipeDirection?,
    isCompleted: Boolean,
) {
    when (direction) {
        SwipeDirection.Right -> Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(start = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(
                        if (isCompleted) R.string.task_swipe_undo
                        else R.string.task_swipe_complete,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        SwipeDirection.Left -> Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(end = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.task_delete_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        null -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}

// ── Subtask row ────────────────────────────────────────────────────────────

@Composable
private fun SubtaskListRow(
    subtask: TaskUi,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Checkbox(checked = subtask.isCompleted, onCheckedChange = onToggle)
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtask.deadlineAt != null) {
                DeadlineChip(
                    deadlineAt = subtask.deadlineAt,
                    isOverdue = subtask.isDeadlineOverdue,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

// ── Utility composables ────────────────────────────────────────────────────

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorMessage(message: String, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        Text(message)
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.task_list_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Preview ────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun TaskListPreview() {
    KitsudoTheme {
        TaskListContent(
            uiState = TaskListUiState(
                tasks = listOf(
                    TaskWithSubtasksUi(
                        id = "1", title = "Buy groceries", description = "Milk, eggs",
                        isCompleted = false, deadlineAt = null, isDeadlineOverdue = false,
                        syncStatus = SyncStatus.SYNCED,
                        subtasks = listOf(
                            TaskUi(
                                id = "1a", title = "Milk", description = "",
                                isCompleted = true, deadlineAt = null, isDeadlineOverdue = false,
                                syncStatus = SyncStatus.SYNCED,
                            ),
                        ),
                    ),
                ),
            ),
            onOpenSettings = {}, onOpenTask = {}, onShowAddSheet = {},
            onHideAddSheet = {}, onAddTask = { _, _, _ -> },
            onToggleComplete = { _, _ -> },
            onToggleSubtaskComplete = { _, _ -> },
            onDeleteTask = {},
        )
    }
}
