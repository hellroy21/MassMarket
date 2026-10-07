package ru.massmarket.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    api: ApiClient,
    serverUrl: String,
    onAuthenticated: (AuthResponse) -> Unit,
    onServerUrlChanged: (String) -> Unit
) {
    var registration by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editServer by remember { mutableStateOf(false) }
    var serverDraft by remember(serverUrl) { mutableStateOf(serverUrl) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer).imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(86.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ShoppingBasket, null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("MassMarket", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(if (registration) "Создайте аккаунт" else "Продукты с доставкой", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(26.dp))

            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), tonalElevation = 3.dp) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(if (registration) "Регистрация" else "Вход", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = email, onValueChange = { email = it; error = null }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Электронная почта") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Mail, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it; error = null }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Пароль") }, singleLine = true, leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    if (registration) {
                        OutlinedTextField(
                            value = confirmation, onValueChange = { confirmation = it; error = null }, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Повторите пароль") }, singleLine = true, leadingIcon = { Icon(Icons.Default.Lock, null) },
                            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank() || registration && confirmation.isBlank()) {
                                error = "Заполните все поля"
                                return@Button
                            }
                            loading = true
                            scope.launch {
                                runCatching {
                                    if (registration) api.register(email, password, confirmation) else api.login(email, password)
                                }.onSuccess(onAuthenticated).onFailure { error = it.message }
                                loading = false
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(if (registration) "Зарегистрироваться" else "Войти")
                    }
                    TextButton(onClick = { registration = !registration; error = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(if (registration) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (editServer) {
                OutlinedTextField(
                    value = serverDraft, onValueChange = { serverDraft = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Адрес сервера") }, singleLine = true,
                    trailingIcon = { TextButton(onClick = { onServerUrlChanged(serverDraft); editServer = false }) { Text("Сохранить") } }
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Сервер: $serverUrl", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    IconButton(onClick = { editServer = true }) { Icon(Icons.Default.Edit, "Изменить сервер", modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

