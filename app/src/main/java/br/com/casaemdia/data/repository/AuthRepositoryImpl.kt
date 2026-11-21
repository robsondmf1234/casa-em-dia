package br.com.casaemdia.data.repository

import androidx.credentials.exceptions.GetCredentialException
import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.model.UserSession
import br.com.casaemdia.domain.repository.AuthRepository
import br.com.casaemdia.domain.repository.GoogleCredentialProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl : AuthRepository {

    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    override fun observeAuthState(): Flow<AuthState> = authStateFlow.asStateFlow()

    override suspend fun signInWithGoogle(provider: GoogleCredentialProvider): Result<UserSession> =
        runCatching { provider.requestCredential() }
            .onSuccess { authStateFlow.value = AuthState.Authenticated(it) }
            .onFailure { exception ->
                if (exception is GetCredentialException) {
                    authStateFlow.value = AuthState.Unauthenticated
                }
            }

    override suspend fun signOut() {
        authStateFlow.value = AuthState.Unauthenticated
    }
}
