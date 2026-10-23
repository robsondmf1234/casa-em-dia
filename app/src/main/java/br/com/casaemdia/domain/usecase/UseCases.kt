package br.com.casaemdia.domain.usecase

import br.com.casaemdia.domain.model.ExpirationStatus
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.repository.ShoppingRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val EXPIRING_SOON_DAYS = 7L

fun InventoryItem.expirationStatus(today: LocalDate): ExpirationStatus {
    val date = expirationDate ?: return ExpirationStatus.NONE
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0 -> ExpirationStatus.EXPIRED
        days <= EXPIRING_SOON_DAYS -> ExpirationStatus.EXPIRING_SOON
        else -> ExpirationStatus.OK
    }
}

fun InventoryItem.daysUntilExpiration(today: LocalDate): Long? =
    expirationDate?.let { ChronoUnit.DAYS.between(today, it) }

/** Items that are expired or about to expire, soonest first. */
fun List<InventoryItem>.nearingExpiration(today: LocalDate): List<InventoryItem> =
    filter {
        val s = it.expirationStatus(today)
        s == ExpirationStatus.EXPIRED || s == ExpirationStatus.EXPIRING_SOON
    }.sortedBy { it.expirationDate }

fun List<InventoryItem>.lowStock(): List<InventoryItem> = filter { it.isLowStock }

/** Low-stock products that are not yet pending on the shopping list. */
fun suggestShoppingItems(
    inventory: List<InventoryItem>,
    shopping: List<ShoppingItem>,
): List<InventoryItem> {
    val pending = shopping.filter { !it.purchased }.mapNotNull { it.inventoryItemId }.toSet()
    return inventory.lowStock().filter { it.id !in pending }
}

/** Quantity worth buying to bring a product back above its minimum level. */
fun InventoryItem.suggestedPurchaseQuantity(): Double =
    maxOf(1.0, (minQuantity * 2) - quantity)

class AddSuggestionToShoppingList(private val shopping: ShoppingRepository) {
    suspend operator fun invoke(item: InventoryItem): Long =
        shopping.add(
            ShoppingItem(
                name = item.name,
                quantity = item.suggestedPurchaseQuantity(),
                unit = item.unit,
                inventoryItemId = item.id,
            ),
        )
}

/** Marks an item as purchased and, when linked to the inventory, restocks it. */
class SetItemPurchased(
    private val shopping: ShoppingRepository,
    private val inventory: InventoryRepository,
) {
    suspend operator fun invoke(item: ShoppingItem, purchased: Boolean) {
        shopping.setPurchased(item.id, purchased)
        val linked = item.inventoryItemId?.let { inventory.getItem(it) } ?: return
        val delta = if (purchased) item.quantity else -item.quantity
        inventory.save(linked.copy(quantity = maxOf(0.0, linked.quantity + delta)))
    }
}
