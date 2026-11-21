package br.com.casaemdia.ui.viewmodel

import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.repository.GoogleCredentialProvider
import br.com.casaemdia.testing.FakeAuthRepository
import br.com.casaemdia.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val provider = GoogleCredentialProvider { error("FakeAuthRepository does not call the provider") }

    @Test
    fun initialStateIsUnauthenticatedAndIdle() = runTest {
        val repo = FakeAuthRepository()
        val vm = AuthViewModel(repo)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.authState.collect {} }

        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertFalse(vm.loginUiState.value.isLoading)
        assertNull(vm.loginUiState.value.errorMessage)
        assertNull(vm.loginUiState.value.userSession)
    }

    @Test
    fun signInWithGoogleSuccessUpdatesSessionAndState() = runTest {
        val repo = FakeAuthRepository()
        val vm = AuthViewModel(repo)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.authState.collect {} }

        vm.signInWithGoogle(provider)

        assertFalse(vm.loginUiState.value.isLoading)
        assertEquals("teste@casaemdia.com", vm.loginUiState.value.userSession?.email)
        assertTrue(vm.authState.value is AuthState.Authenticated)
    }

    @Test
    fun signInWithGoogleFailureSetsErrorMessage() = runTest {
        val repo = FakeAuthRepository().apply { shouldFailSignIn = true }
        val vm = AuthViewModel(repo)

        vm.signInWithGoogle(provider)

        assertFalse(vm.loginUiState.value.isLoading)
        assertEquals("Erro ao realizar login", vm.loginUiState.value.errorMessage)
        assertNull(vm.loginUiState.value.userSession)
    }

    @Test
    fun signOutResetsAuthState() = runTest {
        val repo = FakeAuthRepository()
        val vm = AuthViewModel(repo)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.authState.collect {} }

        vm.signInWithGoogle(provider)
        assertTrue(vm.authState.value is AuthState.Authenticated)

        vm.signOut()
        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertNull(vm.loginUiState.value.userSession)
    }

    @Test
    fun clearErrorResetsErrorMessage() = runTest {
        val repo = FakeAuthRepository().apply { shouldFailSignIn = true }
        val vm = AuthViewModel(repo)

        vm.signInWithGoogle(provider)
        assertEquals("Erro ao realizar login", vm.loginUiState.value.errorMessage)

        vm.clearError()
        assertNull(vm.loginUiState.value.errorMessage)
    }
}
