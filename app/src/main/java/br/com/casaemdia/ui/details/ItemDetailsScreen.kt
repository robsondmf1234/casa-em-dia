package br.com.casaemdia.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import br.com.casaemdia.ui.components.ExpirationChip
import br.com.casaemdia.ui.components.LoadingState
import br.com.casaemdia.ui.components.StatusChip
import br.com.casaemdia.ui.components.formatDate
import br.com.casaemdia.ui.components.formatQuantity
import br.com.casaemdia.ui.components.labelRes
import br.com.casaemdia.ui.theme.CasaEmDiaTheme
import br.com.casaemdia.ui.theme.WarningAmber
import java.time.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(
    itemId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ItemDetailsViewModel = koinViewModel(parameters = { parametersOf(itemId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }
    val addedMessage = stringResource(R.string.details_added_to_shopping)
    val failedMessage = stringResource(R.string.action_failed)

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }
    LaunchedEffect(state.addedToShopping, state.actionFailed) {
        when {
            state.addedToShopping -> snackbar.showSnackbar(addedMessage)
            state.actionFailed -> snackbar.showSnackbar(failedMessage)
            else -> return@LaunchedEffect
        }
        viewModel.messageShown()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.item != null) {
                        IconButton(onClick = { onEdit(itemId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        val item = state.item
        when {
            state.isLoading -> LoadingState(modifier)
            state.hasError -> ErrorState(stringResource(R.string.generic_error), onBack, modifier)
            item == null -> EmptyState(
                title = stringResource(R.string.details_title),
                message = stringResource(R.string.details_not_found),
                modifier = modifier,
            )
            else -> DetailsContent(item, state.today, viewModel::addToShoppingList, modifier)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.details_delete_title)) },
            text = { Text(stringResource(R.string.details_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun DetailsContent(item: InventoryItem, today: LocalDate, onAddToShopping: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${formatQuantity(item.quantity)} ${stringResource(item.unit.labelRes())}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.isLowStock) StatusChip(stringResource(R.string.status_low_stock), WarningAmber)
                    ExpirationChip(item, today)
                }
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow(stringResource(R.string.field_category), stringResource(item.category.labelRes()))
                DetailRow(stringResource(R.string.field_location), stringResource(item.location.labelRes()))
                DetailRow(
                    stringResource(R.string.field_min_quantity),
                    "${formatQuantity(item.minQuantity)} ${stringResource(item.unit.labelRes())}",
                )
                DetailRow(
                    stringResource(R.string.field_expiration),
                    item.expirationDate?.let(::formatDate) ?: stringResource(R.string.expiration_none),
                )
                if (item.notes.isNotBlank()) DetailRow(stringResource(R.string.field_notes), item.notes)
            }
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onAddToShopping, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.details_add_to_shopping))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemDetailsScreenPreview() {
    val today = LocalDate.now()
    val sampleItem = InventoryItem(
        id = 1,
        name = "Azeite de Oliva Extra Virgem 500ml",
        category = Category.FOOD,
        quantity = 1.0,
        unit = UnitType.UNIT,
        minQuantity = 2.0,
        expirationDate = today.plusDays(15),
        location = StorageLocation.PANTRY,
        notes = "Preferencialmente da marca tradicional.",
    )
    CasaEmDiaTheme {
        DetailsContent(
            item = sampleItem,
            today = today,
            onAddToShopping = {},
            modifier = Modifier,
        )
    }
}
