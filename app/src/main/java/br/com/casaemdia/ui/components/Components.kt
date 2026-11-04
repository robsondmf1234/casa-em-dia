package br.com.casaemdia.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.casaemdia.R
import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.ExpirationStatus
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.StorageLocation
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.domain.usecase.daysUntilExpiration
import br.com.casaemdia.domain.usecase.expirationStatus
import br.com.casaemdia.ui.theme.CasaEmDiaTheme
import br.com.casaemdia.ui.theme.DangerRed
import br.com.casaemdia.ui.theme.Green40
import br.com.casaemdia.ui.theme.WarningAmber
import java.time.LocalDate

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Inventory2,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    fillSize: Boolean = true,
) {
    Column(
        (if (fillSize) modifier.fillMaxSize() else modifier.fillMaxWidth()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
fun statusColor(status: ExpirationStatus): Color = when (status) {
    ExpirationStatus.EXPIRED -> DangerRed
    ExpirationStatus.EXPIRING_SOON -> WarningAmber
    ExpirationStatus.OK -> Green40
    ExpirationStatus.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun expirationText(item: InventoryItem, today: LocalDate): String? {
    val days = item.daysUntilExpiration(today) ?: return null
    return when {
        days < -1 -> stringResource(R.string.expired_days_ago, -days.toInt())
        days == -1L -> stringResource(R.string.expired_yesterday)
        days == 0L -> stringResource(R.string.expires_today)
        days == 1L -> stringResource(R.string.expires_tomorrow)
        else -> stringResource(R.string.expires_in_days, days.toInt())
    }
}

@Composable
fun ExpirationChip(item: InventoryItem, today: LocalDate, modifier: Modifier = Modifier) {
    val status = item.expirationStatus(today)
    val text = expirationText(item, today) ?: return
    StatusChip(text, statusColor(status), modifier)
}

@Composable
fun ItemCard(item: InventoryItem, today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${stringResource(item.category.labelRes())} • ${stringResource(item.location.labelRes())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.isLowStock) StatusChip(stringResource(R.string.status_low_stock), WarningAmber)
                    ExpirationChip(item, today)
                }
            }
            Text(
                "${formatQuantity(item.quantity)} ${stringResource(item.unit.labelRes())}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun <T> OptionSelector(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            shape = RoundedCornerShape(10),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(10))
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingStatePreview() {
    CasaEmDiaTheme {
        Box(modifier = Modifier.size(200.dp)) {
            LoadingState()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    CasaEmDiaTheme {
        EmptyState(
            title = "Nenhum item encontrado",
            message = "Sua despensa está vazia. Adicione itens para começar.",
            actionLabel = "Adicionar Item",
            onAction = {},
            fillSize = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    CasaEmDiaTheme {
        ErrorState(
            message = "Ocorreu um erro ao carregar os dados.",
            onRetry = {},
            modifier = Modifier.height(200.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusChipPreview() {
    CasaEmDiaTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusChip(text = "Estoque baixo", color = WarningAmber)
            StatusChip(text = "Vencido", color = DangerRed)
            StatusChip(text = "No prazo", color = Green40)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpirationChipPreview() {
    val today = LocalDate.now()
    val item = InventoryItem(
        id = 1,
        name = "Leite",
        category = Category.FOOD,
        quantity = 2.0,
        unit = UnitType.L,
        expirationDate = today.plusDays(3),
        location = StorageLocation.FRIDGE,
    )
    CasaEmDiaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ExpirationChip(item = item, today = today)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemCardPreview() {
    val today = LocalDate.now()
    val item = InventoryItem(
        id = 1,
        name = "Arroz Integral",
        category = Category.FOOD,
        quantity = 1.0,
        unit = UnitType.KG,
        minQuantity = 2.0,
        expirationDate = today.plusDays(5),
        location = StorageLocation.PANTRY,
    )
    CasaEmDiaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ItemCard(item = item, today = today, onClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OptionSelectorPreview() {
    val options = listOf("Despensa", "Geladeira", "Freezer")
    CasaEmDiaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            OptionSelector(
                label = "Local de Armazenamento",
                options = options,
                selected = options[0],
                optionLabel = { it },
                onSelected = {},
            )
        }
    }
}
