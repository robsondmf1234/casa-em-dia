package br.com.casaemdia.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.model.UserSession
import br.com.casaemdia.domain.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(
    private val webClientId: String = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
) : AuthRepository {

    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    override fun observeAuthState(): Flow<AuthState> = authStateFlow.asStateFlow()

    override suspend fun signInWithGoogle(context: Context): Result<UserSession> = runCatching {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(
            context = context,
            request = request
        )

        when (val credential = response.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val userSession = UserSession(
                        id = googleCredential.id,
                        email = googleCredential.id,
                        displayName = googleCredential.displayName ?: googleCredential.givenName,
                        profilePictureUrl = googleCredential.profilePictureUri?.toString()
                    )
                    authStateFlow.value = AuthState.Authenticated(userSession)
                    userSession
                } else {
                    throw IllegalStateException("Tipo de credencial não suportado: ${credential.type}")
                }
            }
            else -> throw IllegalStateException("Credencial não reconhecida")
        }
    }.onFailure { exception ->
        if (exception is GetCredentialException) {
            authStateFlow.value = AuthState.Unauthenticated
        }
    }

    override suspend fun signOut() {
        authStateFlow.value = AuthState.Unauthenticated
    }
}
