package br.com.casaemdia.ui.home

import br.com.casaemdia.testing.FIXED_CLOCK
import br.com.casaemdia.testing.FakeInventoryRepository
import br.com.casaemdia.testing.MainDispatcherRule
import br.com.casaemdia.testing.TODAY
import br.com.casaemdia.testing.inventoryItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun summarizesTotalLowStockAndExpiring() = runTest {
        val repo = FakeInventoryRepository(
            listOf(
                inventoryItem(1, quantity = 0.0),
                inventoryItem(2, quantity = 10.0, expires = TODAY.plusDays(2)),
                inventoryItem(3, quantity = 10.0, expires = TODAY.plusDays(60)),
            ),
        )
        val vm = HomeViewModel(repo, FIXED_CLOCK)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(TODAY, state.today)
        assertEquals(3, state.totalItems)
        assertEquals(listOf(1L), state.lowStock.map { it.id })
        assertEquals(listOf(2L), state.expiring.map { it.id })
    }

    @Test
    fun updatesWhenInventoryChanges() = runTest {
        val repo = FakeInventoryRepository()
        val vm = HomeViewModel(repo, FIXED_CLOCK)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(0, vm.uiState.value.totalItems)

        repo.save(inventoryItem(id = 0, quantity = 0.0))

        assertEquals(1, vm.uiState.value.totalItems)
        assertEquals(1, vm.uiState.value.lowStock.size)
    }

    @Test
    fun showsErrorAndRecoversOnRetry() = runTest {
        val repo = FakeInventoryRepository(listOf(inventoryItem(1))).apply { failObserve = true }
        val vm = HomeViewModel(repo, FIXED_CLOCK)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertTrue(vm.uiState.value.hasError)

        repo.failObserve = false
        vm.retry()

        assertFalse(vm.uiState.value.hasError)
        assertEquals(1, vm.uiState.value.totalItems)
    }
}
