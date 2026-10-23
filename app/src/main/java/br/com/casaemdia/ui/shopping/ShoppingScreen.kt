package br.com.casaemdia.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.casaemdia.R
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.ui.components.EmptyState
import br.com.casaemdia.ui.components.ErrorState
import br.com.casaemdia.ui.components.LoadingState
import br.com.casaemdia.ui.components.formatQuantity
import br.com.casaemdia.ui.components.labelRes
import org.koin.androidx.compose.koinViewModel

@Composable
fun ShoppingScreen(
    modifier: Modifier = Modifier,
    viewModel: ShoppingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        state.isLoading -> LoadingState(modifier)
        state.hasError -> ErrorState(stringResource(R.string.generic_error), viewModel::retry, modifier)
        else -> ShoppingContent(state, viewModel, modifier)
    }
}

@Composable
private fun ShoppingContent(state: ShoppingUiState, vm: ShoppingViewModel, modifier: Modifier) {
    val isEmpty = state.pending.isEmpty() && state.purchased.isEmpty() && state.suggestions.isEmpty()
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { AddManualRow(onAdd = vm::addManual) }
        if (state.actionFailed) {
            item {
                Text(
                    stringResource(R.string.action_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (isEmpty) {
            item {
                EmptyState(
                    title = stringResource(R.string.shopping_empty_title),
                    message = stringResource(R.string.shopping_empty_message),
                    icon = Icons.Filled.ShoppingCart,
                    modifier = Modifier.padding(top = 32.dp),
                    fillSize = false,
                )
            }
        }
        if (state.suggestions.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.shopping_suggestions),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = vm::addAllSuggestions) { Text(stringResource(R.string.shopping_add_all)) }
                }
            }
            items(state.suggestions, key = { "s${it.id}" }) { SuggestionRow(it) { vm.addSuggestion(it) } }
        }
        if (state.pending.isNotEmpty()) {
            item { Text(stringResource(R.string.shopping_to_buy), style = MaterialTheme.typography.titleLarge) }
            items(state.pending, key = { "p${it.id}" }) { ShoppingRow(it, vm) }
        }
        if (state.purchased.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.shopping_purchased),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = vm::clearPurchased) {
                        Text(stringResource(R.string.shopping_clear_purchased))
                    }
                }
            }
            items(state.purchased, key = { "b${it.id}" }) { ShoppingRow(it, vm) }
        }
    }
}

@Composable
private fun AddManualRow(onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text(stringResource(R.string.shopping_add_placeholder)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                onAdd(name, "1")
                name = ""
            },
            enabled = name.isNotBlank(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add))
        }
    }
}

@Composable
private fun SuggestionRow(item: InventoryItem, onAdd: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${item.name} (${formatQuantity(item.quantity)} ${stringResource(item.unit.labelRes())})",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add))
            }
        }
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, vm: ShoppingViewModel) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.purchased, onCheckedChange = { vm.togglePurchased(item) })
            Text(
                "${item.name} • ${formatQuantity(item.quantity)} ${stringResource(item.unit.labelRes())}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.purchased) TextDecoration.LineThrough else null,
            )
            IconButton(onClick = { vm.delete(item) }) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}
