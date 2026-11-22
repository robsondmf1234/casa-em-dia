package br.com.casaemdia.data.repository

import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.model.UserSession
import br.com.casaemdia.domain.repository.AuthRepository
import br.com.casaemdia.domain.repository.GoogleCredentialProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Sessão guardada pelo Firebase Auth, que a mantém salva no aparelho entre aberturas do app
 * e renova o token sozinho. O estado inicial vem de [FirebaseAuth.getCurrentUser], então já
 * na abertura sabemos se há alguém logado.
 */
class AuthRepositoryImpl(private val auth: FirebaseAuth) : AuthRepository {

    private val _authState = MutableStateFlow(auth.currentUser.toAuthState())
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Acompanha mudanças feitas fora deste repositório (ex.: sessão revogada ou expirada).
        auth.addAuthStateListener { _authState.value = it.currentUser.toAuthState() }
    }

    override suspend fun signInWithGoogle(provider: GoogleCredentialProvider): Result<UserSession> =
        runCatching {
            val credential = provider.requestCredential()
            val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
            val user = auth.signInWithCredential(firebaseCredential).await().user
                ?: throw IllegalStateException("Firebase não retornou o usuário")
            user.toSession().also { _authState.value = AuthState.Authenticated(it) }
        }

    override suspend fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}

internal fun FirebaseUser.toSession() = UserSession(
    id = uid,
    email = email.orEmpty(),
    displayName = displayName,
    profilePictureUrl = photoUrl?.toString(),
)

private fun FirebaseUser?.toAuthState(): AuthState =
    this?.let { AuthState.Authenticated(it.toSession()) } ?: AuthState.Unauthenticated
