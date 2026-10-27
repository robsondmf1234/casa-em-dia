package br.com.casaemdia.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.casaemdia.R
import br.com.casaemdia.ui.details.ItemDetailsScreen
import br.com.casaemdia.ui.edit.EditItemScreen
import br.com.casaemdia.ui.home.HomeScreen
import br.com.casaemdia.ui.inventory.InventoryScreen
import br.com.casaemdia.ui.shopping.ShoppingScreen

object Routes {
    const val HOME = "home"
    const val INVENTORY = "inventory"
    const val SHOPPING = "shopping"
    const val ITEM_ID = "itemId"
    const val NEW_ITEM_ID = -1L
    const val DETAILS = "details/{$ITEM_ID}"
    const val EDIT = "edit/{$ITEM_ID}"

    fun details(id: Long) = "details/$id"
    fun edit(id: Long) = "edit/$id"
}

private enum class Tab(
    val route: String,
    val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(
        route = Routes.HOME,
        label = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    Inventory(
        route = Routes.INVENTORY,
        label = R.string.nav_inventory,
        selectedIcon = Icons.Filled.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2,
    ),
    Shopping(
        route = Routes.SHOPPING,
        label = R.string.nav_shopping,
        selectedIcon = Icons.Filled.ShoppingCart,
        unselectedIcon = Icons.Outlined.ShoppingCart,
    ),
}

@Composable
fun CasaEmDiaNavHost(navController: NavHostController = rememberNavController()) {
    val openDetails: (Long) -> Unit = { navController.navigate(Routes.details(it)) }
    val addItem: () -> Unit = { navController.navigate(Routes.edit(Routes.NEW_ITEM_ID)) }
    val idArg = listOf(navArgument(Routes.ITEM_ID) { type = NavType.LongType })

    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            TabScaffold(Tab.Home, navController, showBrandHeader = true, onAdd = addItem) { modifier ->
                HomeScreen(
                    onItemClick = openDetails,
                    onAddItem = addItem,
                    onNavigateToInventory = {
                        navController.navigate(Routes.INVENTORY) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = modifier,
                )
            }
        }
        composable(Routes.INVENTORY) {
            TabScaffold(Tab.Inventory, navController, onAdd = addItem) { modifier ->
                InventoryScreen(onItemClick = openDetails, onAddItem = addItem, modifier = modifier)
            }
        }
        composable(Routes.SHOPPING) {
            TabScaffold(Tab.Shopping, navController) { modifier -> ShoppingScreen(modifier = modifier) }
        }
        composable(Routes.DETAILS, arguments = idArg) { entry ->
            val id = entry.arguments?.getLong(Routes.ITEM_ID) ?: Routes.NEW_ITEM_ID
            ItemDetailsScreen(
                itemId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.edit(it)) },
            )
        }
        composable(Routes.EDIT, arguments = idArg) { entry ->
            val id = entry.arguments?.getLong(Routes.ITEM_ID) ?: Routes.NEW_ITEM_ID
            EditItemScreen(itemId = id, onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabScaffold(
    current: Tab,
    navController: NavHostController,
    showBrandHeader: Boolean = false,
    onAdd: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showBrandHeader) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(36.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.home_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        Text(stringResource(current.label))
                    }
                },
                actions = {
                    if (showBrandHeader) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Tab.entries.forEach { tab ->
                    val selected = tab == current
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (tab != current) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(tab.label),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (onAdd != null) {
                FloatingActionButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.inventory_add_item))
                }
            }
        },
    ) { padding ->
        content(Modifier.padding(padding))
    }
}
