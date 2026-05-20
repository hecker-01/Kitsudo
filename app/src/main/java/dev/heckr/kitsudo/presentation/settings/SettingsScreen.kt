package dev.heckr.kitsudo.presentation.settings

import android.text.format.Formatter
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.data.update.AppUpdater
import dev.heckr.kitsudo.domain.model.CatppuccinFlavor
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.presentation.theme.labelRes
import dev.heckr.kitsudo.ui.theme.KitsudoTheme

// The three dark Catppuccin palettes shown in the segmented row
private val darkPalettes = listOf(
    ThemePalette.FRAPPE,
    ThemePalette.MACCHIATO,
    ThemePalette.MOCHA,
)

// ── Entry composable ───────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.onInstallPermissionResult() }

    LaunchedEffect(Unit) {
        viewModel.installPermissionIntent.collect { intent ->
            installPermissionLauncher.launch(intent)
        }
    }

    SettingsContent(
        uiState = uiState,
        onBack = onBack,
        onPaletteChange = viewModel::setPalette,
        onUpdateCardTapped = { viewModel.onUpdateCardTapped() },
        onUpdateConfirmed = viewModel::confirmUpdate,
        onUpdateDialogDismissed = viewModel::dismissUpdateDialog,
        modifier = modifier,
    )
}

// ── Content ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onPaletteChange: (ThemePalette) -> Unit,
    onUpdateCardTapped: () -> Unit,
    onUpdateConfirmed: () -> Unit,
    onUpdateDialogDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.task_detail_back),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    // Reserve space so content is never hidden behind the footer
                    .padding(bottom = 56.dp),
            ) {
                // ── Appearance ────────────────────────────────────────────────
                SectionLabel(stringResource(R.string.settings_section_appearance))
                AppearanceCard(palette = uiState.palette, onPaletteChange = onPaletteChange)

                // ── Updates ───────────────────────────────────────────────────
                SectionLabel(stringResource(R.string.settings_section_updates))
                UpdateCard(status = uiState.updateStatus, onTap = onUpdateCardTapped)

                // ── About ─────────────────────────────────────────────────────
                SectionLabel(stringResource(R.string.settings_section_about))
                AboutCard(uiState = uiState)
            }

            SettingsFooter(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (uiState.showUpdateDialog) {
        UpdateConfirmDialog(
            onConfirm = onUpdateConfirmed,
            onDismiss = onUpdateDialogDismissed,
        )
    }
}

// ── Section header ─────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

/** Card title inside a section card — always full-brightness. */
@Composable
private fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

// ── Appearance card ────────────────────────────────────────────────────────

