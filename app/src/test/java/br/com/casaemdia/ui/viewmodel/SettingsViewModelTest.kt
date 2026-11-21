package br.com.casaemdia.ui.viewmodel

import br.com.casaemdia.domain.model.ThemeMode
import br.com.casaemdia.testing.FakeSettingsRepository
import br.com.casaemdia.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateUsesSavedThemeBeforeAnyCollection() {
        val vm = SettingsViewModel(FakeSettingsRepository(ThemeMode.DARK))

        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
    }

    @Test
    fun selectingThemeUpdatesStateAndRepository() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onThemeModeSelected(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)
        assertEquals(ThemeMode.LIGHT, repo.themeMode.value)

        vm.onThemeModeSelected(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
    }
}
