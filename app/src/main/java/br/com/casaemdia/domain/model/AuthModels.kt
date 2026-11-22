package br.com.casaemdia.domain.model

data class UserSession(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val profilePictureUrl: String? = null,
)

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(val user: UserSession) : AuthState
}

/** Token de identidade devolvido pela conta Google, trocado por uma sessão do Firebase. */
data class GoogleCredential(val idToken: String)

/** O usuário fechou a escolha de conta sem entrar. Não é um erro a mostrar. */
class SignInCancelledException : Exception("Login cancelado pelo usuário")
