package com.example.model

import kotlin.math.abs

data class DotCoord(
    val row: Int,
    val col: Int,
)

enum class LineType {
    HORIZONTAL,
    VERTICAL;

    companion object {
        fun fromStringOrNull(value: String?): LineType? {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}

data class LineId(
    val type: LineType,
    val row: Int,
    val col: Int,
) {
    val key: String get() = "${type.name}:$row,$col"

    companion object {
        fun fromDots(a: DotCoord, b: DotCoord): LineId? {
            val dr = abs(a.row - b.row)
            val dc = abs(a.col - b.col)
            if (dr + dc != 1) return null
            return if (a.row == b.row) {
                LineId(LineType.HORIZONTAL, a.row, minOf(a.col, b.col))
            } else {
                LineId(LineType.VERTICAL, minOf(a.row, b.row), a.col)
            }
        }
    }
}

data class PlayerInfo(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val isHost: Boolean = false,
    val isAi: Boolean = false,
    val isConnected: Boolean = true,
    val score: Int = 0,
) {
    val color: PlayerColor get() = PlayerColor.fromIndex(colorIndex)
    val initial: String get() = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P"
}

enum class GameMode {
    PRACTICE_AI,
    HOST_MULTIPLAYER,
    CLIENT_MULTIPLAYER,
}

enum class GamePhase {
    WAITING,
    IN_PROGRESS,
    PAUSED,
    FINISHED;

    companion object {
        fun fromStringOrDefault(value: String?): GamePhase {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: IN_PROGRESS
        }
    }
}

data class LastMoveInfo(
    val lineType: LineType,
    val row: Int,
    val col: Int,
    val playerId: String,
) {
    val lineKey: String get() = "${lineType.name}:$row,$col"
}

data class GameState(
    val boardSize: BoardSize,
    val players: List<PlayerInfo>,
    val horizontalLines: List<Int>,
    val verticalLines: List<Int>,
    val boxes: List<Int>,
    val currentTurnIndex: Int = 0,
    val bonusTurnAwarded: Boolean = false,
    val lastMove: LastMoveInfo? = null,
    val lastClaimedBoxIndices: List<Int> = emptyList(),
    val phase: GamePhase = GamePhase.IN_PROGRESS,
    val moveSequence: Long = 0L,
    val statusMessage: String? = null,
) {
    val lastMoveLineKey: String?
        get() = lastMove?.lineKey
    val currentPlayer: PlayerInfo?
        get() = players.getOrNull(currentTurnIndex)

    val claimedBoxesCount: Int
        get() = boxes.count { it >= 0 }

    val remainingBoxesCount: Int
        get() = boxes.size - claimedBoxesCount

    val isGameOver: Boolean
        get() = phase == GamePhase.FINISHED || remainingBoxesCount == 0

    val rankedPlayers: List<PlayerInfo>
        get() = players.sortedByDescending { it.score }

    val winners: List<PlayerInfo>
        get() {
            val maxScore = players.maxOfOrNull { it.score } ?: return emptyList()
            return players.filter { it.score == maxScore }
        }
}
