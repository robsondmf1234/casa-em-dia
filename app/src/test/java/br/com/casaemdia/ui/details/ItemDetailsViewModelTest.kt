package br.com.casaemdia.ui.details

import br.com.casaemdia.domain.usecase.AddSuggestionToShoppingList
import br.com.casaemdia.testing.FIXED_CLOCK
import br.com.casaemdia.testing.FakeInventoryRepository
import br.com.casaemdia.testing.FakeShoppingRepository
import br.com.casaemdia.testing.MainDispatcherRule
import br.com.casaemdia.testing.inventoryItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ItemDetailsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val inventory = FakeInventoryRepository(listOf(inventoryItem(7, "Sabão", quantity = 0.0, min = 2.0)))
    private val shopping = FakeShoppingRepository()

    private fun viewModel(id: Long = 7) =
        ItemDetailsViewModel(id, inventory, AddSuggestionToShoppingList(shopping), FIXED_CLOCK)

    @Test
    fun showsTheRequestedItem() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Sabão", vm.uiState.value.item?.name)
    }

    @Test
    fun missingItemHasNoItem() = runTest {
        val vm = viewModel(id = 99)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.item)
    }

    @Test
    fun deleteRemovesItemAndFlagsDeleted() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.delete()

        assertTrue(vm.uiState.value.deleted)
        assertTrue(inventory.items.value.isEmpty())
    }

    @Test
    fun addToShoppingListCreatesLinkedEntryUntilMessageShown() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.addToShoppingList()

        assertTrue(vm.uiState.value.addedToShopping)
        val entry = shopping.items.value.single()
        assertEquals(7L, entry.inventoryItemId)
        assertEquals(4.0, entry.quantity, 0.0)

        vm.messageShown()
        assertFalse(vm.uiState.value.addedToShopping)
    }

    @Test
    fun failedActionIsReportedAndCleared() = runTest {
        inventory.failWrites = true
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.delete()

        assertTrue(vm.uiState.value.actionFailed)
        assertFalse(vm.uiState.value.deleted)
        vm.messageShown()
        assertFalse(vm.uiState.value.actionFailed)
    }
}
