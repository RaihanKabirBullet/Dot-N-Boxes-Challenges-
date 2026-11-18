package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import com.example.game.DotsAndBoxesEngine
import com.example.game.MoveResult
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class HostRoomSnapshot(
    val isRunning: Boolean = false,
    val port: Int = NetworkProtocol.DEFAULT_PORT,
    val hostIp: String? = null,
    val alternateIps: List<String> = emptyList(),
    val boardSize: BoardSize = BoardSize.SMALL,
    val players: List<PlayerInfo> = emptyList(),
    val gameState: GameState? = null,
    val lastError: String? = null,
)

class TcpHostServer(
    private val appContext: Context? = null,
) {

    companion object {
        const val MAX_PLAYERS = 4
        private const val HEARTBEAT_INTERVAL_MS = 4000L
        private const val CLIENT_SO_TIMEOUT_MS = 15000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateMutex = Mutex()

    private var serverSocket: ServerSocket? = null
    private var discoverySocket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var acceptJob: Job? = null
    private var heartbeatJob: Job? = null
    private var discoveryJob: Job? = null

    private val clientSessions = ConcurrentHashMap<String, ClientHandler>()

    private val _snapshot = MutableStateFlow(HostRoomSnapshot())
    val snapshot: StateFlow<HostRoomSnapshot> = _snapshot.asStateFlow()

    private var onSoundEvent: ((HostGameEvent) -> Unit)? = null

    enum class HostGameEvent {
        LINE_DRAWN,
        BOX_CLAIMED,
        BONUS_TURN,
        GAME_FINISHED,
        PLAYER_JOINED,
        PLAYER_LEFT,
    }

    fun setEventListener(listener: ((HostGameEvent) -> Unit)?) {
        onSoundEvent = listener
    }

    suspend fun startServer(
        hostId: String,
        hostName: String,
        initialBoardSize: BoardSize,
        port: Int = NetworkProtocol.DEFAULT_PORT,
    ): Result<Unit> = stateMutex.withLock {
        stopServerInternal()
        return runCatching {
            val ss = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(port))
            }
            serverSocket = ss

            val ipResult = IpAddressProvider.detectLocalIpv4Addresses()
            val hostPlayer = PlayerInfo(
                id = hostId,
                name = hostName.trim().ifEmpty { "Host" },
                colorIndex = 0,
                isHost = true,
                isAi = false,
                isConnected = true,
                score = 0,
            )

            _snapshot.value = HostRoomSnapshot(
                isRunning = true,
                port = ss.localPort,
                hostIp = ipResult.primaryIp,
                alternateIps = ipResult.allLocalIps.drop(1),
                boardSize = initialBoardSize,
                players = listOf(hostPlayer),
                gameState = null,
                lastError = null,
            )

            acceptJob = scope.launch { acceptLoop(ss) }
            heartbeatJob = scope.launch { heartbeatLoop() }
            if (port == NetworkProtocol.DEFAULT_PORT) {
                discoveryJob = scope.launch { runDiscoveryResponder() }
            }
        }.onFailure { err ->
            _snapshot.value = _snapshot.value.copy(
                isRunning = false,
                lastError = err.message ?: "Failed to start server on port $port",
            )
        }
    }

    fun refreshHostIp() {
        val ipResult = IpAddressProvider.detectLocalIpv4Addresses()
        _snapshot.value = _snapshot.value.copy(
            hostIp = ipResult.primaryIp,
            alternateIps = ipResult.allLocalIps.drop(1),
        )
    }

    suspend fun updateBoardSize(boardSize: BoardSize) = stateMutex.withLock {
        val current = _snapshot.value
        if (current.gameState != null && current.gameState.phase == GamePhase.IN_PROGRESS) return@withLock
        _snapshot.value = current.copy(boardSize = boardSize)
        broadcastLocked(ProtocolMessage.PlayerList(boardSize, current.players))
    }

    suspend fun updateHostName(newName: String) = stateMutex.withLock {
        val sanitized = newName.trim().ifEmpty { "Host" }
        val current = _snapshot.value
        val updatedPlayers = current.players.map {
            if (it.isHost) it.copy(name = sanitized) else it
        }
        _snapshot.value = current.copy(players = updatedPlayers)
        broadcastLocked(ProtocolMessage.PlayerList(current.boardSize, updatedPlayers))
    }

    suspend fun removePlayer(playerId: String) = stateMutex.withLock {
        val current = _snapshot.value
        val target = current.players.firstOrNull { it.id == playerId } ?: return@withLock
        if (target.isHost) return@withLock

        clientSessions.remove(playerId)?.let { handler ->
            handler.sendAndClose(
                ProtocolMessage.Disconnect(
                    playerId = playerId,
                    reason = "Removed from room by host.",
                ),
            )
        }

        val updatedPlayers = current.players.filterNot { it.id == playerId }
        _snapshot.value = current.copy(players = updatedPlayers)
        broadcastLocked(ProtocolMessage.PlayerList(current.boardSize, updatedPlayers))
        onSoundEvent?.invoke(HostGameEvent.PLAYER_LEFT)
    }

    suspend fun startMatch(): Result<GameState> = stateMutex.withLock {
        val current = _snapshot.value
        val connectedPlayers = current.players.filter { it.isConnected }
        if (connectedPlayers.size < 2) {
            return Result.failure(IllegalStateException("At least 2 connected players are required to start."))
        }
        val initialState = DotsAndBoxesEngine.createInitialState(
            boardSize = current.boardSize,
            players = connectedPlayers,
        )
        _snapshot.value = current.copy(
            players = initialState.players,
            gameState = initialState,
            lastError = null,
        )
        broadcastLocked(ProtocolMessage.GameStart(initialState))
        return Result.success(initialState)
    }

    suspend fun restartMatch(): Result<GameState> = stateMutex.withLock {
        val current = _snapshot.value
        val activePlayers = (current.gameState?.players ?: current.players).filter { it.isConnected }
        if (activePlayers.size < 2) {
            return Result.failure(IllegalStateException("Need at least 2 connected players to restart."))
        }
        val freshState = DotsAndBoxesEngine.createInitialState(
            boardSize = current.boardSize,
            players = activePlayers,
        )
        _snapshot.value = current.copy(
            players = freshState.players,
            gameState = freshState,
            lastError = null,
        )
        broadcastLocked(ProtocolMessage.GameStart(freshState))
        return Result.success(freshState)
    }

    suspend fun setPaused(paused: Boolean) = stateMutex.withLock {
        val current = _snapshot.value
        val gs = current.gameState ?: return@withLock
        if (gs.phase == GamePhase.FINISHED) return@withLock
        val nextPhase = if (paused) GamePhase.PAUSED else GamePhase.IN_PROGRESS
        val updated = gs.copy(
            phase = nextPhase,
            statusMessage = if (paused) "Match paused by host" else "Match resumed",
        )
        _snapshot.value = current.copy(gameState = updated)
        broadcastLocked(ProtocolMessage.GameStateSync(updated))
    }

    suspend fun returnToLobby() = stateMutex.withLock {
        val current = _snapshot.value
        val lobbyPlayers = current.players.filter { it.isConnected }.map { it.copy(score = 0) }
        _snapshot.value = current.copy(
            players = lobbyPlayers,
            gameState = null,
        )
        broadcastLocked(ProtocolMessage.PlayerList(current.boardSize, lobbyPlayers))
    }

    suspend fun submitHostMove(hostId: String, line: LineId): MoveResult = stateMutex.withLock {
        return handleMoveLocked(hostId, line, senderHandler = null)
    }

    suspend fun stopServer() = stateMutex.withLock {
        stopServerInternal()
    }

    fun close() {
        runCatching {
            discoveryJob?.cancel()
            discoverySocket?.close()
            multicastLock?.let { if (it.isHeld) it.release() }
            serverSocket?.close()
            clientSessions.values.forEach { it.closeQuietly() }
            clientSessions.clear()
        }
        scope.cancel()
    }

    private fun stopServerInternal() {
        acceptJob?.cancel()
        heartbeatJob?.cancel()
        discoveryJob?.cancel()
        acceptJob = null
        heartbeatJob = null
        discoveryJob = null
        runCatching { discoverySocket?.close() }
        discoverySocket = null
        runCatching { multicastLock?.let { if (it.isHeld) it.release() } }
        multicastLock = null
        val handlers = clientSessions.values.toList()
        clientSessions.clear()
        handlers.forEach { handler ->
            runCatching {
                handler.sendAndClose(
                    ProtocolMessage.Disconnect(
                        playerId = handler.playerId ?: "",
                        reason = "Host closed the room.",
                    ),
                )
            }
        }
        runCatching { serverSocket?.close() }
        serverSocket = null
        _snapshot.value = HostRoomSnapshot()
    }

    private suspend fun runDiscoveryResponder() {
        runCatching {
            val wifiMgr = appContext?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = runCatching {
                wifiMgr?.createMulticastLock("dotnbox_host_discovery")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }.getOrNull()

            val udp = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                soTimeout = 1500
                bind(InetSocketAddress(NetworkProtocol.DISCOVERY_PORT))
            }
            discoverySocket = udp

            val buffer = ByteArray(1024)
            while (scope.isActive && !udp.isClosed) {
                val snap = _snapshot.value
                if (!snap.isRunning) break

                val packet = DatagramPacket(buffer, buffer.size)
                val received = try {
                    udp.receive(packet)
                    true
                } catch (_: Exception) {
                    false
                }

                if (received) {
                    val query = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                    if (query == NetworkProtocol.DISCOVERY_QUERY && snap.gameState == null) {
                        val payload = buildDiscoveryPayload(snap)
                        val reply = DatagramPacket(payload, payload.size, packet.address, packet.port)
                        runCatching { udp.send(reply) }
                    }
                } else if (snap.gameState == null && snap.players.size < MAX_PLAYERS) {
                    // Periodic broadcast beacon so nearby clients discover the room automatically
                    val payload = buildDiscoveryPayload(snap)
                    for (bcast in IpAddressProvider.detectBroadcastAddresses()) {
                        runCatching {
                            val addr = InetAddress.getByName(bcast)
                            udp.send(DatagramPacket(payload, payload.size, addr, NetworkProtocol.DISCOVERY_PORT))
                        }
                    }
                }
            }
        }
        runCatching { discoverySocket?.close() }
        discoverySocket = null
    }

    private fun buildDiscoveryPayload(snap: HostRoomSnapshot): ByteArray {
        val hostPlayer = snap.players.firstOrNull { it.isHost }
        val json = NetworkProtocol.encodeDiscoveryAnnouncement(
            hostId = hostPlayer?.id ?: "host",
            hostName = hostPlayer?.name ?: "Host",
            port = snap.port,
            boardSize = snap.boardSize,
            playerCount = snap.players.count { it.isConnected },
            maxPlayers = MAX_PLAYERS,
        )
        return json.toByteArray(Charsets.UTF_8)
    }

    private suspend fun acceptLoop(ss: ServerSocket) {
        while (scope.isActive && !ss.isClosed) {
            val socket = try {
                ss.accept()
            } catch (_: Exception) {
                break
            }
            scope.launch {
                handleNewSocket(socket)
            }
        }
    }

    private suspend fun handleNewSocket(socket: Socket) {
        runCatching {
            socket.tcpNoDelay = true
            socket.soTimeout = CLIENT_SO_TIMEOUT_MS
        }
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

        val handler = ClientHandler(socket, reader, writer)
        try {
            val firstLine = reader.readLine() ?: run {
                handler.closeQuietly()
                return
            }
            val joinMsg = NetworkProtocol.decode(firstLine) as? ProtocolMessage.Join
            if (joinMsg == null) {
                handler.writeImmediate(ProtocolMessage.Error("INVALID_HANDSHAKE", "Expected JOIN message."))
                handler.closeQuietly()
                return
            }

            val accepted = registerClientLocked(joinMsg, handler)
            if (!accepted) {
                handler.closeQuietly()
                return
            }

            // Read loop for authenticated client
            while (scope.isActive && !socket.isClosed) {
                val line = reader.readLine() ?: break
                val msg = NetworkProtocol.decode(line) ?: continue
                when (msg) {
                    is ProtocolMessage.Move -> {
                        val lineId = LineId(msg.lineType, msg.row, msg.col)
                        stateMutex.withLock {
                            val senderId = handler.playerId ?: msg.playerId
                            if (senderId != msg.playerId) {
                                handler.writeImmediate(ProtocolMessage.Error("UNAUTHORIZED", "Player ID mismatch."))
                            } else {
                                handleMoveLocked(senderId, lineId, handler)
                            }
                        }
                    }
                    is ProtocolMessage.Ping -> {
                        // Heartbeat acknowledged
                    }
                    is ProtocolMessage.Disconnect -> {
                        break
                    }
                    else -> Unit
                }
            }
        } catch (_: Exception) {
            // Connection dropped or timed out
        } finally {
            handler.playerId?.let { pid ->
                handleClientDisconnected(pid, handler)
            } ?: handler.closeQuietly()
        }
    }

    private suspend fun registerClientLocked(
        joinMsg: ProtocolMessage.Join,
        handler: ClientHandler,
    ): Boolean = stateMutex.withLock {
        val current = _snapshot.value
        val activeGame = current.gameState
        val cleanName = joinMsg.playerName.trim().take(18).ifEmpty { "Player" }

        // Check if this player is reconnecting (in lobby or active match)
        val existingInGame = activeGame?.players?.firstOrNull { it.id == joinMsg.playerId }
        if (activeGame != null && activeGame.phase != GamePhase.FINISHED) {
            if (existingInGame != null) {
                clientSessions.remove(joinMsg.playerId)?.closeQuietly()
                handler.playerId = joinMsg.playerId
                clientSessions[joinMsg.playerId] = handler

                val updatedGamePlayers = activeGame.players.map {
                    if (it.id == joinMsg.playerId) it.copy(isConnected = true, name = cleanName) else it
                }
                val updatedState = activeGame.copy(
                    players = updatedGamePlayers,
                    statusMessage = "$cleanName reconnected!",
                )
                _snapshot.value = current.copy(
                    players = updatedGamePlayers,
                    gameState = updatedState,
                )
                handler.writeImmediate(
                    ProtocolMessage.JoinAccepted(
                        playerId = joinMsg.playerId,
                        assignedColorIndex = existingInGame.colorIndex,
                        boardSize = updatedState.boardSize,
                        players = updatedGamePlayers,
                    ),
                )
                handler.writeImmediate(ProtocolMessage.GameStateSync(updatedState))
                broadcastLocked(ProtocolMessage.GameStateSync(updatedState), excludePlayerId = joinMsg.playerId)
                onSoundEvent?.invoke(HostGameEvent.PLAYER_JOINED)
                return true
            } else {
                handler.writeImmediate(
                    ProtocolMessage.Error("GAME_IN_PROGRESS", "A match is already in progress."),
                )
                return false
            }
        }

        // Lobby join or reconnect
        val existingLobbyPlayer = current.players.firstOrNull { it.id == joinMsg.playerId }
        if (existingLobbyPlayer != null) {
            clientSessions.remove(joinMsg.playerId)?.closeQuietly()
            handler.playerId = joinMsg.playerId
            clientSessions[joinMsg.playerId] = handler
            val updatedPlayers = current.players.map {
                if (it.id == joinMsg.playerId) it.copy(name = cleanName, isConnected = true) else it
            }
            _snapshot.value = current.copy(players = updatedPlayers)
            handler.writeImmediate(
                ProtocolMessage.JoinAccepted(
                    playerId = joinMsg.playerId,
                    assignedColorIndex = existingLobbyPlayer.colorIndex,
                    boardSize = current.boardSize,
                    players = updatedPlayers,
                ),
            )
            broadcastLocked(ProtocolMessage.PlayerList(current.boardSize, updatedPlayers))
            return true
        }

        if (current.players.size >= MAX_PLAYERS) {
            handler.writeImmediate(
                ProtocolMessage.Error("ROOM_FULL", "Room is full (maximum 4 players)."),
            )
            return false
        }

        val usedColors = current.players.map { it.colorIndex }.toSet()
        val nextColor = (0 until MAX_PLAYERS).firstOrNull { it !in usedColors } ?: current.players.size
        val newPlayer = PlayerInfo(
            id = joinMsg.playerId,
            name = cleanName,
            colorIndex = nextColor,
            isHost = false,
            isAi = false,
            isConnected = true,
            score = 0,
        )
        val updatedPlayers = current.players + newPlayer
        handler.playerId = joinMsg.playerId
        clientSessions[joinMsg.playerId] = handler
        _snapshot.value = current.copy(players = updatedPlayers)

        handler.writeImmediate(
            ProtocolMessage.JoinAccepted(
                playerId = joinMsg.playerId,
                assignedColorIndex = nextColor,
                boardSize = current.boardSize,
                players = updatedPlayers,
            ),
        )
        broadcastLocked(ProtocolMessage.PlayerList(current.boardSize, updatedPlayers))
        onSoundEvent?.invoke(HostGameEvent.PLAYER_JOINED)
        return true
    }

    private suspend fun handleClientDisconnected(playerId: String, expectedHandler: ClientHandler) {
        stateMutex.withLock {
            val currentHandler = clientSessions[playerId]
            if (currentHandler !== expectedHandler) {
                expectedHandler.closeQuietly()
                return
            }
            clientSessions.remove(playerId)
            expectedHandler.closeQuietly()

            val current = _snapshot.value
            val gs = current.gameState
            if (gs == null || gs.phase == GamePhase.FINISHED) {
                val updatedPlayers = current.players.filterNot { it.id == playerId }
                _snapshot.value = current.copy(players = updatedPlayers)
                broadcastLocked(ProtocolMessage.PlayerList(current.boardSize, updatedPlayers))
            } else {
                val leavingPlayer = gs.players.firstOrNull { it.id == playerId } ?: return
                val updatedPlayers = gs.players.map {
                    if (it.id == playerId) it.copy(isConnected = false) else it
                }
                val connectedCount = updatedPlayers.count { it.isConnected }
                val currentTurnPlayer = updatedPlayers.getOrNull(gs.currentTurnIndex)
                val nextTurnIndex = if (currentTurnPlayer?.id == playerId && connectedCount >= 2) {
                    DotsAndBoxesEngine.findNextConnectedPlayerIndex(updatedPlayers, gs.currentTurnIndex)
                } else {
                    gs.currentTurnIndex
                }
                val statusMsg = if (connectedCount < 2) {
                    "${leavingPlayer.name} disconnected. Waiting for reconnect..."
                } else {
                    "${leavingPlayer.name} disconnected."
                }
                val updatedState = gs.copy(
                    players = updatedPlayers,
                    currentTurnIndex = nextTurnIndex,
                    bonusTurnAwarded = false,
                    statusMessage = statusMsg,
                )
                _snapshot.value = current.copy(
                    players = updatedPlayers,
                    gameState = updatedState,
                )
                broadcastLocked(ProtocolMessage.GameStateSync(updatedState))
            }
            onSoundEvent?.invoke(HostGameEvent.PLAYER_LEFT)
        }
    }

    private fun handleMoveLocked(
        playerId: String,
        line: LineId,
        senderHandler: ClientHandler?,
    ): MoveResult {
        val current = _snapshot.value
        val gs = current.gameState
            ?: return MoveResult.Invalid("NO_GAME", "No active match.").also {
                senderHandler?.writeImmediate(ProtocolMessage.Error(it.code, it.reason))
            }

        val result = DotsAndBoxesEngine.applyMove(gs, playerId, line)
        when (result) {
            is MoveResult.Invalid -> {
                senderHandler?.writeImmediate(ProtocolMessage.Error(result.code, result.reason))
            }
            is MoveResult.Success -> {
                _snapshot.value = current.copy(
                    players = result.newState.players,
                    gameState = result.newState,
                    lastError = null,
                )
                broadcastLocked(ProtocolMessage.GameStateSync(result.newState))
                when {
                    result.gameFinished -> onSoundEvent?.invoke(HostGameEvent.GAME_FINISHED)
                    result.grantedBonusTurn -> onSoundEvent?.invoke(HostGameEvent.BONUS_TURN)
                    result.claimedCount > 0 -> onSoundEvent?.invoke(HostGameEvent.BOX_CLAIMED)
                    else -> onSoundEvent?.invoke(HostGameEvent.LINE_DRAWN)
                }
            }
        }
        return result
    }

    private suspend fun heartbeatLoop() {
        while (scope.isActive) {
            delay(HEARTBEAT_INTERVAL_MS)
            val ping = ProtocolMessage.Ping(System.currentTimeMillis())
            val deadIds = mutableListOf<String>()
            for ((id, handler) in clientSessions) {
                val ok = handler.writeImmediate(ping)
                if (!ok) {
                    deadIds.add(id)
                }
            }
            for (id in deadIds) {
                clientSessions[id]?.let { handleClientDisconnected(id, it) }
            }
        }
    }

    private fun broadcastLocked(message: ProtocolMessage, excludePlayerId: String? = null) {
        for ((id, handler) in clientSessions) {
            if (id != excludePlayerId) {
                handler.writeImmediate(message)
            }
        }
    }

    private class ClientHandler(
        private val socket: Socket,
        private val reader: BufferedReader,
        private val writer: BufferedWriter,
    ) {
        @Volatile
        var playerId: String? = null

        private val writeLock = Any()

        fun writeImmediate(message: ProtocolMessage): Boolean {
            return synchronized(writeLock) {
                runCatching {
                    writer.write(NetworkProtocol.encode(message))
                    writer.write("\n")
                    writer.flush()
                    true
                }.getOrElse { false }
            }
        }

        fun sendAndClose(message: ProtocolMessage) {
            writeImmediate(message)
            closeQuietly()
        }

        fun closeQuietly() {
            runCatching { reader.close() }
            runCatching { writer.close() }
            runCatching { socket.close() }
        }
    }
}
