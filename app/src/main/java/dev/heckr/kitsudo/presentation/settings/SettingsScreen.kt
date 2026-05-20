package dev.heckr.kitsudo.presentation.settings

import android.text.format.Formatter
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
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

            // ── Footer ────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_copyright_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
            )
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

@OptIn(ExperimentalMaterial3Api::class)
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
            Spacer(modifier = Modifier.height(12.dp))

            // ── Top-level: Material You vs Catppuccin ──────────────────
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !palette.isCatppuccin,
                    onClick = { onPaletteChange(ThemePalette.MATERIAL3) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text(stringResource(R.string.theme_palette_material3)) },
                )
                SegmentedButton(
                    selected = palette.isCatppuccin,
                    onClick = { onPaletteChange(lastCatppuccinPalette) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text(stringResource(R.string.theme_palette_catppuccin)) },
                )
            }

            // ── Material You note ──────────────────────────────────────
            AnimatedVisibility(visible = !palette.isCatppuccin) {
                Text(
                    text = stringResource(R.string.theme_palette_material3_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            // ── Catppuccin section ─────────────────────────────────────
            AnimatedVisibility(visible = palette.isCatppuccin) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

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
                            Spacer(modifier = Modifier.height(10.dp))
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
        )
        Switch(
            checked = isDark,
            onCheckedChange = onToggle,
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

// ── Frappé / Macchiato / Mocha segmented row ──────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkFlavorRow(
    selected: ThemePalette,
    onSelect: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        darkPalettes.forEachIndexed { index, palette ->
            SegmentedButton(
                selected = palette == selected,
                onClick = { onSelect(palette) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = darkPalettes.size,
                ),
                label = { Text(stringResource(palette.labelRes())) },
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
    val isActive = status is AppUpdater.Status.Downloading ||
        status is AppUpdater.Status.Installing ||
        status is AppUpdater.Status.Checking

    Card(
        onClick = onTap,
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
