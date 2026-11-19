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