@Composable
private fun AppearanceCard(
    palette: ThemePalette,
    onPaletteChange: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Remember the last-used Catppuccin palette so we can restore it when
    // the user switches back from Material You.
    var lastCatppuccinPalette by remember {
        mutableStateOf(if (palette.isCatppuccin) palette else ThemePalette.MOCHA)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CardTitle(stringResource(R.string.settings_theme_title))
            Spacer(modifier = Modifier.height(8.dp))

            // ── Top-level: Material You vs Catppuccin ──────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ThemeOptionCard(
                    label = stringResource(R.string.theme_palette_material3),
                    selected = !palette.isCatppuccin,
                    onClick = { onPaletteChange(ThemePalette.MATERIAL3) },
                    modifier = Modifier.weight(1f),
                )
                ThemeOptionCard(
                    label = stringResource(R.string.theme_palette_catppuccin),
                    selected = palette.isCatppuccin,
                    onClick = { onPaletteChange(lastCatppuccinPalette) },
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Material You note ──────────────────────────────────────
            AnimatedVisibility(visible = !palette.isCatppuccin) {
                Text(
                    text = stringResource(R.string.theme_palette_material3_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // ── Catppuccin section ─────────────────────────────────────
            AnimatedVisibility(visible = palette.isCatppuccin) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dark mode switch (M3 Expressive — icon thumb when checked)
                    DarkModeRow(
                        isDark = palette.isDark,
                        onToggle = { dark ->
                            if (dark) {
                                val target = if (lastCatppuccinPalette.isDark) {
                                    lastCatppuccinPalette
                                } else {
                                    ThemePalette.MOCHA
                                }
                                lastCatppuccinPalette = target
                                onPaletteChange(target)
                            } else {
                                lastCatppuccinPalette = ThemePalette.LATTE
                                onPaletteChange(ThemePalette.LATTE)
                            }
                        },
                    )

                    // Dark flavor row
                    AnimatedVisibility(visible = palette.isDark) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            DarkFlavorRow(
                                selected = palette,
                                onSelect = { chosen ->
                                    lastCatppuccinPalette = chosen
                                    onPaletteChange(chosen)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Dark mode switch (M3 Expressive) ──────────────────────────────────────

@Composable
private fun DarkModeRow(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val desc = stringResource(
        if (isDark) R.string.theme_dark_mode_on_description
        else R.string.theme_dark_mode_off_description,
    )
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.theme_dark_mode_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = isDark,
            onCheckedChange = { checked ->
                view.performHapticFeedback(
                    if (checked) HapticFeedbackConstants.TOGGLE_ON
                    else HapticFeedbackConstants.TOGGLE_OFF,
                )
                onToggle(checked)
            },
            thumbContent = if (isDark) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            },
            modifier = Modifier.semantics { contentDescription = desc },
        )
    }
}

// ── Frappé / Macchiato / Mocha card row ───────────────────────────────────

@Composable
private fun DarkFlavorRow(
    selected: ThemePalette,
    onSelect: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        darkPalettes.forEach { palette ->
            ThemeOptionCard(
                label = stringResource(palette.labelRes()),
                selected = palette == selected,
                onClick = { onSelect(palette) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Theme option card (shared by both picker rows) ─────────────────────────

@Composable
private fun ThemeOptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Card(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
        modifier = modifier,
    ) {
        // Inline checkmark + label. labelMedium (12 sp) + reduced horizontal
        // padding gives "Macchiato" enough room in the 3-column row without
        // wrapping. maxLines/Ellipsis is a safety net for any future labels.
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(14.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Updates card ───────────────────────────────────────────────────────────

@Composable
private fun UpdateCard(
    status: AppUpdater.Status,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val isActive = status is AppUpdater.Status.Downloading ||
        status is AppUpdater.Status.Installing ||
        status is AppUpdater.Status.Checking

    Card(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onTap()
        },
        enabled = !isActive,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_updates_check),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = updateSubtitle(status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (isActive) {
                val progress = (status as? AppUpdater.Status.Downloading)?.progress ?: -1
                Spacer(modifier = Modifier.height(8.dp))
                if (progress < 0) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun updateSubtitle(status: AppUpdater.Status): String = when (status) {
    is AppUpdater.Status.Idle -> stringResource(R.string.settings_updates_tap_hint)
    is AppUpdater.Status.Checking -> stringResource(R.string.settings_updates_checking)
    is AppUpdater.Status.UpToDate -> stringResource(R.string.settings_updates_up_to_date)
    is AppUpdater.Status.Available ->
        stringResource(R.string.settings_updates_available_format, status.version)
    is AppUpdater.Status.Downloading ->
        if (status.progress < 0) {
            stringResource(R.string.settings_updates_downloading_init)
        } else {
            stringResource(R.string.settings_updates_downloading_format, status.progress)
        }
    is AppUpdater.Status.Installing ->
        stringResource(R.string.settings_updates_installing)
    is AppUpdater.Status.Error ->
        stringResource(R.string.settings_updates_error_format, status.message)
}

// ── About card ─────────────────────────────────────────────────────────────

@Composable
private fun AboutCard(
    uiState: SettingsUiState,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CardTitle(stringResource(R.string.app_name))
            Text(
                text = stringResource(R.string.settings_about_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = stringResource(R.string.settings_version_format, uiState.versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.settings_build_format,
                    uiState.versionCode,
                    stringResource(
                        if (uiState.isDebug) R.string.settings_build_debug
                        else R.string.settings_build_release,
                    ),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = uiState.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ── Update confirm dialog ──────────────────────────────────────────────────

@Composable
private fun UpdateConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val checker = dev.heckr.kitsudo.data.update.UpdateChecker
    val version = checker.latestVersion ?: return
    val body = checker.releaseBody
    val sizeBytes = checker.apkSizeBytes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_update_dialog_title_format, version))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        R.string.settings_update_dialog_size_format,
                        Formatter.formatFileSize(context, sizeBytes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (!body.isNullOrBlank()) body
                    else stringResource(R.string.settings_update_no_changelog),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_update_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_update_dialog_cancel))
            }
        },
    )
}

// ── Footer (always pinned to screen bottom) ────────────────────────────────

@Composable
private fun SettingsFooter(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.settings_footer_url)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            // Opaque background so scrolling content doesn't bleed through
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_footer_prefix),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Explicit spacer — trailing spaces in XML string resources are trimmed by AAPT
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.settings_footer_link),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { uriHandler.openUri(url) },
        )
        Text(
            text = stringResource(R.string.settings_footer_suffix),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun SettingsPreviewMocha() {
    KitsudoTheme(palette = ThemePalette.MOCHA) {
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsContent(
                uiState = SettingsUiState(
                    palette = ThemePalette.MOCHA,
                    versionName = "1.0.0",
                    versionCode = 7L,
                    isDebug = true,
                    packageName = "dev.heckr.kitsudo.dev",
                ),
                onBack = {},
                onPaletteChange = {},
                onUpdateCardTapped = {},
                onUpdateConfirmed = {},
                onUpdateDialogDismissed = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreviewMaterial3() {
    KitsudoTheme(palette = ThemePalette.MATERIAL3) {
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsContent(
                uiState = SettingsUiState(
                    palette = ThemePalette.MATERIAL3,
                    updateStatus = AppUpdater.Status.Available("1.1.0"),
                    versionName = "1.0.0",
                    versionCode = 7L,
                    isDebug = false,
                    packageName = "dev.heckr.kitsudo",
                ),
                onBack = {},
                onPaletteChange = {},
                onUpdateCardTapped = {},
                onUpdateConfirmed = {},
                onUpdateDialogDismissed = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreviewLatte() {
    KitsudoTheme(palette = ThemePalette.LATTE) {
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsContent(
                uiState = SettingsUiState(
                    palette = ThemePalette.LATTE,
                    versionName = "1.0.0",
                    versionCode = 7L,
                    isDebug = false,
                    packageName = "dev.heckr.kitsudo",
                ),
                onBack = {},
                onPaletteChange = {},
                onUpdateCardTapped = {},
                onUpdateConfirmed = {},
                onUpdateDialogDismissed = {},
            )
        }
    }
}
