package dev.heckr.kitsudo.presentation.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.domain.model.CatppuccinFlavor
import dev.heckr.kitsudo.domain.model.SyncStatus
import dev.heckr.kitsudo.presentation.theme.ThemePickerSheet
import dev.heckr.kitsudo.ui.theme.KitsudoTheme

@Composable
fun TaskListScreen(
    currentFlavor: CatppuccinFlavor,
    onFlavorChange: (CatppuccinFlavor) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TaskListContent(
        uiState = uiState,
        currentFlavor = currentFlavor,
        onFlavorChange = onFlavorChange,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListContent(
    uiState: TaskListUiState,
    currentFlavor: CatppuccinFlavor,
    onFlavorChange: (CatppuccinFlavor) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isThemePickerVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_list_title)) },
                actions = {
                    TextButton(onClick = { isThemePickerVisible = true }) {
                        Text(stringResource(R.string.theme_picker_action))
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(innerPadding))
            uiState.error != null -> ErrorMessage(uiState.error, Modifier.padding(innerPadding))
            uiState.tasks.isEmpty() -> EmptyState(Modifier.padding(innerPadding))
            else -> TaskList(uiState.tasks, Modifier.padding(innerPadding))
        }
    }

    if (isThemePickerVisible) {
        ThemePickerSheet(
            currentFlavor = currentFlavor,
            onFlavorSelected = { flavor ->
                onFlavorChange(flavor)
                isThemePickerVisible = false
            },
            onDismiss = { isThemePickerVisible = false },
        )
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(message)
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(stringResource(R.string.task_list_empty))
    }
}

@Composable
private fun TaskList(
    tasks: List<TaskUi>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskItem(task = task)
        }
    }
}

@Composable
private fun TaskItem(
    task: TaskUi,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(task.title) },
        supportingContent = { Text(task.description) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
private fun TaskListEmptyPreview() {
    KitsudoTheme {
        TaskListContent(
            uiState = TaskListUiState(),
            currentFlavor = CatppuccinFlavor.MOCHA,
            onFlavorChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskListLoadedPreview() {
    KitsudoTheme {
        TaskListContent(
            uiState = TaskListUiState(
                tasks = listOf(
                    TaskUi(
                        id = "1",
                        title = "Buy groceries",
                        description = "Milk, eggs, bread",
                        isCompleted = false,
                        syncStatus = SyncStatus.SYNCED,
                    ),
                    TaskUi(
                        id = "2",
                        title = "Call dentist",
                        description = "Schedule appointment",
                        isCompleted = true,
                        syncStatus = SyncStatus.PENDING,
                    ),
                ),
            ),
            currentFlavor = CatppuccinFlavor.MOCHA,
            onFlavorChange = {},
        )
    }
}
