package br.com.casaemdia.data.repository

import android.content.SharedPreferences
import br.com.casaemdia.domain.model.ThemeMode
import br.com.casaemdia.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda as preferências do app em [SharedPreferences]. Nomes desconhecidos voltam para [ThemeMode.SYSTEM]. */
class SettingsRepositoryImpl(private val prefs: SharedPreferences) : SettingsRepository {

    private val _themeMode = MutableStateFlow(readThemeMode())
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun readThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null)
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    companion object {
        const val PREFS_NAME = "casa_em_dia_settings"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
