package br.com.casaemdia.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.casaemdia.R
import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.ExpirationStatus
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.StorageLocation
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.domain.usecase.expirationStatus
import br.com.casaemdia.ui.components.EmptyState
import br.com.casaemdia.ui.components.ExpirationChip
import br.com.casaemdia.ui.components.ErrorState
import br.com.casaemdia.ui.components.LoadingState
import br.com.casaemdia.ui.components.StatusChip
import br.com.casaemdia.ui.components.labelRes
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.home_overview),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val mainCategories = listOf(Category.FOOD, Category.CLEANING, Category.PERSONAL_CARE)
                    mainCategories.forEach { category ->
                        val count = state.categoryCounts[category] ?: 0
                        CategoryCard(
                            category = category,
                            count = count,
                            onClick = onNavigateToInventory,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.home_recent_items_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onNavigateToInventory) {
                    Text(
                        text = stringResource(R.string.home_see_all),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (state.recentItems.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_no_alerts),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.recentItems, key = { it.id }) { item ->
                RecentItemCard(item = item, today = state.today, onClick = { onItemClick(item.id) })
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

@Composable
private fun CategoryCard(
    category: Category,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(
                painter = painterResource(categoryDrawableRes(category)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(72.dp),
            )
            Text(
                text = stringResource(category.labelRes()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "$count itens",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecentItemCard(
    item: InventoryItem,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(categoryDrawableRes(item.category)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(item.location.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val expirationStatus = item.expirationStatus(today)
            if (item.isLowStock) {
                StatusChip(stringResource(R.string.status_low_stock), Color(0xFFE65100))
            } else if (expirationStatus == ExpirationStatus.EXPIRED || expirationStatus == ExpirationStatus.EXPIRING_SOON) {
                ExpirationChip(item, today)
            } else {
                StatusChip(stringResource(R.string.status_in_order), Green40)
            }
        }
    }
}

@DrawableRes
private fun categoryDrawableRes(category: Category): Int = when (category) {
    Category.FOOD -> R.drawable.image2
    Category.CLEANING -> R.drawable.image3
    Category.PERSONAL_CARE -> R.drawable.image1
    Category.OTHER -> R.drawable.image1
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
            name = "Detergente",
            category = Category.CLEANING,
            quantity = 2.0,
            unit = UnitType.UNIT,
            minQuantity = 1.0,
            location = StorageLocation.KITCHEN,
        ),
        InventoryItem(
            id = 2,
            name = "Macarrão",
            category = Category.FOOD,
            quantity = 3.0,
            unit = UnitType.PACK,
            minQuantity = 1.0,
            location = StorageLocation.PANTRY,
        ),
        InventoryItem(
            id = 3,
            name = "Arroz",
            category = Category.FOOD,
            quantity = 5.0,
            unit = UnitType.KG,
            minQuantity = 2.0,
            location = StorageLocation.PANTRY,
        ),
    )
    CasaEmDiaTheme {
        HomeContent(
            state = HomeUiState(
                isLoading = false,
                today = today,
                totalItems = 18,
                lowStock = listOf(sampleItems[0]),
                expiring = emptyList(),
                categoryCounts = mapOf(
                    Category.FOOD to 9,
                    Category.CLEANING to 5,
                    Category.PERSONAL_CARE to 4,
                ),
                recentItems = sampleItems,
            ),
            onItemClick = {},
            onNavigateToInventory = {},
            modifier = Modifier,
        )
    }
}
