package com.stopvpn.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

class ServerBackupManager(private val context: Context) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val storage = ServerStorage(context)

    fun exportToFile(): Uri? {
        val servers = storage.loadServers()
        val backup = BackupData(
            version = 1,
            servers = servers,
            exportDate = System.currentTimeMillis()
        )
        val jsonString = json.encodeToString(backup)

        val file = File(context.cacheDir, "stop_vpn_backup.json")
        file.writeText(jsonString)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun importFromStream(inputStream: InputStream): Boolean {
        return try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val backup = json.decodeFromString<BackupData>(jsonString)
            storage.saveServers(backup.servers)
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

@kotlinx.serialization.Serializable
data class BackupData(
    val version: Int,
    val servers: List<ServerInfo>,
    val exportDate: Long
)