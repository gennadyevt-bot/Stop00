package com.stopvpn.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream

class ServerBackupManager(private val context: Context) {

    private val storage = ServerStorage(context)

    fun exportToFile(): Uri? {
        val servers = storage.loadServers()
        val json = JSONObject().apply {
            put("version", 1)
            put("exportDate", System.currentTimeMillis())
            val array = JSONArray()
            for (s in servers) {
                array.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("country", s.country)
                    put("city", s.city)
                    put("flagEmoji", s.flagEmoji)
                    put("config", s.config)
                })
            }
            put("servers", array)
        }

        val file = File(context.cacheDir, "stop_vpn_backup.json")
        file.writeText(json.toString(2))

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun importFromStream(inputStream: InputStream): Boolean {
        return try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            val array = json.getJSONArray("servers")
            val servers = mutableListOf<ServerInfo>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                servers.add(ServerInfo(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    country = obj.getString("country"),
                    city = obj.getString("city"),
                    flagEmoji = obj.getString("flagEmoji"),
                    config = obj.getString("config")
                ))
            }
            storage.saveServers(servers)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareBackup() {
        val uri = exportToFile() ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "STOP VPN Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться конфигами"))
    }
}