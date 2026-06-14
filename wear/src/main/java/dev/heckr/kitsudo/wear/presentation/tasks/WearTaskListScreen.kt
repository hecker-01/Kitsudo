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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
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
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import dev.heckr.kitsudo.wear.BuildConfig
import dev.heckr.kitsudo.wear.ui.theme.LocalWearPaletteColors

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
            val p = LocalWearPaletteColors.current
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            // Scrollable so the app-info footer sits just below the fold; the
            // message fills the first screen (centered) on its own.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight),
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
                            color = p.text,
                        )
                        Text(
                            text = "Add tasks in Kitsudo on your phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = p.subtext0,
                        )
                    }
                }
                AppInfoFooter()
                Spacer(modifier = Modifier.height(16.dp))
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

        item { AppInfoFooter() }
    }
}

/** App identity footer: package name, release (version) name, and build number. */
@Composable
private fun AppInfoFooter() {
    val p = LocalWearPaletteColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = BuildConfig.APPLICATION_ID,
            style = MaterialTheme.typography.labelSmall,
            color = p.subtext0,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = p.subtext0,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Build ${BuildConfig.VERSION_CODE}",
            style = MaterialTheme.typography.labelSmall,
            color = p.subtext0,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TaskRow(
    tws: TaskWithSubtasks,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val p = LocalWearPaletteColors.current
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
            containerColor = if (completed) p.surface0 else p.surface1,
            contentColor   = if (completed) p.subtext1 else p.text,
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
                    color = if (completed) p.subtext1 else p.text,
                )
                if (tws.totalSubtaskCount > 0) {
                    Text(
                        text = "${tws.completedSubtaskCount}/${tws.totalSubtaskCount} subtasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = p.subtext0,
                    )
                }
            }
        }
    }
}

/**
 * Small circular check indicator.
 *
 * - **Unchecked**: outlined ring — clearly interactive, not filled.
 * - **Checked**: solid accent-green fill with a Material check icon.
 */
@Composable
fun CheckDot(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalWearPaletteColors.current
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
                containerColor = p.accent,
                contentColor   = p.base,
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
                .border(width = 2.dp, color = p.overlay2, shape = CircleShape),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = p.surface1,
                contentColor   = p.overlay2,
            ),
            contentPadding = PaddingValues(0.dp),
        ) {}
    }
}
