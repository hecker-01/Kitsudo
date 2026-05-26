package dev.heckr.kitsudo.wear.presentation.tasks

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import dev.heckr.kitsudo.ui.theme.Mocha

@Composable
fun WearTaskListScreen(
    onTaskClick: (String) -> Unit,
    viewModel: WearTaskListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is WearTaskListUiState.Syncing -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is WearTaskListUiState.NoData -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = "No tasks yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = Mocha.Text,
                    )
                    Text(
                        text = "Add tasks in Kitsudo on your phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = Mocha.Subtext0,
                    )
                }
            }
        }

        is WearTaskListUiState.Success -> {
            TaskList(
                tasks = state.tasks,
                onTaskClick = onTaskClick,
                onToggle = { id, completed -> viewModel.toggleTask(id, completed) },
            )
        }
    }
}

@Composable
private fun TaskList(
    tasks: List<TaskWithSubtasks>,
    onTaskClick: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { ListHeader { Text("Tasks") } }

        items(tasks, key = { it.task.id }) { tws ->
            TaskRow(
                tws = tws,
                onClick = { onTaskClick(tws.task.id) },
                onToggle = { onToggle(tws.task.id, tws.task.isCompleted) },
            )
        }
    }
}

@Composable
private fun TaskRow(
    tws: TaskWithSubtasks,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val task = tws.task
    val completed = task.isCompleted
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (completed) Mocha.Surface0 else Mocha.Surface1,
            contentColor = if (completed) Mocha.Subtext1 else Mocha.Text,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckDot(checked = completed, onClick = onToggle)

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                    color = if (completed) Mocha.Subtext1 else Mocha.Text,
                )
                if (tws.totalSubtaskCount > 0) {
                    Text(
                        text = "${tws.completedSubtaskCount}/${tws.totalSubtaskCount} subtasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = Mocha.Subtext0,
                    )
                }
            }
        }
    }
}

/**
 * Small circular check indicator.
 *
 * - **Unchecked**: outlined ring in Overlay2 colour — clearly interactive, not filled.
 * - **Checked**: solid green fill with a ✓ glyph.
 */
@Composable
fun CheckDot(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val hapticClick = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
    }

    if (checked) {
        Button(
            onClick = hapticClick,
            modifier = modifier.size(28.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Mocha.Green,
                contentColor = Mocha.Base,
            ),
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    } else {
        Button(
            onClick = hapticClick,
            modifier = modifier
                .size(28.dp)
                .border(width = 2.dp, color = Mocha.Overlay2, shape = CircleShape),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Mocha.Surface1,
                contentColor = Mocha.Overlay2,
            ),
            contentPadding = PaddingValues(0.dp),
        ) {}
    }
}
