package br.com.casaemdia.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.casaemdia.R
import br.com.casaemdia.ui.components.EmptyState
import br.com.casaemdia.ui.components.ErrorState
import br.com.casaemdia.ui.components.ItemCard
import br.com.casaemdia.ui.components.LoadingState
import br.com.casaemdia.ui.theme.DangerRed
import br.com.casaemdia.ui.theme.WarningAmber
import androidx.compose.ui.graphics.Color
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onItemClick: (Long) -> Unit,
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        state.isLoading -> LoadingState(modifier)
        state.hasError -> ErrorState(stringResource(R.string.generic_error), viewModel::retry, modifier)
        state.totalItems == 0 -> EmptyState(
            title = stringResource(R.string.home_empty_title),
            message = stringResource(R.string.home_empty_message),
            modifier = modifier,
            actionLabel = stringResource(R.string.home_add_first_item),
            onAction = onAddItem,
        )
        else -> HomeContent(state, onItemClick, modifier)
    }
}

@Composable
private fun HomeContent(state: HomeUiState, onItemClick: (Long) -> Unit, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stringResource(R.string.home_total_items), state.totalItems, MaterialTheme.colorScheme.primary)
                StatCard(stringResource(R.string.home_low_stock_count), state.lowStock.size, WarningAmber)
                StatCard(stringResource(R.string.home_expiring_count), state.expiring.size, DangerRed)
            }
        }
        if (state.lowStock.isEmpty() && state.expiring.isEmpty()) {
            item {
                Text(stringResource(R.string.home_no_alerts), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (state.expiring.isNotEmpty()) {
            item { Text(stringResource(R.string.home_expiring_title), style = MaterialTheme.typography.titleLarge) }
            items(state.expiring, key = { "e${it.id}" }) { ItemCard(it, state.today, { onItemClick(it.id) }) }
        }
        if (state.lowStock.isNotEmpty()) {
            item { Text(stringResource(R.string.home_low_stock_title), style = MaterialTheme.typography.titleLarge) }
            items(state.lowStock, key = { "l${it.id}" }) { ItemCard(it, state.today, { onItemClick(it.id) }) }
        }
    }
}

@Composable
private fun RowScope.StatCard(label: String, value: Int, accent: Color) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = accent)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
