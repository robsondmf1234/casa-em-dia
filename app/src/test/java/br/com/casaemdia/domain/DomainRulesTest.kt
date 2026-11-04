package br.com.casaemdia.domain

import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.domain.usecase.SetItemPurchased
import br.com.casaemdia.domain.usecase.daysUntilExpiration
import br.com.casaemdia.domain.usecase.filterInventory
import br.com.casaemdia.domain.usecase.parseQuantity
import br.com.casaemdia.domain.usecase.suggestShoppingItems
import br.com.casaemdia.domain.usecase.suggestedPurchaseQuantity
import br.com.casaemdia.testing.FakeInventoryRepository
import br.com.casaemdia.testing.FakeShoppingRepository
import br.com.casaemdia.testing.TODAY
import br.com.casaemdia.testing.inventoryItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainRulesTest {

    @Test
    fun isLowStock_trueAtMinimumAndFalseAbove() {
        assertTrue(inventoryItem(quantity = 2.0, min = 2.0).isLowStock)
        assertFalse(inventoryItem(quantity = 2.1, min = 2.0).isLowStock)
    }

    @Test
    fun suggestedPurchaseQuantity_refillsToTwiceMinimumWithAtLeastOne() {
        assertEquals(4.0, inventoryItem(quantity = 0.0, min = 2.0).suggestedPurchaseQuantity(), 0.0)
        assertEquals(3.0, inventoryItem(quantity = 1.0, min = 2.0).suggestedPurchaseQuantity(), 0.0)
        assertEquals(1.0, inventoryItem(quantity = 0.5, min = 0.5).suggestedPurchaseQuantity(), 0.0)
    }

    @Test
    fun daysUntilExpiration_nullWithoutDateAndSignedOtherwise() {
        assertNull(inventoryItem().daysUntilExpiration(TODAY))
        assertEquals(0L, inventoryItem(expires = TODAY).daysUntilExpiration(TODAY))
        assertEquals(5L, inventoryItem(expires = TODAY.plusDays(5)).daysUntilExpiration(TODAY))
        assertEquals(-3L, inventoryItem(expires = TODAY.minusDays(3)).daysUntilExpiration(TODAY))
    }

    @Test
    fun suggestions_emptyWhenNothingIsLowStock() {
        val inventory = listOf(inventoryItem(1, quantity = 10.0), inventoryItem(2, quantity = 3.0))
        assertTrue(suggestShoppingItems(inventory, emptyList()).isEmpty())
    }

    @Test
    fun filterInventory_matchesNotesAndIgnoresSurroundingSpaces() {
        val list = listOf(
            inventoryItem(1, "Arroz", notes = "integral"),
            inventoryItem(2, "Feijão"),
        )
        assertEquals(listOf(1L), filterInventory(list, "  INTEGRAL ", null, false).map { it.id })
        assertEquals(listOf(1L, 2L), filterInventory(list, "   ", null, false).map { it.id })
    }

    @Test
    fun parseQuantity_trimsAndRejectsNonFiniteValues() {
        assertEquals(3.0, parseQuantity("  3 ")!!, 0.0)
        assertEquals(0.0, parseQuantity("0")!!, 0.0)
        assertNull(parseQuantity(""))
        assertNull(parseQuantity("Infinity"))
        assertNull(parseQuantity("NaN"))
    }

    @Test
    fun setPurchased_unmarkingRemovesQuantityFromInventory() = runTest {
        val entry = ShoppingItem(id = 3, name = "Arroz", quantity = 2.0, unit = UnitType.KG, purchased = true, inventoryItemId = 7)
        val shopping = FakeShoppingRepository(listOf(entry))
        val inventory = FakeInventoryRepository(listOf(inventoryItem(7, quantity = 5.0)))

        SetItemPurchased(shopping, inventory)(entry, false)

        assertFalse(shopping.items.value.single().purchased)
        assertEquals(3.0, inventory.getItem(7)!!.quantity, 0.0)
    }

    @Test
    fun setPurchased_unmarkingNeverLeavesNegativeStock() = runTest {
        val entry = ShoppingItem(id = 3, name = "Arroz", quantity = 4.0, unit = UnitType.KG, purchased = true, inventoryItemId = 7)
        val inventory = FakeInventoryRepository(listOf(inventoryItem(7, quantity = 1.0)))

        SetItemPurchased(FakeShoppingRepository(listOf(entry)), inventory)(entry, false)

        assertEquals(0.0, inventory.getItem(7)!!.quantity, 0.0)
    }

    @Test
    fun setPurchased_linkedToDeletedItemOnlyUpdatesList() = runTest {
        val entry = ShoppingItem(id = 3, name = "Arroz", quantity = 1.0, unit = UnitType.KG, inventoryItemId = 99)
        val shopping = FakeShoppingRepository(listOf(entry))
        val inventory = FakeInventoryRepository()

        SetItemPurchased(shopping, inventory)(entry, true)

        assertTrue(shopping.items.value.single().purchased)
        assertTrue(inventory.items.value.isEmpty())
    }
}
