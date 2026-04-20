package com.theaterphone.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.theaterphone.data.model.CommunicationMode
import com.theaterphone.data.model.OscCommand
import com.theaterphone.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.*

class OscListenerService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "osc_listener"
        /** 12 hours — covers a show comfortably, but bounded so a killed process can't leak the lock forever. */
        private const val WAKE_LOCK_TIMEOUT_MS = 12L * 60 * 60 * 1000

        val isListening = MutableStateFlow(false)
        val lastMessage = MutableStateFlow("")
        val localIp = MutableStateFlow("...")

        var port: Int = 9000
        var mode: CommunicationMode = CommunicationMode.OSC
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var udpSocket: DatagramSocket? = null
    private var tcpServer: ServerSocket? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startListening()
        return START_STICKY
    }

    override fun onDestroy() {
        stopListening()
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    private fun startListening() {
        stopListening()
        updateLocalIp()
        startUdpListener()
        if (mode == CommunicationMode.PLAIN_TEXT) {
            startTcpListener()
        }
    }

    private fun stopListening() {
        udpSocket?.close()
        udpSocket = null
        tcpServer?.close()
        tcpServer = null
        isListening.value = false
    }

    fun restart(newPort: Int, newMode: CommunicationMode) {
        port = newPort
        mode = newMode
        startListening()
    }

    private fun startUdpListener() {
        scope.launch {
            try {
                udpSocket = DatagramSocket(port).apply {
                    reuseAddress = true
                }
                withContext(Dispatchers.Main) {
                    isListening.value = true
                }

                val buffer = ByteArray(4096)
                while (isActive && udpSocket != null) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        udpSocket?.receive(packet)
                        val data = packet.data.copyOf(packet.length)
                        val senderIp = packet.address.hostAddress ?: ""
                        handleReceivedData(data, senderIp)
                    } catch (e: SocketException) {
                        break // socket closed
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isListening.value = false
                }
            }
        }
    }

    private fun startTcpListener() {
        scope.launch {
            try {
                tcpServer = ServerSocket(port).apply {
                    reuseAddress = true
                }
                while (isActive && tcpServer != null) {
                    try {
                        val client = tcpServer?.accept() ?: break
                        launch { handleTcpClient(client) }
                    } catch (e: SocketException) {
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun handleTcpClient(socket: Socket) {
        try {
            val senderIp = socket.inetAddress.hostAddress ?: ""
            val reader = socket.getInputStream().bufferedReader()
            var line = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    val command = PlainTextParser.parse(trimmed)
                    if (command != null) {
                        withContext(Dispatchers.Main) {
                            lastMessage.value = "Text: $trimmed"
                        }
                        CommandDispatcher.dispatch(command, senderIp)
                    }
                }
                line = reader.readLine()
            }
        } catch (e: Exception) {
            // client disconnected
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private suspend fun handleReceivedData(data: ByteArray, senderIp: String) {
        if (mode == CommunicationMode.OSC) {
            val command = OscParser.parse(data)
            if (command != null) {
                withContext(Dispatchers.Main) {
                    lastMessage.value = "OSC: ${String(data).takeWhile { it != '\u0000' }}"
                }
                CommandDispatcher.dispatch(command, senderIp)
            }
        } else {
            val text = String(data, Charsets.UTF_8)
            for (line in text.split("\n")) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    val command = PlainTextParser.parse(trimmed)
                    if (command != null) {
                        withContext(Dispatchers.Main) {
                            lastMessage.value = "Text: $trimmed"
                        }
                        CommandDispatcher.dispatch(command, senderIp)
                    }
                }
            }
        }
    }

    init {
        CommandDispatcher.onPing = { senderIp -> sendPong(senderIp) }
    }

    private fun sendPong(host: String) {
        scope.launch {
            try {
                val socket = DatagramSocket()
                val data = if (mode == CommunicationMode.OSC) {
                    OscParser.buildMessage("/theaterphone/pong", "ready")
                } else {
                    "pong ready\n".toByteArray(Charsets.UTF_8)
                }
                val address = InetAddress.getByName(host)
                val packet = DatagramPacket(data, data.size, address, 9001)
                socket.send(packet)
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "OSC Listener",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps OSC listener running in background"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("TheaterPhone")
            .setContentText("Listening on port $port (${mode.displayName})")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TheaterPhone::OscListener")
        wakeLock?.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    private fun releaseWakeLock() {
        wakeLock?.release()
        wakeLock = null
    }

    private fun updateLocalIp() {
        try {
            var fallback: String? = null
            for (intf in NetworkInterface.getNetworkInterfaces()) {
                for (addr in intf.inetAddresses) {
                    if (addr.isLoopbackAddress || addr !is Inet4Address) continue
                    val host = addr.hostAddress ?: continue
                    if (intf.name == "wlan0" || intf.name == "en0") {
                        localIp.value = host
                        return
                    }
                    if (fallback == null) fallback = host
                }
            }
            localIp.value = fallback ?: "Not available"
        } catch (e: Exception) {
            localIp.value = "Not available"
        }
    }
}
