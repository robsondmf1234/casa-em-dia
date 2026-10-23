package br.com.casaemdia.ui.components

import androidx.annotation.StringRes
import br.com.casaemdia.R
import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.StorageLocation
import br.com.casaemdia.domain.model.UnitType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@StringRes
fun Category.labelRes(): Int = when (this) {
    Category.FOOD -> R.string.category_food
    Category.CLEANING -> R.string.category_cleaning
    Category.PERSONAL_CARE -> R.string.category_personal_care
    Category.OTHER -> R.string.category_other
}

@StringRes
fun UnitType.labelRes(): Int = when (this) {
    UnitType.UNIT -> R.string.unit_unit
    UnitType.KG -> R.string.unit_kg
    UnitType.G -> R.string.unit_g
    UnitType.L -> R.string.unit_l
    UnitType.ML -> R.string.unit_ml
    UnitType.PACK -> R.string.unit_pack
}

@StringRes
fun StorageLocation.labelRes(): Int = when (this) {
    StorageLocation.PANTRY -> R.string.location_pantry
    StorageLocation.FRIDGE -> R.string.location_fridge
    StorageLocation.FREEZER -> R.string.location_freezer
    StorageLocation.KITCHEN -> R.string.location_kitchen
    StorageLocation.BATHROOM -> R.string.location_bathroom
    StorageLocation.LAUNDRY -> R.string.location_laundry
    StorageLocation.OTHER -> R.string.location_other
}

fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale("pt", "BR"), "%.2f", value).trimEnd('0').trimEnd(',')

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun formatDate(date: LocalDate): String = date.format(dateFormatter)
