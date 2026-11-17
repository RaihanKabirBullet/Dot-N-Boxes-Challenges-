package com.example.game

import com.example.model.BoardSize
import com.example.model.GamePhase
import com.example.model.GameState
import com.example.model.LastMoveInfo
import com.example.model.LineId
import com.example.model.LineType
import com.example.model.PlayerInfo

sealed interface MoveResult {
    data class Success(
        val newState: GameState,
        val claimedCount: Int,
        val grantedBonusTurn: Boolean,
        val gameFinished: Boolean,
    ) : MoveResult

    data class Invalid(val code: String, val reason: String) : MoveResult
}

object DotsAndBoxesEngine {

    fun createInitialState(boardSize: BoardSize, players: List<PlayerInfo>): GameState {
        val dots = boardSize.dots
        val boxDim = boardSize.boxesPerSide
        val cleanPlayers = players.map { it.copy(score = 0) }
        return GameState(
            boardSize = boardSize,
            players = cleanPlayers,
            horizontalLines = List(dots * boxDim) { -1 },
            verticalLines = List(boxDim * dots) { -1 },
            boxes = List(boxDim * boxDim) { -1 },
            currentTurnIndex = 0,
            bonusTurnAwarded = false,
            lastMove = null,
            lastClaimedBoxIndices = emptyList(),
            phase = GamePhase.IN_PROGRESS,
            moveSequence = 0L,
            statusMessage = null,
        )
    }

    fun isLineWithinBounds(boardSize: BoardSize, line: LineId): Boolean {
        val dots = boardSize.dots
        val boxDim = boardSize.boxesPerSide
        return when (line.type) {
            LineType.HORIZONTAL -> line.row in 0 until dots && line.col in 0 until boxDim
            LineType.VERTICAL -> line.row in 0 until boxDim && line.col in 0 until dots
        }
    }

    fun isLineDrawn(state: GameState, line: LineId): Boolean {
        if (!isLineWithinBounds(state.boardSize, line)) return false
        return getLineOwner(state, line) >= 0
    }

    fun getLineOwner(state: GameState, line: LineId): Int {
        val dots = state.boardSize.dots
        val boxDim = state.boardSize.boxesPerSide
        return when (line.type) {
            LineType.HORIZONTAL -> state.horizontalLines[line.row * boxDim + line.col]
            LineType.VERTICAL -> state.verticalLines[line.row * dots + line.col]
        }
    }

    fun applyMove(state: GameState, playerId: String, line: LineId): MoveResult {
        if (state.phase != GamePhase.IN_PROGRESS) {
            return MoveResult.Invalid("GAME_NOT_ACTIVE", "Match is not currently active.")
        }
        val currentPlayer = state.players.getOrNull(state.currentTurnIndex)
            ?: return MoveResult.Invalid("NO_PLAYER", "No active player found.")

        if (currentPlayer.id != playerId) {
            return MoveResult.Invalid("NOT_YOUR_TURN", "Wait for ${currentPlayer.name}'s turn.")
        }

        if (!isLineWithinBounds(state.boardSize, line)) {
            return MoveResult.Invalid("OUT_OF_BOUNDS", "Line coordinates are outside the board.")
        }

        if (isLineDrawn(state, line)) {
            return MoveResult.Invalid("DUPLICATE_LINE", "That line is already drawn.")
        }

        val dots = state.boardSize.dots
        val boxDim = state.boardSize.boxesPerSide
        val playerIndex = state.currentTurnIndex

        val nextHorizontal = state.horizontalLines.toMutableList()
        val nextVertical = state.verticalLines.toMutableList()
        val nextBoxes = state.boxes.toMutableList()

        when (line.type) {
            LineType.HORIZONTAL -> nextHorizontal[line.row * boxDim + line.col] = playerIndex
            LineType.VERTICAL -> nextVertical[line.row * dots + line.col] = playerIndex
        }

        val candidateBoxes = adjacentBoxCoords(line, boxDim)
        val newlyClaimedIndices = mutableListOf<Int>()

        for ((br, bc) in candidateBoxes) {
            val boxIndex = br * boxDim + bc
            if (nextBoxes[boxIndex] == -1 && isBoxComplete(br, bc, dots, boxDim, nextHorizontal, nextVertical)) {
                nextBoxes[boxIndex] = playerIndex
                newlyClaimedIndices.add(boxIndex)
            }
        }

        val claimedCount = newlyClaimedIndices.size
        val updatedPlayers = state.players.mapIndexed { idx, info ->
            if (idx == playerIndex) info.copy(score = info.score + claimedCount) else info
        }

        val allBoxesClaimed = nextBoxes.none { it == -1 }
        val nextPhase = if (allBoxesClaimed) GamePhase.FINISHED else GamePhase.IN_PROGRESS

        val grantedBonus = claimedCount > 0 && !allBoxesClaimed
        val nextTurnIndex = if (allBoxesClaimed || grantedBonus) {
            playerIndex
        } else {
            findNextConnectedPlayerIndex(updatedPlayers, playerIndex)
        }

        val nextState = state.copy(
            players = updatedPlayers,
            horizontalLines = nextHorizontal,
            verticalLines = nextVertical,
            boxes = nextBoxes,
            currentTurnIndex = nextTurnIndex,
            bonusTurnAwarded = grantedBonus,
            lastMove = LastMoveInfo(
                lineType = line.type,
                row = line.row,
                col = line.col,
                playerId = playerId,
            ),
            lastClaimedBoxIndices = newlyClaimedIndices,
            phase = nextPhase,
            moveSequence = state.moveSequence + 1L,
            statusMessage = when {
                allBoxesClaimed -> "Match complete!"
                grantedBonus -> "${updatedPlayers[playerIndex].name} claimed $claimedCount box${if (claimedCount > 1) "es" else ""}! Bonus turn!"
                else -> null
            },
        )

        return MoveResult.Success(
            newState = nextState,
            claimedCount = claimedCount,
            grantedBonusTurn = grantedBonus,
            gameFinished = allBoxesClaimed,
        )
    }

