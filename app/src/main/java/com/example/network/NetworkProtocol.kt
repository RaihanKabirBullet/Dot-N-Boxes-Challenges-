package com.example.network

import com.example.model.BoardSize
import com.example.model.GamePhase
import com.example.model.GameState
import com.example.model.LastMoveInfo
import com.example.model.LineType
import com.example.model.PlayerInfo
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okio.Buffer

sealed interface ProtocolMessage {
    data class Join(
        val playerId: String,
        val playerName: String,
    ) : ProtocolMessage

    data class JoinAccepted(
        val playerId: String,
        val assignedColorIndex: Int,
        val boardSize: BoardSize,
        val players: List<PlayerInfo>,
    ) : ProtocolMessage

    data class PlayerList(
        val boardSize: BoardSize,
        val players: List<PlayerInfo>,
    ) : ProtocolMessage

    data class GameStart(
        val gameState: GameState,
    ) : ProtocolMessage

    data class Move(
        val playerId: String,
        val lineType: LineType,
        val row: Int,
        val col: Int,
    ) : ProtocolMessage

    data class GameStateSync(
        val gameState: GameState,
    ) : ProtocolMessage

    data class Error(
        val code: String,
        val message: String,
    ) : ProtocolMessage

    data class Ping(
        val timestamp: Long,
    ) : ProtocolMessage

    data class Disconnect(
        val playerId: String,
        val reason: String,
    ) : ProtocolMessage
}

data class DiscoveredRoom(
    val hostId: String,
    val hostName: String,
    val hostIp: String,
    val port: Int,
    val boardSize: BoardSize,
    val playerCount: Int,
    val maxPlayers: Int,
    val lastSeenMs: Long = System.currentTimeMillis(),
)

object NetworkProtocol {

    const val DEFAULT_PORT = 45678
    const val DISCOVERY_PORT = 45679
    const val DISCOVERY_QUERY = "DOTNBOX_DISCOVER_V1"

    fun encode(message: ProtocolMessage): String {
        val buffer = Buffer()
        val writer = JsonWriter.of(buffer)
        writer.beginObject()
        when (message) {
            is ProtocolMessage.Join -> {
                writer.name("type").value("JOIN")
                writer.name("playerId").value(message.playerId)
                writer.name("playerName").value(message.playerName)
            }
            is ProtocolMessage.JoinAccepted -> {
                writer.name("type").value("JOIN_ACCEPTED")
                writer.name("playerId").value(message.playerId)
                writer.name("assignedColorIndex").value(message.assignedColorIndex)
                writer.name("boardSize").value(message.boardSize.name)
                writer.name("players")
                writePlayers(writer, message.players)
            }
            is ProtocolMessage.PlayerList -> {
                writer.name("type").value("PLAYER_LIST")
                writer.name("boardSize").value(message.boardSize.name)
                writer.name("players")
                writePlayers(writer, message.players)
            }
            is ProtocolMessage.GameStart -> {
                writer.name("type").value("GAME_START")
                writer.name("gameState")
                writeGameState(writer, message.gameState)
            }
            is ProtocolMessage.Move -> {
                writer.name("type").value("MOVE")
                writer.name("playerId").value(message.playerId)
                writer.name("lineType").value(message.lineType.name)
                writer.name("row").value(message.row)
                writer.name("col").value(message.col)
            }
            is ProtocolMessage.GameStateSync -> {
                writer.name("type").value("GAME_STATE")
                writer.name("gameState")
                writeGameState(writer, message.gameState)
            }
            is ProtocolMessage.Error -> {
                writer.name("type").value("ERROR")
                writer.name("code").value(message.code)
                writer.name("message").value(message.message)
            }
            is ProtocolMessage.Ping -> {
                writer.name("type").value("PING")
                writer.name("timestamp").value(message.timestamp)
            }
            is ProtocolMessage.Disconnect -> {
                writer.name("type").value("DISCONNECT")
                writer.name("playerId").value(message.playerId)
                writer.name("reason").value(message.reason)
            }
        }
        writer.endObject()
        writer.flush()
        return buffer.readUtf8()
    }

