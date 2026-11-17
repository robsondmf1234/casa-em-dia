package br.com.casaemdia.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.model.UserSession
import br.com.casaemdia.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userSession: UserSession? = null,
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState.Unauthenticated
        )

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true)
            authRepository.signInWithGoogle(context)
                .onSuccess { session ->
                    _loginUiState.value = LoginUiState(isLoading = false, userSession = session)
                }
                .onFailure { error ->
                    _loginUiState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "Falha ao realizar login"
                    )
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _loginUiState.value = LoginUiState()
        }
    }

    fun clearError() {
        _loginUiState.value = _loginUiState.value.copy(errorMessage = null)
    }
}
