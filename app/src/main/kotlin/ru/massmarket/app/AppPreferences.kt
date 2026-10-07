package ru.massmarket.app

import android.content.Context

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("massmarket_preferences", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString("server_url", DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString("server_url", value).apply()

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().apply { if (value == null) remove("token") else putString("token", value) }.apply()

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name)!!) }.getOrDefault(ThemeMode.SYSTEM)
        set(value) = prefs.edit().putString("theme", value.name).apply()

    companion object { const val DEFAULT_SERVER_URL = "http://10.0.2.2:8080" }
}

