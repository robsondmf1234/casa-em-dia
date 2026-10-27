package br.com.casaemdia.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import br.com.casaemdia.ui.theme.CasaEmDiaTheme
import br.com.casaemdia.ui.theme.DangerRed
import br.com.casaemdia.ui.theme.Green40
import java.time.LocalDate
import java.time.LocalTime
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onItemClick: (Long) -> Unit,
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToInventory: () -> Unit = {},
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
        else -> HomeContent(state, onItemClick, onNavigateToInventory, modifier)
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onItemClick: (Long) -> Unit,
    onNavigateToInventory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = getGreetingMessage(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.home_greeting_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TotalItemsCard(
                    totalItems = state.totalItems,
                    onClick = onNavigateToInventory,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StatSubCard(
                        count = state.lowStock.size,
                        label = stringResource(R.string.home_low_stock_short),
                        icon = Icons.Filled.Error,
                        containerColor = Color(0xFFFFF3DB),
                        contentColor = Color(0xFFE65100),
                        onClick = onNavigateToInventory,
                        modifier = Modifier.weight(1f),
                    )
                    StatSubCard(
                        count = state.expiring.size,
                        label = stringResource(R.string.home_expiring_short),
                        icon = Icons.Filled.CalendarMonth,
                        containerColor = Color(0xFFFFEAEA),
                        contentColor = DangerRed,
                        onClick = onNavigateToInventory,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.home_overview),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        if (state.lowStock.isEmpty() && state.expiring.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_no_alerts),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.expiring.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_expiring_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(state.expiring, key = { "e${it.id}" }) {
                ItemCard(it, state.today, { onItemClick(it.id) })
            }
        }
        if (state.lowStock.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_low_stock_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(state.lowStock, key = { "l${it.id}" }) {
                ItemCard(it, state.today, { onItemClick(it.id) })
            }
        }
    }
}

@Composable
private fun TotalItemsCard(
    totalItems: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F0E2)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = Green40,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = totalItems.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Green40,
                )
                Text(
                    text = stringResource(R.string.home_items_at_home),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Green40,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Green40,
            )
        }
    }
}

@Composable
private fun StatSubCard(
    count: Int,
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = contentColor,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )
        }
    }
}

private fun getGreetingMessage(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Bom dia!"
    in 12..17 -> "Boa tarde!"
    else -> "Boa noite!"
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val today = LocalDate.now()
    val sampleItems = listOf(
        InventoryItem(
            id = 1,
            name = "Leite Desnatado 1L",
            category = Category.FOOD,
            quantity = 1.0,
            unit = UnitType.L,
            minQuantity = 2.0,
            expirationDate = today.plusDays(2),
            location = StorageLocation.FRIDGE,
        ),
        InventoryItem(
            id = 2,
            name = "Detergente Líquido",
            category = Category.CLEANING,
            quantity = 0.0,
            unit = UnitType.UNIT,
            minQuantity = 1.0,
            expirationDate = today.plusDays(30),
            location = StorageLocation.KITCHEN,
        ),
    )
    CasaEmDiaTheme {
        HomeContent(
            state = HomeUiState(
                isLoading = false,
                today = today,
                totalItems = 18,
                lowStock = listOf(sampleItems[1]),
                expiring = listOf(sampleItems[0]),
            ),
            onItemClick = {},
            onNavigateToInventory = {},
            modifier = Modifier,
        )
    }
}
