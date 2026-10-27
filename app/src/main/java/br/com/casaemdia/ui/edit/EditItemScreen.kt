package br.com.casaemdia.ui.edit

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.casaemdia.R
import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.StorageLocation
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.ui.components.ErrorState
import br.com.casaemdia.ui.components.LoadingState
import br.com.casaemdia.ui.components.OptionSelector
import br.com.casaemdia.ui.components.formatDate
import br.com.casaemdia.ui.components.labelRes
import br.com.casaemdia.ui.theme.CasaEmDiaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    itemId: Long,
    onBack: () -> Unit,
    viewModel: EditItemViewModel = koinViewModel(parameters = { parametersOf(itemId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.item_saved)

    LaunchedEffect(state.saved) {
        if (state.saved) {
            Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isEditing) R.string.edit_title_edit else R.string.edit_title_new))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.notFound -> ErrorState(stringResource(R.string.edit_not_found), onBack, Modifier.padding(padding))
            else -> EditForm(
                state = state,
                today = viewModel.today,
                onNameChange = viewModel::onNameChange,
                onCategoryChange = viewModel::onCategoryChange,
                onQuantityChange = viewModel::onQuantityChange,
                onUnitChange = viewModel::onUnitChange,
                onMinQuantityChange = viewModel::onMinQuantityChange,
                onLocationChange = viewModel::onLocationChange,
                onExpirationChange = viewModel::onExpirationChange,
                onNotesChange = viewModel::onNotesChange,
                onSave = viewModel::save,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditForm(
    state: EditItemUiState,
    today: LocalDate,
    onNameChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (UnitType) -> Unit,
    onMinQuantityChange: (String) -> Unit,
    onLocationChange: (StorageLocation) -> Unit,
    onExpirationChange: (LocalDate?) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    val numberError = stringResource(R.string.error_number_invalid)

    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.field_name)) },
            isError = state.nameError,
            supportingText = { if (state.nameError) Text(stringResource(R.string.error_name_required)) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth(),
        )
        OptionSelector(
            label = stringResource(R.string.field_category),
            options = Category.entries,
            selected = state.category,
            optionLabel = { stringResource(it.labelRes()) },
            onSelected = onCategoryChange,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.quantity,
                onValueChange = onQuantityChange,
                label = { Text(stringResource(R.string.field_quantity)) },
                isError = state.quantityError,
                supportingText = { if (state.quantityError) Text(numberError) },
                keyboardOptions = numberKeyboard,
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f),
            )
            OptionSelector(
                label = stringResource(R.string.field_unit),
                options = UnitType.entries,
                selected = state.unit,
                optionLabel = { stringResource(it.labelRes()) },
                onSelected = onUnitChange,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = state.minQuantity,
            onValueChange = onMinQuantityChange,
            label = { Text(stringResource(R.string.field_min_quantity)) },
            isError = state.minQuantityError,
            supportingText = { if (state.minQuantityError) Text(numberError) },
            keyboardOptions = numberKeyboard,
            singleLine = true,
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth(),
        )
        OptionSelector(
            label = stringResource(R.string.field_location),
            options = StorageLocation.entries,
            selected = state.location,
            optionLabel = { stringResource(it.labelRes()) },
            onSelected = onLocationChange,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Text(
                    "  " + (state.expirationDate?.let(::formatDate) ?: stringResource(R.string.expiration_none)),
                )
            }
            if (state.expirationDate != null) {
                TextButton(onClick = { onExpirationChange(null) }) {
                    Text(stringResource(R.string.expiration_remove))
                }
            }
        }
        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.field_notes)) },
            minLines = 2,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.saveFailed) {
            Text(stringResource(R.string.edit_save_failed), color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (state.expirationDate ?: today)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onExpirationChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditItemScreenPreview() {
    val today = LocalDate.now()
    CasaEmDiaTheme {
        EditForm(
            state = EditItemUiState(
                isLoading = false,
                name = "Detergente de Maçã",
                category = Category.CLEANING,
                quantity = "2",
                unit = UnitType.UNIT,
                minQuantity = "1",
                location = StorageLocation.KITCHEN,
                expirationDate = today.plusMonths(6),
                notes = "Comprar na promoção se possível.",
            ),
            today = today,
            onNameChange = {},
            onCategoryChange = {},
            onQuantityChange = {},
            onUnitChange = {},
            onMinQuantityChange = {},
            onLocationChange = {},
            onExpirationChange = {},
            onNotesChange = {},
            onSave = {},
        )
    }
}
