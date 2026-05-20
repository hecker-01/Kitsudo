package dev.heckr.kitsudo.presentation.tasks.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeadlineChip(
    deadlineAt: Long,
    isOverdue: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = remember(deadlineAt) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(deadlineAt))
    }
    val containerColor = if (isOverdue) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isOverdue) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
