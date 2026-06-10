package dev.heckr.kitsudo.presentation.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.data.update.WhatsNewManager
import dev.heckr.kitsudo.presentation.settings.MarkdownText

/**
 * Shown over the whole app on the first launch after an update. Renders the
 * GitHub release notes for the installed version, with a brief loading state
 * while they fetch and a graceful fallback when none are available (e.g. when
 * offline).
 */
@Composable
fun WhatsNewDialog(
    state: WhatsNewManager.State,
    onDismiss: () -> Unit,
) {
    if (state is WhatsNewManager.State.Hidden) return

    val version = (state as? WhatsNewManager.State.Shown)?.version

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (version != null) {
                    stringResource(R.string.whats_new_title_format, version)
                } else {
                    stringResource(R.string.whats_new_title)
                },
            )
        },
        text = {
            when (state) {
                is WhatsNewManager.State.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is WhatsNewManager.State.Shown -> {
                    val notes = state.notes
                    if (!notes.isNullOrBlank()) {
                        MarkdownText(
                            markdown = notes,
                            modifier = Modifier
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.whats_new_no_notes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                else -> Unit
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.whats_new_dismiss))
            }
        },
    )
}
