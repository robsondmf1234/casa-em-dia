package br.com.casaemdia.data.repository

import android.net.Uri
import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.model.GoogleCredential
import br.com.casaemdia.domain.model.SignInCancelledException
import br.com.casaemdia.domain.repository.GoogleCredentialProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val listener = slot<FirebaseAuth.AuthStateListener>()
    private val googleCredential = mockk<AuthCredential>()

    @Before
    fun setUp() {
        every { auth.addAuthStateListener(capture(listener)) } returns Unit
        mockkStatic(GoogleAuthProvider::class)
        every { GoogleAuthProvider.getCredential("token-123", null) } returns googleCredential
    }

    @After
    fun tearDown() = unmockkStatic(GoogleAuthProvider::class)

    private fun user(uid: String = "uid-1", email: String = "pessoa@exemplo.com") = mockk<FirebaseUser> {
        every { this@mockk.uid } returns uid
        every { this@mockk.email } returns email
        every { displayName } returns "Pessoa"
        every { photoUrl } returns null as Uri?
    }

    private fun completedTask(result: AuthResult): Task<AuthResult> = mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns null
        every { this@mockk.result } returns result
    }

    @Test
    fun startsUnauthenticatedWhenNoSavedUser() {
        every { auth.currentUser } returns null

        assertEquals(AuthState.Unauthenticated, AuthRepositoryImpl(auth).authState.value)
    }

    @Test
    fun restoresSavedSessionOnStart() {
        every { auth.currentUser } returns user()

        val state = AuthRepositoryImpl(auth).authState.value

        assertTrue(state is AuthState.Authenticated)
        assertEquals("uid-1", (state as AuthState.Authenticated).user.id)
        assertEquals("pessoa@exemplo.com", state.user.email)
    }

    @Test
    fun signInExchangesGoogleTokenForFirebaseSession() = runTest {
        every { auth.currentUser } returns null
        val authResult = mockk<AuthResult> { every { this@mockk.user } returns user(uid = "uid-9") }
        every { auth.signInWithCredential(googleCredential) } returns completedTask(authResult)
        val repo = AuthRepositoryImpl(auth)

        val result = repo.signInWithGoogle(GoogleCredentialProvider { GoogleCredential("token-123") })

        assertEquals("uid-9", result.getOrThrow().id)
        assertEquals("uid-9", (repo.authState.value as AuthState.Authenticated).user.id)
    }

    @Test
    fun cancelledPickerKeepsUnauthenticatedAndReportsCancellation() = runTest {
        every { auth.currentUser } returns null
        val repo = AuthRepositoryImpl(auth)

        val result = repo.signInWithGoogle(GoogleCredentialProvider { throw SignInCancelledException() })

        assertTrue(result.exceptionOrNull() is SignInCancelledException)
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
        verify(exactly = 0) { auth.signInWithCredential(any()) }
    }

    @Test
    fun signOutClearsFirebaseSession() = runTest {
        every { auth.currentUser } returns user()
        val repo = AuthRepositoryImpl(auth)

        repo.signOut()

        verify { auth.signOut() }
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }

    @Test
    fun followsFirebaseAuthStateChanges() {
        every { auth.currentUser } returns user()
        val repo = AuthRepositoryImpl(auth)

        every { auth.currentUser } returns null
        listener.captured.onAuthStateChanged(auth)

        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }
}
