package br.com.casaemdia.domain.repository

import br.com.casaemdia.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    /** Tema atual. É um StateFlow para que a tela já abra com o tema certo, sem piscar. */
    val themeMode: StateFlow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
