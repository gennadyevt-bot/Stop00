package com.stopvpn.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import kotlinx.coroutines.*

class AutoConnectManager(private val context: Context) {

    private val storage = AutoConnectStorage(context)
    private val vpnManager = VpnManager(context)
    private val serverStorage = ServerStorage(context)
    private var monitoringJob: Job? = null

    fun startMonitoring() {
        if (!hasUsageStatsPermission()) return

        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                checkForegroundApp()
                delay(2000)
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun checkForegroundApp() {
        val enabled = storage.isEnabled()
        if (!enabled) return

        val targetApps = storage.getTargetApps()
        if (targetApps.isEmpty()) return

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 5000

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastForeground = ""

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForeground = event.packageName
            }
        }

        if (lastForeground in targetApps && vpnManager.getStatus() == VpnStatus.DISCONNECTED) {
            val servers = serverStorage.loadServers()
            val server = servers.firstOrNull { it.id == storage.getPreferredServer() }
                ?: servers.firstOrNull()
            server?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    vpnManager.connect(it)
                }
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}