package br.com.casaemdia.data.repository

import br.com.casaemdia.data.local.InventoryDao
import br.com.casaemdia.data.local.ShoppingDao
import br.com.casaemdia.data.local.toDomain
import br.com.casaemdia.data.local.toEntity
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InventoryRepositoryImpl(private val dao: InventoryDao) : InventoryRepository {
    override fun observeItems(): Flow<List<InventoryItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeItem(id: Long): Flow<InventoryItem?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getItem(id: Long): InventoryItem? = dao.getById(id)?.toDomain()

    override suspend fun save(item: InventoryItem): Long = dao.upsert(item.toEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

class ShoppingRepositoryImpl(private val dao: ShoppingDao) : ShoppingRepository {
    override fun observeItems(): Flow<List<ShoppingItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getItem(id: Long): ShoppingItem? = dao.getById(id)?.toDomain()

    override suspend fun add(item: ShoppingItem): Long = dao.upsert(item.toEntity())

    override suspend fun setPurchased(id: Long, purchased: Boolean) = dao.setPurchased(id, purchased)

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun clearPurchased() = dao.deletePurchased()
}
