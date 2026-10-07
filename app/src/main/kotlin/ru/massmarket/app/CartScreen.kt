package ru.massmarket.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CartScreen(
    products: List<Product>,
    cart: Map<Long, Int>,
    onQuantityChanged: (Long, Int) -> Unit,
    onPay: (String) -> Unit
) {
    var address by remember { mutableStateOf("") }
    var addressError by remember { mutableStateOf(false) }
    val selected = products.filter { cart.containsKey(it.id) }
    val productsTotal = selected.sumOf { it.price * (cart[it.id] ?: 0) }

    if (selected.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Корзина пуста", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Добавьте продукты из каталога", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Корзина", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${cart.values.sum()} товаров", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        items(selected, key = { it.id }) { product ->
            val quantity = cart[product.id] ?: 1
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(product.emoji, fontSize = 36.sp, modifier = Modifier.padding(end = 12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.SemiBold)
                        Text("${product.price * quantity} ₽", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { onQuantityChanged(product.id, quantity - 1) }) {
                        Icon(if (quantity == 1) Icons.Default.DeleteOutline else Icons.Default.Remove, "Уменьшить")
                    }
                    Text(quantity.toString(), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onQuantityChanged(product.id, quantity + 1) }) { Icon(Icons.Default.Add, "Увеличить") }
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it; addressError = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Адрес доставки") },
                placeholder = { Text("Город, улица, дом, квартира") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                isError = addressError,
                supportingText = if (addressError) ({ Text("Введите адрес доставки") }) else null,
                minLines = 2,
                shape = RoundedCornerShape(16.dp)
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriceRow("Товары", productsTotal)
                    PriceRow("Доставка", 150)
                    androidx.compose.material3.HorizontalDivider()
                    PriceRow("Итого", productsTotal + 150, true)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { if (address.isBlank()) addressError = true else onPay(address) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Оплатить ${productsTotal + 150} ₽", fontSize = 17.sp) }
        }
    }
}

@Composable
private fun PriceRow(label: String, value: Int, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text("$value ₽", fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

