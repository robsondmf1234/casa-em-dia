package br.com.casaemdia.data.remote

import br.com.casaemdia.data.local.InventoryItemEntity
import br.com.casaemdia.data.local.ShoppingItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class FirestoreMappersTest {

    private val inventory = InventoryItemEntity(
        id = "abc", name = "Leite", category = "FOOD", quantity = 2.5, unit = "L",
        minQuantity = 1.0, expirationEpochDay = 20_100L, location = "FRIDGE", notes = "integral",
        createdAt = 1_700_000_000_000L,
    )

    @Test
    fun inventoryRoundTripKeepsAllFields() {
        assertEquals(inventory, inventoryEntityFrom("abc", inventory.toFirestore()))
    }

    @Test
    fun inventoryReadsWholeNumbersStoredAsLong() {
        // O Firestore devolve 3.0 como Long 3 depois de um FieldValue.increment inteiro.
        val data = inventory.toFirestore() + mapOf(Fields.QUANTITY to 3L, Fields.MIN_QUANTITY to 1L)

        val entity = inventoryEntityFrom("abc", data)

        assertEquals(3.0, entity.quantity, 0.0)
        assertEquals(1.0, entity.minQuantity, 0.0)
    }

    @Test
    fun inventoryMissingFieldsUseSafeDefaults() {
        val entity = inventoryEntityFrom("x", emptyMap())

        assertEquals("x", entity.id)
        assertEquals("", entity.name)
        assertEquals(0.0, entity.quantity, 0.0)
        assertNull(entity.expirationEpochDay)
        assertEquals(0L, entity.createdAt)
    }

    @Test
    fun shoppingRoundTripKeepsAllFields() {
        val shopping = ShoppingItemEntity(
            id = "s1", name = "Arroz", quantity = 2.0, unit = "KG", purchased = true, inventoryItemId = "abc",
        )

        assertEquals(shopping, shoppingEntityFrom("s1", shopping.toFirestore()))
    }

    @Test
    fun shoppingWithoutLinkOrFlagDefaultsToPendingAndUnlinked() {
        val entity = shoppingEntityFrom("s2", mapOf(Fields.NAME to "Pão", Fields.QUANTITY to 1L))

        assertFalse(entity.purchased)
        assertNull(entity.inventoryItemId)
        assertEquals(1.0, entity.quantity, 0.0)
    }
}
