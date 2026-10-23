package br.com.casaemdia.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.usecase.AddSuggestionToShoppingList
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemDetailsUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val item: InventoryItem? = null,
    val today: LocalDate = LocalDate.now(),
    val deleted: Boolean = false,
    val addedToShopping: Boolean = false,
    val actionFailed: Boolean = false,
)

private data class Actions(val deleted: Boolean, val addedToShopping: Boolean, val failed: Boolean)

class ItemDetailsViewModel(
    private val itemId: Long,
    private val inventory: InventoryRepository,
    private val addToShopping: AddSuggestionToShoppingList,
    private val clock: Clock,
) : ViewModel() {

    private val actions = MutableStateFlow(Actions(deleted = false, addedToShopping = false, failed = false))

    val uiState: StateFlow<ItemDetailsUiState> = combine(inventory.observeItem(itemId), actions) { item, a ->
        ItemDetailsUiState(
            isLoading = false,
            item = item,
            today = LocalDate.now(clock),
            deleted = a.deleted,
            addedToShopping = a.addedToShopping,
            actionFailed = a.failed,
        )
    }
        .catch { emit(ItemDetailsUiState(isLoading = false, hasError = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemDetailsUiState())

    fun delete() {
        viewModelScope.launch {
            runCatching { inventory.delete(itemId) }
                .onSuccess { actions.update { it.copy(deleted = true, failed = false) } }
                .onFailure { actions.update { it.copy(failed = true) } }
        }
    }

    fun addToShoppingList() {
        val item = uiState.value.item ?: return
        viewModelScope.launch {
            runCatching { addToShopping(item) }
                .onSuccess { actions.update { it.copy(addedToShopping = true, failed = false) } }
                .onFailure { actions.update { it.copy(failed = true) } }
        }
    }

    fun messageShown() = actions.update { it.copy(addedToShopping = false, failed = false) }
}
