package br.com.casaemdia.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.casaemdia.R
import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.StorageLocation
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.ui.components.EmptyState
import br.com.casaemdia.ui.components.ErrorState
import br.com.casaemdia.ui.components.ItemCard
import br.com.casaemdia.ui.components.LoadingState
import br.com.casaemdia.ui.components.labelRes
import br.com.casaemdia.ui.theme.CasaEmDiaTheme
import br.com.casaemdia.ui.theme.containerColor
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate

@Composable
fun InventoryScreen(
    onItemClick: (Long) -> Unit,
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InventoryContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onCategorySelected = viewModel::onCategorySelected,
        onLowStockToggled = viewModel::onLowStockToggled,
        onRetry = viewModel::retry,
        onItemClick = onItemClick,
        onAddItem = onAddItem,
        modifier = modifier,
    )
}

@Composable
fun InventoryContent(
    state: InventoryUiState,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onLowStockToggled: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (Long) -> Unit,
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> LoadingState(modifier)
        state.hasError -> ErrorState(stringResource(R.string.generic_error), onRetry, modifier)
        else -> Column(modifier) {
            OutlinedTextField(
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = containerColor,
                    unfocusedContainerColor = containerColor,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                ),
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.inventory_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.category == null && !state.lowStockOnly,
                        onClick = {
                            onCategorySelected(null)
                            if (state.lowStockOnly) onLowStockToggled()
                        },
                        label = { Text(stringResource(R.string.inventory_filter_all)) },
                    )
                }
                items(Category.entries) { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { onCategorySelected(if (state.category == category) null else category) },
                        label = { Text(stringResource(category.labelRes())) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.lowStockOnly,
                        onClick = onLowStockToggled,
                        label = { Text(stringResource(R.string.inventory_filter_low_stock)) },
                    )
                }
            }
            when {
                state.totalCount == 0 -> EmptyState(
                    title = stringResource(R.string.inventory_empty_title),
                    message = stringResource(R.string.inventory_empty_message),
                    actionLabel = stringResource(R.string.inventory_add_item),
                    onAction = onAddItem,
                )

                state.items.isEmpty() -> EmptyState(
                    title = stringResource(R.string.inventory_title),
                    message = stringResource(R.string.inventory_no_results),
                    icon = Icons.Filled.Search,
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        ItemCard(item, state.today, { onItemClick(item.id) })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryScreenPreview() {
    val today = LocalDate.now()
    val sampleItems = listOf(
        InventoryItem(
            id = 1,
            name = "Arroz Integral 5kg",
            category = Category.FOOD,
            quantity = 1.0,
            unit = UnitType.KG,
            minQuantity = 2.0,
            expirationDate = today.plusDays(10),
            location = StorageLocation.PANTRY,
        ),
        InventoryItem(
            id = 2,
            name = "Detergente Neutro",
            category = Category.CLEANING,
            quantity = 3.0,
            unit = UnitType.UNIT,
            minQuantity = 1.0,
            expirationDate = today.plusDays(30),
            location = StorageLocation.KITCHEN,
        ),
        InventoryItem(
            id = 3,
            name = "Sabonete Líquido",
            category = Category.PERSONAL_CARE,
            quantity = 0.0,
            unit = UnitType.UNIT,
            minQuantity = 1.0,
            expirationDate = today.minusDays(2),
            location = StorageLocation.BATHROOM,
        ),
    )
    CasaEmDiaTheme {
        InventoryContent(
            state = InventoryUiState(
                isLoading = false,
                today = today,
                items = sampleItems,
                totalCount = sampleItems.size,
            ),
            onQueryChange = {},
            onCategorySelected = {},
            onLowStockToggled = {},
            onRetry = {},
            onItemClick = {},
            onAddItem = {},
        )
    }
}
