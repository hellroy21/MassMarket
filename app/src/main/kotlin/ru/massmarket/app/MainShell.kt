package ru.massmarket.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun MainShell(
    api: ApiClient,
    token: String,
    initialUser: User,
    themeMode: ThemeMode,
    serverUrl: String,
    onUserChanged: (User) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onServerChanged: (String) -> Unit,
    onLogout: () -> Unit
) {
    var section by remember { mutableStateOf(MainSection.CATALOG) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val cart = remember { mutableStateMapOf<Long, Int>() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun refreshOrders() {
        scope.launch {
            runCatching { api.orders(token) }
                .onSuccess { orders = it }
                .onFailure { snackbar.showSnackbar(it.message ?: "Не удалось загрузить заказы") }
        }
    }

    LaunchedEffect(api, token) {
        runCatching {
            products = api.products()
            orders = api.orders(token)
        }.onFailure { snackbar.showSnackbar(it.message ?: "Не удалось загрузить данные") }
        loading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationItem(MainSection.CATALOG, section, "Каталог", { Icon(Icons.Default.Storefront, null) }) { section = it }
                NavigationItem(MainSection.CART, section, "Корзина", {
                    BadgedBox(badge = { if (cart.values.sum() > 0) Badge { Text(cart.values.sum().toString()) } }) {
                        Icon(Icons.Default.ShoppingCart, null)
                    }
                }) { section = it }
                NavigationItem(MainSection.ORDERS, section, "Заказы", { Icon(Icons.Default.History, null) }) { section = it }
                NavigationItem(MainSection.PROFILE, section, "Профиль", { Icon(Icons.Default.Person, null) }) { section = it }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (loading) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else when (section) {
                MainSection.CATALOG -> CatalogScreen(
                    products = products,
                    cart = cart,
                    onAdd = { product ->
                        cart[product.id] = (cart[product.id] ?: 0) + 1
                        scope.launch { snackbar.showSnackbar("${product.name} добавлен в корзину") }
                    }
                )
                MainSection.CART -> CartScreen(
                    products = products,
                    cart = cart,
                    onQuantityChanged = { id, quantity -> if (quantity <= 0) cart.remove(id) else cart[id] = quantity },
                    onPay = { address ->
                        scope.launch {
                            runCatching {
                                api.createOrder(token, CreateOrderRequest(address, cart.map { OrderItemRequest(it.key, it.value) }))
                            }.onSuccess {
                                cart.clear()
                                refreshOrders()
                                section = MainSection.ORDERS
                                snackbar.showSnackbar("Заказ №${it.id} успешно оформлен")
                            }.onFailure { snackbar.showSnackbar(it.message ?: "Не удалось оформить заказ") }
                        }
                    }
                )
                MainSection.ORDERS -> OrdersScreen(orders, onRefresh = ::refreshOrders)
                MainSection.PROFILE -> ProfileScreen(
                    user = initialUser,
                    api = api,
                    token = token,
                    themeMode = themeMode,
                    serverUrl = serverUrl,
                    onUserChanged = onUserChanged,
                    onThemeChanged = onThemeChanged,
                    onServerChanged = onServerChanged,
                    onLogout = onLogout,
                    showMessage = { scope.launch { snackbar.showSnackbar(it) } }
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavigationItem(
    target: MainSection,
    current: MainSection,
    label: String,
    icon: @Composable () -> Unit,
    onSelected: (MainSection) -> Unit
) {
    NavigationBarItem(
        selected = target == current,
        onClick = { onSelected(target) },
        icon = icon,
        label = { Text(label) }
    )
}
