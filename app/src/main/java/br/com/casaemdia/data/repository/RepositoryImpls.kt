package br.com.casaemdia.data.repository

import br.com.casaemdia.data.local.InventoryDao
import br.com.casaemdia.data.local.ShoppingDao
import br.com.casaemdia.data.local.toDomain
import br.com.casaemdia.data.local.toEntity
import br.com.casaemdia.data.remote.FirestoreDataSource
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.repository.ShoppingRepository
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Lê da cópia local (Room) e grava no Firestore; a sincronização traz as gravações de volta ao Room. */
class InventoryRepositoryImpl(
    private val dao: InventoryDao,
    private val remote: FirestoreDataSource,
    private val clock: Clock,
) : InventoryRepository {
    override fun observeItems(): Flow<List<InventoryItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeItem(id: String): Flow<InventoryItem?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getItem(id: String): InventoryItem? = dao.getById(id)?.toDomain()

    override suspend fun save(item: InventoryItem): String {
        val stored = if (item.id.isEmpty()) {
            item.copy(id = remote.newInventoryId(), createdAt = clock.millis())
        } else {
            item
        }
        remote.saveInventory(stored.toEntity())
        return stored.id
    }

    override suspend fun delete(id: String) = remote.deleteInventory(id)
}

class ShoppingRepositoryImpl(
    private val dao: ShoppingDao,
    private val remote: FirestoreDataSource,
) : ShoppingRepository {
    override fun observeItems(): Flow<List<ShoppingItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getItem(id: String): ShoppingItem? = dao.getById(id)?.toDomain()

    override suspend fun add(item: ShoppingItem): String {
        val stored = if (item.id.isEmpty()) item.copy(id = remote.newShoppingId()) else item
        remote.saveShopping(stored.toEntity())
        return stored.id
    }

    override suspend fun setPurchased(id: String, purchased: Boolean, inventoryItemId: String?, stockDelta: Double) =
        remote.setPurchased(id, purchased, inventoryItemId, stockDelta)

    override suspend fun delete(id: String) = remote.deleteShopping(listOf(id))

    override suspend fun clearPurchased() = remote.deleteShopping(dao.purchasedIds())
}
