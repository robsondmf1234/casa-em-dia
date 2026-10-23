package br.com.casaemdia.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.usecase.filterInventory
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class InventoryUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val items: List<InventoryItem> = emptyList(),
    val totalCount: Int = 0,
    val query: String = "",
    val category: Category? = null,
    val lowStockOnly: Boolean = false,
)

private data class Filters(val query: String, val category: Category?, val lowStockOnly: Boolean)

class InventoryViewModel(
    private val inventory: InventoryRepository,
    private val clock: Clock,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<Category?>(null)
    private val lowStockOnly = MutableStateFlow(false)
    private val reload = MutableStateFlow(0)

    val uiState: StateFlow<InventoryUiState> = reload
        .flatMapLatest {
            combine(
                inventory.observeItems(),
                combine(query, category, lowStockOnly, ::Filters),
            ) { items, f ->
                InventoryUiState(
                    isLoading = false,
                    today = LocalDate.now(clock),
                    items = filterInventory(items, f.query, f.category, f.lowStockOnly),
                    totalCount = items.size,
                    query = f.query,
                    category = f.category,
                    lowStockOnly = f.lowStockOnly,
                )
            }.catch { emit(InventoryUiState(isLoading = false, hasError = true)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onCategorySelected(value: Category?) {
        category.value = value
    }

    fun onLowStockToggled() {
        lowStockOnly.value = !lowStockOnly.value
    }

    fun retry() {
        reload.value++
    }
}
