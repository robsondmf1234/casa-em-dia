package br.com.casaemdia.ui.viewmodel

import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.model.SignInCancelledException
import br.com.casaemdia.domain.model.UserSession
import br.com.casaemdia.domain.repository.GoogleCredentialProvider
import br.com.casaemdia.testing.FakeAuthRepository
import br.com.casaemdia.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val provider = GoogleCredentialProvider { error("FakeAuthRepository não chama o provider") }

    @Test
    fun initialStateIsUnauthenticatedAndIdle() = runTest {
        val vm = AuthViewModel(FakeAuthRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.authState.collect {} }

        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertFalse(vm.loginUiState.value.isLoading)
        assertFalse(vm.loginUiState.value.hasError)
        assertNull(vm.loginUiState.value.userSession)
    }

    @Test
    fun savedSessionIsAvailableBeforeAnyCollection() {
        val saved = AuthState.Authenticated(UserSession(id = "u1", email = "a@b.com"))
        val vm = AuthViewModel(FakeAuthRepository(saved))

        assertEquals(saved, vm.authState.value)
    }

    @Test
    fun signInWithGoogleSuccessUpdatesSessionAndState() = runTest {
        val vm = AuthViewModel(FakeAuthRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.authState.collect {} }

        vm.signInWithGoogle(provider)

        assertFalse(vm.loginUiState.value.isLoading)
        assertEquals("teste@casaemdia.com", vm.loginUiState.value.userSession?.email)
        assertTrue(vm.authState.value is AuthState.Authenticated)
    }

    @Test
    fun signInWithGoogleFailureFlagsError() = runTest {
        val vm = AuthViewModel(FakeAuthRepository().apply { signInError = RuntimeException("falhou") })

        vm.signInWithGoogle(provider)

        assertFalse(vm.loginUiState.value.isLoading)
        assertTrue(vm.loginUiState.value.hasError)
        assertNull(vm.loginUiState.value.userSession)
    }

    @Test
    fun cancellingAccountPickerIsNotAnError() = runTest {
        val vm = AuthViewModel(FakeAuthRepository().apply { signInError = SignInCancelledException() })

        vm.signInWithGoogle(provider)

        assertFalse(vm.loginUiState.value.isLoading)
        assertFalse(vm.loginUiState.value.hasError)
    }

    @Test
    fun signOutResetsAuthState() = runTest {
        val vm = AuthViewModel(FakeAuthRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.authState.collect {} }

        vm.signInWithGoogle(provider)
        assertTrue(vm.authState.value is AuthState.Authenticated)

        vm.signOut()
        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertNull(vm.loginUiState.value.userSession)
    }

    @Test
    fun errorShownClearsErrorFlag() = runTest {
        val vm = AuthViewModel(FakeAuthRepository().apply { signInError = RuntimeException("falhou") })

        vm.signInWithGoogle(provider)
        vm.errorShown()

        assertFalse(vm.loginUiState.value.hasError)
    }
}
