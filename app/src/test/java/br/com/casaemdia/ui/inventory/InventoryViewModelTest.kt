package br.com.casaemdia.ui.inventory

import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.testing.FIXED_CLOCK
import br.com.casaemdia.testing.FakeInventoryRepository
import br.com.casaemdia.testing.MainDispatcherRule
import br.com.casaemdia.testing.inventoryItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InventoryViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repo = FakeInventoryRepository(
        listOf(
            inventoryItem(1, "Arroz", Category.FOOD, quantity = 0.0),
            inventoryItem(2, "Feijão", Category.FOOD, quantity = 10.0),
            inventoryItem(3, "Detergente", Category.CLEANING, quantity = 0.0),
        ),
    )

    @Test
    fun showsAllItemsWithoutFilters() = runTest {
        val vm = InventoryViewModel(repo, FIXED_CLOCK)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf(1L, 2L, 3L), vm.uiState.value.items.map { it.id })
        assertEquals(3, vm.uiState.value.totalCount)
    }

    @Test
    fun appliesQueryCategoryAndLowStockFilters() = runTest {
        val vm = InventoryViewModel(repo, FIXED_CLOCK)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onCategorySelected(Category.FOOD)
        assertEquals(listOf(1L, 2L), vm.uiState.value.items.map { it.id })

        vm.onLowStockToggled()
        assertTrue(vm.uiState.value.lowStockOnly)
        assertEquals(listOf(1L), vm.uiState.value.items.map { it.id })

        vm.onCategorySelected(null)
        vm.onQueryChange("deter")
        assertEquals("deter", vm.uiState.value.query)
        assertEquals(listOf(3L), vm.uiState.value.items.map { it.id })
        assertEquals(3, vm.uiState.value.totalCount)
    }

    @Test
    fun togglingLowStockTwiceTurnsItOff() = runTest {
        val vm = InventoryViewModel(repo, FIXED_CLOCK)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onLowStockToggled()
        vm.onLowStockToggled()

        assertFalse(vm.uiState.value.lowStockOnly)
        assertEquals(3, vm.uiState.value.items.size)
    }

    @Test
    fun showsErrorAndRecoversOnRetry() = runTest {
        repo.failObserve = true
        val vm = InventoryViewModel(repo, FIXED_CLOCK)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertTrue(vm.uiState.value.hasError)

        repo.failObserve = false
        vm.retry()

        assertFalse(vm.uiState.value.hasError)
        assertEquals(3, vm.uiState.value.items.size)
    }
}
