package br.com.casaemdia.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.casaemdia.R
import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.ui.components.EmptyState
import br.com.casaemdia.ui.components.ErrorState
import br.com.casaemdia.ui.components.ItemCard
import br.com.casaemdia.ui.components.LoadingState
import br.com.casaemdia.ui.components.labelRes
import org.koin.androidx.compose.koinViewModel

@Composable
fun InventoryScreen(
    onItemClick: (Long) -> Unit,
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        state.isLoading -> LoadingState(modifier)
        state.hasError -> ErrorState(stringResource(R.string.generic_error), viewModel::retry, modifier)
        else -> Column(modifier) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.inventory_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.category == null && !state.lowStockOnly,
                        onClick = {
                            viewModel.onCategorySelected(null)
                            if (state.lowStockOnly) viewModel.onLowStockToggled()
                        },
                        label = { Text(stringResource(R.string.inventory_filter_all)) },
                    )
                }
                items(Category.entries) { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { viewModel.onCategorySelected(if (state.category == category) null else category) },
                        label = { Text(stringResource(category.labelRes())) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.lowStockOnly,
                        onClick = viewModel::onLowStockToggled,
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
