package br.com.casaemdia.data.remote

import android.util.Log
import br.com.casaemdia.data.local.InventoryItemEntity
import br.com.casaemdia.data.local.ShoppingItemEntity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Acesso às coleções do usuário logado: `users/{uid}/inventory` e `users/{uid}/shopping`.
 *
 * As gravações não esperam a confirmação do servidor (o Task só completa quando o servidor
 * responde, o que nunca acontece offline). O Firestore aplica a gravação no cache local na hora,
 * os listeners de [inventorySnapshots]/[shoppingSnapshots] a recebem e a enviam ao servidor
 * quando houver conexão. Falhas (ex.: regras de segurança) só aparecem no log.
 */
class FirestoreDataSource(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    private fun userDoc(uid: String = requireUid()) = firestore.collection(USERS).document(uid)
    private fun inventory(uid: String = requireUid()): CollectionReference = userDoc(uid).collection(INVENTORY)
    private fun shopping(uid: String = requireUid()): CollectionReference = userDoc(uid).collection(SHOPPING)

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Nenhum usuário logado")

    fun newInventoryId(): String = inventory().document().id
    fun newShoppingId(): String = shopping().document().id

    fun saveInventory(item: InventoryItemEntity) {
        inventory().document(item.id).set(item.toFirestore()).logFailure("salvar item de estoque")
    }

    fun deleteInventory(id: String) {
        inventory().document(id).delete().logFailure("apagar item de estoque")
    }

    fun saveShopping(item: ShoppingItemEntity) {
        shopping().document(item.id).set(item.toFirestore()).logFailure("salvar item da lista")
    }

    fun deleteShopping(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val batch = firestore.batch()
        ids.forEach { batch.delete(shopping().document(it)) }
        batch.commit().logFailure("apagar itens da lista")
    }

    /** Marca a compra e ajusta o estoque numa única gravação em lote (atômica e válida offline). */
    fun setPurchased(id: String, purchased: Boolean, inventoryItemId: String?, stockDelta: Double) {
        val batch = firestore.batch()
        batch.update(shopping().document(id), Fields.PURCHASED, purchased)
        if (inventoryItemId != null && stockDelta != 0.0) {
            batch.update(inventory().document(inventoryItemId), Fields.QUANTITY, FieldValue.increment(stockDelta))
        }
        batch.commit().logFailure("marcar compra")
    }

    fun inventorySnapshots(uid: String): Flow<List<InventoryItemEntity>> =
        snapshots(inventory(uid)) { id, data -> inventoryEntityFrom(id, data) }

    fun shoppingSnapshots(uid: String): Flow<List<ShoppingItemEntity>> =
        snapshots(shopping(uid)) { id, data -> shoppingEntityFrom(id, data) }

    private fun <T> snapshots(
        collection: CollectionReference,
        map: (String, Map<String, Any?>) -> T,
    ): Flow<List<T>> = callbackFlow {
        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null) {
                trySend(snapshot.documents.map { map(it.id, it.data.orEmpty()) })
            }
        }
        awaitClose { registration.remove() }
    }

    private fun <T> Task<T>.logFailure(action: String) {
        addOnFailureListener { Log.e(TAG, "Falha ao $action no Firestore", it) }
    }

    private companion object {
        const val TAG = "FirestoreDataSource"
        const val USERS = "users"
        const val INVENTORY = "inventory"
        const val SHOPPING = "shopping"
    }
}
