package com.stopvpn.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.*

class StopVpnWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_VPN = "com.stopvpn.app.TOGGLE_VPN"
        const val ACTION_UPDATE_WIDGET = "com.stopvpn.app.UPDATE_WIDGET"

        fun updateWidget(context: Context, status: VpnStatus) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, StopVpnWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            val views = RemoteViews(context.packageName, R.layout.widget_stop_vpn)

            val logoRes = when (status) {
                VpnStatus.CONNECTED -> R.drawable.ic_logo_big_green
                else -> R.drawable.ic_logo_big
            }
            views.setImageViewResource(R.id.ivWidgetLogo, logoRes)

            // PendingIntent для нажатия
            val intent = Intent(context, StopVpnWidget::class.java).apply {
                action = ACTION_TOGGLE_VPN
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.ivWidgetLogo, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val status = VpnManager.globalStatus
        updateWidget(context, status)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE_VPN) {
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope.launch {
                val vpnManager = VpnManager(context)
                when (vpnManager.getStatus()) {
                    VpnStatus.CONNECTED, VpnStatus.CONNECTING -> {
                        vpnManager.disconnect()
                    }
                    else -> {
                        val storage = ServerStorage(context)
                        val servers = storage.loadServers()
                        val server = servers.firstOrNull { it.id == "premiusa-01" }
                            ?: servers.firstOrNull()
                        server?.let {
                            vpnManager.connect(it)
                        }
                    }
                }
                delay(500)
                updateWidget(context, vpnManager.getStatus())
            }
        } else if (intent.action == ACTION_UPDATE_WIDGET) {
            val status = intent.getSerializableExtra("status") as? VpnStatus ?: VpnStatus.DISCONNECTED
            updateWidget(context, status)
        }
    }
}