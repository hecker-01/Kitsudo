package dev.heckr.kitsudo.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.heckr.kitsudo.BuildConfig
import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.data.update.AppUpdater
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.usecase.ExportTasksUseCase
import dev.heckr.kitsudo.domain.usecase.GetAccentUseCase
import dev.heckr.kitsudo.domain.usecase.GetNotificationPreferencesUseCase
import dev.heckr.kitsudo.domain.usecase.GetThemePaletteUseCase
import dev.heckr.kitsudo.domain.usecase.ImportTasksUseCase
import dev.heckr.kitsudo.domain.usecase.SetAccentUseCase
import dev.heckr.kitsudo.domain.usecase.SetThemePaletteUseCase
import dev.heckr.kitsudo.domain.usecase.UpdateNotificationPreferenceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val getThemePaletteUseCase: GetThemePaletteUseCase,
    private val setThemePaletteUseCase: SetThemePaletteUseCase,
    private val getAccentUseCase: GetAccentUseCase,
    private val setAccentUseCase: SetAccentUseCase,
    private val getNotificationPreferencesUseCase: GetNotificationPreferencesUseCase,
    private val updateNotificationPreferenceUseCase: UpdateNotificationPreferenceUseCase,
    private val exportTasksUseCase: ExportTasksUseCase,
    private val importTasksUseCase: ImportTasksUseCase,
    private val appUpdater: AppUpdater,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            isDebug = BuildConfig.DEBUG,
            packageName = BuildConfig.APPLICATION_ID,
            usePlayStoreUpdates = BuildConfig.PLAY_STORE_BUILD ||
                appUpdater.isInstalledFromPlayStore(appContext),
            // Snapshot at screen open - an update already found by the launch check
            // pins the Updates section to the top this session.
            pinUpdatesToTop = appUpdater.status.value is AppUpdater.Status.Available,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val installPermissionIntent: SharedFlow<Intent> = appUpdater.installPermissionIntent

    /** One-shot user-facing messages (string res ids) for backup/restore results. */
    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    /** Uri of the file chosen for import, held while the merge/replace dialog is up. */
    private var pendingImportUri: Uri? = null

    init {
        getThemePaletteUseCase()
            .onEach { palette -> _uiState.update { it.copy(palette = palette) } }
            .catch { /* retain default */ }
            .launchIn(viewModelScope)

        getAccentUseCase()
            .onEach { accent -> _uiState.update { it.copy(accent = accent) } }
            .catch { /* retain default */ }
            .launchIn(viewModelScope)

        getNotificationPreferencesUseCase()
            .onEach { prefs -> _uiState.update { it.copy(notifications = prefs) } }
            .catch { /* retain default */ }
            .launchIn(viewModelScope)

        appUpdater.status
            .onEach { status ->
                _uiState.update {
                    it.copy(updateStatus = status, updateInfo = appUpdater.availableUpdate)
                }
            }
            .launchIn(viewModelScope)
    }

    fun setPalette(palette: ThemePalette) {
        viewModelScope.launch { setThemePaletteUseCase(palette) }
    }

    fun setAccent(accent: CatppuccinAccent) {
        viewModelScope.launch { setAccentUseCase(accent) }
    }

    // -- Notification preferences ---------------------------------------

    fun setPreReminderLeadMinutes(minutes: Set<Int>) {
        viewModelScope.launch { updateNotificationPreferenceUseCase.setPreReminderLeadMinutes(minutes) }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch { updateNotificationPreferenceUseCase.setQuietHoursEnabled(enabled) }
    }

    fun setQuietStartMinutes(minutes: Int) {
        viewModelScope.launch { updateNotificationPreferenceUseCase.setQuietStartMinutes(minutes) }
    }

    fun setQuietEndMinutes(minutes: Int) {
        viewModelScope.launch { updateNotificationPreferenceUseCase.setQuietEndMinutes(minutes) }
    }

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { updateNotificationPreferenceUseCase.setSnoozeMinutes(minutes) }
    }

    fun onUpdateCardTapped(): Boolean {
        val shouldShow = appUpdater.onUpdateTapped(appContext)
        if (shouldShow) _uiState.update { it.copy(showUpdateDialog = true) }
        return shouldShow
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun confirmUpdate() {
        _uiState.update { it.copy(showUpdateDialog = false) }
        appUpdater.startDownload(appContext)
    }

    fun onInstallPermissionResult() {
        appUpdater.onInstallPermissionResult(appContext)
    }

    // -- Backup & restore ------------------------------------------------------

    /** Writes a JSON backup of all tasks to the user-chosen [uri]. */
    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val data = exportTasksUseCase()
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri)?.use {
                        it.write(data.toByteArray())
                    } ?: error("Could not open output stream")
                }
            }.isSuccess
            _messages.tryEmit(
                if (ok) R.string.settings_backup_export_success
                else R.string.settings_backup_export_failed,
            )
        }
    }

    /** Stores the chosen import [uri] and opens the merge/replace dialog. */
    fun onImportFileChosen(uri: Uri) {
        pendingImportUri = uri
        _uiState.update { it.copy(showImportModeDialog = true) }
    }

    fun dismissImportDialog() {
        pendingImportUri = null
        _uiState.update { it.copy(showImportModeDialog = false) }
    }

    /** Reads the pending file and imports the chosen parts (tasks and/or settings). */
    fun importSelected(
        importTasks: Boolean,
        importSettings: Boolean,
        mode: ImportTasksUseCase.Mode,
    ) {
        val uri = pendingImportUri
        pendingImportUri = null
        _uiState.update { it.copy(showImportModeDialog = false) }
        if (uri == null || (!importTasks && !importSettings)) return
        viewModelScope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().decodeToString()
                    } ?: error("Could not open input stream")
                }
                importTasksUseCase(text, importTasks, importSettings, mode).getOrThrow()
            }
            _messages.tryEmit(
                if (result.isSuccess) R.string.settings_backup_import_success
                else R.string.settings_backup_import_failed,
            )
        }
    }

    override fun onCleared() {
        appUpdater.cancel()
        super.onCleared()
    }
}
