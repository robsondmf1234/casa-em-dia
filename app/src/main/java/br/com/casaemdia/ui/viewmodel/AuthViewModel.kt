package br.com.casaemdia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.model.SignInCancelledException
import br.com.casaemdia.domain.model.UserSession
import br.com.casaemdia.domain.repository.AuthRepository
import br.com.casaemdia.domain.repository.GoogleCredentialProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val userSession: UserSession? = null,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            // Já começa com a sessão salva, para a abertura do app decidir o destino sem esperar.
            initialValue = authRepository.authState.value,
        )

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    fun signInWithGoogle(provider: GoogleCredentialProvider) {
        if (_loginUiState.value.isLoading) return
        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true)
            authRepository.signInWithGoogle(provider)
                .onSuccess { session ->
                    _loginUiState.value = LoginUiState(userSession = session)
                }
                .onFailure { error ->
                    // Fechar a escolha de conta não é erro: só volta ao botão de entrar.
                    _loginUiState.value = LoginUiState(hasError = error !is SignInCancelledException)
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _loginUiState.value = LoginUiState()
        }
    }

    fun errorShown() = _loginUiState.update { it.copy(hasError = false) }
}
