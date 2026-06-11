package dev.heckr.kitsudo.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.heckr.kitsudo.data.onboarding.OnboardingManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingManager: OnboardingManager,
) : ViewModel() {

    /** Whether the welcome flow should be shown over the rest of the app. */
    val state: StateFlow<OnboardingState> = onboardingManager.isComplete
        .map { if (it) OnboardingState.Completed else OnboardingState.Required }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OnboardingState.Loading,
        )

    fun complete() {
        viewModelScope.launch { onboardingManager.markComplete() }
    }
}

/** Resolves from [Loading] to either [Required] (first install) or [Completed]. */
enum class OnboardingState { Loading, Required, Completed }
