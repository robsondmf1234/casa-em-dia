package br.com.casaemdia.domain.model

import java.time.LocalDate

enum class Category { FOOD, CLEANING, PERSONAL_CARE, OTHER }

enum class UnitType { UNIT, KG, G, L, ML, PACK }

enum class StorageLocation { PANTRY, FRIDGE, FREEZER, KITCHEN, BATHROOM, LAUNDRY, OTHER }

enum class ExpirationStatus { NONE, OK, EXPIRING_SOON, EXPIRED }

data class InventoryItem(
    val id: Long = 0,
    val name: String,
    val category: Category,
    val quantity: Double,
    val unit: UnitType,
    val minQuantity: Double = 1.0,
    val expirationDate: LocalDate? = null,
    val location: StorageLocation = StorageLocation.PANTRY,
    val notes: String = "",
) {
    val isLowStock: Boolean get() = quantity <= minQuantity
}

data class ShoppingItem(
    val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: UnitType,
    val purchased: Boolean = false,
    val inventoryItemId: Long? = null,
)
