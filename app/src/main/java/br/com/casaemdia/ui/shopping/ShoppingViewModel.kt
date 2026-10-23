package br.com.casaemdia.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.repository.ShoppingRepository
import br.com.casaemdia.domain.usecase.AddSuggestionToShoppingList
import br.com.casaemdia.domain.usecase.SetItemPurchased
import br.com.casaemdia.domain.usecase.parseQuantity
import br.com.casaemdia.domain.usecase.suggestShoppingItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val pending: List<ShoppingItem> = emptyList(),
    val purchased: List<ShoppingItem> = emptyList(),
    val suggestions: List<InventoryItem> = emptyList(),
    val actionFailed: Boolean = false,
)

class ShoppingViewModel(
    inventory: InventoryRepository,
    private val shopping: ShoppingRepository,
    private val addSuggestionToList: AddSuggestionToShoppingList,
    private val setPurchased: SetItemPurchased,
) : ViewModel() {

    private val failed = MutableStateFlow(false)
    private val reload = MutableStateFlow(0)

    val uiState: StateFlow<ShoppingUiState> = reload
        .flatMapLatest {
            combine(inventory.observeItems(), shopping.observeItems(), failed) { stock, list, failure ->
                ShoppingUiState(
                    isLoading = false,
                    pending = list.filter { !it.purchased },
                    purchased = list.filter { it.purchased },
                    suggestions = suggestShoppingItems(stock, list),
                    actionFailed = failure,
                )
            }.catch { emit(ShoppingUiState(isLoading = false, hasError = true)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            failed.value = runCatching { block() }.isFailure
        }
    }

    fun togglePurchased(item: ShoppingItem) = launchAction { setPurchased(item, !item.purchased) }

    fun addSuggestion(item: InventoryItem) = launchAction { addSuggestionToList(item) }

    fun addAllSuggestions() = launchAction { uiState.value.suggestions.forEach { addSuggestionToList(it) } }

    fun addManual(name: String, quantityText: String) = launchAction {
        val quantity = parseQuantity(quantityText) ?: 1.0
        if (name.isNotBlank()) {
            shopping.add(ShoppingItem(name = name.trim(), quantity = quantity, unit = UnitType.UNIT))
        }
    }

    fun delete(item: ShoppingItem) = launchAction { shopping.delete(item.id) }

    fun clearPurchased() = launchAction { shopping.clearPurchased() }

    fun dismissError() {
        failed.update { false }
    }

    fun retry() {
        reload.value++
    }
}
