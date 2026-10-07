package ru.massmarket.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun ProfileScreen(
    user: User,
    api: ApiClient,
    token: String,
    themeMode: ThemeMode,
    serverUrl: String,
    onUserChanged: (User) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onServerChanged: (String) -> Unit,
    onLogout: () -> Unit,
    showMessage: (String) -> Unit
) {
    var name by remember(user.id, user.name) { mutableStateOf(user.name) }
    var birthDate by remember(user.id, user.birthDate) { mutableStateOf(user.birthDate.orEmpty()) }
    var avatar by remember(user.id, user.avatarBase64) { mutableStateOf(user.avatarBase64) }
    var saving by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }
    var serverDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val encoded = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri).use { input ->
                        val bitmap = BitmapFactory.decodeStream(input) ?: error("Не удалось прочитать изображение")
                        encodeAvatar(bitmap)
                    }
                }.getOrNull()
            }
            if (encoded == null) showMessage("Не удалось обработать изображение") else avatar = encoded
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Мой профиль", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        Box {
            Avatar(avatar, Modifier.size(112.dp))
            IconButton(
                onClick = { avatarPicker.launch("image/*") },
                modifier = Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.primary, CircleShape).size(38.dp)
            ) { Icon(Icons.Default.CameraAlt, "Выбрать аватар", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp)) }
        }
        Text(user.email, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { if (it.length <= 10) birthDate = it },
                    label = { Text("Дата рождения") },
                    placeholder = { Text("ГГГГ-ММ-ДД") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (name.isBlank()) { showMessage("Введите имя"); return@Button }
                        saving = true
                        scope.launch {
                            runCatching { api.updateProfile(token, UpdateProfileRequest(name, birthDate.ifBlank { null }, avatar)) }
                                .onSuccess { onUserChanged(it); showMessage("Профиль сохранён") }
                                .onFailure { showMessage(it.message ?: "Не удалось сохранить профиль") }
                            saving = false
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(15.dp)
                ) { Text(if (saving) "Сохранение…" else "Сохранить изменения") }
            }
        }

        Spacer(Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { settingsExpanded = !settingsExpanded },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Настройки", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    Text(if (settingsExpanded) "▲" else "▼")
                }
                if (settingsExpanded) {
                    Spacer(Modifier.height(14.dp))
                    Text("Тема приложения", fontWeight = FontWeight.SemiBold)
                    ThemeChoice("Как в системе", ThemeMode.SYSTEM, themeMode, onThemeChanged)
                    ThemeChoice("Светлая", ThemeMode.LIGHT, themeMode, onThemeChanged)
                    ThemeChoice("Тёмная", ThemeMode.DARK, themeMode, onThemeChanged)
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("Сервер", fontWeight = FontWeight.SemiBold)
                    Text(serverUrl, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { serverDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Изменить адрес сервера") }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) {
            Icon(Icons.Default.Logout, null)
            Text("Выйти из аккаунта", modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(20.dp))
    }

    if (serverDialog) {
        ServerDialog(serverUrl, onDismiss = { serverDialog = false }, onSave = { serverDialog = false; onServerChanged(it) })
    }
}

@Composable
private fun Avatar(base64: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(base64) {
        base64?.let { runCatching { BitmapFactory.decodeByteArray(Base64.decode(it, Base64.DEFAULT), 0, Base64.decode(it, Base64.DEFAULT).size) }.getOrNull() }
    }
    Box(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), "Аватар", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.Person, null, modifier = Modifier.size(62.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ThemeChoice(label: String, value: ThemeMode, selected: ThemeMode, onSelected: (ThemeMode) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onSelected(value) }, verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected == value, onClick = { onSelected(value) })
        Text(label)
    }
}

@Composable
private fun ServerDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Адрес сервера") },
        text = {
            Column {
                Text("После изменения потребуется войти снова. Для телефона используйте IP компьютера в локальной сети.")
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("http://адрес:8080") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onSave(value) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun encodeAvatar(bitmap: Bitmap): String {
    val maxSide = 512
    val scale = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
    val resized = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap
    val output = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 82, output)
    return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}