    fun decode(rawLine: String): ProtocolMessage? {
        if (rawLine.isBlank()) return null
        return runCatching {
            val buffer = Buffer().writeUtf8(rawLine.trim())
            val reader = JsonReader.of(buffer)
            reader.beginObject()
            var type: String? = null
            var playerId: String? = null
            var playerName: String? = null
            var assignedColorIndex = 0
            var boardSize: BoardSize? = null
            var players: List<PlayerInfo>? = null
            var gameState: GameState? = null
            var lineType: LineType? = null
            var row = -1
            var col = -1
            var code: String? = null
            var message: String? = null
            var timestamp = 0L
            var reason: String? = null

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "type" -> type = reader.nextStringOrNull()
                    "playerId" -> playerId = reader.nextStringOrNull()
                    "playerName" -> playerName = reader.nextStringOrNull()
                    "assignedColorIndex" -> assignedColorIndex = reader.nextInt()
                    "boardSize" -> boardSize = BoardSize.fromNameOrDefault(reader.nextStringOrNull())
                    "players" -> players = readPlayers(reader)
                    "gameState" -> gameState = readGameState(reader)
                    "lineType" -> lineType = LineType.fromStringOrNull(reader.nextStringOrNull())
                    "row" -> row = reader.nextInt()
                    "col" -> col = reader.nextInt()
                    "code" -> code = reader.nextStringOrNull()
                    "message" -> message = reader.nextStringOrNull()
                    "timestamp" -> timestamp = reader.nextLong()
                    "reason" -> reason = reader.nextStringOrNull()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            when (type) {
                "JOIN" -> ProtocolMessage.Join(
                    playerId = playerId ?: return@runCatching null,
                    playerName = playerName ?: "Player",
                )
                "JOIN_ACCEPTED" -> ProtocolMessage.JoinAccepted(
                    playerId = playerId ?: return@runCatching null,
                    assignedColorIndex = assignedColorIndex,
                    boardSize = boardSize ?: BoardSize.SMALL,
                    players = players ?: emptyList(),
                )
                "PLAYER_LIST" -> ProtocolMessage.PlayerList(
                    boardSize = boardSize ?: BoardSize.SMALL,
                    players = players ?: emptyList(),
                )
                "GAME_START" -> ProtocolMessage.GameStart(
                    gameState = gameState ?: return@runCatching null,
                )
                "MOVE" -> ProtocolMessage.Move(
                    playerId = playerId ?: return@runCatching null,
                    lineType = lineType ?: return@runCatching null,
                    row = row,
                    col = col,
                )
                "GAME_STATE" -> ProtocolMessage.GameStateSync(
                    gameState = gameState ?: return@runCatching null,
                )
                "ERROR" -> ProtocolMessage.Error(
                    code = code ?: "UNKNOWN",
                    message = message ?: "Unknown error",
                )
                "PING" -> ProtocolMessage.Ping(timestamp = timestamp)
                "DISCONNECT" -> ProtocolMessage.Disconnect(
                    playerId = playerId ?: "",
                    reason = reason ?: "Disconnected",
                )
                else -> null
            }
        }.getOrNull()
    }

    private fun writePlayers(writer: JsonWriter, players: List<PlayerInfo>) {
        writer.beginArray()
        for (p in players) {
            writer.beginObject()
            writer.name("id").value(p.id)
            writer.name("name").value(p.name)
            writer.name("colorIndex").value(p.colorIndex)
            writer.name("isHost").value(p.isHost)
            writer.name("isAi").value(p.isAi)
            writer.name("isConnected").value(p.isConnected)
            writer.name("score").value(p.score)
            writer.endObject()
        }
        writer.endArray()
    }

