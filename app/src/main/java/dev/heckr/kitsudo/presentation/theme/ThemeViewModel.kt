package dev.heckr.kitsudo.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.heckr.kitsudo.domain.model.CatppuccinFlavor
import dev.heckr.kitsudo.domain.usecase.GetThemeFlavorUseCase
import dev.heckr.kitsudo.domain.usecase.SetThemeFlavorUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val getThemeFlavorUseCase: GetThemeFlavorUseCase,
    private val setThemeFlavorUseCase: SetThemeFlavorUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeUiState())
    val uiState: StateFlow<ThemeUiState> = _uiState.asStateFlow()

    init {
        getThemeFlavorUseCase()
            .onEach { flavor -> _uiState.update { it.copy(flavor = flavor) } }
            .catch { /* retain default */ }
            .launchIn(viewModelScope)
    }

    fun setFlavor(flavor: CatppuccinFlavor) {
        viewModelScope.launch { setThemeFlavorUseCase(flavor) }
    }
}
