package dev.heckr.kitsudo.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.heckr.kitsudo.BuildConfig
import dev.heckr.kitsudo.data.update.AppUpdater
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.usecase.GetThemeFlavorUseCase
import dev.heckr.kitsudo.domain.usecase.SetThemeFlavorUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val getThemeFlavorUseCase: GetThemeFlavorUseCase,
    private val setThemeFlavorUseCase: SetThemeFlavorUseCase,
    private val appUpdater: AppUpdater,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            isDebug = BuildConfig.DEBUG,
            packageName = BuildConfig.APPLICATION_ID,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val installPermissionIntent: SharedFlow<Intent> = appUpdater.installPermissionIntent

    init {
        getThemeFlavorUseCase()
            .onEach { palette -> _uiState.update { it.copy(palette = palette) } }
            .catch { /* retain default */ }
            .launchIn(viewModelScope)

        appUpdater.status
            .onEach { status -> _uiState.update { it.copy(updateStatus = status) } }
            .launchIn(viewModelScope)

        appUpdater.syncFromChecker()
    }

    fun setPalette(palette: ThemePalette) {
        viewModelScope.launch { setThemeFlavorUseCase(palette) }
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

    override fun onCleared() {
        appUpdater.cancel()
        super.onCleared()
    }
}
