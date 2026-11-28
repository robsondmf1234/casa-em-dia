package br.com.casaemdia.data.remote

import android.util.Log
import androidx.room.withTransaction
import br.com.casaemdia.data.local.AppDatabase
import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

/**
 * Espelho Firestore → Room. Enquanto há um usuário logado, escuta as coleções dele no Firestore
 * e substitui as tabelas do Room a cada mudança (inclusive as gravações locais ainda não enviadas).
 * Sem usuário (logout), apaga a cópia local para outra conta não ver esses dados.
 * O Room nunca envia nada ao Firestore.
 */
class FirestoreSync(
    private val auth: AuthRepository,
    private val remote: FirestoreDataSource,
    private val db: AppDatabase,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            auth.authState
                .map { (it as? AuthState.Authenticated)?.user?.id }
                .distinctUntilChanged()
                .collectLatest { uid ->
                    if (uid == null) {
                        clearLocalCopy()
                    } else {
                        mirror(uid)
                    }
                }
        }
    }

    private suspend fun mirror(uid: String) = coroutineScope {
        launch {
            remote.inventorySnapshots(uid)
                .retryWithBackoff("estoque")
                .collect { items ->
                    db.withTransaction {
                        db.inventoryDao().deleteAll()
                        db.inventoryDao().insertAll(items)
                    }
                }
        }
        launch {
            remote.shoppingSnapshots(uid)
                .retryWithBackoff("lista de compras")
                .collect { items ->
                    db.withTransaction {
                        db.shoppingDao().deleteAll()
                        db.shoppingDao().insertAll(items)
                    }
                }
        }
    }

    /**
     * Um listener do Firestore que recebe erro (ex.: regras ainda não publicadas, falha temporária
     * no servidor) é encerrado. Em vez de parar a sincronização até o app ser reaberto, tenta de
     * novo com espera crescente (2 s, 4 s, 8 s... até 60 s). O retry é cancelado no logout.
     */
    private fun <T> Flow<T>.retryWithBackoff(what: String): Flow<T> = retryWhen { cause, attempt ->
        val wait = minOf(MAX_RETRY_DELAY_MS, INITIAL_RETRY_DELAY_MS shl attempt.coerceAtMost(5).toInt())
        Log.w(TAG, "Falha ao ouvir $what no Firestore; nova tentativa em ${wait / 1000} s", cause)
        delay(wait)
        true
    }

    private suspend fun clearLocalCopy() {
        db.withTransaction {
            db.inventoryDao().deleteAll()
            db.shoppingDao().deleteAll()
        }
    }

    private companion object {
        const val TAG = "FirestoreSync"
        const val INITIAL_RETRY_DELAY_MS = 2_000L
        const val MAX_RETRY_DELAY_MS = 60_000L
    }
}
