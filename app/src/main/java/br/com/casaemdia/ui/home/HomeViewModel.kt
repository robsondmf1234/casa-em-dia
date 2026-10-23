package br.com.casaemdia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.usecase.lowStock
import br.com.casaemdia.domain.usecase.nearingExpiration
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val totalItems: Int = 0,
    val lowStock: List<InventoryItem> = emptyList(),
    val expiring: List<InventoryItem> = emptyList(),
)

class HomeViewModel(
    private val inventory: InventoryRepository,
    private val clock: Clock,
) : ViewModel() {

    private val reload = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = reload
        .flatMapLatest {
            inventory.observeItems()
                .map { items ->
                    val today = LocalDate.now(clock)
                    HomeUiState(
                        isLoading = false,
                        today = today,
                        totalItems = items.size,
                        lowStock = items.lowStock(),
                        expiring = items.nearingExpiration(today),
                    )
                }
                .catch { emit(HomeUiState(isLoading = false, hasError = true)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun retry() {
        reload.value++
    }
}
