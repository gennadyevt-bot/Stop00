package com.stopvpn.app

import android.content.Context
import android.content.SharedPreferences

class AutoConnectStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("auto_connect", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)
    fun setEnabled(enabled: Boolean) = prefs.edit().putBoolean("enabled", enabled).apply()

    fun getTargetApps(): Set<String> = prefs.getStringSet("target_apps", emptySet()) ?: emptySet()
    fun setTargetApps(apps: Set<String>) = prefs.edit().putStringSet("target_apps", apps).apply()

    fun getPreferredServer(): String = prefs.getString("preferred_server", "") ?: ""
    fun setPreferredServer(serverId: String) = prefs.edit().putString("preferred_server", serverId).apply()
}