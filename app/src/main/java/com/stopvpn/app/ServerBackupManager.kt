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
                    put("flagEmoji", s.flagEmoji)
                    put("interfaceAddress", s.interfaceAddress)
                    put("interfaceDns", s.interfaceDns)
                    put("interfacePrivateKey", s.interfacePrivateKey)
                    put("peerPublicKey", s.peerPublicKey)
                    put("peerPresharedKey", s.peerPresharedKey)
                    put("peerAllowedIPs", s.peerAllowedIPs)
                    put("peerEndpoint", s.peerEndpoint)
                    put("peerPersistentKeepalive", s.peerPersistentKeepalive)
                    put("jc", s.jc)
                    put("jmin", s.jmin)
                    put("jmax", s.jmax)
                    put("s1", s.s1)
                    put("s2", s.s2)
                    put("h1", s.h1)
                    put("h2", s.h2)
                    put("h3", s.h3)
                    put("h4", s.h4)
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
                    flagEmoji = obj.getString("flagEmoji"),
                    interfaceAddress = obj.getString("interfaceAddress"),
                    interfaceDns = obj.getString("interfaceDns"),
                    interfacePrivateKey = obj.getString("interfacePrivateKey"),
                    peerPublicKey = obj.getString("peerPublicKey"),
                    peerPresharedKey = obj.optString("peerPresharedKey", ""),
                    peerAllowedIPs = obj.optString("peerAllowedIPs", "0.0.0.0/0"),
                    peerEndpoint = obj.getString("peerEndpoint"),
                    peerPersistentKeepalive = obj.optString("peerPersistentKeepalive", "25"),
                    jc = obj.optString("jc", "5"),
                    jmin = obj.optString("jmin", "50"),
                    jmax = obj.optString("jmax", "1000"),
                    s1 = obj.optString("s1", "50"),
                    s2 = obj.optString("s2", "100"),
                    h1 = obj.optString("h1", "1"),
                    h2 = obj.optString("h2", "2"),
                    h3 = obj.optString("h3", "3"),
                    h4 = obj.optString("h4", "4")
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