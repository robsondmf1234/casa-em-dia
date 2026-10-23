package br.com.casaemdia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.model.StorageLocation
import br.com.casaemdia.domain.model.UnitType
import java.time.LocalDate

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val minQuantity: Double,
    val expirationEpochDay: Long?,
    val location: String,
    val notes: String,
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val purchased: Boolean,
    val inventoryItemId: Long?,
)

private inline fun <reified T : Enum<T>> String.toEnumOr(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default

fun InventoryItemEntity.toDomain() = InventoryItem(
    id = id,
    name = name,
    category = category.toEnumOr(Category.OTHER),
    quantity = quantity,
    unit = unit.toEnumOr(UnitType.UNIT),
    minQuantity = minQuantity,
    expirationDate = expirationEpochDay?.let(LocalDate::ofEpochDay),
    location = location.toEnumOr(StorageLocation.OTHER),
    notes = notes,
)

fun InventoryItem.toEntity() = InventoryItemEntity(
    id = id,
    name = name,
    category = category.name,
    quantity = quantity,
    unit = unit.name,
    minQuantity = minQuantity,
    expirationEpochDay = expirationDate?.toEpochDay(),
    location = location.name,
    notes = notes,
)

fun ShoppingItemEntity.toDomain() = ShoppingItem(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit.toEnumOr(UnitType.UNIT),
    purchased = purchased,
    inventoryItemId = inventoryItemId,
)

fun ShoppingItem.toEntity() = ShoppingItemEntity(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit.name,
    purchased = purchased,
    inventoryItemId = inventoryItemId,
)
