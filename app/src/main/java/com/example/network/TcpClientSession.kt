package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import com.example.model.BoardSize
import com.example.model.GamePhase
import com.example.model.GameState
import com.example.model.LineId
import com.example.model.PlayerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

enum class ClientConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED_LOBBY,
    IN_GAME,
    ERROR,
}

data class ClientSessionSnapshot(
    val status: ClientConnectionStatus = ClientConnectionStatus.DISCONNECTED,
    val hostIp: String = "",
    val port: Int = NetworkProtocol.DEFAULT_PORT,
    val localPlayerId: String = "",
    val assignedColorIndex: Int = 0,
    val boardSize: BoardSize = BoardSize.SMALL,
    val players: List<PlayerInfo> = emptyList(),
    val gameState: GameState? = null,
    val errorMessage: String? = null,
)

sealed interface ClientEvent {
    data object JoinedLobby : ClientEvent
    data object MatchStarted : ClientEvent
    data class ErrorBanner(val message: String) : ClientEvent
    data class Disconnected(val reason: String) : ClientEvent
}

class TcpClientSession(
    private val appContext: Context? = null,
) {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 6000
        private const val READ_TIMEOUT_MS = 15000
        private const val DISCOVERY_STALE_MS = 6500L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _snapshot = MutableStateFlow(ClientSessionSnapshot())
    val snapshot: StateFlow<ClientSessionSnapshot> = _snapshot.asStateFlow()

    private val _events = MutableSharedFlow<ClientEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ClientEvent> = _events.asSharedFlow()

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<DiscoveredRoom>> = _discoveredRooms.asStateFlow()

    private var socket: Socket? = null
    private var outgoingChannel: Channel<ProtocolMessage>? = null
    private var connectionJob: Job? = null
    private var writerJob: Job? = null
    private var discoveryJob: Job? = null
    private var discoverySocket: DatagramSocket? = null

    @Volatile
    private var userInitiatedDisconnect = false

    fun startDiscovery(extraTargetIps: List<String> = emptyList()) {
        stopDiscovery()
        _discoveredRooms.value = emptyList()
        discoveryJob = scope.launch {
            val wifiMgr = appContext?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val lock = runCatching {
                wifiMgr?.createMulticastLock("dotnbox_client_discovery")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }.getOrNull()

            val roomsByKey = LinkedHashMap<String, DiscoveredRoom>()
            try {
                val udp = DatagramSocket().apply {
                    reuseAddress = true
                    broadcast = true
                    soTimeout = 1000
                }
                discoverySocket = udp
                if (appContext != null) {
                    IpAddressProvider.findWifiNetwork(appContext)?.let { wifiNet ->
                        runCatching { wifiNet.bindSocket(udp) }
                    }
                }

                val probeBytes = NetworkProtocol.DISCOVERY_QUERY.toByteArray(Charsets.UTF_8)
                val recvBuf = ByteArray(1024)
                var lastProbeAt = 0L

                while (isActive && !udp.isClosed) {
                    val now = System.currentTimeMillis()
                    if (now - lastProbeAt >= 1500L) {
                        lastProbeAt = now
                        val targets = linkedSetOf<String>()
                        targets.addAll(IpAddressProvider.detectBroadcastAddresses())
                        if (appContext != null) {
                            IpAddressProvider.detectWifiGatewayIp(appContext)?.let { targets.add(it) }
                        }
                        extraTargetIps.filter { it.isNotBlank() }.forEach { targets.add(it.trim()) }
                        for (ip in targets) {
                            runCatching {
                                val addr = InetAddress.getByName(ip)
                                udp.send(DatagramPacket(probeBytes, probeBytes.size, addr, NetworkProtocol.DISCOVERY_PORT))
                            }
                        }
                    }

                    val pkt = DatagramPacket(recvBuf, recvBuf.size)
                    val gotPacket = try {
                        udp.receive(pkt)
                        true
                    } catch (_: Exception) {
                        false
                    }

                    if (gotPacket) {
                        val raw = String(pkt.data, 0, pkt.length, Charsets.UTF_8)
                        val srcIp = pkt.address?.hostAddress
                        if (!srcIp.isNullOrBlank()) {
                            val room = NetworkProtocol.decodeDiscoveryAnnouncement(raw, srcIp)
                            if (room != null) {
                                roomsByKey["${room.hostIp}:${room.port}"] = room
                            }
                        }
                    }

                    val cutoff = System.currentTimeMillis() - DISCOVERY_STALE_MS
                    roomsByKey.entries.removeAll { it.value.lastSeenMs < cutoff }
                    _discoveredRooms.value = roomsByKey.values.sortedByDescending { it.lastSeenMs }
                }
            } catch (_: Exception) {
                // Best-effort discovery
            } finally {
                runCatching { discoverySocket?.close() }
                discoverySocket = null
                runCatching { lock?.let { if (it.isHeld) it.release() } }
            }
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        runCatching { discoverySocket?.close() }
        discoverySocket = null
    }

    fun connect(
        hostIp: String,
        port: Int,
        playerId: String,
        playerName: String,
    ) {
        val cleanIp = hostIp.trim()
        val cleanName = playerName.trim().take(18).ifEmpty { "Player" }
        disconnectInternal(sendBye = false)
        userInitiatedDisconnect = false

        _snapshot.value = ClientSessionSnapshot(
            status = ClientConnectionStatus.CONNECTING,
            hostIp = cleanIp,
            port = port,
            localPlayerId = playerId,
            errorMessage = null,
        )

        connectionJob = scope.launch {
            var activeSocket: Socket? = null
            var activeChannel: Channel<ProtocolMessage>? = null
            var activeWriter: Job? = null
            try {
                val sock = createRoutedSocket(cleanIp)
                activeSocket = sock
                sock.tcpNoDelay = true
                sock.connect(InetSocketAddress(cleanIp, port), CONNECT_TIMEOUT_MS)
                sock.soTimeout = READ_TIMEOUT_MS
                socket = sock

                val writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8))
                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))

                val channel = Channel<ProtocolMessage>(capacity = Channel.UNLIMITED)
                activeChannel = channel
                outgoingChannel = channel

                val wJob = launch {
                    for (msg in channel) {
                        val ok = runCatching {
                            writer.write(NetworkProtocol.encode(msg))
                            writer.write("\n")
                            writer.flush()
                            true
                        }.getOrElse { false }
                        if (!ok) break
                    }
                }
                activeWriter = wJob
                writerJob = wJob

                // Send JOIN handshake
                channel.trySend(ProtocolMessage.Join(playerId = playerId, playerName = cleanName))

                while (isActive && !sock.isClosed) {
                    val line = reader.readLine() ?: break
                    val msg = NetworkProtocol.decode(line) ?: continue
                    handleServerMessage(msg)
                }

                if (!userInitiatedDisconnect && _snapshot.value.status != ClientConnectionStatus.ERROR) {
                    val wasInGame = _snapshot.value.gameState != null
                    _snapshot.value = _snapshot.value.copy(
                        status = ClientConnectionStatus.DISCONNECTED,
                        errorMessage = if (wasInGame) "Connection lost. Tap Reconnect to rejoin." else "Disconnected from host.",
                    )
                    _events.tryEmit(ClientEvent.Disconnected("Connection lost."))
                }
            } catch (e: Exception) {
                if (!userInitiatedDisconnect) {
                    val msg = when {
                        e.message?.contains("ECONNREFUSED", ignoreCase = true) == true ->
                            "Connection refused. Make sure the Host opened Create Room."
                        e.message?.contains("ENETUNREACH", ignoreCase = true) == true ||
                            e.message?.contains("EHOSTUNREACH", ignoreCase = true) == true ->
                            "Host unreachable. Connect to the Host's Wi-Fi Hotspot and check the IP."
                        e.message?.contains("timed out", ignoreCase = true) == true ->
                            "Connection timed out. Verify hotspot connection and Host IP ($cleanIp)."
                        else -> e.message ?: "Unable to connect to $cleanIp:$port"
                    }
                    _snapshot.value = _snapshot.value.copy(
                        status = ClientConnectionStatus.ERROR,
                        errorMessage = msg,
                    )
                    _events.tryEmit(ClientEvent.ErrorBanner(msg))
                }
            } finally {
                runCatching { activeChannel?.close() }
                activeWriter?.cancel()
                runCatching { activeSocket?.close() }
                if (outgoingChannel === activeChannel) {
                    outgoingChannel = null
                }
                if (writerJob === activeWriter) {
                    writerJob = null
                }
                if (socket === activeSocket) {
                    socket = null
                }
            }
        }
    }

    private fun createRoutedSocket(hostIp: String): Socket {
        val isLoopback = hostIp == "localhost" || hostIp.startsWith("127.") || hostIp == "::1"
        if (!isLoopback && appContext != null) {
            val wifiNet = IpAddressProvider.findWifiNetwork(appContext)
            if (wifiNet != null) {
                runCatching { return wifiNet.socketFactory.createSocket() }
            }
        }
        return Socket()
    }

    fun sendMove(playerId: String, line: LineId) {
        outgoingChannel?.trySend(
            ProtocolMessage.Move(
                playerId = playerId,
                lineType = line.type,
                row = line.row,
                col = line.col,
            ),
        )
    }

    fun disconnect(reason: String = "Player left room") {
        userInitiatedDisconnect = true
        disconnectInternal(sendBye = true, reason = reason)
        _snapshot.value = ClientSessionSnapshot()
    }

    fun close() {
        userInitiatedDisconnect = true
        stopDiscovery()
        disconnectInternal(sendBye = false)
        scope.cancel()
    }

    private fun handleServerMessage(msg: ProtocolMessage) {
        when (msg) {
            is ProtocolMessage.JoinAccepted -> {
                _snapshot.value = _snapshot.value.copy(
                    status = ClientConnectionStatus.CONNECTED_LOBBY,
                    assignedColorIndex = msg.assignedColorIndex,
                    boardSize = msg.boardSize,
                    players = msg.players,
                    errorMessage = null,
                )
                _events.tryEmit(ClientEvent.JoinedLobby)
            }
            is ProtocolMessage.PlayerList -> {
                _snapshot.value = _snapshot.value.copy(
                    status = ClientConnectionStatus.CONNECTED_LOBBY,
                    boardSize = msg.boardSize,
                    players = msg.players,
                    gameState = null,
                    errorMessage = null,
                )
            }
            is ProtocolMessage.GameStart -> {
                _snapshot.value = _snapshot.value.copy(
                    status = ClientConnectionStatus.IN_GAME,
                    boardSize = msg.gameState.boardSize,
                    players = msg.gameState.players,
                    gameState = msg.gameState,
                    errorMessage = null,
                )
                _events.tryEmit(ClientEvent.MatchStarted)
            }
            is ProtocolMessage.GameStateSync -> {
                val prevStatus = _snapshot.value.status
                _snapshot.value = _snapshot.value.copy(
                    status = ClientConnectionStatus.IN_GAME,
                    boardSize = msg.gameState.boardSize,
                    players = msg.gameState.players,
                    gameState = msg.gameState,
                    errorMessage = null,
                )
                if (prevStatus != ClientConnectionStatus.IN_GAME && msg.gameState.phase != GamePhase.FINISHED) {
                    _events.tryEmit(ClientEvent.MatchStarted)
                }
            }
            is ProtocolMessage.Error -> {
                val fatal = msg.code == "ROOM_FULL" || msg.code == "GAME_IN_PROGRESS" || msg.code == "INVALID_HANDSHAKE"
                _snapshot.value = _snapshot.value.copy(
                    status = if (fatal) ClientConnectionStatus.ERROR else _snapshot.value.status,
                    errorMessage = msg.message,
                )
                _events.tryEmit(ClientEvent.ErrorBanner(msg.message))
            }
            is ProtocolMessage.Ping -> {
                outgoingChannel?.trySend(ProtocolMessage.Ping(System.currentTimeMillis()))
            }
            is ProtocolMessage.Disconnect -> {
                userInitiatedDisconnect = true
                _snapshot.value = _snapshot.value.copy(
                    status = ClientConnectionStatus.DISCONNECTED,
                    errorMessage = msg.reason,
                )
                _events.tryEmit(ClientEvent.Disconnected(msg.reason))
                runCatching { socket?.close() }
            }
            is ProtocolMessage.Join, is ProtocolMessage.Move -> Unit
        }
    }

    private fun disconnectInternal(sendBye: Boolean, reason: String = "Left") {
        if (sendBye) {
            val pid = _snapshot.value.localPlayerId
            outgoingChannel?.trySend(ProtocolMessage.Disconnect(pid, reason))
        }
        connectionJob?.cancel()
        writerJob?.cancel()
        runCatching { outgoingChannel?.close() }
        runCatching { socket?.close() }
        outgoingChannel = null
        socket = null
        connectionJob = null
        writerJob = null
    }
}
