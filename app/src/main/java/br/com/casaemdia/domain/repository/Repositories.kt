package br.com.casaemdia.domain.repository

import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun observeItems(): Flow<List<InventoryItem>>
    fun observeItem(id: Long): Flow<InventoryItem?>
    suspend fun getItem(id: Long): InventoryItem?
    suspend fun save(item: InventoryItem): Long
    suspend fun delete(id: Long)
}

interface ShoppingRepository {
    fun observeItems(): Flow<List<ShoppingItem>>
    suspend fun getItem(id: Long): ShoppingItem?
    suspend fun add(item: ShoppingItem): Long
    suspend fun setPurchased(id: Long, purchased: Boolean)
    suspend fun delete(id: Long)
    suspend fun clearPurchased()
}
