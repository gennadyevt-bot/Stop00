package com.stopvpn.app

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var vpnManager: VpnManager
    private lateinit var serverAdapter: ServerAdapter
    private lateinit var serverStorage: ServerStorage
    private lateinit var tvStatus: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var rvServers: RecyclerView
    private lateinit var tvCurrentServer: TextView
    private lateinit var tvTrafficDown: TextView
    private lateinit var tvTrafficUp: TextView
    private lateinit var ivMenu: ImageView
    private lateinit var fabAddServer: FloatingActionButton

    private var selectedServer: ServerInfo? = null
    private val servers = mutableListOf<ServerInfo>()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedServer?.let { connectToServer(it) }
        } else {
            Toast.makeText(this, "Разрешение VPN отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Уведомления отключены — VPN может работать нестабильно", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vpnManager = VpnManager(this)
        serverStorage = ServerStorage(this)

        tvStatus = findViewById(R.id.tvStatus)
        ivLogo = findViewById(R.id.ivLogo)
        rvServers = findViewById(R.id.rvServers)
        tvCurrentServer = findViewById(R.id.tvCurrentServer)
        tvTrafficDown = findViewById(R.id.tvTrafficDown)
        tvTrafficUp = findViewById(R.id.tvTrafficUp)
        ivMenu = findViewById(R.id.ivMenu)
        fabAddServer = findViewById(R.id.fabAddServer)

        requestNotificationPermission()
        loadServers()
        setupRecyclerView()
        setupVpnCallbacks()
        updateUiState(VpnStatus.DISCONNECTED)

        ivLogo.setOnClickListener {
            when (vpnManager.getStatus()) {
                VpnStatus.CONNECTED, VpnStatus.CONNECTING -> vpnManager.disconnect()
                else -> {
                    selectedServer?.let { requestVpnPermissionAndConnect(it) }
                        ?: Toast.makeText(this, "Сначала выберите сервер из списка", Toast.LENGTH_SHORT).show()
                }
            }
        }

        ivMenu.setOnClickListener {
            showMenuDialog()
        }

        fabAddServer.setOnClickListener {
            showAddServerDialog()
        }
    }

    private fun loadServers() {
        servers.clear()
        val saved = serverStorage.loadServers()
        if (saved.isEmpty()) {
            servers.addAll(getDefaultServers())
            serverStorage.saveServers(servers)
        } else {
            servers.addAll(saved)
        }
    }

    private fun getDefaultServers(): List<ServerInfo> {
        return listOf(
            ServerInfo(
                id = "premiusa-01",
                name = "VPNJantit Premium USA",
                country = "США, Премиум",
                flagEmoji = "🇺🇸",
                interfaceAddress = "192.168.6.99/32",
                interfaceDns = "1.1.1.1, 8.8.8.8",
                interfacePrivateKey = "aApi17qSZVvAg05MSr6+4AHudvMZJ2jKOEu/HO3e7mk=",
                peerPublicKey = "5EhTY/DjbqjL4M7v3KaMOl84FVt/ZtOnAKIGpQy4GSY=",
                peerEndpoint = "premiusa2.vpnjantit.com:1024",
                peerAllowedIPs = "0.0.0.0/0",
                jc = "",
                jmin = "",
                jmax = "",
                s1 = "",
                s2 = "",
                h1 = "",
                h2 = "",
                h3 = "",
                h4 = ""
            ),
            ServerInfo(
                id = "nl-ams-01",
                name = "NL-AMS-01",
                country = "Нидерланды, Амстердам",
                flagEmoji = "🇳🇱",
                interfaceAddress = "192.168.6.54/32",
                interfaceDns = "1.1.1.1, 8.8.8.8",
                interfacePrivateKey = "YOUR_PRIVATE_KEY_HERE",
                peerPublicKey = "YOUR_SERVER_PUBLIC_KEY_HERE",
                peerEndpoint = "nl-ams-01.stopvpn.example:51820"
            ),
            ServerInfo(
                id = "de-fra-01",
                name = "DE-FRA-01",
                country = "Германия, Франкфурт",
                flagEmoji = "🇩🇪",
                interfaceAddress = "192.168.6.54/32",
                interfaceDns = "1.1.1.1, 8.8.8.8",
                interfacePrivateKey = "YOUR_PRIVATE_KEY_HERE",
                peerPublicKey = "YOUR_SERVER_PUBLIC_KEY_HERE",
                peerEndpoint = "de-fra-01.stopvpn.example:51820"
            ),
            ServerInfo(
                id = "us-nyc-01",
                name = "US-NYC-01",
                country = "США, Нью-Йорк",
                flagEmoji = "🇺🇸",
                interfaceAddress = "192.168.6.54/32",
                interfaceDns = "1.1.1.1, 8.8.8.8",
                interfacePrivateKey = "YOUR_PRIVATE_KEY_HERE",
                peerPublicKey = "YOUR_SERVER_PUBLIC_KEY_HERE",
                peerEndpoint = "us-nyc-01.stopvpn.example:51820"
            )
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED -> { }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(this, "Уведомления нужны для стабильной работы VPN в фоне", Toast.LENGTH_LONG).show()
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        serverAdapter = ServerAdapter(
            servers,
            onServerClick = { server ->
                when (vpnManager.getStatus()) {
                    VpnStatus.CONNECTED -> {
                        if (vpnManager.getCurrentServer()?.id != server.id) {
                            vpnManager.switchServer(server)
                        }
                    }
                    VpnStatus.CONNECTING, VpnStatus.SWITCHING -> { }
                    else -> {
                        selectedServer = server
                        serverAdapter.setSelectedServer(server.id)
                        requestVpnPermissionAndConnect(server)
                    }
                }
            },
            onEditClick = { server ->
                showEditServerDialog(server)
            }
        )
        rvServers.layoutManager = LinearLayoutManager(this)
        rvServers.adapter = serverAdapter
    }

    private fun setupVpnCallbacks() {
        vpnManager.onStatusChanged = { status ->
            updateUiState(status)
            serverAdapter.setStatus(status)
        }
        vpnManager.onServerChanged = { server ->
            server?.let {
                tvCurrentServer.text = "Сервер: ${it.flagEmoji} ${it.name}"
                serverAdapter.setSelectedServer(it.id)
            } ?: run {
                tvCurrentServer.text = "Сервер: не выбран"
                serverAdapter.setSelectedServer(null)
            }
        }
    }

    private fun requestVpnPermissionAndConnect(server: ServerInfo) {
        val intent = vpnManager.getPrepareIntent(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            connectToServer(server)
        }
    }

    private fun connectToServer(server: ServerInfo) {
        if (server.interfacePrivateKey == "YOUR_PRIVATE_KEY_HERE" ||
            server.peerPublicKey == "YOUR_SERVER_PUBLIC_KEY_HERE") {
            Toast.makeText(this, "Сначала добавь конфиг через плюсик →", Toast.LENGTH_LONG).show()
            return
        }
        vpnManager.connect(server)
    }

    private fun showEditServerDialog(server: ServerInfo) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_server, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvDialogSubtitle)
        val etEndpoint = view.findViewById<EditText>(R.id.etEndpoint)
        val etPrivateKey = view.findViewById<EditText>(R.id.etPrivateKey)
        val etPublicKey = view.findViewById<EditText>(R.id.etPublicKey)
        val etPresharedKey = view.findViewById<EditText>(R.id.etPresharedKey)
        val etJc = view.findViewById<EditText>(R.id.etJc)
        val etJmin = view.findViewById<EditText>(R.id.etJmin)
        val etJmax = view.findViewById<EditText>(R.id.etJmax)
        val etS1 = view.findViewById<EditText>(R.id.etS1)
        val etS2 = view.findViewById<EditText>(R.id.etS2)
        val etH1 = view.findViewById<EditText>(R.id.etH1)
        val etH2 = view.findViewById<EditText>(R.id.etH2)
        val etH3 = view.findViewById<EditText>(R.id.etH3)
        val etH4 = view.findViewById<EditText>(R.id.etH4)

        tvTitle.text = "Конфиг сервера"
        tvSubtitle.text = "${server.flagEmoji} ${server.name} — ${server.country}"

        if (!server.peerEndpoint.contains(".stopvpn.example")) {
            etEndpoint.setText(server.peerEndpoint)
        }
        if (server.interfacePrivateKey != "YOUR_PRIVATE_KEY_HERE") {
            etPrivateKey.setText(server.interfacePrivateKey)
        }
        if (server.peerPublicKey != "YOUR_SERVER_PUBLIC_KEY_HERE") {
            etPublicKey.setText(server.peerPublicKey)
        }
        if (server.peerPresharedKey.isNotEmpty()) {
            etPresharedKey.setText(server.peerPresharedKey)
        }
        etJc.setText(server.jc)
        etJmin.setText(server.jmin)
        etJmax.setText(server.jmax)
        etS1.setText(server.s1)
        etS2.setText(server.s2)
        etH1.setText(server.h1)
        etH2.setText(server.h2)
        etH3.setText(server.h3)
        etH4.setText(server.h4)

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Сохранить") { _, _ ->
                val endpoint = etEndpoint.text.toString().trim()
                val privateKey = etPrivateKey.text.toString().trim()
                val publicKey = etPublicKey.text.toString().trim()
                val presharedKey = etPresharedKey.text.toString().trim()
                val jc = etJc.text.toString().trim().ifEmpty { "5" }
                val jmin = etJmin.text.toString().trim().ifEmpty { "50" }
                val jmax = etJmax.text.toString().trim().ifEmpty { "1000" }
                val s1 = etS1.text.toString().trim().ifEmpty { "50" }
                val s2 = etS2.text.toString().trim().ifEmpty { "100" }
                val h1 = etH1.text.toString().trim().ifEmpty { "1" }
                val h2 = etH2.text.toString().trim().ifEmpty { "2" }
                val h3 = etH3.text.toString().trim().ifEmpty { "3" }
                val h4 = etH4.text.toString().trim().ifEmpty { "4" }

                if (endpoint.isEmpty() || privateKey.isEmpty() || publicKey.isEmpty()) {
                    Toast.makeText(this, "Заполни обязательные поля", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val idx = servers.indexOfFirst { it.id == server.id }
                if (idx >= 0) {
                    val updated = server.copy(
                        peerEndpoint = endpoint,
                        interfacePrivateKey = privateKey,
                        peerPublicKey = publicKey,
                        peerPresharedKey = presharedKey,
                        jc = jc,
                        jmin = jmin,
                        jmax = jmax,
                        s1 = s1,
                        s2 = s2,
                        h1 = h1,
                        h2 = h2,
                        h3 = h3,
                        h4 = h4
                    )
                    servers[idx] = updated
                    serverAdapter.notifyItemChanged(idx)
                    serverStorage.saveServers(servers)
                    Toast.makeText(this, "Конфиг ${server.name} сохранён", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showAddServerDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_server, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etCountry = view.findViewById<EditText>(R.id.etCountry)
        val etEndpoint = view.findViewById<EditText>(R.id.etEndpoint)
        val etPrivateKey = view.findViewById<EditText>(R.id.etPrivateKey)
        val etPublicKey = view.findViewById<EditText>(R.id.etPublicKey)
        val etJc = view.findViewById<EditText>(R.id.etJc)
        val etJmin = view.findViewById<EditText>(R.id.etJmin)
        val etJmax = view.findViewById<EditText>(R.id.etJmax)
        val etS1 = view.findViewById<EditText>(R.id.etS1)
        val etS2 = view.findViewById<EditText>(R.id.etS2)
        val etH1 = view.findViewById<EditText>(R.id.etH1)
        val etH2 = view.findViewById<EditText>(R.id.etH2)
        val etH3 = view.findViewById<EditText>(R.id.etH3)
        val etH4 = view.findViewById<EditText>(R.id.etH4)

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Добавить") { _, _ ->
                val name = etName.text.toString().trim()
                val country = etCountry.text.toString().trim()
                val endpoint = etEndpoint.text.toString().trim()
                val privateKey = etPrivateKey.text.toString().trim()
                val publicKey = etPublicKey.text.toString().trim()
                val jc = etJc.text.toString().trim().ifEmpty { "5" }
                val jmin = etJmin.text.toString().trim().ifEmpty { "50" }
                val jmax = etJmax.text.toString().trim().ifEmpty { "1000" }
                val s1 = etS1.text.toString().trim().ifEmpty { "50" }
                val s2 = etS2.text.toString().trim().ifEmpty { "100" }
                val h1 = etH1.text.toString().trim().ifEmpty { "1" }
                val h2 = etH2.text.toString().trim().ifEmpty { "2" }
                val h3 = etH3.text.toString().trim().ifEmpty { "3" }
                val h4 = etH4.text.toString().trim().ifEmpty { "4" }

                if (name.isEmpty() || endpoint.isEmpty() || privateKey.isEmpty() || publicKey.isEmpty()) {
                    Toast.makeText(this, "Заполни все обязательные поля", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newServer = ServerInfo(
                    id = "custom_${System.currentTimeMillis()}",
                    name = name,
                    country = country.ifEmpty { "Custom" },
                    flagEmoji = "🌍",
                    interfaceAddress = "192.168.6.54/32",
                    interfaceDns = "1.1.1.1, 8.8.8.8",
                    interfacePrivateKey = privateKey,
                    peerPublicKey = publicKey,
                    peerEndpoint = endpoint,
                    jc = jc,
                    jmin = jmin,
                    jmax = jmax,
                    s1 = s1,
                    s2 = s2,
                    h1 = h1,
                    h2 = h2,
                    h3 = h3,
                    h4 = h4
                )
                servers.add(newServer)
                serverAdapter.notifyItemInserted(servers.size - 1)
                rvServers.scrollToPosition(servers.size - 1)
                serverStorage.saveServers(servers)
                Toast.makeText(this, "Сервер добавлен и сохранён", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateUiState(status: VpnStatus) {
        StopVpnWidget.updateWidget(this, status)
        when (status) {
            VpnStatus.CONNECTED -> {
                ivLogo.setImageResource(R.drawable.ic_logo_big_green)
                tvStatus.text = "VPN активен"
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                tvTrafficDown.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                tvTrafficUp.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                startTrafficMonitor()
            }
            VpnStatus.CONNECTING -> {
                ivLogo.setImageResource(R.drawable.ic_logo_big)
                tvStatus.text = "Подключение..."
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                tvTrafficDown.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                tvTrafficUp.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            }
            VpnStatus.SWITCHING -> {
                ivLogo.setImageResource(R.drawable.ic_logo_big)
                tvStatus.text = "Смена сервера..."
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                tvTrafficDown.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                tvTrafficUp.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            }
            VpnStatus.DISCONNECTING -> {
                ivLogo.setImageResource(R.drawable.ic_logo_big)
                tvStatus.text = "Отключение..."
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                tvTrafficDown.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                tvTrafficUp.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            }
            else -> {
                ivLogo.setImageResource(R.drawable.ic_logo_big)
                tvStatus.text = "VPN отключен"
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                tvTrafficDown.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                tvTrafficUp.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                stopTrafficMonitor()
            }
        }
    }

    private var trafficHandler: android.os.Handler? = null
    private var trafficRunnable: Runnable? = null

    private fun startTrafficMonitor() {
        trafficHandler = android.os.Handler(android.os.Looper.getMainLooper())
        trafficRunnable = object : Runnable {
            override fun run() {
                val stats = vpnManager.getTrafficStats()
                tvTrafficDown.text = "↓ ${formatBytes(stats.rxBytes)}/s"
                tvTrafficUp.text = "↑ ${formatBytes(stats.txBytes)}/s"
                trafficHandler?.postDelayed(this, 1000)
            }
        }
        trafficHandler?.post(trafficRunnable!!)
    }

    private fun stopTrafficMonitor() {
        trafficRunnable?.let { trafficHandler?.removeCallbacks(it) }
        trafficHandler = null
        trafficRunnable = null
        tvTrafficDown.text = "↓ 0 B/s"
        tvTrafficUp.text = "↑ 0 B/s"
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (vpnManager.getStatus() == VpnStatus.CONNECTED) {
            vpnManager.disconnect()
        }
    }

    private fun showMenuDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_menu, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_NoActionBar)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.btnBackup).setOnClickListener {
            showBackupDialog()
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnAutoConnect).setOnClickListener {
            showAutoConnectDialog()
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnWidget).setOnClickListener {
            Toast.makeText(this, "Долгое нажатие на рабочий стол → Виджеты → STOP VPN", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnAbout).setOnClickListener {
            Toast.makeText(this, "STOP VPN v4.0.0
AmneziaWG + Kotlin", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.black)
        dialog.show()
    }

    private fun showBackupDialog() {
        val backupManager = ServerBackupManager(this)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Резервное копирование")
            .setMessage("Экспорт или импорт серверов?")
            .setPositiveButton("Экспорт") { _, _ ->
                backupManager.shareBackup()
                Toast.makeText(this, "Конфиги экспортированы", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Импорт") { _, _ ->
                // TODO: открыть file picker
                Toast.makeText(this, "Импорт: выберите .json файл", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    private fun showAutoConnectDialog() {
        val storage = AutoConnectStorage(this)
        val enabled = storage.isEnabled()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Автоподключение")
            .setMessage("VPN будет включаться автоматически при открытии выбранных приложений.

Статус: ${if (enabled) "ВКЛ" else "ВЫКЛ"}")
            .setPositiveButton(if (enabled) "Выключить" else "Включить") { _, _ ->
                storage.setEnabled(!enabled)
                Toast.makeText(this, "Автоподключение: ${if (!enabled) "ВКЛ" else "ВЫКЛ"}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
