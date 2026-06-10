package dev.heckr.kitsudo.presentation.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.heckr.kitsudo.data.update.WhatsNewManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val whatsNewManager: WhatsNewManager,
) : ViewModel() {

    val state: StateFlow<WhatsNewManager.State> = whatsNewManager.state

    init {
        viewModelScope.launch { whatsNewManager.maybeShowOnLaunch() }
    }

    fun dismiss() = whatsNewManager.dismiss()
}
