package br.com.casaemdia.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import br.com.casaemdia.ui.login.LoginScreen
import android.content.res.Configuration
import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.model.StorageLocation
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.ui.home.HomeContent
import br.com.casaemdia.ui.home.HomeScreen
import br.com.casaemdia.ui.inventory.InventoryContent
import br.com.casaemdia.ui.inventory.InventoryScreen
import br.com.casaemdia.ui.shopping.ShoppingContent
import br.com.casaemdia.ui.shopping.ShoppingScreen
import br.com.casaemdia.ui.theme.CasaEmDiaTheme
import br.com.casaemdia.ui.viewmodel.HomeUiState
import br.com.casaemdia.ui.viewmodel.InventoryUiState
import br.com.casaemdia.ui.viewmodel.ShoppingUiState
import java.time.LocalDate

object Routes {
    const val LOGIN = "login"
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
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            TabScaffold(Tab.Home, navController, showBrandHeader = true) { modifier ->
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
            TabScaffold(Tab.Inventory, navController, titleText = "Meus itens", onAdd = addItem) { modifier ->
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
    titleText: String? = null,
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
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        Text(
                            text = titleText ?: stringResource(current.label),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
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
                FloatingActionButton(
                    onClick = onAdd,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.inventory_add_item),
                    )
                }
            }
        },
    ) { padding ->
        content(Modifier.padding(padding))
    }
}

@Preview(name = "Home Tab", showBackground = true)
@Composable
private fun TabScaffoldHomePreview() {
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
    )
    CasaEmDiaTheme {
        TabScaffold(
            current = Tab.Home,
            navController = rememberNavController(),
            showBrandHeader = true,
        ) { modifier ->
            HomeContent(
                state = HomeUiState(
                    isLoading = false,
                    today = LocalDate.now(),
                    totalItems = 12,
                    lowStock = listOf(sampleItems[0]),
                    expiring = emptyList(),
                    categoryCounts = mapOf(
                        Category.FOOD to 8,
                        Category.CLEANING to 4,
                    ),
                    recentItems = sampleItems,
                ),
                onItemClick = {},
                onNavigateToInventory = {},
                modifier = modifier,
            )
        }
    }
}

@Preview(name = "Inventory Tab - Light", showBackground = true)
@Preview(name = "Inventory Tab - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TabScaffoldInventoryPreview() {
    CasaEmDiaTheme {
        TabScaffold(
            current = Tab.Inventory,
            navController = rememberNavController(),
            titleText = "Meus itens",
            onAdd = {},
        ) { modifier ->
            InventoryContent(
                state = InventoryUiState(
                    isLoading = false,
                    items = listOf(
                        InventoryItem(
                            id = 1,
                            name = "Macarrão",
                            category = Category.FOOD,
                            quantity = 2.0,
                            unit = UnitType.PACK,
                            minQuantity = 1.0,
                        ),
                        InventoryItem(
                            id = 2,
                            name = "Arroz",
                            category = Category.FOOD,
                            quantity = 1.0,
                            unit = UnitType.KG,
                            minQuantity = 2.0,
                        ),
                    ),
                    totalCount = 2,
                ),
                onQueryChange = {},
                onCategorySelected = {},
                onLowStockToggled = {},
                onRetry = {},
                onItemClick = {},
                onAddItem = {},
                modifier = modifier,
            )
        }
    }
}

@Preview(name = "Shopping Tab", showBackground = true)
@Composable
private fun TabScaffoldShoppingPreview() {
    val sampleItem = InventoryItem(
        id = 1,
        name = "Café Torrado 500g",
        category = Category.FOOD,
        quantity = 0.0,
        unit = UnitType.PACK,
        minQuantity = 1.0,
    )
    val pendingShopping = listOf(
        ShoppingItem(id = 1, name = "Açúcar 1kg", quantity = 1.0, unit = UnitType.KG, purchased = false),
        ShoppingItem(id = 2, name = "Papel Toalha", quantity = 2.0, unit = UnitType.PACK, purchased = false),
    )
    val purchasedShopping = listOf(
        ShoppingItem(id = 3, name = "Pão de Forma", quantity = 1.0, unit = UnitType.PACK, purchased = true),
    )
    CasaEmDiaTheme {
        TabScaffold(
            current = Tab.Shopping,
            navController = rememberNavController(),
        ) { modifier ->
            ShoppingContent(
                state = ShoppingUiState(
                    isLoading = false,
                    suggestions = listOf(sampleItem),
                    pending = pendingShopping,
                    purchased = purchasedShopping,
                ),
                onAddManual = { _, _ -> },
                onAddSuggestion = {},
                onAddAllSuggestions = {},
                onTogglePurchased = {},
                onDelete = {},
                onClearPurchased = {},
                modifier = modifier,
            )
        }
    }
}
