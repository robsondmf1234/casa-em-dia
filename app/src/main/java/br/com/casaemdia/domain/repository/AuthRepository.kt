package br.com.casaemdia.domain.repository

import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.model.GoogleCredential
import br.com.casaemdia.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

/**
 * Obtém a credencial Google do usuário pela plataforma (Credential Manager no Android).
 * As implementações ficam na camada data e guardam o contexto de tela que a plataforma exige.
 * Lança [br.com.casaemdia.domain.model.SignInCancelledException] se o usuário fechar a escolha de conta.
 */
fun interface GoogleCredentialProvider {
    suspend fun requestCredential(): GoogleCredential
}

interface AuthRepository {
    /**
     * Sessão atual. É um StateFlow para que o app saiba já na abertura se há alguém logado,
     * sem passar pela tela de login. A sessão fica salva entre aberturas do app.
     */
    val authState: StateFlow<AuthState>

    suspend fun signInWithGoogle(provider: GoogleCredentialProvider): Result<UserSession>
    suspend fun signOut()
}
