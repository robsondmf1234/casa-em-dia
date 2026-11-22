package br.com.casaemdia.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import br.com.casaemdia.R
import br.com.casaemdia.domain.model.GoogleCredential
import br.com.casaemdia.domain.model.SignInCancelledException
import br.com.casaemdia.domain.repository.GoogleCredentialProvider
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Pede um ID token do Google pelo Credential Manager. [context] precisa ser o de uma Activity.
 * O client ID Web vem do `google-services.json` (recurso `default_web_client_id`, gerado pelo plugin).
 */
class CredentialManagerGoogleProvider(
    private val context: Context,
    private val webClientId: String = context.getString(R.string.default_web_client_id),
) : GoogleCredentialProvider {

    override suspend fun requestCredential(): GoogleCredential {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = try {
            CredentialManager.create(context).getCredential(context = context, request = request)
        } catch (e: GetCredentialCancellationException) {
            throw SignInCancelledException()
        }

        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Tipo de credencial não suportado: ${credential.type}")
        }
        return GoogleCredential(idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken)
    }
}
