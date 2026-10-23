package br.com.casaemdia.domain.usecase

import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.InventoryItem

fun filterInventory(
    items: List<InventoryItem>,
    query: String,
    category: Category?,
    lowStockOnly: Boolean,
): List<InventoryItem> {
    val q = query.trim()
    return items.filter { item ->
        (q.isEmpty() || item.name.contains(q, ignoreCase = true) || item.notes.contains(q, ignoreCase = true)) &&
            (category == null || item.category == category) &&
            (!lowStockOnly || item.isLowStock)
    }
}

/** Parses a user-entered non-negative number, accepting comma as decimal separator. */
fun parseQuantity(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && it.isFinite() }
