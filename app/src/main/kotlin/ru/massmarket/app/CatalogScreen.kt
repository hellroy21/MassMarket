package ru.massmarket.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CatalogScreen(products: List<Product>, cart: Map<Long, Int>, onAdd: (Product) -> Unit) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Все") }
    val categories = remember(products) { listOf("Все") + products.map { it.category }.distinct() }
    val filtered = products.filter {
        (category == "Все" || it.category == category) &&
            (search.isBlank() || it.name.contains(search, true) || it.description.contains(search, true))
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("Свежие продукты", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Доставим всё необходимое", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Найти продукт") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { item ->
                AssistChip(onClick = { category = item }, label = { Text(item) }, leadingIcon = if (category == item) ({ Text("✓") }) else null)
            }
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ничего не найдено") }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { product -> ProductCard(product, cart[product.id] ?: 0, onAdd) }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, inCart: Int, onAdd: (Product) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Text(product.emoji, fontSize = 52.sp) }
            Spacer(Modifier.height(10.dp))
            Text(product.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Text(product.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${product.price} ₽", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(product.unit, style = MaterialTheme.typography.labelSmall)
                }
                Box {
                    FilledIconButton(onClick = { onAdd(product) }) { Icon(Icons.Default.AddShoppingCart, "Добавить в корзину") }
                    if (inCart > 0) {
                        Text(
                            inCart.toString(),
                            modifier = Modifier.align(Alignment.TopEnd).background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp)).padding(horizontal = 5.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

