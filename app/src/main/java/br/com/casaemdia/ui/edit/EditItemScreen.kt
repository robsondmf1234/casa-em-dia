package br.com.casaemdia.ui.edit

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.casaemdia.R
import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.StorageLocation
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.ui.components.ErrorState
import br.com.casaemdia.ui.components.LoadingState
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
                    Text(
                        text = stringResource(
                            if (state.isEditing) R.string.edit_title_edit else R.string.edit_title_new,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
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
    onLocationChange: (StorageLocation) -> Unit,
    onExpirationChange: (LocalDate?) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    val numberError = stringResource(R.string.error_number_invalid)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        errorContainerColor = Color.White,
    )

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EditField(label = stringResource(R.string.edit_product_name)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    colors = fieldColors,
                    placeholder = { Text(stringResource(R.string.edit_product_name_hint)) },
                    isError = state.nameError,
                    supportingText = { if (state.nameError) Text(stringResource(R.string.error_name_required)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            EditField(label = stringResource(R.string.edit_category_required)) {
                EditOptionSelector(
                    label = stringResource(R.string.edit_category_hint),
                    options = Category.entries,
                    selected = state.category,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelected = onCategoryChange,
                    leadingIcon = { Icon(Icons.Filled.Restaurant, contentDescription = null) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditField(
                    label = stringResource(R.string.edit_quantity_required),
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = state.quantity,
                        onValueChange = onQuantityChange,
                        colors = fieldColors,
                        placeholder = { Text(stringResource(R.string.edit_quantity_hint)) },
                        isError = state.quantityError,
                        supportingText = { if (state.quantityError) Text(numberError) },
                        keyboardOptions = numberKeyboard,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                EditField(
                    label = stringResource(R.string.edit_unit_required),
                    modifier = Modifier.weight(1f),
                ) {
                    EditOptionSelector(
                        label = stringResource(R.string.edit_unit_hint),
                        options = UnitType.entries,
                        selected = state.unit,
                        optionLabel = { stringResource(it.labelRes()) },
                        onSelected = onUnitChange,
                    )
                }
            }
            EditField(label = stringResource(R.string.edit_expiration_optional)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                        Text(
                            text = state.expirationDate?.let(::formatDate)
                                ?: stringResource(R.string.edit_expiration_hint),
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                            textAlign = TextAlign.Start,
                            color = if (state.expirationDate == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    if (state.expirationDate != null) {
                        IconButton(onClick = { onExpirationChange(null) }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.expiration_remove))
                        }
                    }
                }
            }
            EditField(label = stringResource(R.string.edit_location_optional)) {
                EditOptionSelector(
                    label = stringResource(R.string.edit_location_hint),
                    options = StorageLocation.entries,
                    selected = state.location,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelected = onLocationChange,
                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                )
            }
            EditField(label = stringResource(R.string.edit_notes_optional)) {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    colors = fieldColors,
                    placeholder = { Text(stringResource(R.string.edit_notes_hint)) },
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.saveFailed) {
                Text(stringResource(R.string.edit_save_failed), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(stringResource(R.string.edit_save_item))
            }
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

@Composable
private fun EditField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun <T> EditOptionSelector(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                errorContainerColor = Color.White,
            ),
            placeholder = { Text(label) },
            leadingIcon = leadingIcon,
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = true },
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
            onLocationChange = {},
            onExpirationChange = {},
            onNotesChange = {},
            onSave = {},
        )
    }
}
