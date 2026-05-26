package dev.heckr.kitsudo.wear.presentation.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import dev.heckr.kitsudo.domain.model.Task
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import dev.heckr.kitsudo.ui.theme.Mocha

@Composable
fun WearTaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    viewModel: WearTaskDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is WearTaskDetailUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is WearTaskDetailUiState.NotFound -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Task not found", style = MaterialTheme.typography.bodyMedium, color = Mocha.Text)
            }
        }
        is WearTaskDetailUiState.Success -> {
            TaskDetail(
                data = state.data,
                onToggleTask = { id, completed -> viewModel.toggleTask(id, completed) },
            )
        }
    }
}

@Composable
private fun TaskDetail(
    data: TaskWithSubtasks,
    onToggleTask: (String, Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val task = data.task
    val completed = task.isCompleted

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // ── Title row ──────────────────────────────────────────────────────────
        item {
            ListHeader {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (completed) Mocha.Subtext0 else Mocha.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                )
            }
        }

        // ── Description (optional) ─────────────────────────────────────────────
        if (task.description.isNotBlank()) {
            item {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Mocha.Subtext1,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ── Complete / un-complete action ──────────────────────────────────────
        item {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleTask(task.id, completed)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (completed) Mocha.Surface2 else Mocha.Green,
                    contentColor = if (completed) Mocha.Subtext0 else Mocha.Base,
                ),
            ) {
                Text(
                    text = if (completed) "Mark Incomplete" else "Mark Complete",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        // ── Subtasks ────────────────────────────────────────────────────────────
        if (data.subtasks.isNotEmpty()) {
            item { ListHeader { Text("Subtasks", color = Mocha.Subtext0) } }

            items(data.subtasks, key = { it.id }) { subtask ->
                SubtaskRow(
                    subtask = subtask,
                    onToggle = { onToggleTask(subtask.id, subtask.isCompleted) },
                )
            }
        }
    }
}

@Composable
private fun SubtaskRow(
    subtask: Task,
    onToggle: () -> Unit,
) {
    val completed = subtask.isCompleted
    Button(
        onClick = onToggle,
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
            // Reuse the same CheckDot from the list screen for visual consistency
            CheckDot(checked = completed, onClick = onToggle)

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = subtask.title,
                style = MaterialTheme.typography.bodySmall,
                color = if (completed) Mocha.Subtext1 else Mocha.Text,
                textDecoration = if (completed) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
