package br.com.casaemdia.domain.repository

import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

/**
 * Leituras vêm da cópia local (funcionam offline). Gravações vão para a nuvem e voltam para a
 * cópia local pela sincronização; não esperam a confirmação do servidor, então funcionam offline.
 */
interface InventoryRepository {
    fun observeItems(): Flow<List<InventoryItem>>
    fun observeItem(id: String): Flow<InventoryItem?>
    suspend fun getItem(id: String): InventoryItem?

    /** Cria (id vazio) ou atualiza o item. Devolve o id. */
    suspend fun save(item: InventoryItem): String
    suspend fun delete(id: String)
}

interface ShoppingRepository {
    fun observeItems(): Flow<List<ShoppingItem>>
    suspend fun getItem(id: String): ShoppingItem?

    /** Cria o item na lista. Devolve o id. */
    suspend fun add(item: ShoppingItem): String

    /**
     * Marca/desmarca a compra e, numa única gravação atômica, soma [stockDelta] à quantidade do
     * item de estoque [inventoryItemId] (quando houver). Funciona offline.
     */
    suspend fun setPurchased(id: String, purchased: Boolean, inventoryItemId: String?, stockDelta: Double)
    suspend fun delete(id: String)
    suspend fun clearPurchased()
}
