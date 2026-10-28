package br.com.casaemdia.data.local

import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.model.StorageLocation
import br.com.casaemdia.domain.model.UnitType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntityMappersTest {

    @Test
    fun inventoryItem_roundTripsThroughEntity() {
        val item = InventoryItem(
            id = 4,
            name = "Leite",
            category = Category.FOOD,
            quantity = 2.5,
            unit = UnitType.L,
            minQuantity = 1.0,
            expirationDate = LocalDate.of(2025, 3, 1),
            location = StorageLocation.FRIDGE,
            notes = "integral",
        )
        assertEquals(item, item.toEntity().toDomain())
    }

    @Test
    fun inventoryItem_withoutExpirationStoresNull() {
        val entity = InventoryItem(name = "Sabão", category = Category.CLEANING, quantity = 1.0, unit = UnitType.UNIT).toEntity()
        assertNull(entity.expirationEpochDay)
        assertNull(entity.toDomain().expirationDate)
    }

    @Test
    fun inventoryEntity_unknownEnumValuesFallBackToDefaults() {
        val entity = InventoryItemEntity(
            id = 1, name = "X", category = "PETS", quantity = 1.0, unit = "BOX",
            minQuantity = 1.0, expirationEpochDay = null, location = "GARAGE", notes = "",
        )
        val item = entity.toDomain()
        assertEquals(Category.OTHER, item.category)
        assertEquals(UnitType.UNIT, item.unit)
        assertEquals(StorageLocation.OTHER, item.location)
    }

    @Test
    fun shoppingItem_roundTripsThroughEntity() {
        val item = ShoppingItem(id = 2, name = "Café", quantity = 3.0, unit = UnitType.PACK, purchased = true, inventoryItemId = 9)
        assertEquals(item, item.toEntity().toDomain())
    }

    @Test
    fun shoppingEntity_unknownUnitFallsBackToUnit() {
        val entity = ShoppingItemEntity(id = 1, name = "X", quantity = 1.0, unit = "DOZEN", purchased = false, inventoryItemId = null)
        assertEquals(UnitType.UNIT, entity.toDomain().unit)
    }
}
