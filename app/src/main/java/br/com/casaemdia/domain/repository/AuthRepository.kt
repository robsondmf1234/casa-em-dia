package br.com.casaemdia.domain.repository

import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Obtains the signed-in user's credential from the platform (e.g. Android Credential Manager).
 * Implementations live in the data layer and hold whatever UI context the platform needs.
 */
fun interface GoogleCredentialProvider {
    suspend fun requestCredential(): UserSession
}

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun signInWithGoogle(provider: GoogleCredentialProvider): Result<UserSession>
    suspend fun signOut()
}
