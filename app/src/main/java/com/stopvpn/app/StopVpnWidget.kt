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
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE_VPN) {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("toggle_vpn", true)
            }
            context.startActivity(mainIntent)
        } else if (intent.action == ACTION_UPDATE_WIDGET) {
            val statusStr = intent.getStringExtra("status") ?: "DISCONNECTED"
            val status = VpnStatus.valueOf(statusStr)
            updateWidget(context, status)
        }
    }
}