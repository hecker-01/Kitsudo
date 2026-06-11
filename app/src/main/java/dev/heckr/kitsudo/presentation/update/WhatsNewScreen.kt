package dev.heckr.kitsudo.presentation.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.data.update.WhatsNewManager
import dev.heckr.kitsudo.presentation.settings.MarkdownText

/**
 * Full-screen "What's New" shown on the first launch after an update, styled to
 * match the onboarding flow: a circular icon, the version title, the GitHub
 * release notes, and a single dismiss button. A brief loading state covers the
 * notes fetch, with a graceful fallback when none are available (e.g. offline).
 */
@Composable
fun WhatsNewScreen(
    state: WhatsNewManager.State,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is WhatsNewManager.State.Hidden) return

    val version = (state as? WhatsNewManager.State.Shown)?.version

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = if (version != null) {
                    stringResource(R.string.whats_new_title_format, version)
                } else {
                    stringResource(R.string.whats_new_title)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            // Notes area fills the space between the title and the dismiss button.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    is WhatsNewManager.State.Loading -> CircularProgressIndicator()

                    is WhatsNewManager.State.Shown -> {
                        val notes = state.notes
                        if (!notes.isNullOrBlank()) {
                            MarkdownText(
                                markdown = notes,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.whats_new_no_notes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    else -> Unit
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.whats_new_dismiss))
            }
        }
    }
}
