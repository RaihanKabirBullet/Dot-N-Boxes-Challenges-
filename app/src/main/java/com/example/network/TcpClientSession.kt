package com.example.network

import com.example.model.BoardSize
import com.example.model.GameState
import com.example.model.LineId
import com.example.model.PlayerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
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

class TcpClientSession {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_MS = 15000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _snapshot = MutableStateFlow(ClientSessionSnapshot())
    val snapshot: StateFlow<ClientSessionSnapshot> = _snapshot.asStateFlow()

    private val _events = MutableSharedFlow<ClientEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ClientEvent> = _events.asSharedFlow()

    private var socket: Socket? = null
    private var outgoingChannel: Channel<ProtocolMessage>? = null
    private var connectionJob: Job? = null
    private var writerJob: Job? = null

    @Volatile
    private var userInitiatedDisconnect = false

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
            try {
                val sock = Socket()
                activeSocket = sock
                sock.tcpNoDelay = true
                sock.connect(InetSocketAddress(cleanIp, port), CONNECT_TIMEOUT_MS)
                sock.soTimeout = READ_TIMEOUT_MS
                socket = sock

                val writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8))
                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))

                val channel = Channel<ProtocolMessage>(capacity = Channel.UNLIMITED)
                outgoingChannel = channel

                writerJob = launch {
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

                // Send JOIN handshake
                channel.trySend(ProtocolMessage.Join(playerId = playerId, playerName = cleanName))

                while (isActive && !sock.isClosed) {
                    val line = reader.readLine() ?: break
                    val msg = NetworkProtocol.decode(line) ?: continue
                    handleServerMessage(msg, playerId)
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
                            "Connection refused. Make sure host has opened the room."
                        e.message?.contains("timed out", ignoreCase = true) == true ->
                            "Connection timed out. Verify hotspot connection and host IP."
                        else -> e.message ?: "Unable to connect to $cleanIp:$port"
                    }
                    _snapshot.value = _snapshot.value.copy(
                        status = ClientConnectionStatus.ERROR,
                        errorMessage = msg,
                    )
                    _events.tryEmit(ClientEvent.ErrorBanner(msg))
                }
            } finally {
                runCatching { outgoingChannel?.close() }
                writerJob?.cancel()
                runCatching { activeSocket?.close() }
                if (socket === activeSocket) {
                    socket = null
                }
            }
        }
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
        disconnectInternal(sendBye = false)
        scope.cancel()
    }

    private fun handleServerMessage(msg: ProtocolMessage, localPlayerId: String) {
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
                _snapshot.value = _snapshot.value.copy(
                    status = ClientConnectionStatus.IN_GAME,
                    boardSize = msg.gameState.boardSize,
                    players = msg.gameState.players,
                    gameState = msg.gameState,
                    errorMessage = null,
                )
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
