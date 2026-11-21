package br.com.casaemdia.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import br.com.casaemdia.BuildConfig
import br.com.casaemdia.domain.model.UserSession
import br.com.casaemdia.domain.repository.GoogleCredentialProvider
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Requests a Google ID token through Credential Manager. [context] must be an Activity context. */
class CredentialManagerGoogleProvider(
    private val context: Context,
    private val webClientId: String = BuildConfig.WEB_CLIENT_ID,
) : GoogleCredentialProvider {

    override suspend fun requestCredential(): UserSession {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = CredentialManager.create(context).getCredential(context = context, request = request)

        val credential = response.credential
        if (credential !is CustomCredential) throw IllegalStateException("Credencial não reconhecida")
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("Tipo de credencial não suportado: ${credential.type}")
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        return UserSession(
            id = googleCredential.id,
            email = googleCredential.id,
            displayName = googleCredential.displayName ?: googleCredential.givenName,
            profilePictureUrl = googleCredential.profilePictureUri?.toString(),
        )
    }
}
