package br.com.casaemdia.domain

import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.ExpirationStatus
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.repository.ShoppingRepository
import br.com.casaemdia.domain.usecase.AddSuggestionToShoppingList
import br.com.casaemdia.domain.usecase.SetItemPurchased
import br.com.casaemdia.domain.usecase.expirationStatus
import br.com.casaemdia.domain.usecase.filterInventory
import br.com.casaemdia.domain.usecase.lowStock
import br.com.casaemdia.domain.usecase.nearingExpiration
import br.com.casaemdia.domain.usecase.parseQuantity
import br.com.casaemdia.domain.usecase.suggestShoppingItems
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UseCasesTest {
    private val today = LocalDate.of(2025, 1, 10)

    private fun item(
        id: Long = 1,
        name: String = "Arroz",
        category: Category = Category.FOOD,
        quantity: Double = 5.0,
        min: Double = 1.0,
        expires: LocalDate? = null,
    ) = InventoryItem(
        id = id, name = name, category = category, quantity = quantity,
        unit = UnitType.KG, minQuantity = min, expirationDate = expires,
    )

    @Test
    fun expirationStatus_coversAllStates() {
        assertEquals(ExpirationStatus.NONE, item().expirationStatus(today))
        assertEquals(ExpirationStatus.EXPIRED, item(expires = today.minusDays(1)).expirationStatus(today))
        assertEquals(ExpirationStatus.EXPIRING_SOON, item(expires = today).expirationStatus(today))
        assertEquals(ExpirationStatus.EXPIRING_SOON, item(expires = today.plusDays(7)).expirationStatus(today))
        assertEquals(ExpirationStatus.OK, item(expires = today.plusDays(8)).expirationStatus(today))
    }

    @Test
    fun nearingExpiration_sortedBySoonest() {
        val list = listOf(
            item(1, expires = today.plusDays(3)),
            item(2, expires = today.plusDays(30)),
            item(3, expires = today.minusDays(2)),
            item(4),
        )
        assertEquals(listOf(3L, 1L), list.nearingExpiration(today).map { it.id })
    }

    @Test
    fun lowStock_includesItemsAtOrBelowMinimum() {
        val list = listOf(item(1, quantity = 1.0, min = 1.0), item(2, quantity = 0.0), item(3, quantity = 4.0))
        assertEquals(listOf(1L, 2L), list.lowStock().map { it.id })
    }

    @Test
    fun suggestions_skipItemsAlreadyPending() {
        val inventory = listOf(item(1, quantity = 0.0), item(2, quantity = 0.0), item(3, quantity = 0.0))
        val shopping = listOf(
            ShoppingItem(id = 1, name = "a", quantity = 1.0, unit = UnitType.KG, inventoryItemId = 1),
            ShoppingItem(id = 2, name = "b", quantity = 1.0, unit = UnitType.KG, purchased = true, inventoryItemId = 2),
        )
        assertEquals(listOf(2L, 3L), suggestShoppingItems(inventory, shopping).map { it.id })
    }

    @Test
    fun filterInventory_combinesQueryCategoryAndLowStock() {
        val list = listOf(
            item(1, "Arroz branco", Category.FOOD, quantity = 0.0),
            item(2, "Feijão", Category.FOOD, quantity = 10.0),
            item(3, "Detergente", Category.CLEANING, quantity = 0.0),
        )
        assertEquals(listOf(1L), filterInventory(list, "arroz", null, false).map { it.id })
        assertEquals(listOf(1L, 2L), filterInventory(list, "", Category.FOOD, false).map { it.id })
        assertEquals(listOf(1L, 3L), filterInventory(list, "", null, true).map { it.id })
    }

    @Test
    fun parseQuantity_acceptsCommaAndRejectsInvalid() {
        assertEquals(2.5, parseQuantity("2,5")!!, 0.0)
        assertNull(parseQuantity("abc"))
        assertNull(parseQuantity("-1"))
    }

    @Test
    fun addSuggestion_createsLinkedShoppingItem() = runTest {
        val shopping = mockk<ShoppingRepository>()
        val captured = slot<ShoppingItem>()
        coEvery { shopping.add(capture(captured)) } returns 1L

        AddSuggestionToShoppingList(shopping)(item(7, "Sabão", quantity = 0.0, min = 2.0))

        assertEquals(7L, captured.captured.inventoryItemId)
        assertEquals(4.0, captured.captured.quantity, 0.0)
    }

    @Test
    fun setPurchased_restocksLinkedInventoryItem() = runTest {
        val shopping = mockk<ShoppingRepository>(relaxed = true)
        val inventory = mockk<InventoryRepository>()
        val saved = slot<InventoryItem>()
        coEvery { inventory.getItem(7) } returns item(7, quantity = 1.0)
        coEvery { inventory.save(capture(saved)) } returns 7L
        val entry = ShoppingItem(id = 3, name = "Arroz", quantity = 4.0, unit = UnitType.KG, inventoryItemId = 7)

        SetItemPurchased(shopping, inventory)(entry, true)

        coVerify { shopping.setPurchased(3, true) }
        assertEquals(5.0, saved.captured.quantity, 0.0)
    }

    @Test
    fun setPurchased_withoutLinkOnlyUpdatesList() = runTest {
        val shopping = mockk<ShoppingRepository>(relaxed = true)
        val inventory = mockk<InventoryRepository>()
        val entry = ShoppingItem(id = 3, name = "Café", quantity = 1.0, unit = UnitType.PACK)

        SetItemPurchased(shopping, inventory)(entry, true)

        coVerify { shopping.setPurchased(3, true) }
        coVerify(exactly = 0) { inventory.save(any()) }
    }
}
