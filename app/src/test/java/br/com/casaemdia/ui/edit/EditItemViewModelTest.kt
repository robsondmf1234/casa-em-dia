package br.com.casaemdia.ui.edit

import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.StorageLocation
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.testing.FIXED_CLOCK
import br.com.casaemdia.testing.FakeInventoryRepository
import br.com.casaemdia.testing.MainDispatcherRule
import br.com.casaemdia.testing.TODAY
import br.com.casaemdia.testing.inventoryItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditItemViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun newItemStartsWithDefaults() {
        val vm = EditItemViewModel(null, FakeInventoryRepository(), FIXED_CLOCK)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEditing)
        assertEquals("", state.name)
        assertEquals("1", state.quantity)
        assertEquals(TODAY, vm.today)
    }

    @Test
    fun loadsExistingItemFormattingNumbers() {
        val repo = FakeInventoryRepository(
            listOf(inventoryItem(5, "Leite", quantity = 2.0, unit = UnitType.L, min = 1.5, expires = TODAY, notes = "x")),
        )
        val vm = EditItemViewModel(5, repo, FIXED_CLOCK)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEditing)
        assertEquals("Leite", state.name)
        assertEquals("2", state.quantity)
        assertEquals("1.5", state.minQuantity)
        assertEquals(UnitType.L, state.unit)
        assertEquals(TODAY, state.expirationDate)
        assertEquals("x", state.notes)
    }

    @Test
    fun missingItemIsReportedAsNotFound() {
        val vm = EditItemViewModel(42, FakeInventoryRepository(), FIXED_CLOCK)

        assertTrue(vm.uiState.value.notFound)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun invalidFieldsShowErrorsAndAreNotSaved() {
        val repo = FakeInventoryRepository()
        val vm = EditItemViewModel(null, repo, FIXED_CLOCK)

        vm.onNameChange("   ")
        vm.onQuantityChange("abc")
        vm.onMinQuantityChange("-1")
        vm.save()

        val state = vm.uiState.value
        assertTrue(state.nameError)
        assertTrue(state.quantityError)
        assertTrue(state.minQuantityError)
        assertFalse(state.saved)
        assertTrue(repo.items.value.isEmpty())
    }

    @Test
    fun editingAFieldClearsItsError() {
        val vm = EditItemViewModel(null, FakeInventoryRepository(), FIXED_CLOCK)
        vm.save()
        assertTrue(vm.uiState.value.nameError)

        vm.onNameChange("Arroz")

        assertFalse(vm.uiState.value.nameError)
    }

    @Test
    fun savesNewItemWithTrimmedTextAndCommaDecimals() {
        val repo = FakeInventoryRepository()
        val vm = EditItemViewModel(null, repo, FIXED_CLOCK)

        vm.onNameChange("  Sabão em pó ")
        vm.onCategoryChange(Category.CLEANING)
        vm.onQuantityChange("2,5")
        vm.onUnitChange(UnitType.KG)
        vm.onMinQuantityChange("1")
        vm.onLocationChange(StorageLocation.LAUNDRY)
        vm.onNotesChange(" marca X ")
        vm.save()

        assertTrue(vm.uiState.value.saved)
        val saved = repo.items.value.single()
        assertEquals("Sabão em pó", saved.name)
        assertEquals(Category.CLEANING, saved.category)
        assertEquals(2.5, saved.quantity, 0.0)
        assertEquals(StorageLocation.LAUNDRY, saved.location)
        assertEquals("marca X", saved.notes)
    }

    @Test
    fun savingExistingItemUpdatesItInPlace() {
        val repo = FakeInventoryRepository(listOf(inventoryItem(5, "Arroz", quantity = 1.0)))
        val vm = EditItemViewModel(5, repo, FIXED_CLOCK)

        vm.onQuantityChange("8")
        vm.save()

        val saved = repo.items.value.single()
        assertEquals(5L, saved.id)
        assertEquals(8.0, saved.quantity, 0.0)
    }

    @Test
    fun saveFailureIsReported() {
        val repo = FakeInventoryRepository().apply { failWrites = true }
        val vm = EditItemViewModel(null, repo, FIXED_CLOCK)

        vm.onNameChange("Arroz")
        vm.save()

        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.saved)
        assertFalse(vm.uiState.value.isSaving)
    }
}
