package ru.massmarket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val preferences = AppPreferences(this)

        setContent {
            var themeMode by remember { mutableStateOf(preferences.themeMode) }
            var serverUrl by remember { mutableStateOf(preferences.serverUrl) }
            var token by remember { mutableStateOf(preferences.token) }
            var user by remember { mutableStateOf<User?>(null) }
            var checkingSession by remember { mutableStateOf(token != null) }
            val api = remember(serverUrl) { ApiClient(serverUrl) }

            MassMarketTheme(themeMode) {
                when {
                    checkingSession && token != null -> {
                        SessionLoader(
                            api = api,
                            token = token!!,
                            onLoaded = { user = it; checkingSession = false },
                            onFailed = { preferences.token = null; token = null; checkingSession = false }
                        )
                    }
                    token == null || user == null -> AuthScreen(
                        api = api,
                        serverUrl = serverUrl,
                        onAuthenticated = { auth ->
                            preferences.token = auth.token
                            token = auth.token
                            user = auth.user
                        },
                        onServerUrlChanged = { newUrl ->
                            val normalized = normalizeServerUrl(newUrl)
                            preferences.serverUrl = normalized
                            serverUrl = normalized
                        }
                    )
                    else -> MainShell(
                        api = api,
                        token = token!!,
                        initialUser = user!!,
                        themeMode = themeMode,
                        serverUrl = serverUrl,
                        onUserChanged = { user = it },
                        onThemeChanged = {
                            preferences.themeMode = it
                            themeMode = it
                        },
                        onServerChanged = { newUrl ->
                            val normalized = normalizeServerUrl(newUrl)
                            preferences.serverUrl = normalized
                            preferences.token = null
                            serverUrl = normalized
                            token = null
                            user = null
                        },
                        onLogout = {
                            preferences.token = null
                            token = null
                            user = null
                        }
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SessionLoader(api: ApiClient, token: String, onLoaded: (User) -> Unit, onFailed: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(api, token) {
        runCatching { api.profile(token) }.onSuccess(onLoaded).onFailure { onFailed() }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

private fun normalizeServerUrl(value: String): String {
    val trimmed = value.trim().trimEnd('/')
    if (trimmed.isBlank()) return AppPreferences.DEFAULT_SERVER_URL
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
}

