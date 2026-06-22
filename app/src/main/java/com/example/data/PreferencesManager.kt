package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vault_pref", Context.MODE_PRIVATE)

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean("app_lock_enabled", false)
        set(value) = prefs.edit().putBoolean("app_lock_enabled", value).apply()

    var appLockPin: String
        get() = prefs.getString("app_lock_pin", "") ?: ""
        set(value) = prefs.edit().putString("app_lock_pin", value).apply()

    var trashAutoEmptyDays: Int
        get() = prefs.getInt("trash_auto_empty_days", 30)
        set(value) = prefs.edit().putInt("trash_auto_empty_days", value).apply()

    var appTheme: String
        get() = prefs.getString("app_theme", "Dark") ?: "Dark" // Default to Dark mode as in visual reference
        set(value) = prefs.edit().putString("app_theme", value).apply()

    var isUnlocked: Boolean = false // Memory-only session state to handle app launching
}
