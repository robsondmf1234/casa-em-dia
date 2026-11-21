package br.com.casaemdia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.casaemdia.domain.model.ThemeMode
import br.com.casaemdia.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

class SettingsViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settings.themeMode
        .map { SettingsUiState(themeMode = it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            // Começa com o valor já salvo para o primeiro frame usar o tema certo.
            SettingsUiState(themeMode = settings.themeMode.value),
        )

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}