    fun availableLines(state: GameState): List<LineId> {
        val dots = state.boardSize.dots
        val boxDim = state.boardSize.boxesPerSide
        val result = ArrayList<LineId>(dots * boxDim * 2)
        for (r in 0 until dots) {
            for (c in 0 until boxDim) {
                if (state.horizontalLines[r * boxDim + c] == -1) {
                    result.add(LineId(LineType.HORIZONTAL, r, c))
                }
            }
        }
        for (r in 0 until boxDim) {
            for (c in 0 until dots) {
                if (state.verticalLines[r * dots + c] == -1) {
                    result.add(LineId(LineType.VERTICAL, r, c))
                }
            }
        }
        return result
    }

    fun countDrawnSidesForBox(
        br: Int,
        bc: Int,
        dots: Int,
        boxDim: Int,
        horizontal: List<Int>,
        vertical: List<Int>,
    ): Int {
        var count = 0
        if (horizontal[br * boxDim + bc] >= 0) count++
        if (horizontal[(br + 1) * boxDim + bc] >= 0) count++
        if (vertical[br * dots + bc] >= 0) count++
        if (vertical[br * dots + (bc + 1)] >= 0) count++
        return count
    }

    fun adjacentBoxCoords(line: LineId, boxDim: Int): List<Pair<Int, Int>> {
        val list = ArrayList<Pair<Int, Int>>(2)
        when (line.type) {
            LineType.HORIZONTAL -> {
                if (line.row > 0) list.add((line.row - 1) to line.col)
                if (line.row < boxDim) list.add(line.row to line.col)
            }
            LineType.VERTICAL -> {
                if (line.col > 0) list.add(line.row to (line.col - 1))
                if (line.col < boxDim) list.add(line.row to line.col)
            }
        }
        return list
    }

    fun findNextConnectedPlayerIndex(players: List<PlayerInfo>, fromIndex: Int): Int {
        if (players.isEmpty()) return 0
        val count = players.size
        for (offset in 1..count) {
            val candidate = (fromIndex + offset) % count
            if (players[candidate].isConnected) {
                return candidate
            }
        }
        return (fromIndex + 1) % count
    }

    private fun isBoxComplete(
        br: Int,
        bc: Int,
        dots: Int,
        boxDim: Int,
        horizontal: List<Int>,
        vertical: List<Int>,
    ): Boolean {
        return countDrawnSidesForBox(br, bc, dots, boxDim, horizontal, vertical) == 4
    }
}
