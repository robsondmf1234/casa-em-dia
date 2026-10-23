package br.com.casaemdia.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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

private enum class Tab(val route: String, val label: Int, val icon: ImageVector) {
    Home(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    Inventory(Routes.INVENTORY, R.string.nav_inventory, Icons.Filled.Inventory2),
    Shopping(Routes.SHOPPING, R.string.nav_shopping, Icons.Filled.ShoppingCart),
}

@Composable
fun CasaEmDiaNavHost(navController: NavHostController = rememberNavController()) {
    val openDetails: (Long) -> Unit = { navController.navigate(Routes.details(it)) }
    val addItem: () -> Unit = { navController.navigate(Routes.edit(Routes.NEW_ITEM_ID)) }
    val idArg = listOf(navArgument(Routes.ITEM_ID) { type = NavType.LongType })

    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            TabScaffold(Tab.Home, navController, showTitle = false, onAdd = addItem) { modifier ->
                HomeScreen(onItemClick = openDetails, onAddItem = addItem, modifier = modifier)
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
    showTitle: Boolean = true,
    onAdd: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            if (showTitle) TopAppBar(title = { Text(stringResource(current.label)) })
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == current,
                        onClick = {
                            if (tab != current) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
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
