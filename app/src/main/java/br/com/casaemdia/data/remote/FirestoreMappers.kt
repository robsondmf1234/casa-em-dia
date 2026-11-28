package br.com.casaemdia.data.remote

import br.com.casaemdia.data.local.InventoryItemEntity
import br.com.casaemdia.data.local.ShoppingItemEntity

/**
 * Conversão entre os documentos do Firestore (mapas de campos) e as entidades do Room.
 * Enums e datas seguem o mesmo formato do Room (nome do enum e epoch day). Números chegam do
 * Firestore como Long ou Double, então são lidos como [Number].
 */
internal object Fields {
    const val NAME = "name"
    const val CATEGORY = "category"
    const val QUANTITY = "quantity"
    const val UNIT = "unit"
    const val MIN_QUANTITY = "minQuantity"
    const val EXPIRATION_EPOCH_DAY = "expirationEpochDay"
    const val LOCATION = "location"
    const val NOTES = "notes"
    const val CREATED_AT = "createdAt"
    const val PURCHASED = "purchased"
    const val INVENTORY_ITEM_ID = "inventoryItemId"
}

internal fun InventoryItemEntity.toFirestore(): Map<String, Any?> = mapOf(
    Fields.NAME to name,
    Fields.CATEGORY to category,
    Fields.QUANTITY to quantity,
    Fields.UNIT to unit,
    Fields.MIN_QUANTITY to minQuantity,
    Fields.EXPIRATION_EPOCH_DAY to expirationEpochDay,
    Fields.LOCATION to location,
    Fields.NOTES to notes,
    Fields.CREATED_AT to createdAt,
)

internal fun inventoryEntityFrom(id: String, data: Map<String, Any?>) = InventoryItemEntity(
    id = id,
    name = data.string(Fields.NAME),
    category = data.string(Fields.CATEGORY),
    quantity = data.double(Fields.QUANTITY),
    unit = data.string(Fields.UNIT),
    minQuantity = data.double(Fields.MIN_QUANTITY),
    expirationEpochDay = (data[Fields.EXPIRATION_EPOCH_DAY] as? Number)?.toLong(),
    location = data.string(Fields.LOCATION),
    notes = data.string(Fields.NOTES),
    createdAt = (data[Fields.CREATED_AT] as? Number)?.toLong() ?: 0L,
)

internal fun ShoppingItemEntity.toFirestore(): Map<String, Any?> = mapOf(
    Fields.NAME to name,
    Fields.QUANTITY to quantity,
    Fields.UNIT to unit,
    Fields.PURCHASED to purchased,
    Fields.INVENTORY_ITEM_ID to inventoryItemId,
)

internal fun shoppingEntityFrom(id: String, data: Map<String, Any?>) = ShoppingItemEntity(
    id = id,
    name = data.string(Fields.NAME),
    quantity = data.double(Fields.QUANTITY),
    unit = data.string(Fields.UNIT),
    purchased = data[Fields.PURCHASED] as? Boolean ?: false,
    inventoryItemId = data[Fields.INVENTORY_ITEM_ID] as? String,
)

private fun Map<String, Any?>.string(key: String) = this[key] as? String ?: ""
private fun Map<String, Any?>.double(key: String) = (this[key] as? Number)?.toDouble() ?: 0.0
