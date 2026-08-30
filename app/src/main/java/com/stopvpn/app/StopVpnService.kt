package com.stopvpn.app

import org.amnezia.awg.backend.AbstractBackend
import android.content.Intent
import android.os.IBinder
import android.util.Log

class StopVpnService : AbstractBackend.VpnService() {

    companion object {
        private const val TAG = "StopVpnService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")
        val serverName = intent?.getStringExtra("server_name") ?: "STOP VPN"
        val notification = NotificationHelper.buildNotification(this, serverName)
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
