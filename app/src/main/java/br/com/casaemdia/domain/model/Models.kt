package br.com.casaemdia.domain.model

import java.time.LocalDate

enum class Category { FOOD, CLEANING, PERSONAL_CARE, OTHER }

enum class UnitType { UNIT, KG, G, L, ML, PACK }

enum class StorageLocation { PANTRY, FRIDGE, FREEZER, KITCHEN, BATHROOM, LAUNDRY, OTHER }

enum class ExpirationStatus { NONE, OK, EXPIRING_SOON, EXPIRED }

/** Id vazio = item ainda não salvo. Os ids são os ids dos documentos no Firestore. */
data class InventoryItem(
    val id: String = "",
    val name: String,
    val category: Category,
    val quantity: Double,
    val unit: UnitType,
    val minQuantity: Double = 1.0,
    val expirationDate: LocalDate? = null,
    val location: StorageLocation = StorageLocation.PANTRY,
    val notes: String = "",
    /** Momento da criação (epoch millis), usado para listar os itens mais recentes. */
    val createdAt: Long = 0,
) {
    val isLowStock: Boolean get() = quantity <= minQuantity
}

data class ShoppingItem(
    val id: String = "",
    val name: String,
    val quantity: Double,
    val unit: UnitType,
    val purchased: Boolean = false,
    val inventoryItemId: String? = null,
)
