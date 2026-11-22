package br.com.casaemdia.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.casaemdia.BuildConfig
import br.com.casaemdia.R
import br.com.casaemdia.domain.model.AuthState
import br.com.casaemdia.domain.model.ThemeMode
import br.com.casaemdia.ui.theme.CasaEmDiaTheme
import br.com.casaemdia.ui.viewmodel.AuthViewModel
import br.com.casaemdia.ui.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

/** Ícone de engrenagem da barra superior, ligado ao [SettingsViewModel] e ao [AuthViewModel]. */
@Composable
fun SettingsMenuButton(
    viewModel: SettingsViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    SettingsMenu(
        themeMode = state.themeMode,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        userEmail = (authState as? AuthState.Authenticated)?.user?.email,
        onSignOut = authViewModel::signOut,
    )
}

private enum class SettingsDialog { NONE, THEME, ABOUT, SIGN_OUT }

@Composable
fun SettingsMenu(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    userEmail: String? = null,
    onSignOut: () -> Unit = {},
    initiallyExpanded: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    var dialog by rememberSaveable { mutableStateOf(SettingsDialog.NONE) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.nav_settings),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_theme)) },
                leadingIcon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                trailingIcon = {
                    Text(
                        text = stringResource(themeMode.labelRes()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = {
                    expanded = false
                    dialog = SettingsDialog.THEME
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_about)) },
                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                onClick = {
                    expanded = false
                    dialog = SettingsDialog.ABOUT
                },
            )
            if (userEmail != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(stringResource(R.string.sign_out))
                            Text(
                                text = stringResource(R.string.signed_in_as, userEmail),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                    onClick = {
                        expanded = false
                        dialog = SettingsDialog.SIGN_OUT
                    },
                )
            }
        }
    }

    when (dialog) {
        SettingsDialog.THEME -> ThemeDialog(
            selected = themeMode,
            onSelect = {
                onThemeModeSelected(it)
                dialog = SettingsDialog.NONE
            },
            onDismiss = { dialog = SettingsDialog.NONE },
        )
        SettingsDialog.ABOUT -> AboutDialog(onDismiss = { dialog = SettingsDialog.NONE })
        SettingsDialog.SIGN_OUT -> SignOutDialog(
            onConfirm = {
                dialog = SettingsDialog.NONE
                onSignOut()
            },
            onDismiss = { dialog = SettingsDialog.NONE },
        )
        SettingsDialog.NONE -> Unit
    }
}

@Composable
private fun ThemeDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                THEME_OPTIONS.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = mode == selected,
                                onClick = { onSelect(mode) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == selected, onClick = null)
                        Text(
                            text = stringResource(mode.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close)) }
        },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column {
                Text(stringResource(R.string.settings_about_description))
                Text(
                    text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close)) }
        },
    )
}

@Composable
private fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_sign_out_title)) },
        text = { Text(stringResource(R.string.settings_sign_out_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.sign_out)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) }
        },
    )
}

private val THEME_OPTIONS = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
    ThemeMode.SYSTEM -> R.string.settings_theme_system
}

@Preview(name = "Menu - Claro", showBackground = true, heightDp = 280)
@Preview(name = "Menu - Escuro", showBackground = true, heightDp = 280, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsMenuPreview() {
    CasaEmDiaTheme {
        SettingsMenu(
            themeMode = ThemeMode.SYSTEM,
            onThemeModeSelected = {},
            userEmail = "voce@exemplo.com",
            initiallyExpanded = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemeDialogPreview() {
    CasaEmDiaTheme {
        ThemeDialog(selected = ThemeMode.DARK, onSelect = {}, onDismiss = {})
    }
}
