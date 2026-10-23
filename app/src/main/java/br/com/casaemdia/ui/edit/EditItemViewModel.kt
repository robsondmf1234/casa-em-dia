package br.com.casaemdia.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.StorageLocation
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.usecase.parseQuantity
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditItemUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val notFound: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: Boolean = false,
    val name: String = "",
    val category: Category = Category.FOOD,
    val quantity: String = "1",
    val unit: UnitType = UnitType.UNIT,
    val minQuantity: String = "1",
    val expirationDate: LocalDate? = null,
    val location: StorageLocation = StorageLocation.PANTRY,
    val notes: String = "",
    val nameError: Boolean = false,
    val quantityError: Boolean = false,
    val minQuantityError: Boolean = false,
)

class EditItemViewModel(
    private val itemId: Long?,
    private val inventory: InventoryRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditItemUiState(isLoading = itemId != null, isEditing = itemId != null))
    val uiState: StateFlow<EditItemUiState> = _uiState.asStateFlow()

    val today: LocalDate get() = LocalDate.now(clock)

    init {
        if (itemId != null) load(itemId)
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            val item = runCatching { inventory.getItem(id) }.getOrNull()
            _uiState.update {
                if (item == null) {
                    it.copy(isLoading = false, notFound = true)
                } else {
                    it.copy(
                        isLoading = false,
                        name = item.name,
                        category = item.category,
                        quantity = formatNumber(item.quantity),
                        unit = item.unit,
                        minQuantity = formatNumber(item.minQuantity),
                        expirationDate = item.expirationDate,
                        location = item.location,
                        notes = item.notes,
                    )
                }
            }
        }
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = false) }
    fun onCategoryChange(v: Category) = _uiState.update { it.copy(category = v) }
    fun onQuantityChange(v: String) = _uiState.update { it.copy(quantity = v, quantityError = false) }
    fun onUnitChange(v: UnitType) = _uiState.update { it.copy(unit = v) }
    fun onMinQuantityChange(v: String) = _uiState.update { it.copy(minQuantity = v, minQuantityError = false) }
    fun onExpirationChange(v: LocalDate?) = _uiState.update { it.copy(expirationDate = v) }
    fun onLocationChange(v: StorageLocation) = _uiState.update { it.copy(location = v) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }

    fun save() {
        val s = _uiState.value
        if (s.isSaving) return
        val quantity = parseQuantity(s.quantity)
        val min = parseQuantity(s.minQuantity)
        val nameInvalid = s.name.isBlank()
        if (nameInvalid || quantity == null || min == null) {
            _uiState.update {
                it.copy(nameError = nameInvalid, quantityError = quantity == null, minQuantityError = min == null)
            }
            return
        }
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val result = runCatching {
                inventory.save(
                    InventoryItem(
                        id = itemId ?: 0,
                        name = s.name.trim(),
                        category = s.category,
                        quantity = quantity,
                        unit = s.unit,
                        minQuantity = min,
                        expirationDate = s.expirationDate,
                        location = s.location,
                        notes = s.notes.trim(),
                    ),
                )
            }
            _uiState.update { it.copy(isSaving = false, saved = result.isSuccess, saveFailed = result.isFailure) }
        }
    }
}
