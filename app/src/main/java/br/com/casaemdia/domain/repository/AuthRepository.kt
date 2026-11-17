package br.com.casaemdia.domain.repository

import android.content.Context
import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun signInWithGoogle(context: Context): Result<UserSession>
    suspend fun signOut()
}