    private fun readPlayers(reader: JsonReader): List<PlayerInfo> {
        val list = mutableListOf<PlayerInfo>()
        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginObject()
            var id = ""
            var name = "Player"
            var colorIndex = 0
            var isHost = false
            var isAi = false
            var isConnected = true
            var score = 0
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> id = reader.nextStringOrNull() ?: ""
                    "name" -> name = reader.nextStringOrNull() ?: "Player"
                    "colorIndex" -> colorIndex = reader.nextInt()
                    "isHost" -> isHost = reader.nextBoolean()
                    "isAi" -> isAi = reader.nextBoolean()
                    "isConnected" -> isConnected = reader.nextBoolean()
                    "score" -> score = reader.nextInt()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            list.add(
                PlayerInfo(
                    id = id,
                    name = name,
                    colorIndex = colorIndex,
                    isHost = isHost,
                    isAi = isAi,
                    isConnected = isConnected,
                    score = score,
                ),
            )
        }
        reader.endArray()
        return list
    }

    private fun writeGameState(writer: JsonWriter, state: GameState) {
        writer.beginObject()
        writer.name("boardSize").value(state.boardSize.name)
        writer.name("players")
        writePlayers(writer, state.players)
        writer.name("horizontalLines")
        writeIntList(writer, state.horizontalLines)
        writer.name("verticalLines")
        writeIntList(writer, state.verticalLines)
        writer.name("boxes")
        writeIntList(writer, state.boxes)
        writer.name("currentTurnIndex").value(state.currentTurnIndex)
        writer.name("bonusTurnAwarded").value(state.bonusTurnAwarded)
        writer.name("lastMove")
        val lm = state.lastMove
        if (lm != null) {
            writer.beginObject()
            writer.name("lineType").value(lm.lineType.name)
            writer.name("row").value(lm.row)
            writer.name("col").value(lm.col)
            writer.name("playerId").value(lm.playerId)
            writer.endObject()
        } else {
            writer.nullValue()
        }
        writer.name("lastClaimedBoxIndices")
        writeIntList(writer, state.lastClaimedBoxIndices)
        writer.name("phase").value(state.phase.name)
        writer.name("moveSequence").value(state.moveSequence)
        writer.name("statusMessage")
        if (state.statusMessage != null) writer.value(state.statusMessage) else writer.nullValue()
        writer.endObject()
    }

    private fun readGameState(reader: JsonReader): GameState {
        reader.beginObject()
        var boardSize = BoardSize.SMALL
        var players = emptyList<PlayerInfo>()
        var horizontal = emptyList<Int>()
        var vertical = emptyList<Int>()
        var boxes = emptyList<Int>()
        var currentTurnIndex = 0
        var bonusTurnAwarded = false
        var lastMove: LastMoveInfo? = null
        var lastClaimedBoxIndices = emptyList<Int>()
        var phase = GamePhase.IN_PROGRESS
        var moveSequence = 0L
        var statusMessage: String? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "boardSize" -> boardSize = BoardSize.fromNameOrDefault(reader.nextStringOrNull())
                "players" -> players = readPlayers(reader)
                "horizontalLines" -> horizontal = readIntList(reader)
                "verticalLines" -> vertical = readIntList(reader)
                "boxes" -> boxes = readIntList(reader)
                "currentTurnIndex" -> currentTurnIndex = reader.nextInt()
                "bonusTurnAwarded" -> bonusTurnAwarded = reader.nextBoolean()
                "lastMove" -> lastMove = readLastMove(reader)
                "lastClaimedBoxIndices" -> lastClaimedBoxIndices = readIntList(reader)
                "phase" -> phase = GamePhase.fromStringOrDefault(reader.nextStringOrNull())
                "moveSequence" -> moveSequence = reader.nextLong()
                "statusMessage" -> statusMessage = reader.nextStringOrNull()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return GameState(
            boardSize = boardSize,
            players = players,
            horizontalLines = horizontal,
            verticalLines = vertical,
            boxes = boxes,
            currentTurnIndex = currentTurnIndex,
            bonusTurnAwarded = bonusTurnAwarded,
            lastMove = lastMove,
            lastClaimedBoxIndices = lastClaimedBoxIndices,
            phase = phase,
            moveSequence = moveSequence,
            statusMessage = statusMessage,
        )
    }

    private fun readLastMove(reader: JsonReader): LastMoveInfo? {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Unit>()
            return null
        }
        reader.beginObject()
        var lineType: LineType? = null
        var row = -1
        var col = -1
        var playerId: String? = null
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "lineType" -> lineType = LineType.fromStringOrNull(reader.nextStringOrNull())
                "row" -> row = reader.nextInt()
                "col" -> col = reader.nextInt()
                "playerId" -> playerId = reader.nextStringOrNull()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val resolvedType = lineType ?: return null
        val resolvedPlayer = playerId ?: return null
        if (row < 0 || col < 0) return null
        return LastMoveInfo(
            lineType = resolvedType,
            row = row,
            col = col,
            playerId = resolvedPlayer,
        )
    }

    private fun writeIntList(writer: JsonWriter, values: List<Int>) {
        writer.beginArray()
        for (v in values) {
            writer.value(v)
        }
        writer.endArray()
    }

    private fun readIntList(reader: JsonReader): List<Int> {
        val list = mutableListOf<Int>()
        reader.beginArray()
        while (reader.hasNext()) {
            list.add(reader.nextInt())
        }
        reader.endArray()
        return list
    }

    fun encodeDiscoveryAnnouncement(
        hostId: String,
        hostName: String,
        port: Int,
        boardSize: BoardSize,
        playerCount: Int,
        maxPlayers: Int,
    ): String {
        val buffer = Buffer()
        val writer = JsonWriter.of(buffer)
        writer.beginObject()
        writer.name("type").value("ROOM_ANNOUNCE")
        writer.name("hostId").value(hostId)
        writer.name("hostName").value(hostName)
        writer.name("port").value(port)
        writer.name("boardSize").value(boardSize.name)
        writer.name("playerCount").value(playerCount)
        writer.name("maxPlayers").value(maxPlayers)
        writer.endObject()
        writer.flush()
        return buffer.readUtf8()
    }

    fun decodeDiscoveryAnnouncement(raw: String, sourceIp: String): DiscoveredRoom? {
        if (raw.isBlank()) return null
        return runCatching {
            val buffer = Buffer().writeUtf8(raw.trim())
            val reader = JsonReader.of(buffer)
            reader.beginObject()
            var type: String? = null
            var hostId: String? = null
            var hostName = "Host"
            var port = DEFAULT_PORT
            var boardSize = BoardSize.SMALL
            var playerCount = 1
            var maxPlayers = 4
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "type" -> type = reader.nextStringOrNull()
                    "hostId" -> hostId = reader.nextStringOrNull()
                    "hostName" -> hostName = reader.nextStringOrNull() ?: "Host"
                    "port" -> port = reader.nextInt()
                    "boardSize" -> boardSize = BoardSize.fromNameOrDefault(reader.nextStringOrNull())
                    "playerCount" -> playerCount = reader.nextInt()
                    "maxPlayers" -> maxPlayers = reader.nextInt()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            if (type != "ROOM_ANNOUNCE" || hostId.isNullOrBlank()) return@runCatching null
            DiscoveredRoom(
                hostId = hostId,
                hostName = hostName,
                hostIp = sourceIp,
                port = port,
                boardSize = boardSize,
                playerCount = playerCount,
                maxPlayers = maxPlayers,
            )
        }.getOrNull()
    }

    private fun JsonReader.nextStringOrNull(): String? {
        return if (peek() == JsonReader.Token.NULL) {
            nextNull<Unit>()
            null
        } else {
            nextString()
        }
    }
}
