package br.com.casaemdia.ui.shopping

import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.domain.usecase.AddSuggestionToShoppingList
import br.com.casaemdia.domain.usecase.SetItemPurchased
import br.com.casaemdia.testing.FakeInventoryRepository
import br.com.casaemdia.testing.FakeShoppingRepository
import br.com.casaemdia.testing.MainDispatcherRule
import br.com.casaemdia.testing.inventoryItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShoppingViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val inventory = FakeInventoryRepository(
        listOf(
            inventoryItem(1, "Arroz", quantity = 0.0, min = 1.0),
            inventoryItem(2, "Feijão", quantity = 0.0, min = 1.0),
            inventoryItem(3, "Café", quantity = 10.0),
        ),
    )
    private val shopping = FakeShoppingRepository(
        listOf(
            ShoppingItem(id = 1, name = "Arroz", quantity = 2.0, unit = UnitType.KG, inventoryItemId = 1),
            ShoppingItem(id = 2, name = "Pão", quantity = 1.0, unit = UnitType.UNIT, purchased = true),
        ),
    )

    private fun TestScope.viewModel(): ShoppingViewModel {
        val vm = ShoppingViewModel(
            inventory,
            shopping,
            AddSuggestionToShoppingList(shopping),
            SetItemPurchased(shopping, inventory),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun splitsPendingAndPurchasedAndSuggestsMissingLowStock() = runTest {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(listOf(1L), state.pending.map { it.id })
        assertEquals(listOf(2L), state.purchased.map { it.id })
        assertEquals(listOf(2L), state.suggestions.map { it.id })
    }

    @Test
    fun togglingLinkedItemRestocksInventory() = runTest {
        val vm = viewModel()

        vm.togglePurchased(vm.uiState.value.pending.single())

        assertEquals(listOf(1L, 2L), vm.uiState.value.purchased.map { it.id }.sorted())
        assertEquals(2.0, inventory.getItem(1)!!.quantity, 0.0)
    }

    @Test
    fun addSuggestionMovesItFromSuggestionsToPending() = runTest {
        val vm = viewModel()

        vm.addSuggestion(vm.uiState.value.suggestions.single())

        assertTrue(vm.uiState.value.suggestions.isEmpty())
        assertEquals(listOf("Arroz", "Feijão"), vm.uiState.value.pending.map { it.name })
    }

    @Test
    fun addAllSuggestionsAddsEveryLowStockItem() = runTest {
        shopping.items.value = emptyList()
        val vm = viewModel()
        assertEquals(2, vm.uiState.value.suggestions.size)

        vm.addAllSuggestions()

        assertTrue(vm.uiState.value.suggestions.isEmpty())
        assertEquals(setOf(1L, 2L), vm.uiState.value.pending.mapNotNull { it.inventoryItemId }.toSet())
    }

    @Test
    fun addManualTrimsNameAndDefaultsInvalidQuantityToOne() = runTest {
        val vm = viewModel()

        vm.addManual("  Detergente ", "abc")

        val added = vm.uiState.value.pending.single { it.name == "Detergente" }
        assertEquals(1.0, added.quantity, 0.0)
        assertEquals(UnitType.UNIT, added.unit)
        assertEquals(null, added.inventoryItemId)
    }

    @Test
    fun addManualIgnoresBlankName() = runTest {
        val vm = viewModel()

        vm.addManual("   ", "2")

        assertEquals(2, shopping.items.value.size)
        assertFalse(vm.uiState.value.actionFailed)
    }

    @Test
    fun deleteAndClearPurchasedRemoveEntries() = runTest {
        val vm = viewModel()

        vm.clearPurchased()
        assertTrue(vm.uiState.value.purchased.isEmpty())

        vm.delete(vm.uiState.value.pending.single())
        assertTrue(vm.uiState.value.pending.isEmpty())
    }

    @Test
    fun failedActionIsReportedUntilDismissed() = runTest {
        shopping.failWrites = true
        val vm = viewModel()

        vm.clearPurchased()
        assertTrue(vm.uiState.value.actionFailed)

        vm.dismissError()
        assertFalse(vm.uiState.value.actionFailed)
    }
}
