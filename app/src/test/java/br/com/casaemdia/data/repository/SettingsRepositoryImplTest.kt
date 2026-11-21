package br.com.casaemdia.data.repository

import android.content.SharedPreferences
import br.com.casaemdia.domain.model.ThemeMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryImplTest {

    private fun prefs(stored: String?): Pair<SharedPreferences, SharedPreferences.Editor> {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putString(any(), any()) } returns editor
        val prefs = mockk<SharedPreferences>()
        every { prefs.getString("theme_mode", null) } returns stored
        every { prefs.edit() } returns editor
        return prefs to editor
    }

    @Test
    fun defaultsToSystemWhenNothingSaved() {
        val (prefs, _) = prefs(null)
        assertEquals(ThemeMode.SYSTEM, SettingsRepositoryImpl(prefs).themeMode.value)
    }

    @Test
    fun readsSavedTheme() {
        val (prefs, _) = prefs("DARK")
        assertEquals(ThemeMode.DARK, SettingsRepositoryImpl(prefs).themeMode.value)
    }

    @Test
    fun unknownSavedValueFallsBackToSystem() {
        val (prefs, _) = prefs("SEPIA")
        assertEquals(ThemeMode.SYSTEM, SettingsRepositoryImpl(prefs).themeMode.value)
    }

    @Test
    fun setThemeModePersistsAndEmits() = runTest {
        val (prefs, editor) = prefs(null)
        val repo = SettingsRepositoryImpl(prefs)

        repo.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, repo.themeMode.value)
        verify { editor.putString("theme_mode", "LIGHT") }
        verify { editor.apply() }
    }
}
